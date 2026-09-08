package dev.aidev.ai.tools.impl;

import dev.aidev.ai.tools.AiTool;
import dev.aidev.ai.tools.ToolContext;
import dev.aidev.git.GitService;
import org.eclipse.jgit.api.Git;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;

/** git_diff - unified diff of the generated project's working tree. */
@Component
public class GitDiffTool implements AiTool {

    private final GitService git;

    public GitDiffTool(GitService git) {
        this.git = git;
    }

    public String name() { return "git_diff"; }

    public String label() { return "Git diff"; }

    public String description() {
        return "Return a unified diff of all uncommitted changes in the generated project. "
                + "Use to show the user what changed after edits.";
    }

    public String parametersSchema() {
        return "{\"type\":\"object\",\"properties\":{},\"required\":[]}";
    }

    public Object execute(Map<String, Object> args, ToolContext ctx) throws Exception {
        java.nio.file.Path dir = java.nio.file.Path.of(ctx.workspaceRoot());
        if (!java.nio.file.Files.isDirectory(dir.resolve(".git"))) {
            return Map.of("initialized", false, "hint", "No git repo yet in the project workspace.");
        }
        try (Git g = git.open(dir)) {
            String diff = git.diff(g);
            return Map.of("initialized", true, "diff", diff == null ? "" : diff);
        }
    }
}
