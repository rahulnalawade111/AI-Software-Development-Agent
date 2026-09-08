package dev.aidev.ai.tools.impl;

import dev.aidev.ai.tools.AiTool;
import dev.aidev.ai.tools.ToolContext;
import dev.aidev.build.SandboxRunner;
import dev.aidev.workspace.WorkspaceService;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;

/** run_command — run an allowlisted command inside the project workspace. */
@Component
public class RunCommandTool implements AiTool {

    private final SandboxRunner runner;
    private final WorkspaceService workspace;

    public RunCommandTool(SandboxRunner runner, WorkspaceService workspace) {
        this.runner = runner;
        this.workspace = workspace;
    }

    @Override public String name() { return "run_command"; }
    @Override public String label() { return "Run command"; }
    @Override public String description() {
        return "Run a shell command inside the project workspace sandbox. Allowlisted executables: npm, npx, node, mvn, java, git, ls, cat, mkdir, grep, find, python3, pip. No chaining with && or ; .";
    }
    @Override public String parametersSchema() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"command\":{\"type\":\"string\",\"description\":\"Command to run, e.g. npm install\"},"
                + "\"timeoutSec\":{\"type\":\"integer\",\"description\":\"Timeout in seconds (default 300, max 600)\"}"
                + "},\"required\":[\"command\"]}";
    }

    @Override
    public boolean requiresApproval(Map<String, Object> args) {
        String c = ToolUtil.str(args, "command").trim();
        String exe = c.split("\\s+")[0];
        return switch (exe) {
            case "ls", "cat", "pwd", "echo", "grep", "find", "head", "tail", "wc" -> false;
            default -> true;
        };
    }

    @Override
    public Object execute(Map<String, Object> args, ToolContext ctx) {
        String command = ToolUtil.str(args, "command");
        if (command.isBlank()) return Map.of("error", "command is required");
        long timeout = (long) ToolUtil.num(args, "timeoutSec", 300);
        timeout = Math.min(Math.max(timeout, 10), 600);
        File dir = workspace.ensureWorkspace(ctx.projectId()).toFile();
        ctx.publish(Map.of("type", "command", "command", command));
        var r = runner.run(dir, command, timeout);
        return Map.of("command", command,
                "exitCode", r.exitCode(),
                "stdout", r.stdout(),
                "stderr", r.stderr(),
                "durationMs", r.durationMs(),
                "timedOut", r.timedOut());
    }
}
