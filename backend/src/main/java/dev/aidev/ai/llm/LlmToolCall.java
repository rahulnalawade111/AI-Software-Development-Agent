package dev.aidev.ai.llm;

/** A tool invocation the model wants performed. */
public class LlmToolCall {
    private String id;
    private String name;
    private String argumentsJson;

    public LlmToolCall() {}
    public LlmToolCall(String id, String name, String argumentsJson) {
        this.id = id; this.name = name; this.argumentsJson = argumentsJson;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getArgumentsJson() { return argumentsJson; }
    public void setArgumentsJson(String argumentsJson) { this.argumentsJson = argumentsJson; }
}
