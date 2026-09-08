package dev.aidev.ai.tools.impl;

import dev.aidev.ai.tools.AiTool;
import dev.aidev.ai.tools.ToolContext;
import dev.aidev.build.TestService;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/** run_tests - runs the project test suite (npm test / mvn test). */
@Component
public class RunTestsTool implements AiTool {

    private final TestService tests;

    public RunTestsTool(TestService tests) {
        this.tests = tests;
    }

    public String name() { return "run_tests"; }

    public String label() { return "Run tests"; }

    public String description() {
        return "Run the project test suite (npm test for frontend, mvn test for Java backend). "
                + "Returns pass/fail counts and failure details.";
    }

    public String parametersSchema() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"target\":{\"type\":\"string\",\"enum\":[\"frontend\",\"backend\",\"all\"],"
                + "\"description\":\"Which side to test (default all)\"}},\"required\":[]}";
    }

    public Object execute(Map<String, Object> args, ToolContext ctx) throws Exception {
        String target = str(args.get("target"), "all");
        TestService.TestOutcome to = tests.runTests(ctx.projectId(), ctx.conversationId(),
                new File(ctx.workspaceRoot()), target.toUpperCase(), null);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("command", to.command());
        out.put("success", to.success());
        out.put("total", to.total());
        out.put("passed", to.passed());
        out.put("failed", to.failed());
        out.put("skipped", to.skipped());
        out.put("output", truncate(to.output()));
        out.put("failures", to.failures());
        return out;
    }

    static String str(Object o, String dflt) {
        return (o instanceof String s && !s.isBlank()) ? s : dflt;
    }

    static String truncate(String s) {
        if (s == null) return null;
        return s.length() > 6000 ? s.substring(0, 6000) + "...[truncated]" : s;
    }
}
