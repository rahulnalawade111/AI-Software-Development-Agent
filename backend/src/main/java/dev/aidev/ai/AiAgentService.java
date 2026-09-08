package dev.aidev.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.aidev.ai.llm.LlmChatResult;
import dev.aidev.ai.llm.LlmClient;
import dev.aidev.ai.llm.LlmException;
import dev.aidev.ai.llm.LlmMessage;
import dev.aidev.ai.llm.LlmToolCall;
import dev.aidev.ai.llm.LlmToolSpec;
import dev.aidev.ai.tools.AiTool;
import dev.aidev.ai.tools.ToolContext;
import dev.aidev.ai.tools.ToolRegistry;
import dev.aidev.common.ForbiddenException;
import dev.aidev.common.NotFoundException;
import dev.aidev.project.Project;
import dev.aidev.project.ProjectService;
import dev.aidev.security.AppPrincipal;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Core agent loop: OBSERVE → PLAN → TOOL CALL → EXECUTE → OBSERVE RESULT →
 * ANALYZE → FIX → TEST → FINAL RESPONSE.
 *
 * Each user chat message starts an agent turn on a worker thread. The turn
 * replays the persisted conversation + project memory + tool definitions to
 * the LLM, executes requested tool calls sequentially (pausing for approval
 * on destructive tools), appends results as role=tool messages and loops
 * until the model produces a final assistant message with no tool calls, or
 * a safety limit is hit. Every step is persisted (ai_messages / ai_actions)
 * and streamed live to SSE subscribers via the AgentEventBus.
 */
@Service
public class AiAgentService {

    private static final Logger log = LoggerFactory.getLogger(AiAgentService.class);

    private static final int MAX_TURNS = 48;
    private static final int MAX_TOOL_CALLS = 60;
    private static final long WALL_CLOCK_SECONDS = 15 * 60;
    private static final long APPROVAL_TIMEOUT_SECONDS = 600;
    private static final int MAX_TOOL_RESULT_CHARS = 12_000;

    private final LlmClient llm;
    private final ToolRegistry toolRegistry;
    private final AgentEventBus eventBus;
    private final ProjectService projectService;
    private final AiConversationRepository conversations;
    private final AiMessageRepository messages;
    private final AiActionRepository actions;
    private final ObjectMapper mapper = new ObjectMapper();

    private final ExecutorService executor = Executors.newCachedThreadPool();

    /** Pending approvals: actionId → future completed with true (approve) / false (reject). */
    private final Map<Long, CompletableFuture<Boolean>> pendingApprovals = new ConcurrentHashMap<>();
    /** Running sessions: conversationId → stop flag. */
    private final Map<Long, AtomicBoolean> runningSessions = new ConcurrentHashMap<>();
    /** Serializes agent turns per conversation. */
    private final Map<Long, ReentrantLock> conversationLocks = new ConcurrentHashMap<>();

