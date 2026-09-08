package dev.aidev.git;

import dev.aidev.project.ProjectService;
import dev.aidev.security.AppPrincipal;
import org.eclipse.jgit.api.Git;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** REST endpoints for the git panel: status, diff, log, commit. */
@RestController
@RequestMapping("/api/projects/{projectId}/git")
public class GitController {

    private final GitService git;
    private final ProjectService projects;

    public GitController(GitService git, ProjectService projects) {
        this.git = git;
        this.projects = projects;
    }

    @GetMapping("/status")
    public Map<String, Object> status(@AuthenticationPrincipal AppPrincipal principal,
                                      @PathVariable Long projectId) throws Exception {
        projects.requireRead(projectId, principal);
        var dir = dirOf(projectId);
        if (!dir.resolve(".git").toFile().isDirectory()) {
            return Map.of("initialized", false, "modified", List.of(), "added", List.of(), "deleted", List.of());
        }
        try (Git g = git.open(dir)) {
            Map<String, Object> status = git.status(g);
            status.putIfAbsent("initialized", true);
            return status;
        }
    }

    @GetMapping("/diff")
    public Map<String, Object> diff(@AuthenticationPrincipal AppPrincipal principal,
                                    @PathVariable Long projectId) throws Exception {
        projects.requireRead(projectId, principal);
        var dir = dirOf(projectId);
        if (!dir.resolve(".git").toFile().isDirectory()) {
            return Map.of("initialized", false, "diff", "");
        }
        try (Git g = git.open(dir)) {
            return Map.of("initialized", true, "diff", String.valueOf(git.diff(g)));
        }
    }

    @GetMapping("/log")
    public List<Map<String, Object>> log(@AuthenticationPrincipal AppPrincipal principal,
                                         @PathVariable Long projectId) throws Exception {
        projects.requireRead(projectId, principal);
        var dir = dirOf(projectId);
        if (!dir.resolve(".git").toFile().isDirectory()) {
            return List.of();
        }
        try (Git g = git.open(dir)) {
            return git.log(g, 50);
        }
    }

    @PostMapping("/commit")
    public Map<String, Object> commit(@AuthenticationPrincipal AppPrincipal principal,
                                      @PathVariable Long projectId,
                                      @RequestBody Map<String, String> body) throws Exception {
        projects.requireWrite(projectId, principal);
        var dir = dirOf(projectId);
        String message = body.get("message");
        if (message == null || message.isBlank()) message = "Checkpoint";
        Git g = dir.resolve(".git").toFile().isDirectory() ? git.open(dir) : git.init(dir);
        try (g) {
            git.commitAll(g, message, principal.email() == null ? "user" : principal.email(),
                    String.valueOf(principal.email()));
            Map<String, Object> status = git.status(g);
            status.put("committed", true);
            status.put("message", message);
            return status;
        }
    }

    private java.nio.file.Path dirOf(Long projectId) {
        return java.nio.file.Path.of("/workspace/storage/projects", String.valueOf(projectId));
    }
}
