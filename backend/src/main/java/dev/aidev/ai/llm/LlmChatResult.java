package dev.aidev.ai.llm;

import java.util.List;

/** Result of one chat completion round. */
public class LlmChatResult {
    private String content;             // assistant text (may be empty when tool calls present)
    private List<LlmToolCall> toolCalls;
    private int promptTokens;
    private int completionTokens;
    private String finishReason;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<LlmToolCall> getToolCalls() { return toolCalls; }
    public void setToolCalls(List<LlmToolCall> toolCalls) { this.toolCalls = toolCalls; }
    public int getPromptTokens() { return promptTokens; }
    public void setPromptTokens(int promptTokens) { this.promptTokens = promptTokens; }
    public int getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(int completionTokens) { this.completionTokens = completionTokens; }
    public String getFinishReason() { return finishReason; }
    public void setFinishReason(String finishReason) { this.finishReason = finishReason; }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
