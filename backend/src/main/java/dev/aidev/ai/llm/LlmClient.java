package dev.aidev.ai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * OpenAI-compatible chat client (chat completions with tool calling).
 * Blocking round-trip used by the agent loop; streaming deltas are emitted
 * by AgentSession via the event bus instead of raw SSE passthrough.
 */
@Service
public class LlmClient {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    private final String baseUrl;
    private final String apiKey;
    private final String model;

    public LlmClient(@Value("${app.llm.base-url}") String baseUrl,
                     @Value("${app.llm.api-key}") String apiKey,
                     @Value("${app.llm.model}") String model) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    public String model() { return model; }

    public LlmChatResult chat(List<LlmMessage> messages, List<LlmToolSpec> tools, boolean parallelTools) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.put("stream", false);
            if (parallelTools) body.put("parallel_tool_calls", false);
            ArrayNode msgs = body.putArray("messages");
            for (LlmMessage m : messages) {
                ObjectNode mn = msgs.addObject();
                mn.put("role", m.getRole());
                if (m.getContent() != null) mn.put("content", m.getContent());
                else mn.put("content", "");
                if (m.getToolCalls() != null && !m.getToolCalls().isEmpty()) {
                    ArrayNode tcs = mn.putArray("tool_calls");
                    for (LlmToolCall tc : m.getToolCalls()) {
                        ObjectNode tcn = tcs.addObject();
                        tcn.put("id", tc.getId());
                        tcn.put("type", "function");
                        ObjectNode fn = tcn.putObject("function");
                        fn.put("name", tc.getName());
                        fn.put("arguments", tc.getArgumentsJson());
                    }
                }
                if (m.getToolCallId() != null) mn.put("tool_call_id", m.getToolCallId());
            }
            if (tools != null && !tools.isEmpty()) {
                ArrayNode toolsArr = body.putArray("tools");
                for (LlmToolSpec t : tools) {
                    ObjectNode tn = toolsArr.addObject();
                    tn.put("type", "function");
                    ObjectNode fn = tn.putObject("function");
                    fn.put("name", t.getName());
                    fn.put("description", t.getDescription());
                    fn.set("parameters", mapper.readTree(t.getParametersJsonSchema()));
                }
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimmed(baseUrl) + "/chat/completions"))
                    .timeout(Duration.ofSeconds(180))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new LlmException("LLM gateway error " + resp.statusCode() + ": "
                        + resp.body().substring(0, Math.min(400, resp.body().length())));
            }
            return parse(mapper.readTree(resp.body()));
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("LLM call failed: " + e.getMessage(), e);
        }
    }

    private LlmChatResult parse(JsonNode root) {
        LlmChatResult result = new LlmChatResult();
        JsonNode choice = root.path("choices").path(0);
        result.setFinishReason(choice.path("finish_reason").asText(null));
        JsonNode message = choice.path("message");
        result.setContent(message.path("content").asText(""));
        result.setPromptTokens(root.path("usage").path("prompt_tokens").asInt(0));
        result.setCompletionTokens(root.path("usage").path("completion_tokens").asInt(0));
        JsonNode toolCalls = message.path("tool_calls");
        if (toolCalls.isArray() && !toolCalls.isEmpty()) {
            List<LlmToolCall> calls = new java.util.ArrayList<>();
            for (JsonNode tc : toolCalls) {
                calls.add(new LlmToolCall(
                        tc.path("id").asText(),
                        tc.path("function").path("name").asText(),
                        tc.path("function").path("arguments").asText("{}")));
            }
            result.setToolCalls(calls);
        }
        return result;
    }

    private static String trimmed(String url) {
        String u = url.trim();
        if (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        if (u.endsWith("/v1")) u = u.substring(0, u.length() - 3);
        return u;
    }
}
