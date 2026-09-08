package dev.aidev.build;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Runs builds inside a project workspace: detects the stack from markers
 * (package.json / pom.xml), picks the right command, parses errors from the
 * output and persists every attempt as a build_run row.
 */
@Service
public class BuildService {

    private static final Logger log = LoggerFactory.getLogger(BuildService.class);

    private final SandboxRunner sandbox;
    private final BuildRunRepository runs;
    private final ObjectMapper mapper = new ObjectMapper();

    public BuildService(SandboxRunner sandbox, BuildRunRepository runs) {
        this.sandbox = sandbox;
        this.runs = runs;
    }

    public record BuildOutcome(Long runId, int attempt, String target, String command,
                               boolean success, String output, List<Map<String, Object>> errors,
                               int exitCode, long durationMs) {}

    /** Detect and run the project build. onLine is called with live output chunks. */
    public BuildOutcome build(Long projectId, Long conversationId, File workspaceDir,
                              String target, String startedBy, int attempt,
                              Consumer<String> onLine) {
        String command = detectBuildCommand(workspaceDir, target);
        BuildRun run = new BuildRun();
        run.setProjectId(projectId);
        run.setConversationId(conversationId);
        run.setAttempt(attempt);
        run.setTarget(target == null ? "ALL" : target);
        run.setCommand(command);
        run.setStatus("RUNNING");
        run.setStartedBy(startedBy);
        run = runs.save(run);

        if (onLine != null) onLine.accept("$ " + command + "\n");

        SandboxRunner.RunResult result = sandbox.run(workspaceDir, command, 600);

        String output = (result.stdout() + "\n" + result.stderr()).strip();
        if (onLine != null) onLine.accept(output + "\n");

        List<Map<String, Object>> errors = BuildErrorParser.parse(output);
        boolean success = result.exitCode() == 0 && !result.timedOut();

        run.setOutput(truncate(output, 60_000));
        run.setErrors(toJson(errors));
        run.setExitCode(result.exitCode());
        run.setDurationMs(result.durationMs());
        run.setStatus(success ? "SUCCESS" : (result.timedOut() ? "TIMEOUT" : "FAILED"));
        run.setFinishedAt(java.time.Instant.now());
        runs.save(run);

        log.info("Build for project {} attempt {}: {} (exit {})", projectId, attempt,
                run.getStatus(), result.exitCode());
        return new BuildOutcome(run.getId(), attempt, run.getTarget(), command, success,
                output, errors, result.exitCode(), result.durationMs());
    }

    /** Find the last build_run for a project and return its parsed outcome. */
    public Map<String, Object> lastBuild(Long projectId) {
        List<BuildRun> list = runs.findTop20ByProjectIdOrderByIdDesc(projectId);
        if (list.isEmpty()) return Map.of("found", false);
        BuildRun latest = list.get(0);
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("found", true);
        o.put("runId", latest.getId());
        o.put("attempt", latest.getAttempt());
        o.put("target", latest.getTarget());
        o.put("command", latest.getCommand());
        o.put("status", latest.getStatus());
        o.put("exitCode", latest.getExitCode());
        o.put("durationMs", latest.getDurationMs());
        o.put("output", latest.getOutput());
        o.put("errors", parseJson(latest.getErrors()));
        return o;
    }

    public List<Map<String, Object>> history(Long projectId) {
        return runs.findTop20ByProjectIdOrderByIdDesc(projectId).stream()
                .map(r -> {
                    Map<String, Object> o = new LinkedHashMap<>();
                    o.put("id", r.getId());
                    o.put("attempt", r.getAttempt());
                    o.put("target", r.getTarget());
                    o.put("command", r.getCommand());
                    o.put("status", r.getStatus());
                    o.put("exitCode", r.getExitCode());
                    o.put("durationMs", r.getDurationMs());
                    o.put("createdAt", String.valueOf(r.getCreatedAt()));
                    return o;
                })
                .toList();
    }

    private String detectBuildCommand(File dir, String target) {
        boolean hasFrontend = new File(dir, "package.json").isFile();
        boolean hasBackend = new File(dir, "pom.xml").isFile() || new File(dir, "build.gradle").isFile();
        if ("FRONTEND".equalsIgnoreCase(target)) return "npm run build";
        if ("BACKEND".equalsIgnoreCase(target)) return backendCommand(dir);
        if (hasFrontend && hasBackend) return "npm run build";
        if (hasBackend) return backendCommand(dir);
        if (hasFrontend) return "npm run build";
        return "npm run build"; // best effort
    }

    private String backendCommand(File dir) {
        if (new File(dir, "mvnw").isFile()) return "mvnw package -DskipTests";
        return "mvn package -DskipTests";
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "\n…[truncated]";
    }

    private String toJson(Object o) {
        try { return mapper.writeValueAsString(o); } catch (Exception e) { return "[]"; }
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try { return mapper.readTree(json); } catch (Exception e) { return json; }
    }
}
