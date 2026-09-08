package dev.aidev.project;

import dev.aidev.common.ForbiddenException;
import dev.aidev.common.NotFoundException;
import dev.aidev.git.GitService;
import dev.aidev.security.AppPrincipal;
import dev.aidev.workspace.FileTreeService;
import dev.aidev.workspace.WorkspaceService;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final WorkspaceService workspaceService;
    private final FileTreeService fileTreeService;
    private final GitService gitService;
    private final SecureRandom random = new SecureRandom();

    public ProjectService(ProjectRepository projectRepository,
                          ProjectMemberRepository memberRepository,
                          WorkspaceService workspaceService,
                          FileTreeService fileTreeService,
                          GitService gitService) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.workspaceService = workspaceService;
        this.fileTreeService = fileTreeService;
        this.gitService = gitService;
    }

    // ---------- access helpers ----------

    public Project requireProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
    }

    /** OWNER/DEVELOPER/SUPER_ADMIN read; VIEWER read-only. */
    public Project requireRead(Long projectId, AppPrincipal principal) {
        Project project = requireProject(projectId);
        if (isSuperAdmin(principal)) return project;
        if (project.getOwnerId().equals(principal.userId())) return project;
        boolean member = memberRepository.findByProjectIdAndUserId(projectId, principal.userId()).isPresent();
        if (!member) throw new ForbiddenException("You are not a member of this project");
        return project;
    }

    /** Write requires OWNER or DEVELOPER (not VIEWER) or SUPER_ADMIN. */
    public Project requireWrite(Long projectId, AppPrincipal principal) {
        Project project = requireRead(projectId, principal);
        if (isSuperAdmin(principal)) return project;
        if (project.getOwnerId().equals(principal.userId())) return project;
        String role = memberRepository.findByProjectIdAndUserId(projectId, principal.userId())
                .map(ProjectMember::getRole).orElse(null);
        if (role == null || "VIEWER".equals(role)) {
            throw new ForbiddenException("Read-only access to this project");
        }
        return project;
    }

    private boolean isSuperAdmin(AppPrincipal principal) {
        return principal != null && "SUPER_ADMIN".equals(principal.role());
    }

    // ---------- lifecycle ----------

    @Transactional
    public Project create(AppPrincipal principal, String name, String description,
                          List<String> techStack, String aiModel) {
        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        project.setOwnerId(principal.userId());
        project.setAiModel(aiModel == null ? "default" : aiModel);
        if (techStack != null) {
            var arr = new com.fasterxml.jackson.databind.node.ArrayNode(
                    com.fasterxml.jackson.databind.node.JsonNodeFactory.instance);
            techStack.forEach(arr::add);
            project.setTechStack(arr.toString());
        }
        byte[] token = new byte[24];
        random.nextBytes(token);
        project.setPreviewToken(HexFormat.of().formatHex(token));

        project = projectRepository.save(project);

        ProjectMember owner = new ProjectMember();
        owner.setProject(project);
        owner.setUserId(principal.userId());
        owner.setRole("OWNER");
        memberRepository.save(owner);

        Path dir = workspaceService.ensureWorkspace(project.getId());
        try (Git git = gitService.init(dir)) {
            Files.writeString(dir.resolve("README.md"), "# " + name + "\n\n" +
                    (description == null ? "" : description + "\n"));
            gitService.commitAll(git, "chore: init", principal.email(), "agent@aidev.local");
        } catch (Exception e) {
            log.warn("Git init failed (non-fatal): {}", e.getMessage());
        }
        log.info("Created project {} '{}' for user {}", project.getId(), name, principal.userId());
        return project;
    }

    @Transactional
    public Project duplicate(Long projectId, AppPrincipal principal) {
        Project source = requireWrite(projectId, principal);
        Project copy = new Project();
        copy.setName(source.getName() + " (copy)");
        copy.setDescription(source.getDescription());
        copy.setOwnerId(principal.userId());
        copy.setTechStack(source.getTechStack());
        copy.setArchitecture(source.getArchitecture());
        copy.setRequirements(source.getRequirements());
        copy.setAiModel(source.getAiModel());
        byte[] token = new byte[24];
        random.nextBytes(token);
        copy.setPreviewToken(HexFormat.of().formatHex(token));
        copy = projectRepository.save(copy);

        ProjectMember owner = new ProjectMember();
        owner.setProject(copy);
        owner.setUserId(principal.userId());
        owner.setRole("OWNER");
        memberRepository.save(owner);

        Path srcDir = workspaceService.projectDir(projectId);
        Path dstDir = workspaceService.ensureWorkspace(copy.getId());
        if (workspaceService.workspaceExists(projectId)) {
            try (Stream<Path> walk = Files.walk(srcDir)) {
                walk.forEach(src -> {
                    try {
                        Path rel = srcDir.relativize(src);
                        if (rel.startsWith(".git")) return;
                        Path dst = dstDir.resolve(rel.toString());
                        if (Files.isDirectory(src)) {
                            Files.createDirectories(dst);
                        } else {
                            Files.createDirectories(dst.getParent());
                            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (Exception ignored) {}
                });
            } catch (Exception ignored) {}
            try (Git git = gitService.init(dstDir)) {
                gitService.commitAll(git, "chore: duplicated from project " + projectId, principal.email(), "agent@aidev.local");
            } catch (Exception ignored) {}
        }
        return copy;
    }

    @Transactional
    public Project rename(Long projectId, AppPrincipal principal, String name, String description) {
        Project project = requireWrite(projectId, principal);
        if (name != null && !name.isBlank()) project.setName(name);
        if (description != null) project.setDescription(description);
        return projectRepository.save(project);
    }

    @Transactional
    public Project archive(Long projectId, AppPrincipal principal, boolean archived) {
        Project project = requireWrite(projectId, principal);
        project.setStatus(archived ? "ARCHIVED" : "ACTIVE");
        return projectRepository.save(project);
    }

    @Transactional
    public void delete(Long projectId, AppPrincipal principal) {
        Project project = requireProject(projectId);
        boolean owner = project.getOwnerId().equals(principal.userId());
        if (!owner && !isSuperAdmin(principal)) {
            throw new ForbiddenException("Only the owner or a super admin can delete this project");
        }
        workspaceService.deleteWorkspace(projectId);
        projectRepository.delete(project);
        log.info("Deleted project {}", projectId);
    }

    public Map<String, Object> fileTree(Long projectId, AppPrincipal principal) {
        Project project = requireRead(projectId, principal);
        try {
            Path dir = workspaceService.projectDir(projectId);
            var tree = fileTreeService.buildTree(projectId, dir);
            project.setFileTree(tree.toString());
            projectRepository.save(project);
            return mapperToMap(tree);
        } catch (Exception e) {
            throw new IllegalStateException("Could not build file tree: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> mapperToMap(com.fasterxml.jackson.databind.JsonNode node) {
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .convertValue(node, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
    }

    public Page<Project> listFor(AppPrincipal principal, String status, String search, Pageable pageable) {
        String st = (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) ? null : status.toUpperCase();
        String se = (search == null || search.isBlank()) ? null : search;
        return projectRepository.findAccessible(principal.userId(), st, se, pageable);
    }

    public List<Project> recentFor(AppPrincipal principal) {
        return projectRepository.findRecent(principal.userId(), PageRequest.of(0, 5));
    }
}
