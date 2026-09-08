package dev.aidev.ai.tools;

import java.util.Map;

/**
 * A tool the AI agent can call. Implementations live in the various packages
 * (workspace, build, git, terminal, project) and are discovered via Spring.
 */
public interface AiTool {

    /** Tool name advertised to the model (must be unique). */
    String name();

    /** Human label shown in the AI Action timeline. */
    String label();

    /** Short description for the model. */
    String description();

    /** JSON schema for the arguments object. */
    String parametersSchema();

    /**
     * If true, executing this tool requires explicit user approval before it runs.
     * Only destructive actions should set this.
     */
    default boolean requiresApproval() { return false; }

    /**
     * Per-call approval decision (e.g. database_query approves only write SQL,
     * run_command only non-read-only commands). Defaults to the static flag.
     */
    default boolean requiresApproval(java.util.Map<String, Object> args) { return requiresApproval(); }

    /**
     * Execute the tool. Args are already JSON-parsed. Returns a JSON-serializable
     * result object that will be given back to the model as the tool output.
     */
    Object execute(Map<String, Object> args, ToolContext ctx) throws Exception;
}
