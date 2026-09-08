package dev.aidev.ai.tools.impl;

import dev.aidev.ai.tools.AiTool;
import dev.aidev.ai.tools.ToolContext;
import dev.aidev.git.GitService;
import org.eclipse.jgit.api.Git;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;

/** create_git_commit - checkpoints the generated project (auto-inits the repo). */
@Component
public class CreateGitCommitTool implements AiTool {

    private final GitService git;

    public CreateGitCommitTool(GitService git) {
        this.git = git;
    }

    public String name() { return "create_git_commit"; }

    public String label() { return "Create git commit"; }

    public String description() {
        return "Commit all current changes in the generated project as a checkpoint "
                + "(initializes the repo on first use). Use after completing a milestone.";
    }

    public String parametersSchema() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"message\":{\"type\":\"string\",\"description\":\"Commit message\"}},"
                + "\"required\":[\"message\"]}";
    }

    public Object execute(Map<String, Object> args, ToolContext ctx) throws Exception {
        java.nio.file.Path dir = java.nio.file.Path.of(ctx.workspaceRoot());
        String message = args.get("message") instanceof String s && !s.isBlank()
                ? s : "AI checkpoint";
        Git g = java.nio.file.Files.isDirectory(dir.resolve(".git")) ? git.open(dir) : git.init(dir);
        try (g) {
            git.commitAll(g, message, "AI DevAgent", "ai-agent@aidev.local");
            Map<String, Object> status = git.status(g);
            status.put("committed", true);
            status.put("message", message);
            return status;
        }
    }
}
