package dev.aidev.ai.tools.impl;

import dev.aidev.ai.tools.AiTool;
import dev.aidev.ai.tools.ToolContext;
import dev.aidev.build.PreviewRegistry;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/** preview_application - starts the generated app and verifies it serves HTTP. */
@Component
public class PreviewApplicationTool implements AiTool {

    public String name() { return "preview_application"; }

    public String label() { return "Preview application"; }

    public String description() {
        return "Start the generated application (npm start / java -jar), detect the port and "
                + "verify it responds to HTTP. Returns the local URL. Only call after a successful build.";
    }

    public String parametersSchema() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"command\":{\"type\":\"string\",\"description\":\"Start command (default auto-detect)\"},"
                + "\"port\":{\"type\":\"integer\",\"description\":\"Expected port (default auto-detect)\"}},"
                + "\"required\":[]}";
    }

    public Object execute(Map<String, Object> args, ToolContext ctx) throws Exception {
        File dir = new File(ctx.workspaceRoot());
        String command = args.get("command") instanceof String s && !s.isBlank()
                ? s : detectStartCommand(dir);
        int port = args.get("port") instanceof Number n ? n.intValue() : 3000;

        PreviewRegistry preview = PreviewRegistry.instance();
        preview.stop(ctx.projectId());
        int actualPort = preview.start(ctx.projectId(), dir, command);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("running", true);
        out.put("command", command);
        out.put("port", actualPort);
        out.put("url", "http://localhost:" + actualPort);

        String probe = probe(actualPort);
        out.put("httpCheck", probe);
        if (probe.startsWith("ok")) {
            out.put("responds", true);
        } else {
            out.put("responds", false);
            out.put("hint", "App is starting; wait a few seconds and probe again with run_command curl.");
        }
        return out;
    }

    private String detectStartCommand(File dir) {
        if (new File(dir, "target").isDirectory()) {
            File[] jars = new File(dir, "target").listFiles((d, n) -> n.endsWith(".jar"));
            if (jars != null && jars.length > 0) {
                return "java -jar " + jars[0].getName();
            }
         }
        if (new File(dir, "package.json").isFile()) {
            return "npm start";
        }
        return "npm start";
    }

    private String probe(int port) {
        for (int i = 0; i < 20; i++) {
            try {
                HttpURLConnection c = (HttpURLConnection)
                        new URL("http://localhost:" + port + "/").openConnection();
                c.setConnectTimeout(1000);
                c.setReadTimeout(1000);
                int code = c.getResponseCode();
                return "ok " + code;
            } catch (Exception e) {
                try { Thread.sleep(1000); } catch (InterruptedException ie) { break; }
            }
        }
        return "no-response";
    }
}
