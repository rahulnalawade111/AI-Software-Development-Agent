package dev.aidev.ai.tools.impl;

import dev.aidev.ai.tools.AiTool;
import dev.aidev.ai.tools.ToolContext;
import dev.aidev.workspace.FileService;
import org.springframework.stereotype.Component;

import java.util.Map;

/** update_file — replace the full content of an existing file. */
@Component
public class UpdateFileTool implements AiTool {

    private final FileService files;

    public UpdateFileTool(FileService files) { this.files = files; }

    @Override public String name() { return "update_file"; }
    @Override public String label() { return "Update file"; }
    @Override public String description() {
        return "Replace the entire content of an existing file. Read it first and provide the complete new content (not a diff).";
    }
    @Override public String parametersSchema() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"path\":{\"type\":\"string\",\"description\":\"Relative path of the file to update\"},"
                + "\"content\":{\"type\":\"string\",\"description\":\"Complete new file content\"}"
                + "},\"required\":[\"path\",\"content\"]}";
    }

    @Override
    public Object execute(Map<String, Object> args, ToolContext ctx) {
        String path = ToolUtil.str(args, "path");
        String content = ToolUtil.str(args, "content", "");
        if (path.isBlank()) return Map.of("error", "path is required");
        if (!files.exists(ctx.projectId(), path)) {
            return Map.of("error", "File not found: " + path + " — use create_file instead.");
        }
        if (content.length() > 1_000_000) {
            return Map.of("error", "Content exceeds 1MB limit");
        }
        String before = files.read(ctx.projectId(), path).content();
        var r = files.write(ctx.projectId(), path, content);
        return Map.of("path", r.path(), "updated", true, "size", r.size(),
                "changed_lines", ToolUtil.diffLinesChanged(before, content));
    }
}
