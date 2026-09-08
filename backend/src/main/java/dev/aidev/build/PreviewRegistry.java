package dev.aidev.build;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of running preview processes per project. Static singleton
 * (not a Spring bean) so tools and controllers share one instance.
 */
public final class PreviewRegistry {

    private static final PreviewRegistry INSTANCE = new PreviewRegistry();

    private final Map<Long, Process> processes = new ConcurrentHashMap<>();
    private final Map<Long, Integer> ports = new ConcurrentHashMap<>();

    private PreviewRegistry() {}

    public static PreviewRegistry instance() { return INSTANCE; }

    private static String quote(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }

    /** Starts the command if not already running; returns the assigned port. */
    public synchronized int start(Long projectId, File dir, String command) {
        Process existing = processes.get(projectId);
        if (existing != null && existing.isAlive()) {
            return ports.getOrDefault(projectId, 0);
        }
        int port = 20000 + (int) (projectId % 10000);
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-lc",
                "cd " + quote(dir.getAbsolutePath()) + " && " + command);
        pb.environment().put("PORT", String.valueOf(port));
        pb.environment().put("SERVER_PORT", String.valueOf(port));
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(
                new File("/tmp/preview-p" + projectId + ".log")));
        try {
            Process p = pb.start();
            processes.put(projectId, p);
            ports.put(projectId, port);
            return port;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start preview: " + e.getMessage(), e);
        }
    }

    public synchronized void stop(Long projectId) {
        Process p = processes.remove(projectId);
        ports.remove(projectId);
        if (p != null) {
            p.toHandle().descendants().forEach(h -> h.destroyForcibly());
            p.destroyForcibly();
        }
    }

    public synchronized boolean isRunning(Long projectId) {
        Process p = processes.get(projectId);
        return p != null && p.isAlive();
    }

    public synchronized Integer portOf(Long projectId) {
        return ports.get(projectId);
    }
}
