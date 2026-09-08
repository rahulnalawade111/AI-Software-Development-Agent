package dev.aidev.ai.llm;

/** Tool definition advertised to the model (OpenAI function-tool shape). */
public class LlmToolSpec {
    private final String name;
    private final String description;
    private final String parametersJsonSchema;

    public LlmToolSpec(String name, String description, String parametersJsonSchema) {
        this.name = name;
        this.description = description;
        this.parametersJsonSchema = parametersJsonSchema;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getParametersJsonSchema() { return parametersJsonSchema; }
}
