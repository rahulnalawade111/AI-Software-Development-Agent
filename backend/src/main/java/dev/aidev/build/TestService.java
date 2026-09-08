package dev.aidev.build;

import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs the project's test suite and extracts pass/fail counts from output.
 * Supports Maven surefire and npm/vitest/jest output shapes.
 */
@Service
public class TestService {

    private final SandboxRunner sandbox;
    private final TestRunRepository runs;

    public TestService(SandboxRunner sandbox, TestRunRepository runs) {
        this.sandbox = sandbox;
        this.runs = runs;
    }

    public record TestOutcome(Long runId, String command, boolean success,
                              int total, int passed, int failed, int skipped,
                              String output, List<Map<String, Object>> failures) {}

    public TestOutcome runTests(Long projectId, Long conversationId, File workspaceDir,
                                String target, Consumer<String> onLine) {
        String command = detectTestCommand(workspaceDir, target);
        TestRun run = new TestRun();
        run.setProjectId(projectId);
        run.setBuildRunId(conversationId);
        run.setTarget(target == null ? "ALL" : target);
        run.setStatus("RUNNING");
        run = runs.save(run);

        if (onLine != null) onLine.accept("$ " + command + "\n");
        SandboxRunner.RunResult result = sandbox.run(workspaceDir, command, 600);
        String output = (result.stdout() + "\n" + result.stderr()).strip();
        if (onLine != null) onLine.accept(output + "\n");

        Counts counts = parseCounts(output, result.exitCode());
        List<Map<String, Object>> failures = parseFailures(output);

        run.setTotal(counts.total()); run.setPassed(counts.passed());
        run.setFailed(counts.failed()); run.setSkipped(counts.skipped());
        run.setResults(toJson(failures));
        run.setOutput(truncate(output, 60_000));
        run.setExitCode(result.exitCode());
        run.setStatus(result.exitCode() == 0 ? "SUCCESS" : "FAILED");
        run.setFinishedAt(java.time.Instant.now());
        runs.save(run);

        return new TestOutcome(run.getId(), command, result.exitCode() == 0,
                counts.total(), counts.passed(), counts.failed(), counts.skipped(),
                output, failures);
    }

    public List<Map<String, Object>> history(Long projectId) {
        return runs.findTop20ByProjectIdOrderByIdDesc(projectId).stream()
                .map(r -> {
                    Map<String, Object> o = new LinkedHashMap<>();
                    o.put("id", r.getId());
                    o.put("target", r.getTarget());
                    o.put("status", r.getStatus());
                    o.put("total", r.getTotal());
                    o.put("passed", r.getPassed());
                    o.put("failed", r.getFailed());
                    o.put("skipped", r.getSkipped());
                    o.put("createdAt", String.valueOf(r.getCreatedAt()));
                    return o;
                })
                .toList();
    }

    private String detectTestCommand(File dir, String target) {
        if ("FRONTEND".equalsIgnoreCase(target)) return "npm test";
        if ("BACKEND".equalsIgnoreCase(target)) {
            return new File(dir, "mvnw").isFile() ? "mvnw test" : "mvn test";
        }
        boolean hasBackend = new File(dir, "pom.xml").isFile();
        boolean hasFrontend = new File(dir, "package.json").isFile();
        if (hasBackend) return new File(dir, "mvnw").isFile() ? "mvnw test" : "mvn test";
        if (hasFrontend) return "npm test";
        return "npm test";
    }

    private record Counts(int total, int passed, int failed, int skipped) {}

    private Counts parseCounts(String output, int exitCode) {
        // Maven: "Tests run: 12, Failures: 1, Errors: 0, Skipped: 0"
        Matcher m = Pattern.compile("Tests run:\\s*(\\d+),\\s*Failures:\\s*(\\d+),\\s*Errors:\\s*(\\d+)\\s*,?\\s*(?:Skipped:\\s*(\\d+))?")
                .matcher(output);
        int total = 0, failed = 0, skipped = 0;
        while (m.find()) {
            total += Integer.parseInt(m.group(1));
            failed += Integer.parseInt(m.group(2)) + Integer.parseInt(m.group(3));
            if (m.group(4) != null) skipped += Integer.parseInt(m.group(4));
        }
        if (total > 0) {
            return new Counts(total, total - failed - skipped, failed, skipped);
        }
        // Jest: "Tests: 3 failed, 5 passed, 8 total"
        m = Pattern.compile("Tests:\\s*(\\d+) failed,?\\s*(\\d+) passed,?\\s*(\\d+) total").matcher(output);
        if (m.find()) {
            int f = Integer.parseInt(m.group(1)), p = Integer.parseInt(m.group(2)), t = Integer.parseInt(m.group(3));
            return new Counts(t, p, f, t - p - f);
        }
        m = Pattern.compile("Tests:\\s*(\\d+) passed,?\\s*(\\d+) total").matcher(output);
        if (m.find()) {
            int p = Integer.parseInt(m.group(1)), t = Integer.parseInt(m.group(2));
            return new Counts(t, p, 0, t - p);
        }
        // Vitest: "Test Files  2 passed (2)" / "     Tests  4 passed (4)"
        m = Pattern.compile("Tests\\s+(\\d+) passed \\((\\d+)\\)").matcher(output);
        if (m.find()) {
            int p = Integer.parseInt(m.group(1)), t = Integer.parseInt(m.group(2));
            return new Counts(t, p, 0, t - p);
        }
        // Nothing parsed — treat exit code as signal
        if (exitCode == 0) return new Counts(0, 0, 0, 0);
        return new Counts(0, 0, 1, 0);
    }

    private List<Map<String, Object>> parseFailures(String output) {
        List<Map<String, Object>> failures = new ArrayList<>();
        // Maven: "[ERROR]   ClassName.methodName:12 » NullPointer ..."
        Matcher m = Pattern.compile("\\[ERROR\\]\\s+([\\w.$]+)\\(([\\w.$]+)\\.java):(\\d+)\\)?")
                .matcher(output);
        while (m.find() && failures.size() < 50) {
            failures.add(Map.of("test", m.group(1), "file", m.group(2) + ".java",
                    "line", Integer.parseInt(m.group(3))));
        }
        // Jest: "✕ test name" / Vitest: "× test name"
        m = Pattern.compile("(?m)^\\s*(?:✕|×)\\s+(.+)$").matcher(output);
        while (m.find() && failures.size() < 50) {
            failures.add(Map.of("test", m.group(1).strip()));
        }
        return failures;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "\n…[truncated]";
    }

    private String toJson(Object o) {
        try { return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(o); }
        catch (Exception e) { return "[]"; }
    }
}

