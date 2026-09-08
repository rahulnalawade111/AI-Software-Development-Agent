package dev.aidev.ai.llm;

import java.util.ArrayList;
import java.util.List;

/** A single message in the LLM conversation. */
public class LlmMessage {
    private String role; // system | user | assistant | tool
    private String content;
    private List<LlmToolCall> toolCalls; // assistant only
    private String toolCallId;           // tool only

    public LlmMessage() {}

    public static LlmMessage system(String content) {
        LlmMessage m = new LlmMessage();
        m.role = "system"; m.content = content; return m;
    }
    public static LlmMessage user(String content) {
        LlmMessage m = new LlmMessage();
        m.role = "user"; m.content = content; return m;
    }
    public static LlmMessage assistant(String content, List<LlmToolCall> toolCalls) {
        LlmMessage m = new LlmMessage();
        m.role = "assistant"; m.content = content; m.toolCalls = toolCalls; return m;
    }
    public static LlmMessage tool(String toolCallId, String content) {
        LlmMessage m = new LlmMessage();
        m.role = "tool"; m.toolCallId = toolCallId; m.content = content; return m;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<LlmToolCall> getToolCalls() { return toolCalls; }
    public void setToolCalls(List<LlmToolCall> toolCalls) { this.toolCalls = toolCalls; }
    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }

    /** Convenience: build a copy list without tool messages (for summary prompts). */
    public static List<LlmMessage> withoutTools(List<LlmMessage> messages) {
        List<LlmMessage> out = new ArrayList<>();
        for (LlmMessage m : messages) {
            if (!"tool".equals(m.role)) out.add(m);
        }
        return out;
    }
}
