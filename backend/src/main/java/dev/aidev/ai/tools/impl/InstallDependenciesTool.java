package dev.aidev.ai.tools.impl;

import dev.aidev.ai.tools.AiTool;
import dev.aidev.ai.tools.ToolContext;
import dev.aidev.terminal.TerminalService;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/** install_dependencies — runs npm install / maven resolve in the project workspace. */
@Component
public class InstallDependenciesTool implements AiTool {

    private final TerminalService terminal;

    public InstallDependenciesTool(TerminalService terminal) {
        this.terminal = terminal;
    }

    @Override
    public String name() { return "install_dependencies"; }

    @Override
    public String label() { return "Install dependencies"; }

    @Override
    public String description() {
        return "Install project dependencies (npm install for frontend, mvn dependency:go-offline for Java). "
                + "Run once after scaffolding package.json / pom.xml or after editing dependencies.";
    }

    @Override
    public String parametersSchema() {
        return """
                {"type":"object","properties":{
                  "target":{"type":"string","enum":["frontend","backend","all"],"description":"Side to install for (default all)"}},
                  "required":[]}""";
    }

    @Override
    public Object execute(Map<String, Object> args, ToolContext ctx) throws Exception {
        String target = args.get("target") instanceof String s && !s.isBlank() ? s : "all";
        File workspace = new File(ctx.workspaceRoot());
        boolean hasFrontend = new File(workspace, "package.json").isFile()
                || new File(workspace, "frontend/package.json").isFile();
        boolean hasBackend = new File(workspace, "pom.xml").isFile()
                || new File(workspace, "backend/pom.xml").isFile();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("target", target);
        java.util.List<Map<String, Object>> commands = new java.util.ArrayList<>();
        if (("all".equals(target) || "frontend".equals(target)) && hasFrontend) {
            File dir = new File(workspace, "package.json").isFile()
                    ? workspace : new File(workspace, "frontend");
            commands.add(run(dir, "npm install --no-audit --no-fund"));
        }
        if (("all".equals(target) || "backend".equals(target)) && hasBackend) {
            File dir = new File(workspace, "pom.xml").isFile() ? workspace : new File(workspace, "backend");
            String mvn = new File(dir, "mvnw").isFile() ? "./mvnw" : "mvn";
            commands.add(run(dir, mvn + " dependency:go-offline -q"));
        }
        out.put("commands", commands);
        return out;
    }

    private Map<String, Object> run(File dir, String command) {
        TerminalService.TerminalResult r = terminal.execute(null, null, dir, command);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("command", command);
        m.put("exitCode", r.exitCode());
        m.put("stdout", truncate(r.stdout()));
        m.put("stderr", truncate(r.stderr()));
        m.put("durationMs", r.durationMs());
        return m;
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > 6000 ? s.substring(0, 6000) + "…[truncated]" : s;
    }
}
