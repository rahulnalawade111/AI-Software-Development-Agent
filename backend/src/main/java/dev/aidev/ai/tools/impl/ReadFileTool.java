package dev.aidev.ai.tools.impl;

import dev.aidev.ai.tools.AiTool;
import dev.aidev.ai.tools.ToolContext;
import dev.aidev.workspace.FileService;
import org.springframework.stereotype.Component;

import java.util.Map;

/** read_file — read the content of a workspace file. */
@Component
public class ReadFileTool implements AiTool {

    private final FileService files;

    public ReadFileTool(FileService files) { this.files = files; }

    @Override public String name() { return "read_file"; }
    @Override public String label() { return "Read file"; }
    @Override public String description() {
        return "Read the content of a file in the project workspace (text files; binary files are reported as binary).";
    }
    @Override public String parametersSchema() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"path\":{\"type\":\"string\",\"description\":\"Relative path of the file\"}"
                + "},\"required\":[\"path\"]}";
    }

    @Override
    public Object execute(Map<String, Object> args, ToolContext ctx) {
        String path = ToolUtil.str(args, "path");
        if (path.isBlank()) return Map.of("error", "path is required");
        var r = files.read(ctx.projectId(), path);
        if (r.binary()) return Map.of("path", path, "binary", true, "size", r.size());
        return Map.of("path", path, "content", r.content(), "size", r.size(), "binary", false,
                "truncated", r.content() != null && r.content().endsWith("... [truncated]"));
    }
}
