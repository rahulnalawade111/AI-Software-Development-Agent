package dev.aidev.workspace;

import org.springframework.stereotype.Service;

/**
 * Grep-like content search across a project workspace.
 * (Kept separate from FileService so the AI search_code tool and the IDE
 * search box share one implementation.)
 */
@Service
public class FileSearchService {

    private final FileService fileService;

    public FileSearchService(FileService fileService) {
        this.fileService = fileService;
    }

    public java.util.List<FileService.SearchResult> search(Long projectId, String query, String glob) {
        return fileService.search(projectId, query, glob);
    }
}
