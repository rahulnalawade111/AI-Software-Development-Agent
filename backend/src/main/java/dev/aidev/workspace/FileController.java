package dev.aidev.workspace;

import dev.aidev.project.ProjectService;
import dev.aidev.security.AppPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/files")
public class FileController {

    private final ProjectService projectService;
    private final FileService fileService;
    private final FileSearchService fileSearchService;

    public FileController(ProjectService projectService,
                          FileService fileService,
                          FileSearchService fileSearchService) {
        this.projectService = projectService;
        this.fileService = fileService;
        this.fileSearchService = fileSearchService;
    }

    // ---- IDE endpoints ----

    @GetMapping
    public Map<String, Object> read(@AuthenticationPrincipal AppPrincipal principal,
                                    @PathVariable Long projectId,
                                    @RequestParam String path) {
        projectService.requireRead(projectId, principal);
        FileService.FileContent fc = fileService.read(projectId, path);
        return Map.of("path", fc.path(), "content", fc.content() == null ? "" : fc.content(),
                "size", fc.size(), "binary", fc.binary());
    }

    @PutMapping
    public FileService.WriteResult write(@AuthenticationPrincipal AppPrincipal principal,
                                         @PathVariable Long projectId,
                                         @RequestBody Map<String, String> body) {
        projectService.requireWrite(projectId, principal);
        String path = required(body, "path");
        String content = body.getOrDefault("content", "");
        return fileService.write(projectId, path, content);
    }

    @PostMapping
    public FileService.WriteResult create(@AuthenticationPrincipal AppPrincipal principal,
                                          @PathVariable Long projectId,
                                          @RequestBody Map<String, String> body) {
        projectService.requireWrite(projectId, principal);
        String path = required(body, "path");
        String content = body.getOrDefault("content", "");
        return fileService.write(projectId, path, content);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppPrincipal principal,
                                       @PathVariable Long projectId,
                                       @RequestParam String path) {
        projectService.requireWrite(projectId, principal);
        fileService.delete(projectId, path);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mkdir")
    public Map<String, Object> mkdir(@AuthenticationPrincipal AppPrincipal principal,
                                     @PathVariable Long projectId,
                                     @RequestBody Map<String, String> body) {
        projectService.requireWrite(projectId, principal);
        return Map.of("ok", fileService.createDirectory(projectId, required(body, "path")));
    }

    @GetMapping("/search")
    public List<FileService.SearchResult> search(@AuthenticationPrincipal AppPrincipal principal,
                                                 @PathVariable Long projectId,
                                                 @RequestParam String query,
                                                 @RequestParam(required = false) String glob) {
        projectService.requireRead(projectId, principal);
        return fileSearchService.search(projectId, query, glob);
   }

    private static String required(Map<String, String> body, String key) {
        String v = body.get(key);
        if (v == null || v.isBlank()) throw new IllegalArgumentException(key + " is required");
        return v;
    }
}
