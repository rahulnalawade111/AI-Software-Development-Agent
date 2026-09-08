package dev.aidev.ai.tools.impl;

import dev.aidev.ai.tools.AiTool;
import dev.aidev.ai.tools.ToolContext;
import dev.aidev.build.BuildService;
import org.springframework.stereotype.Component;

import java.util.Map;

/** get_build_errors - returns parsed errors from the most recent build. */
@Component
public class GetBuildErrorsTool implements AiTool {

    private final BuildService builds;

    public GetBuildErrorsTool(BuildService builds) {
        this.builds = builds;
    }

    public String name() { return "get_build_errors"; }

    public String label() { return "Get build errors"; }

    public String description() {
        return "Get the parsed error list and raw output from the most recent build attempt "
                + "without rebuilding. Use when you need to re-inspect errors before fixing them.";
    }

    public String parametersSchema() {
        return "{\"type\":\"object\",\"properties\":{},\"required\":[]}";
    }

    public Object execute(Map<String, Object> args, ToolContext ctx) throws Exception {
        return builds.lastBuild(ctx.projectId());
    }
}
