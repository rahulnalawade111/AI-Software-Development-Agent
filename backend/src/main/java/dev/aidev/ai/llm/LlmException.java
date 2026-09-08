package dev.aidev.ai.llm;

/** Runtime failure talking to the LLM gateway. */
public class LlmException extends RuntimeException {
    public LlmException(String message) { super(message); }
    public LlmException(String message, Throwable cause) { super(message, cause); }
}
