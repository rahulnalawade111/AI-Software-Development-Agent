package dev.aidev.ai.tools.impl;

import dev.aidev.ai.tools.AiTool;
import dev.aidev.ai.tools.ToolContext;
import dev.aidev.git.GitService;
import dev.aidev.workspace.WorkspaceService;
import org.eclipse.jgit.api.Git;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;

/** get_git_status - shows added/modified/deleted files in the generated project. */
@Component
public class GetGitStatusTool implements AiTool {

    private final GitService git;
    private final WorkspaceService workspaces;

    public GetGitStatusTool(GitService git, WorkspaceService workspaces) {
        this.git = git;
        this.workspaces = workspaces;
    }

    public String name() { return "get_git_status"; }

    public String label() { return "Git status"; }

    public String description() {
        return "Show the generated project's working-tree status: modified, added and deleted files. "
                + "Use before overwriting files to understand what changed.";
    }

    public String parametersSchema() {
        return "{\"type\":\"object\",\"properties\":{},\"required\":[]}";
    }

    public Object execute(Map<String, Object> args, ToolContext ctx) throws Exception {
        java.nio.file.Path dir = java.nio.file.Path.of(ctx.workspaceRoot());
        if (!java.nio.file.Files.isDirectory(dir.resolve(".git"))) {
            return Map.of("initialized", false,
                    "hint", "No git repo yet in the project workspace. create_git_commit will init it.");
        }
        try (Git g = git.open(dir)) {
            Map<String, Object> status = git.status(g);
            status.put("initialized", true);
            return status;
        }
    }
}
