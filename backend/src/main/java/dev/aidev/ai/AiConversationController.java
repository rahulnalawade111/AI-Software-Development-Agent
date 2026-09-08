package dev.aidev.ai;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/ai")
public class AiConversationController {

    private final AiAgentService agentService;
    private final AgentEventBus eventBus;
    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;

    public AiConversationController(AiAgentService agentService, AgentEventBus eventBus,
                                    AiConversationRepository conversationRepository,
                                    AiMessageRepository messageRepository) {
        this.agentService = agentService;
        this.eventBus = eventBus;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @GetMapping("/conversations")
    public List<Map<String, Object>> conversations(@AuthenticationPrincipal dev.aidev.security.AppPrincipal principal,
                                                   @PathVariable Long projectId) {
        agentService.requireAccess(projectId, principal);
        return conversationRepository.findByProjectIdOrderByUpdatedAtDesc(projectId).stream()
                .map(c -> Map.<String, Object>of("id", c.getId(), "title", c.getTitle() == null ? "" : c.getTitle(),
                        "state", c.getState(), "updatedAt", String.valueOf(c.getUpdatedAt())))
                .toList();
    }

    @PostMapping("/conversations")
    public Map<String, Object> createConversation(@AuthenticationPrincipal dev.aidev.security.AppPrincipal principal,
                                                  @PathVariable Long projectId,
                                                  @RequestBody(required = false) Map<String, String> body) {
        agentService.requireAccess(projectId, principal);
        AiConversation c = agentService.createConversation(projectId, principal,
                body != null ? body.get("title") : null);
        return Map.of("id", c.getId(), "title", c.getTitle() == null ? "" : c.getTitle(), "state", c.getState());
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<Map<String, Object>> messages(@AuthenticationPrincipal dev.aidev.security.AppPrincipal principal,
                                              @PathVariable Long projectId,
                                              @PathVariable Long conversationId) {
        agentService.requireAccess(projectId, principal);
        return messageRepository.findByConversationIdOrderByCreatedAtAscIdAsc(conversationId).stream()
                .map(m -> {
                    Map<String, Object> o = new java.util.LinkedHashMap<>();
                    o.put("id", m.getId());
                    o.put("role", m.getRole());
                    o.put("content", m.getContent());
                    o.put("toolCalls", m.getToolCalls());
                    o.put("actionId", m.getActionId());
                    o.put("createdAt", String.valueOf(m.getCreatedAt()));
                    return o;
                })
                .toList();
    }

    @PostMapping(value = "/conversations/{conversationId}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@AuthenticationPrincipal dev.aidev.security.AppPrincipal principal,
                           @PathVariable Long projectId,
                           @PathVariable Long conversationId,
                           @RequestBody Map<String, String> body) {
        agentService.requireAccess(projectId, principal);
        String message = body != null ? body.get("message") : null;
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
        return agentService.startAgentTurn(projectId, conversationId, principal, message);
    }

    @GetMapping(value = "/conversations/{conversationId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@AuthenticationPrincipal dev.aidev.security.AppPrincipal principal,
                             @PathVariable Long projectId,
                             @PathVariable Long conversationId) {
        agentService.requireAccess(projectId, principal);
        return eventBus.subscribe(conversationId);
    }

    @PostMapping("/conversations/{conversationId}/approve/{actionId}")
    public Map<String, Object> approve(@AuthenticationPrincipal dev.aidev.security.AppPrincipal principal,
                                       @PathVariable Long projectId,
                                       @PathVariable Long conversationId,
                                       @PathVariable Long actionId) {
        return agentService.approveAction(projectId, conversationId, actionId, principal);
    }

    @PostMapping("/conversations/{conversationId}/cancel/{actionId}")
    public Map<String, Object> cancel(@AuthenticationPrincipal dev.aidev.security.AppPrincipal principal,
                                      @PathVariable Long projectId,
                                      @PathVariable Long conversationId,
                                      @PathVariable Long actionId) {
        return agentService.cancelAction(projectId, conversationId, actionId, principal);
    }

    @GetMapping("/actions")
    public List<Map<String, Object>> actions(@AuthenticationPrincipal dev.aidev.security.AppPrincipal principal,
                                             @PathVariable Long projectId) {
        agentService.requireAccess(projectId, principal);
        return agentService.recentActions(projectId);
    }
}
