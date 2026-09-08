package dev.aidev.ai.tools.impl;

import dev.aidev.ai.tools.AiTool;
import dev.aidev.ai.tools.ToolContext;
import dev.aidev.workspace.FileService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** search_code — case-insensitive text search across the workspace. */
@Component
public class SearchCodeTool implements AiTool {

    private final FileService files;

    public SearchCodeTool(FileService files) { this.files = files; }

    @Override public String name() { return "search_code"; }
    @Override public String label() { return "Search code"; }
    @Override public String description() {
        return "Search for a text pattern across all project files (case-insensitive). Returns path:line:text matches.";
    }
    @Override public String parametersSchema() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"query\":{\"type\":\"string\",\"description\":\"Text to search for\"},"
                + "\"glob\":{\"type\":\"string\",\"description\":\"Optional filename glob filter, e.g. *.java\"}"
                + "},\"required\":[\"query\"]}";
    }

    @Override
    public Object execute(Map<String, Object> args, ToolContext ctx) {
        String query = ToolUtil.str(args, "query");
        var results = files.search(ctx.projectId(), query, ToolUtil.str(args, "glob"));
        List<Map<String, Object>> hits = new ArrayList<>();
        for (var r : results) {
            hits.add(Map.of("path", r.path(), "line", r.line(), "text", r.text()));
        }
        return Map.of("query", query, "matches", hits, "count", hits.size());
    }
}
