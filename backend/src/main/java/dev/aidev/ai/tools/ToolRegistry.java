package dev.aidev.ai.tools;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Registry of all AI tools. Tools are Spring beans implementing AiTool.
 */
@Service
public class ToolRegistry {

    private final Map<String, AiTool> tools = new LinkedHashMap<>();

    public ToolRegistry(List<AiTool> discovered) {
        for (AiTool tool : discovered) {
            if (tools.put(tool.name(), tool) != null) {
                throw new IllegalStateException("Duplicate AI tool: " + tool.name());
            }
        }
    }

    public AiTool get(String name) {
        return tools.get(name);
    }

    public Collection<AiTool> all() {
        return tools.values();
    }

    public List<AiTool> forProject() {
        return new ArrayList<>(tools.values());
    }
}
