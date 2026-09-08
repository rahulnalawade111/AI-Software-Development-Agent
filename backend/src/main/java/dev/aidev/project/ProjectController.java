package dev.aidev.project;

import dev.aidev.security.AppPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    public record CreateProjectRequest(String name, String description, List<String> techStack, String aiModel) {}
    public record UpdateProjectRequest(String name, String description) {}
    public record ArchiveRequest(boolean archived) {}

    @PostMapping
    public ResponseEntity<Project> create(@AuthenticationPrincipal AppPrincipal principal,
                                          @RequestBody CreateProjectRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new IllegalArgumentException("Project name is required");
        }
        Project project = projectService.create(principal, req.name().trim(), req.description(),
                req.techStack(), req.aiModel());
        return ResponseEntity.ok(project);
    }

    @GetMapping
    public Page<Project> list(@AuthenticationPrincipal AppPrincipal principal,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false) String search) {
        return projectService.listFor(principal, status, search, PageRequest.of(page, size));
    }

    @GetMapping("/recent")
    public List<Project> recent(@AuthenticationPrincipal AppPrincipal principal) {
        return projectService.recentFor(principal);
    }

    @GetMapping("/{id}")
    public Project get(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
        return projectService.requireRead(id, principal);
    }

    @GetMapping("/{id}/file-tree")
    public Map<String, Object> fileTree(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
        return projectService.fileTree(id, principal);
    }

    @PostMapping("/{id}/duplicate")
    public Project duplicate(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
        return projectService.duplicate(id, principal);
    }

    @PatchMapping("/{id}")
    public Project update(@AuthenticationPrincipal AppPrincipal principal,
                          @PathVariable Long id,
                          @RequestBody UpdateProjectRequest req) {
        return projectService.rename(id, principal, req.name(), req.description());
    }

    @PatchMapping("/{id}/archive")
    public Project archive(@AuthenticationPrincipal AppPrincipal principal,
                           @PathVariable Long id,
                           @RequestBody ArchiveRequest req) {
        return projectService.archive(id, principal, req.archived());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
        projectService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }
}
