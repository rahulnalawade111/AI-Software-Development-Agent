package dev.aidev.workspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Walks a project workspace and builds a nested file tree DTO,
 * skipping heavy/noisy directories.
 */
@Service
public class FileTreeService {

    private static final Set<String> SKIP_DIRS = Set.of(
            "node_modules", "target", ".git", "dist", "build", ".gradle", ".idea", "__pycache__", ".venv");
    private static final int MAX_DEPTH = 12;
    private static final int MAX_ENTRIES = 2000;

    private final ObjectMapper mapper = new ObjectMapper();

    public ObjectNode buildTree(Long projectId, Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return mapper.createObjectNode();
        }
        ObjectNode node = mapper.createObjectNode();
        node.put("name", root.getFileName().toString());
        node.put("path", "");
        node.put("type", "directory");
        ArrayNode children = mapper.createArrayNode();
        node.set("children", children);
        try (Stream<Path> stream = Files.list(root)) {
            List<Path> entries = stream
                    .filter(p -> !SKIP_DIRS.contains(p.getFileName().toString()))
                    .sorted(Comparator.comparing((Path p) -> !Files.isDirectory(p)).thenComparing(p -> p.getFileName().toString().toLowerCase()))
                    .toList();
            for (Path entry : entries) {
                children.add(buildNode(entry, entry.getFileName().toString(), 1));
            }
        }
        return node;
    }

    private ObjectNode buildNode(Path path, String relPath, int depth) throws IOException {
        ObjectNode node = mapper.createObjectNode();
        node.put("name", path.getFileName().toString());
        node.put("path", relPath);
        if (Files.isDirectory(path)) {
            node.put("type", "directory");
            ArrayNode children = mapper.createArrayNode();
            node.set("children", children);
            if (depth < MAX_DEPTH) {
                try (Stream<Path> stream = Files.list(path)) {
                    List<Path> entries = stream
                            .filter(p -> !SKIP_DIRS.contains(p.getFileName().toString()))
                            .sorted(Comparator.comparing((Path p) -> !Files.isDirectory(p)).thenComparing(p -> p.getFileName().toString().toLowerCase()))
                            .limit(Math.max(0, MAX_ENTRIES))
                            .toList();
                    for (Path entry : entries) {
                        children.add(buildNode(entry, relPath + "/" + entry.getFileName(), depth + 1));
                    }
                }
            }
        } else {
            node.put("type", "file");
            node.put("size", Files.size(path));
            String name = path.getFileName().toString();
            int dot = name.lastIndexOf('.');
            if (dot > 0) node.put("language", languageFor(name.substring(dot + 1)));
        }
        return node;
    }

    public static String languageFor(String ext) {
        return switch (ext.toLowerCase()) {
            case "js", "mjs", "cjs" -> "javascript";
            case "jsx" -> "javascriptreact";
            case "ts" -> "typescript";
            case "tsx" -> "typescriptreact";
            case "html" -> "html";
            case "css" -> "css";
            case "scss" -> "scss";
            case "java" -> "java";
            case "sql" -> "sql";
            case "json" -> "json";
            case "yml", "yaml" -> "yaml";
            case "xml" -> "xml";
            case "md" -> "markdown";
            case "py" -> "python";
            case "properties" -> "properties";
            default -> "plaintext";
        };
    }
}
