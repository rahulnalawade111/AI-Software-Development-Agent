package dev.aidev.ai.tools.impl;

import dev.aidev.ai.tools.AiTool;
import dev.aidev.ai.tools.ToolContext;
import dev.aidev.build.BuildService;
import dev.aidev.terminal.TerminalService;
import dev.aidev.terminal.TerminalRun;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/** build_project — runs the project build (npm run build / mvn package) via BuildService. */
@Component
public class BuildProjectTool implements AiTool {

    private final BuildService builds;

    public BuildProjectTool(BuildService builds) {
        this.builds = builds;
    }

    @Override
    public String name() { return "build_project"; }

    @Override
    public String label() { return "Build project"; }

    @Override
    public String description() {
        return "Run the project build (npm run build for frontend, mvn package -DskipTests for Java backend). "
                + "Returns exit code, parsed errors and the attempt number. Use after creating or changing files, "
                + "then fix errors before declaring success.";
    }

    @Override
    public String parametersSchema() {
        return """
                {"type":"object","properties":{
                  "target":{"type":"string","enum":["frontend","backend","all"],"description":"Which side to build (default all)"}},
                  "required":[]}""";
    }

    @Override
    public Object execute(Map<String, Object> args, ToolContext ctx) throws Exception {
        String target = args.get("target") instanceof String s && !s.isBlank() ? s : "all";
        Map<String, Object> out = new LinkedHashMap<>();
        dev.aidev.build.BuildService.BuildOutcome bo = builds.build(
                ctx.projectId(), ctx.conversationId(), new File(ctx.workspaceRoot()),
                target.toUpperCase(), "AI", nextAttempt(ctx.projectId()), null);
        out.put("attempt", bo.attempt());
        out.put("target", bo.target());
        out.put("counterCommand", bo.command());
        out.put("command", bo.command());
        out.put("success", bo.success());
        out.put("exitCode", bo.exitCode());
        out.put("output", truncate(bo.output()));
        out.put("errors", bo.errors());
        out.put("durationMs", bo.durationMs());
        return out;
    }

    private int nextAttempt(Long projectId) {
        try {
            java.util.List<Map<String, Object>> history = builds.history(projectId);
            return history.isEmpty() ? 1 : (int) history.get(0).get("attempt") + 1;
        } catch (Exception e) {
            return 1;
        }
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > 6000 ? s.substring(0, 6000) + "…[truncated]" : s;
    }
}
