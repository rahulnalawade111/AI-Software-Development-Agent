package dev.aidev.terminal;

import dev.aidev.build.SandboxRunner;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sandboxed one-shot commands for the IDE terminal panel. Every command is
 * screened by SandboxRunner (allowlist + denylist + timeout) and persisted.
 */
@Service
public class TerminalService {

    private final SandboxRunner sandbox;
    private final TerminalRunRepository runs;

    public TerminalService(SandboxRunner sandbox, TerminalRunRepository runs) {
        this.sandbox = sandbox;
        this.runs = runs;
    }

    public record TerminalResult(boolean blocked, String reason, int exitCode,
                                 String stdout, String stderr, long durationMs) {}

    public TerminalResult execute(Long projectId, Long userId, File dir, String command) {
        TerminalRun run = new TerminalRun();
        run.setProjectId(projectId);
        run.setUserId(userId);
        run.setCommand(command);

        if (!sandbox.isAllowed(command)) {
            run.setExitCode(-1);
            run.setExitReason("BLOCKED");
            run.setOutput("Command blocked by sandbox: " + command);
            runs.save(run);
            return new TerminalResult(true, "Command not allowed by the sandbox allowlist/denylist", -1, "",
                    "Command blocked by sandbox: " + command, 0);
        }

        SandboxRunner.RunResult r = sandbox.run(dir, command, 120);
        run.setExitCode(r.exitCode());
        run.setExitReason(r.timedOut() ? "TIMEOUT" : "NORMAL");
        run.setOutput(truncate(r.stdout() + "\n" + r.stderr(), 20_000));
        runs.save(run);
        return new TerminalResult(false, null, r.exitCode(), r.stdout(), r.stderr(), r.durationMs());
    }

    public java.util.List<Map<String, Object>> history(Long projectId) {
        return runs.findTop50ByProjectIdOrderByIdDesc(projectId).stream()
                .map(r -> Map.<String, Object>of(
                        "id", r.getId(),
                        "command", String.valueOf(r.getCommand()),
                        "output", String.valueOf(r.getOutput()),
                        "exitCode", r.getExitCode() == null ? -1 : r.getExitCode(),
                        "exitReason", String.valueOf(r.getExitReason()),
                        "createdAt", String.valueOf(r.getCreatedAt())))
                .toList().reversed();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "\n…[truncated]";
    }
}
