package dev.aidev.ai.tools.impl;

import dev.aidev.ai.tools.AiTool;
import dev.aidev.ai.tools.ToolContext;
import dev.aidev.workspace.FileService;
import org.springframework.stereotype.Component;

import java.util.Map;

/** create_file — create a new file with full content. */
@Component
public class CreateFileTool implements AiTool {

    private final FileService files;

    public CreateFileTool(FileService files) { this.files = files; }

    @Override public String name() { return "create_file"; }
    @Override public String label() { return "Create file"; }
    @Override public String description() {
        return "Create a new file in the project workspace with complete content. Fails if the file already exists (use update_file).";
    }
    @Override public String parametersSchema() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"path\":{\"type\":\"string\",\"description\":\"Relative path inside the project, e.g. src/App.jsx\"},"
                + "\"content\":{\"type\":\"string\",\"description\":\"Complete file content — real code, never placeholders\"}"
                + "},\"required\":[\"path\",\"content\"]}";
    }

    @Override
    public Object execute(Map<String, Object> args, ToolContext ctx) {
        String path = ToolUtil.str(args, "path");
        String content = ToolUtil.str(args, "content", "");
        if (path.isBlank()) return Map.of("error", "path is required");
        if (files.exists(ctx.projectId(), path)) {
            return Map.of("error", "File already exists: " + path + " — use update_file instead.");
        }
        if (content.length() > 1_000_000) {
            return Map.of("error", "Content exceeds 1MB limit");
        }
        var r = files.write(ctx.projectId(), path, content);
        return Map.of("path", r.path(), "created", true, "size", r.size());
    }
}
