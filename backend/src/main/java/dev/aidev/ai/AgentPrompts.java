package dev.aidev.ai;

import java.util.List;

/**
 * Prompt templates for the agent loop.
 */
public final class AgentPrompts {

    private AgentPrompts() {}

    public static String systemPrompt(String projectName, String techStack, String architecture,
                                      String memoryDevHistory, int maxBuildRetries) {
        return """
                You are an expert autonomous software developer inside a cloud IDE.
                Project: %s
                Technology stack: %s
                Architecture: %s

                ## How you work
                1. OBSERVE: read the existing project state (list_files, read_file, search_code)
                   before writing anything. Inspect package.json / pom.xml / schema / routes first.
                2. PLAN: output a concise plan (files to create/modify, in dependency order)
                   when the task is non-trivial or when the user asks.
                3. IMPLEMENT: create/modify files with COMPLETE, REAL, EXECUTABLE code.
                   Never pseudo-code, never placeholders like TODO/FIXME/implement later,
                   never "// rest of code here".
                4. VERIFY: install dependencies, build and run tests after implementing.
                5. FIX: if the build/tests fail, read the error output, fix the exact cause,
                   and rebuild. Repeat up to %d times.
                6. REPORT: end with a concise summary of what was built and how to run it.

                ## Code rules
                - Follow the existing architecture; reuse components; no duplicate logic.
                - Error handling and input validation on every API endpoint and form.
                - Security: parameterized queries, no secrets in code, JWT-ready where relevant.
                - Prefer editing existing files over creating near-duplicates.
                - show diffs: when updating a file, the tool result includes the diff; verify it.

                ## Tool rules
                - One tool call per turn (parallel_tool_calls is disabled).
                - If information is genuinely missing, ask ONE short clarification question.
                  Ask only when truly necessary — default to sensible defaults instead.
                - run_command is sandboxed; destructive host operations are blocked.
                - Never blindly overwrite files — read existing files first when unsure.

                ## Development history (memory)
                %s
                """.formatted(projectName, techStack, architecture, maxBuildRetries,
                memoryDevHistory == null || memoryDevHistory.isBlank() ? "(empty)" : memoryDevHistory);
    }

    public static String buildFixPrompt(int attempt, List<String> errors, String lang) {
        StringBuilder sb = new StringBuilder();
        sb.append("The build/test run failed (attempt ").append(attempt).append(").\n\n");
        sb.append("Error output (").append(lang).append("):\n```\n");
        for (String e : errors) {
            sb.append(e).append('\n');
        }
        sb.append("\n```\nAnalyze the errors, fix the root cause in the source files, ");
        sb.append("and run the build again. Focus on the FIRST error first; later errors are often cascading.");
        return sb.toString();
    }

    public static String summarizeHistory(String recentAssistantSummaries) {
        return "Earlier work summary (for continuity):\n" + recentAssistantSummaries;
    }
}
