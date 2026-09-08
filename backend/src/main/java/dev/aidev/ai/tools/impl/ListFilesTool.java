package dev.aidev.ai.tools.impl;

import dev.aidev.ai.tools.AiTool;
import dev.aidev.ai.tools.ToolContext;
import dev.aidev.workspace.WorkspaceService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** list_files — list files/directories under a workspace path. */
@Component
public class ListFilesTool implements AiTool {

    private final WorkspaceService workspace;

    public ListFilesTool(WorkspaceService workspace) { this.workspace = workspace; }

    @Override public String name() { return "list_files"; }
    @Override public String label() { return "List files"; }
    @Override public String description() {
        return "List files and directories under a path in the project workspace. Returns entries with type and size.";
    }
    @Override public String parametersSchema() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"path\":{\"type\":\"string\",\"description\":\"Relative directory path (empty = workspace root)\"}"
                + "}}";
    }

    @Override
    public Object execute(Map<String, Object> args, ToolContext ctx) {
        String rel = ToolUtil.str(args, "path");
        Path dir = workspace.resolveAndValidate(ctx.projectId(), rel);
        if (!Files.isDirectory(dir)) {
            return Map.of("error", "Not a directory: " + rel);
        }
        List<Map<String, Object>> entries = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            stream.sorted((a, b) -> {
                boolean da = Files.isDirectory(a), db = Files.isDirectory(b);
                if (da != db) return da ? -1 : 1;
                return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
            }).forEach(p -> {
                Map<String, Object> e = new java.util.LinkedHashMap<>();
                e.put("name", p.getFileName().toString());
                e.put("type", Files.isDirectory(p) ? "dir" : "file");
                try { e.put("size", Files.size(p)); } catch (IOException ignored) {}
                entries.add(e);
            });
        } catch (IOException e) {
            return Map.of("error", "Cannot list directory: " + e.getMessage());
        }
        return Map.of("path", rel, "entries", entries);
    }
}
