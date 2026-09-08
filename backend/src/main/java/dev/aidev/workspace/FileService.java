package dev.aidev.workspace;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * File operations inside a project workspace (create/read/update/delete/search).
 * All paths are resolved through WorkspaceService (traversal-proof).
 */
@Service
public class FileService {

    private static final long MAX_READ_BYTES = 2 * 1024 * 1024; // 2MB
    private static final int MAX_SEARCH_RESULTS = 200;

    private final WorkspaceService workspaceService;

    public FileService(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    public record FileContent(String path, String content, long size, boolean binary) {}
    public record SearchResult(String path, int line, String text) {}
    public record WriteResult(String path, boolean created, long size) {}

    public FileContent read(Long projectId, String relPath) {
        Path path = workspaceService.resolveAndValidate(projectId, relPath);
        if (!Files.isRegularFile(path)) {
            throw new dev.aidev.common.NotFoundException("File not found: " + relPath);
        }
        try {
            long size = Files.size(path);
            if (looksBinary(path)) {
                return new FileContent(relPath, null, size, true);
            }
            String content = Files.readString(path, StandardCharsets.UTF_8);
            if (content.length() > MAX_READ_BYTES) {
                content = content.substring(0, (int) MAX_READ_BYTES) + "\n... [truncated]";
            }
            return new FileContent(relPath, content, size, false);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read file: " + relPath, e);
        }
    }

    public WriteResult write(Long projectId, String relPath, String content) {
        Path path = workspaceService.resolveAndValidate(projectId, relPath);
        boolean created = !Files.exists(path);
        try {
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            Files.writeString(path, content == null ? "" : content, StandardCharsets.UTF_8);
            return new WriteResult(relPath, created, Files.size(path));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write file: " + relPath, e);
        }
    }

    public boolean createDirectory(Long projectId, String relPath) {
        Path path = workspaceService.resolveAndValidate(projectId, relPath);
        try {
            Files.createDirectories(path);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create directory: " + relPath, e);
        }
    }

    public boolean delete(Long projectId, String relPath) {
        Path path = workspaceService.resolveAndValidate(projectId, relPath);
        if (!Files.exists(path)) {
            throw new dev.aidev.common.NotFoundException("Path not found: " + relPath);
        }
        try {
            if (Files.isDirectory(path)) {
                try (var walk = Files.walk(path)) {
                    walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
                }
            } else {
                Files.deleteIfExists(path);
            }
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot delete: " + relPath, e);
        }
    }

    public List<SearchResult> search(Long projectId, String query, String glob) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query is required");
        }
        List<SearchResult> results = new ArrayList<>();
        Path root = workspaceService.projectDir(projectId);
        if (!Files.isDirectory(root)) return results;
        String needle = query.toLowerCase();
        try (var walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> !isSkipped(p))
                    .filter(p -> glob == null || glob.isBlank() || matchesGlob(p.getFileName().toString(), glob))
                    .forEach(p -> {
                        if (results.size() >= MAX_SEARCH_RESULTS) return;
                        if (looksBinary(p)) return;
                        String rel = root.relativize(p).toString().replace('\\', '/');
                        try {
                            List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
                            for (int i = 0; i < lines.size() && results.size() < MAX_SEARCH_RESULTS; i++) {
                                if (lines.get(i).toLowerCase().contains(needle)) {
                                    results.add(new SearchResult(rel, i + 1, lines.get(i).trim()));
                                }
                            }
                        } catch (IOException ignored) {}
                    });
        } catch (IOException e) {
            throw new IllegalStateException("Search failed", e);
        }
        return results;
    }

    private boolean looksBinary(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                name.endsWith(".gif") || name.endsWith(".ico") || name.endsWith(".woff") ||
                name.endsWith(".woff2") || name.endsWith(".ttf") || name.endsWith(".eot") ||
                name.endsWith(".zip") || name.endsWith(".jar") || name.endsWith(".class")) {
            return true;
        }
        try {
            byte[] head = new byte[Math.toIntExact(Math.min(2048, Files.size(path)))];
            try (var in = Files.newInputStream(path)) {
                int read = in.read(head);
                for (int i = 0; i < read; i++) {
                    if (head[i] == 0) return true;
                }
            }
        } catch (IOException e) {
            return true;
        }
        return false;
    }

    private boolean isSkipped(Path p) {
        for (Path part : p) {
            String s = part.toString();
            if (s.equals("node_modules") || s.equals(".git") || s.equals("target") ||
                    s.equals("dist") || s.equals("build") || s.equals(".gradle") ||
                    s.equals("__pycache__") || s.equals(".venv")) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesGlob(String name, String glob) {
        // simple wildcard match: support *.ext
        if (glob.startsWith("*.")) return name.toLowerCase().endsWith(glob.substring(1).toLowerCase());
        if (glob.startsWith("*")) return name.toLowerCase().contains(glob.substring(1).toLowerCase());
        return name.equalsIgnoreCase(glob);
    }
}
