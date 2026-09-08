package dev.aidev.build;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

/**
 * Sandboxed command runner for the agent: allowlisted executables,
 * denylist patterns, no shell chaining, wall-clock timeout with
 * process-group destroy, 2 MB output cap.
 */
@Service
public class SandboxRunner {

    public record RunResult(int exitCode, String stdout, String stderr, long durationMs, boolean timedOut) {}

    private static final List<String> ALLOWED = List.of(
            "npm", "npx", "node", "yarn", "pnpm", "mvn", "mvnw", "java", "javac",
            "git", "mkdir", "ls", "cat", "pwd", "echo", "grep", "find", "head", "tail", "wc", "python3", "pip");

    private static final List<String> DENIED_PATTERNS = List.of(
            "rm -rf /", "rm -fr /", "mkfs", "format ", "shutdown", "reboot", "halt",
            "dd if=", ":(){", "curl", "wget", "/etc/shadow", "~/.ssh", "id_rsa",
            "chmod 777 /", "chown -R /", "> /dev/sd", "nc ", "ncat", "telnet");

    private static final long MAX_OUTPUT_BYTES = 2 * 1024 * 1024;

    public boolean isAllowed(String command) {
        String c = command.trim();
        if (c.isEmpty()) return false;
        String exe = c.split("\\s+")[0];
        String bare = exe.endsWith("/") ? exe.substring(exe.lastIndexOf('/') + 1) : exe;
        if (!ALLOWED.contains(bare)) return false;
        String lower = c.toLowerCase();
        for (String p : DENIED_PATTERNS) {
            if (lower.contains(p.toLowerCase())) return false;
        }
        if (lower.contains("&&") || lower.contains("||") || lower.contains(";")) {
            return false;
        }
        return true;
    }

    public RunResult run(File workingDir, String command, long timeoutSeconds) {
        Instant start = Instant.now();
        if (!isAllowed(command)) {
            return new RunResult(-1, "", "Command not allowed by sandbox: " + command, 0, false);
        }
        List<String> argv = parse(command);
        if (argv.isEmpty()) {
            return new RunResult(-1, "", "Empty command", 0, false);
        }
        Process p = null;
        try {
            p = new ProcessBuilder(argv).directory(workingDir).start();
            final Process proc = p;
            StringBuilder out = new StringBuilder();
            StringBuilder err = new StringBuilder();
            Thread tOut = readThread(proc.getInputStream(), out);
            Thread tErr = readThread(proc.getErrorStream(), err);
            boolean finished = proc.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            tOut.join(2000);
            tErr.join(2000);
            long took = Duration.between(start, Instant.now()).toMillis();
            if (!finished) {
                destroyGroup(proc);
                return new RunResult(-1, cap(out), cap(err) + "\n[TIMEOUT after " + timeoutSeconds + "s]", took, true);
            }
            return new RunResult(proc.exitValue(), cap(out), cap(err), took, false);
        } catch (IOException e) {
            return new RunResult(-1, "", "Failed to start command: " + e.getMessage(), 0, false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (p != null) destroyGroup(p);
            return new RunResult(-1, "", "Interrupted", 0, false);
        }
    }

    private void destroyGroup(Process proc) {
        try { proc.toHandle().descendants().forEach(h -> h.destroyForcibly()); } catch (Exception ignored) {}
        proc.destroyForcibly();
    }

    private Thread readThread(java.io.InputStream is, StringBuilder sb) {
        Thread t = new Thread(() -> {
            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (sb.length() < MAX_OUTPUT_BYTES) {
                        sb.append(line).append('\n');
                    }
                }
            } catch (IOException ignored) {}
        });
        t.setDaemon(true);
        t.start();
        return t;
    }

    private String cap(StringBuilder sb) {
        if (sb.length() <= MAX_OUTPUT_BYTES) return sb.toString();
        return sb.substring(0, (int) MAX_OUTPUT_BYTES) + "\n…[output truncated at 2MB]";
    }

    private List<String> parse(String command) {
        List<String> argv = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inSingle = false, inDouble = false;
        for (char c : command.toCharArray()) {
            if (c == '\'' && !inDouble) { inSingle = !inSingle; }
            else if (c == '"' && !inSingle) { inDouble = !inDouble; }
            else if (Character.isWhitespace(c) && !inSingle && !inDouble) {
                if (cur.length() > 0) { argv.add(cur.toString()); cur.setLength(0); }
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) argv.add(cur.toString());
        return argv;
    }
}
