package dev.aidev.build;

import dev.aidev.project.ProjectService;
import dev.aidev.security.AppPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;
import java.util.Map;

/** REST endpoints for build/test runs, error history and preview control. */
@RestController
@RequestMapping("/api/projects/{projectId}/build")
public class BuildController {

    private final BuildService builds;
    private final TestService tests;
    private final ProjectService projects;
    private final PreviewRegistry preview;
    private final BuildStateStore stateStore;

    public BuildController(BuildService builds, TestService tests, ProjectService projects,
                           PreviewRegistry preview, BuildStateStore stateStore) {
        this.builds = builds;
        this.stateStore = stateStore;
        this.tests = tests;
        this.projects = projects;
        this.preview = preview;
    }

    @PostMapping("/run")
    public BuildService.BuildOutcome run(@AuthenticationPrincipal AppPrincipal principal,
                                         @PathVariable Long projectId,
                                         @RequestBody(required = false) Map<String, Object> body) {
        projects.requireWrite(projectId, principal);
        String target = body != null && body.get("target") instanceof String s ? s : "all";
        int attempt = body != null && body.get("attempt") instanceof Number n ? n.intValue() : 1;
        BuildService.BuildOutcome out = builds.build(projectId, null, workspaceDir(projectId),
                target.toUpperCase(), "USER", attempt, null);
        stateStore.record(projectId, Map.of(
                "attempt", out.attempt(), "success", out.success(),
                "errors", out.errors(), "status", out.success() ? "SUCCESS" : "FAILED"));
        return out;
    }

    @GetMapping("/last")
    public Map<String, Object> last(@AuthenticationPrincipal AppPrincipal principal,
                                    @PathVariable Long projectId) {
        projects.requireRead(projectId, principal);
        Map<String, Object> stored = stateStore.lastRun(projectId);
        if (stored != null) {
            return stored;
        }
        return builds.lastBuild(projectId);
    }

    @GetMapping("/history")
    public List<Map<String, Object>> history(@AuthenticationPrincipal AppPrincipal principal,
                                             @PathVariable Long projectId) {
        projects.requireRead(projectId, principal);
        return builds.history(projectId);
    }

    @PostMapping("/tests/run")
    public TestService.TestOutcome runTests(@AuthenticationPrincipal AppPrincipal principal,
                                            @PathVariable Long projectId,
                                            @RequestBody(required = false) Map<String, Object> body) {
        projects.requireWrite(projectId, principal);
        String target = body != null && body.get("target") instanceof String s ? s : "all";
        return tests.runTests(projectId, null, workspaceDir(projectId), target.toUpperCase(), null);
    }

    @GetMapping("/tests/history")
    public List<Map<String, Object>> testHistory(@AuthenticationPrincipal AppPrincipal principal,
                                                 @PathVariable Long projectId) {
        projects.requireRead(projectId, principal);
        return tests.history(projectId);
    }

    @PostMapping("/preview/start")
    public Map<String, Object> previewStart(@AuthenticationPrincipal AppPrincipal principal,
                                            @PathVariable Long projectId,
                                            @RequestBody(required = false) Map<String, Object> body) {
        projects.requireWrite(projectId, principal);
        String command = body != null && body.get("command") instanceof String s && !s.isBlank()
                ? s : detectStartCommand(workspaceDir(projectId));
        preview.stop(projectId);
        int port = preview.start(projectId, workspaceDir(projectId), command);
        return Map.of("running", true, "port", port, "url", "http://localhost:" + port);
    }

    @PostMapping("/preview/stop")
    public Map<String, Object> previewStop(@AuthenticationPrincipal AppPrincipal principal,
                                           @PathVariable Long projectId) {
        projects.requireWrite(projectId, principal);
        preview.stop(projectId);
        return Map.of("running", false);
    }

    @GetMapping("/preview/status")
    public Map<String, Object> previewStatus(@AuthenticationPrincipal AppPrincipal principal,
                                             @PathVariable Long projectId) {
        projects.requireRead(projectId, principal);
        boolean running = preview.isRunning(projectId);
        Integer port = preview.portOf(projectId);
        return Map.of("running", running, "port", port == null ? -1 : port);
    }

    private File workspaceDir(Long projectId) {
        return java.nio.file.Path.of("/workspace/storage/projects", String.valueOf(projectId)).toFile();
    }

    private String detectStartCommand(File dir) {
        File[] jars = new File(dir, "target").listFiles((d, n) -> n.endsWith(".jar"));
        if (jars != null && jars.length > 0) return "java -jar target/" + jars[0].getName();
        return "npm start";
    }

    private record BuildOutcomeHelper(Long id, String status) {}
}
