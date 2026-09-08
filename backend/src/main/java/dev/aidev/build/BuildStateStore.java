package dev.aidev.build;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store of the most recent build/test run per project.
 */
@Service
public class BuildStateStore {

    private final Map<Long, Map<String, Object>> lastRuns = new ConcurrentHashMap<>();

    public void record(Long projectId, Map<String, Object> result) {
        lastRuns.put(projectId, result);
    }

    public Map<String, Object> lastRun(Long projectId) {
        return lastRuns.get(projectId);
    }

    public void clear(Long projectId) {
        lastRuns.remove(projectId);
    }
}