    public AiAgentService(LlmClient llm,
                          ToolRegistry toolRegistry,
                          AgentEventBus eventBus,
                          ProjectService projectService,
                          AiConversationRepository conversations,
                          AiMessageRepository messages,
                          AiActionRepository actions) {
        this.llm = llm;
        this.toolRegistry = toolRegistry;
        this.eventBus = eventBus;
        this.projectService = projectService;
        this.conversations = conversations;
        this.messages = messages;
        this.actions = actions;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    // ------------------------------------------------------------------
    // Access & conversations
    // ------------------------------------------------------------------

    public void requireAccess(Long projectId, AppPrincipal principal) {
        projectService.requireWrite(projectId, principal);
    }

    public AiConversation createConversation(Long projectId, AppPrincipal principal, String title) {
        requireAccess(projectId, principal);
        projectService.requireProject(projectId);
        AiConversation c = new AiConversation();
        c.setProjectId(projectId);
        c.setUserId(principal.userId());
        c.setTitle(title == null || title.isBlank() ? "New chat" : title);
        c.setState("IDLE");
        return conversations.save(c);
    }

    public List<Map<String, Object>> recentActions(Long projectId) {
        return actions.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .limit(200)
                .map(this::actionDto)
                .toList();
    }

    // ------------------------------------------------------------------
    // Agent turn
    // ------------------------------------------------------------------

    public SseEmitter startAgentTurn(Long projectId, Long conversationId, AppPrincipal principal, String message) {
        requireAccess(projectId, principal);
        AiConversation conversation = conversations.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found: " + conversationId));
        if (!Objects.equals(conversation.getProjectId(), projectId)) {
            throw new ForbiddenException("Conversation does not belong to project " + projectId);
        }

        SseEmitter emitter = new SseEmitter(0L); // no timeout — agent turns can be long
        Long userId = principal.userId();

        executor.submit(() -> {
            try {
                runAgentTurn(projectId, conversationId, userId, message, emitter);
                completeQuietly(emitter);
            } catch (Throwable t) {
                log.error("Agent turn failed for conversation {}", conversationId, t);
                trySend(emitter, "error", Map.of("message", String.valueOf(t.getMessage())));
                completeQuietly(emitter);
            }
        });
        return emitter;
    }

    private void runAgentTurn(Long projectId, Long conversationId, Long userId, String userMessage,
                              SseEmitter emitter) {
        ReentrantLock lock = conversationLocks.computeIfAbsent(conversationId, k -> new ReentrantLock());
        lock.lock();
        AtomicBoolean stopped = new AtomicBoolean(false);
        try {
            runningSessions.put(conversationId, stopped);

            setState(conversationId, "RUNNING", emitter);

            AiMessage userMsg = new AiMessage();
            userMsg.setConversationId(conversationId);
            userMsg.setRole("user");
            userMsg.setContent(userMessage);
            messages.saveAndFlush(userMsg);
            trySend(emitter, "message", messageDto(userMsg));

            List<LlmMessage> history = new ArrayList<>();
            history.add(LlmMessage.system(systemPromptFor(projectId)));
            for (AiMessage m : messages.findByConversationIdOrderByCreatedAtAscIdAsc(conversationId)) {
                if ("user".equals(m.getRole())) {
                    history.add(LlmMessage.user(m.getContent()));
                } else if ("assistant".equals(m.getRole()) && m.getContent() != null && !m.getContent().isBlank()) {
                    history.add(LlmMessage.assistant(m.getContent(), null));
                }
            }

            List<LlmToolSpec> toolSpecs = toolSpecs();
            int toolCallCount = 0;
            Instant deadline = Instant.now().plusSeconds(WALL_CLOCK_SECONDS);

            for (int turn = 0; turn < MAX_TURNS; turn++) {
                if (stopped.get()) { finishStopped(conversationId, emitter); return; }
                if (Instant.now().isAfter(deadline)) {
                    failTurn(conversationId, emitter, "Stopped: wall-clock budget of "
                            + WALL_CLOCK_SECONDS / 60 + " minutes exceeded.");
                    return;
                }

                LlmChatResult result;
                try {
                    result = llm.chat(history, toolSpecs, false);
                } catch (LlmException e) {
                    log.warn("LLM call failed: {}", e.getMessage());
                    failTurn(conversationId, emitter, "LLM call failed: " + e.getMessage());
                    return;
                }

                if (result.hasToolCalls()) {
                    for (LlmToolCall call : result.getToolCalls()) {
                        toolCallCount++;
                        if (toolCallCount > MAX_TOOL_CALLS) {
                            failTurn(conversationId, emitter, "Stopped: tool call budget of "
                                    + MAX_TOOL_CALLS + " exceeded.");
                            return;
                        }
                        String toolResult = executeToolCall(projectId, conversationId, userId, call, stopped, emitter);
                        if (toolResult == null) { return; }
                        history.add(LlmMessage.assistant(null, List.of(call)));
                        history.add(LlmMessage.tool(call.getId(), toolResult));
                        if (stopped.get()) { finishStopped(conversationId, emitter); return; }
                    }
                    continue;
                }

                AiMessage finalMsg = new AiMessage();
                finalMsg.setConversationId(conversationId);
                finalMsg.setRole("assistant");
                finalMsg.setContent(result.getContent() == null ? "" : result.getContent());
                finalMsg.setPromptTokens(result.getPromptTokens());
                finalMsg.setCompletionTokens(result.getCompletionTokens());
                messages.saveAndFlush(finalMsg);
                trySend(emitter, "message", messageDto(finalMsg));
                eventBus.publish(conversationId, "message", messageDto(finalMsg));
                touchConversation(conversationId);
                setState(conversationId, "DONE", emitter);
                trySend(emitter, "done", Map.of());
                return;
            }
            failTurn(conversationId, emitter, "Stopped: reached the maximum of " + MAX_TURNS
                    + " reasoning turns without finishing.");
        } finally {
            runningSessions.remove(conversationId);
            lock.unlock();
        }
    }

    // ------------------------------------------------------------------
    // Tool execution
    // ------------------------------------------------------------------

    /** Executes one tool call; returns the tool result JSON for the model, or null if the turn should end. */
    private String executeToolCall(Long projectId, Long conversationId, Long userId, LlmToolCall call,
                                   AtomicBoolean stopped, SseEmitter emitter) {
        AiTool tool = toolRegistry.get(call.getName());

        AiAction action = new AiAction();
        action.setProjectId(projectId);
        action.setConversationId(conversationId);
        action.setUserId(userId);
        action.setType("TOOL_CALL");
        action.setLabel(call.getName());
        action.setStatus("RUNNING");
        action.setPayload(toolsPayload(call));
        action = actions.saveAndFlush(action);
        publishAction(conversationId, emitter, action);

        if (tool == null) {
            String err = "{\"error\":\"Unknown tool: " + jsonEscape(call.getName()) + "\"}";
            action.setStatus("FAILED");
            action.setPayload(payloadWithResult(call, err));
            actions.saveAndFlush(action);
            publishAction(conversationId, emitter, action);
            return err;
        }

        Map<String, Object> args;
        try {
            args = mapper.readValue(call.getArgumentsJson() == null ? "{}" : call.getArgumentsJson(),
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            String err = "{\"error\":\"Malformed tool arguments: " + jsonEscape(String.valueOf(e.getMessage())) + "\"}";
            action.setStatus("FAILED");
            action.setPayload(payloadWithResult(call, err));
            actions.saveAndFlush(action);
            publishAction(conversationId, emitter, action);
            return err;
        }
        action.setLabel(tool.label());
        actions.saveAndFlush(action);

        boolean needsApproval = tool.requiresApproval(args);
        if (needsApproval) {
            action.setStatus("AWAITING_APPROVAL");
            actions.saveAndFlush(action);
            publishAction(conversationId, emitter, action);
            Boolean approved = waitForApproval(action.getId(), stopped);
            if (approved == null || stopped.get()) {
                action.setStatus("CANCELLED");
                actions.saveAndFlush(action);
                publishAction(conversationId, emitter, action);
                return null;
            }
            if (!approved) {
                String rejection = "{\"error\":\"User rejected this action. Do not retry it; ask how to proceed.\"}";
                action.setStatus("CANCELLED");
                action.setPayload(payloadWithResult(call, rejection));
                actions.saveAndFlush(action);
                publishAction(conversationId, emitter, action);
                persistToolMessage(conversationId, call, rejection, action.getId());
                return rejection;
            }
            action.setStatus("RUNNING");
            actions.saveAndFlush(action);
            publishAction(conversationId, emitter, action);
        }

        ToolContext ctx = new ToolContext(projectId, conversationId,
                new AppPrincipal(userId, null, null),
                event -> {
                    eventBus.publish(conversationId, "tool_event", event);
                    trySend(emitter, "tool_event", event);
                });

        String resultJson;
        try {
            Object result = tool.execute(args, ctx);
            resultJson = toJson(result);
            action.setStatus("SUCCESS");
            action.setPayload(payloadWithResult(call, truncate(resultJson)));
            actions.saveAndFlush(action);
        } catch (Exception e) {
            log.warn("Tool {} failed: {}", call.getName(), String.valueOf(e));
            resultJson = "{\"error\":" + jsonString(String.valueOf(e.getMessage())) + "}";
            action.setStatus("FAILED");
            action.setPayload(payloadWithResult(call, truncate(resultJson)));
            actions.saveAndFlush(action);
        }
        publishAction(conversationId, emitter, action);
        persistToolMessage(conversationId, call, resultJson, action.getId());
        touchConversation(conversationId);
        return resultJson;
    }

    private void persistToolMessage(Long conversationId, LlmToolCall call, String result, Long actionId) {
        AiMessage toolMsg = new AiMessage();
        toolMsg.setConversationId(conversationId);
        toolMsg.setRole("tool");
        toolMsg.setToolCallId(call.getId());
        toolMsg.setContent(truncate(result));
        toolMsg.setActionId(actionId);
        messages.saveAndFlush(toolMsg);
    }

    /** Blocks until the user approves/rejects, the session stops, or the approval times out. */
    private Boolean waitForApproval(Long actionId, AtomicBoolean stopped) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        pendingApprovals.put(actionId, future);
        try {
            long deadline = System.currentTimeMillis() + APPROVAL_TIMEOUT_SECONDS * 1000;
            while (System.currentTimeMillis() < deadline && !stopped.get()) {
                try {
                    return future.get(1, TimeUnit.SECONDS);
                } catch (TimeoutException ignore) {
                    // keep waiting while checking the stop flag
                } catch (ExecutionException e) {
                    return null;
                }
            }
            return null; // timeout or stop
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            pendingApprovals.remove(actionId);
        }
    }

    // ------------------------------------------------------------------
    // Approvals (called from the REST controller)
    // ------------------------------------------------------------------

    public Map<String, Object> approveAction(Long projectId, Long conversationId, Long actionId,
                                             AppPrincipal principal) {
        requireAccess(projectId, principal);
        return resolveApproval(conversationId, actionId, true);
    }

    public Map<String, Object> cancelAction(Long projectId, Long conversationId, Long actionId,
                                            AppPrincipal principal) {
        requireAccess(projectId, principal);
        return resolveApproval(conversationId, actionId, false);
    }

    private Map<String, Object> resolveApproval(Long conversationId, Long actionId, boolean approved) {
        AiAction action = actions.findById(actionId)
                .orElseThrow(() -> new NotFoundException("Action not found: " + actionId));
        if (!Objects.equals(action.getConversationId(), conversationId)) {
            throw new ForbiddenException("Action does not belong to conversation " + conversationId);
        }
        CompletableFuture<Boolean> future = pendingApprovals.get(actionId);
        if (future == null) {
            return Map.of("status", "not_pending", "actionId", actionId, "actionStatus", action.getStatus());
        }
        future.complete(approved);
        return Map.of("status", approved ? "approved" : "rejected", "actionId", actionId);
    }

    // ------------------------------------------------------------------
    // Prompt & specs
    // ------------------------------------------------------------------

    private String systemPromptFor(Long projectId) {
        Project project = projectService.requireProject(projectId);
        return AgentPrompts.systemPrompt(
                project.getName(),
                project.getTechStack() == null ? "not set" : project.getTechStack(),
                project.getArchitecture() == null ? "not set" : project.getArchitecture(),
                project.getDevHistory(),
                3);
    }

    private List<LlmToolSpec> toolSpecs() {
        List<LlmToolSpec> specs = new ArrayList<>();
        for (AiTool tool : toolRegistry.all()) {
            specs.add(new LlmToolSpec(tool.name(), tool.description(), tool.parametersSchema()));
        }
        return specs;
    }

    // ------------------------------------------------------------------
    // State transitions
    // ------------------------------------------------------------------

    private void setState(Long conversationId, String state, SseEmitter emitter) {
        AiConversation c = conversations.findById(conversationId).orElse(null);
        if (c != null) {
            c.setState(state);
            conversations.saveAndFlush(c);
        }
        eventBus.publish(conversationId, "state", Map.of("state", state));
        trySend(emitter, "state", Map.of("state", state));
    }

    private void touchConversation(Long conversationId) {
        AiConversation c = conversations.findById(conversationId).orElse(null);
        if (c != null) {
            conversations.saveAndFlush(c);
        }
    }

    private void finishStopped(Long conversationId, SseEmitter emitter) {
        setState(conversationId, "STOPPED", emitter);
        trySend(emitter, "done", Map.of("stopped", true));
        log.info("Agent session for conversation {} stopped by user", conversationId);
    }

    private void failTurn(Long conversationId, SseEmitter emitter, String reason) {
        AiMessage msg = new AiMessage();
        msg.setConversationId(conversationId);
        msg.setRole("assistant");
        msg.setContent("⚠️ " + reason);
        messages.saveAndFlush(msg);
        trySend(emitter, "message", messageDto(msg));
        eventBus.publish(conversationId, "message", messageDto(msg));
        setState(conversationId, "FAILED", emitter);
        trySend(emitter, "done", Map.of("failed", true));
        log.warn("Agent turn for conversation {} failed: {}", conversationId, reason);
    }

    // ------------------------------------------------------------------
    // DTOs & helpers
    // ------------------------------------------------------------------

    private Map<String, Object> messageDto(AiMessage m) {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("id", m.getId());
        o.put("role", m.getRole());
        o.put("content", m.getContent());
        o.put("toolCalls", parseJsonOrNull(m.getToolCalls()));
        o.put("toolCallId", m.getToolCallId());
        o.put("actionId", m.getActionId());
        o.put("createdAt", String.valueOf(m.getCreatedAt()));
        return o;
    }

    private Map<String, Object> actionDto(AiAction a) {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("id", a.getId());
        o.put("type", a.getType());
        o.put("label", a.getLabel());
        o.put("status", a.getStatus());
        o.put("payload", parseJsonOrNull(a.getPayload()));
        o.put("createdAt", String.valueOf(a.getCreatedAt()));
        return o;
    }

    private void publishAction(Long conversationId, SseEmitter emitter, AiAction action) {
        Map<String, Object> dto = actionDto(action);
        eventBus.publish(conversationId, "action", dto);
        trySend(emitter, "action", dto);
    }

    private String toolsPayload(LlmToolCall call) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("tool", call.getName());
        p.put("args", parseJsonOrNull(call.getArgumentsJson()));
        return toJson(p);
    }

    private String payloadWithResult(LlmToolCall call, String resultJson) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("tool", call.getName());
        p.put("args", parseJsonOrNull(call.getArgumentsJson()));
        p.put("result", parseJsonOrNull(resultJson));
        return toJson(p);
    }

    private Object parseJsonOrNull(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            return json;
        }
    }

    private String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{\"error\":\"serialization failed\"}";
        }
    }

    private String jsonString(String s) {
        return "\"" + jsonEscape(s) + "\"";
    }

    private String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(Math.min(64, s.length() + 8));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() <= MAX_TOOL_RESULT_CHARS ? s
                : s.substring(0, MAX_TOOL_RESULT_CHARS) + "\n…(truncated)";
    }

    private void trySend(SseEmitter emitter, String event, Object data) {
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event().name(event).data(toJson(data),
                    org.springframework.http.MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            // subscriber disconnected — the event bus still records for reconnects
        }
    }

    private void completeQuietly(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
        }
    }
}
