package dev.aidev.workspace;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves and validates paths inside a project workspace.
 * All file operations must go through here — traversal-proof by canonicalization.
 */
@Service
public class WorkspaceService {

    private final Path workspaceRoot;

    public WorkspaceService(@Value("${app.workspace.root}") String workspaceRoot) {
        this.workspaceRoot = Paths.get(workspaceRoot);
    }

    public Path workspaceRoot() {
        return workspaceRoot;
    }

    public Path projectDir(Long projectId) {
        return workspaceRoot.resolve(String.valueOf(projectId));
    }

    /** Ensure the project directory exists. */
    public Path ensureWorkspace(Long projectId) {
        Path dir = projectDir(projectId);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create workspace for project " + projectId, e);
        }
        return dir;
    }

    /**
     * Resolve a relative path inside the project workspace. Throws IllegalArgumentException
     * on traversal attempts (../, absolute paths, symlink escapes).
     */
    public Path resolveAndValidate(Long projectId, String relativePath) {
        if (relativePath == null) relativePath = "";
        Path dir = projectDir(projectId);
        Path resolved = dir.resolve(relativePath).normalize();
        if (!resolved.startsWith(dir)) {
            throw new IllegalArgumentException("Path escapes project workspace: " + relativePath);
        }
        // symlink check: the parent chain must stay within the workspace
        Path current = dir;
        try {
            Path absDir = dir.toRealPath();
            Path absResolved = resolved.toFile().exists() ? resolved.toRealPath() : resolved.toAbsolutePath().normalize();
            if (!absResolved.startsWith(absDir)) {
                throw new IllegalArgumentException("Path escapes project workspace (symlink?): " + relativePath);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot resolve path: " + relativePath);
        }
        return resolved;
    }

    public void deleteWorkspace(Long projectId) {
        Path dir = projectDir(projectId);
        if (Files.exists(dir)) {
            try (var walk = Files.walk(dir)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
            } catch (IOException e) {
                throw new IllegalStateException("Could not delete workspace", e);
            }
        }
    }

    public boolean workspaceExists(Long projectId) {
        return Files.isDirectory(projectDir(projectId));
    }
}
