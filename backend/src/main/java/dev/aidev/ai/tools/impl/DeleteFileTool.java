package dev.aidev.ai.tools.impl;

import dev.aidev.ai.tools.AiTool;
import dev.aidev.ai.tools.ToolContext;
import dev.aidev.workspace.FileService;
import org.springframework.stereotype.Component;

import java.util.Map;

/** delete_file — delete a file or directory (destructive → approval). */
@Component
public class DeleteFileTool implements AiTool {

    private final FileService files;

    public DeleteFileTool(FileService files) { this.files = files; }

    @Override public String name() { return "delete_file"; }
    @Override public String label() { return "Delete file"; }
    @Override public String description() {
        return "Delete a file or directory (recursively) from the project workspace. Requires user approval.";
    }
    @Override public String parametersSchema() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"path\":{\"type\":\"string\",\"description\":\"Relative path to delete\"}"
                + "},\"required\":[\"path\"]}";
    }
    @Override public boolean requiresApproval() { return true; }

    @Override
    public Object execute(Map<String, Object> args, ToolContext ctx) {
        String path = ToolUtil.str(args, "path");
        if (!files.exists(ctx.projectId(), path)) {
            return Map.of("error", "Path not found: " + path);
        }
        files.delete(ctx.projectId(), path);
        return Map.of("path", path, "deleted", true);
    }
}
