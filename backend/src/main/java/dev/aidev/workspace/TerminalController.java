package dev.aidev.workspace;

import dev.aidev.project.ProjectService;
import dev.aidev.terminal.TerminalService;
import dev.aidev.security.AppPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** REST endpoints for the terminal panel (sandboxed command execution + history). */
@RestController
@RequestMapping("/api/projects/{projectId}/terminal")
public class TerminalController {

    private final TerminalService terminal;
    private final ProjectService projects;

    public TerminalController(TerminalService terminal, ProjectService projects) {
        this.terminal = terminal;
        this.projects = projects;
    }

    @PostMapping("/execute")
    public Map<String, Object> execute(@AuthenticationPrincipal AppPrincipal principal,
                                       @PathVariable Long projectId,
                                       @RequestBody Map<String, String> body) {
        projects.requireWrite(projectId, principal);
        String command = body.get("command");
        java.io.File dir = workspaceDir(projectId);
        var result = terminal.execute(projectId, principal.userId(), dir, command);
        return Map.of(
                "blocked", result.blocked(),
                "reason", String.valueOf(result.reason()),
                "exitCode", result.exitCode(),
                "stdout", String.valueOf(result.stdout()),
                "stderr", String.valueOf(result.stderr()),
                "durationMs", result.durationMs());
    }

    @GetMapping("/history")
    public List<Map<String, Object>> history(@AuthenticationPrincipal AppPrincipal principal,
                                             @PathVariable Long projectId) {
        projects.requireRead(projectId, principal);
        return terminal.history(projectId);
    }

    private java.io.File workspaceDir(Long projectId) {
        return java.nio.file.Path.of("/workspace/storage/projects", String.valueOf(projectId)).toFile();
    }
}
