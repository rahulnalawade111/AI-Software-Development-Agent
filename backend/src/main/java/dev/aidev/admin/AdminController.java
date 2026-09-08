package dev.aidev.admin;

import dev.aidev.ai.AiActionRepository;
import dev.aidev.ai.AiConversationRepository;
import dev.aidev.ai.AiMessageRepository;
import dev.aidev.project.ProjectRepository;
import dev.aidev.security.AppPrincipal;
import dev.aidev.user.RoleRepository;
import dev.aidev.user.User;
import dev.aidev.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Super Admin console API. Every endpoint requires the SUPER_ADMIN role. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

    private final UserRepository users;
    private final ProjectRepository projects;
    private final AiActionRepository aiActions;
    private final AiConversationRepository conversations;
    private final AiMessageRepository aiMessages;
    private final RoleRepository roles;

    public AdminController(UserRepository users, ProjectRepository projects,
                           AiActionRepository aiActions,
                           AiConversationRepository conversations,
                           AiMessageRepository aiMessages,
                           RoleRepository roles) {
        this.users = users;
        this.projects = projects;
        this.aiActions = aiActions;
        this.conversations = conversations;
        this.aiMessages = aiMessages;
        this.roles = roles;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("users", users.count());
        out.put("projects", projects.count());
        out.put("conversations", conversations.count());
        out.put("aiMessages", aiMessages.count());
        out.put("aiActions", aiActions.count());
        return out;
    }

    @GetMapping("/users")
    public Page<Map<String, Object>> listUsers(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return users.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")))
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", u.getId());
                    m.put("email", u.getEmail());
                    m.put("name", u.getName());
                    m.put("roles", u.getRoles().stream().map(r -> r.getName()).toList());
                    m.put("createdAt", String.valueOf(u.getCreatedAt()));
                    return m;
                });
    }

    @PutMapping("/users/{id}/role")
    public Map<String, Object> setUserRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User u = users.findById(id).orElseThrow();
        String role = body.getOrDefault("role", "USER");
        u.getRoles().clear();
        u.getRoles().add(roles.findByName("SUPER_ADMIN".equals(role) ? "SUPER_ADMIN" : "USER").orElseThrow());
        users.save(u);
        return Map.of("id", id, "role", role);
    }

    @DeleteMapping("/users/{id}")
    public Map<String, Object> deleteUser(@PathVariable Long id,
                                          @AuthenticationPrincipal AppPrincipal principal) {
        if (id.equals(principal.userId())) {
            throw new IllegalArgumentException("Cannot delete your own account");
        }
        users.deleteById(id);
        return Map.of("deleted", id);
    }

    @GetMapping("/projects")
    public Page<Map<String, Object>> listProjects(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return projects.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")))
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", p.getId());
                    m.put("name", p.getName());
                    m.put("status", String.valueOf(p.getStatus()));
                    m.put("ownerId", p.getOwnerId());
                    m.put("createdAt", String.valueOf(p.getCreatedAt()));
                    return m;
                });
    }

    @GetMapping("/ai/actions")
    public List<Map<String, Object>> aiActions(@RequestParam(defaultValue = "50") int size) {
        int limit = Math.min(Math.max(size, 1), 200);
        return aiActions.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id")))
                .stream()
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", a.getId());
                    m.put("projectId", a.getProjectId());
                    m.put("type", a.getType());
                    m.put("label", a.getLabel());
                    m.put("status", a.getStatus());
                    m.put("createdAt", String.valueOf(a.getCreatedAt()));
                    return m;
                })
                .toList();
    }
}
