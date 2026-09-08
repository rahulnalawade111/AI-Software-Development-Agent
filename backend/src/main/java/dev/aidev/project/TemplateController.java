package dev.aidev.project;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.aidev.security.AppPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ProjectService projectService;
    private final dev.aidev.workspace.WorkspaceService workspaceService;

    public TemplateController(ProjectService projectService,
                              dev.aidev.workspace.WorkspaceService workspaceService) {
        this.projectService = projectService;
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public List<Map<String, Object>> list() throws Exception {
        Path root = Paths.get("/workspace/storage/templates");
        List<Map<String, Object>> templates = new ArrayList<>();
        if (!Files.isDirectory(root)) return templates;
        try (var stream = Files.list(root)) {
            for (Path dir : stream.filter(Files::isDirectory).sorted().toList()) {
                Path manifest = dir.resolve("manifest.json");
                if (!Files.exists(manifest)) continue;
                JsonNode node = mapper.readTree(Files.readString(manifest));
                Map<String, Object> m = mapper.convertValue(node, Map.class);
                m.put("id", dir.getFileName().toString());
                long files;
                try (var walk = Files.walk(dir)) {
                    files = walk.filter(Files::isRegularFile).count();
                }
                m.put("files", files);
                templates.add(m);
            }
        }
        return templates;
    }

    public record FromTemplateRequest(String name, String description) {}

    @PostMapping("/{templateId}/create")
    public ResponseEntity<Project> createFromTemplate(@AuthenticationPrincipal AppPrincipal principal,
                                                      @PathVariable String templateId,
                                                      @RequestBody FromTemplateRequest req) throws Exception {
        Path templateDir = Paths.get("/workspace/storage/templates", templateId).normalize();
        if (!templateDir.startsWith(Paths.get("/workspace/storage/templates")) || !Files.isDirectory(templateDir)) {
            throw new dev.aidev.common.NotFoundException("Template not found: " + templateId);
        }
        JsonNode manifest = mapper.readTree(Files.readString(templateDir.resolve("manifest.json")));
        List<String> stack = new ArrayList<>();
        manifest.path("stack").forEach(s -> stack.add(s.asText()));

        Project project = projectService.create(principal, req.name(), req.description(), stack, "default");

        // copy template contents (minus manifest) into workspace
        Path dst = workspaceService.ensureWorkspace(project.getId());
        try (var walk = Files.walk(templateDir)) {
            walk.forEach(src -> {
                try {
                    Path rel = templateDir.relativize(src);
                    if (rel.toString().equals("manifest.json")) return;
                    Path target = dst.resolve(rel.toString());
                    if (Files.isDirectory(src)) Files.createDirectories(target);
                    else {
                        Files.createDirectories(target.getParent());
                        Files.copy(src, target);
                    }
                } catch (Exception ignored) {}
            });
        }
        return ResponseEntity.ok(project);
    }
}
