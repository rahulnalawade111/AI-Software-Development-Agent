package dev.aidev.ai.tools.impl;

import dev.aidev.ai.tools.AiTool;
import dev.aidev.ai.tools.ToolContext;
import dev.aidev.workspace.FileService;
import org.springframework.stereotype.Component;

import java.util.Map;

/** create_directory — create a directory (and parents) in the workspace. */
@Component
public class CreateDirectoryTool implements AiTool {

    private final FileService files;

    public CreateDirectoryTool(FileService files) { this.files = files; }

    @Override public String name() { return "create_directory"; }
    @Override public String label() { return "Create directory"; }
    @Override public String description() {
        return "Create a directory (with parents) inside the project workspace.";
    }
    @Override public String parametersSchema() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"path\":{\"type\":\"string\",\"description\":\"Relative directory path to create\"}"
                + "},\"required\":[\"path\"]}";
    }

    @Override
    public Object execute(Map<String, Object> args, ToolContext ctx) {
        String path = ToolUtil.str(args, "path");
        if (path.isBlank()) return Map.of("error", "path is required");
        files.createDirectory(ctx.projectId(), path);
        return Map.of("path", path, "created", true);
    }
}
