package dev.aidev.ai.tools;

import dev.aidev.security.AppPrincipal;

import java.util.function.Consumer;

/**
 * Per-invocation context handed to every tool execution.
 */
public class ToolContext {
    private final Long projectId;
    private final Long conversationId;
    private final Long userId;
    private final String userName;
    private final boolean superAdmin;
    private final Consumer<Object> eventSink;   // publish progress events to the UI
    private final String workspaceRoot;
    private final String jdbcUrl;
    private final String dbUser;
    private final String dbPassword;

    public ToolContext(Long projectId, Long conversationId, dev.aidev.security.AppPrincipal principal,
                       Consumer<Object> eventSink) {
        this.projectId = projectId;
        this.conversationId = conversationId;
        this.userId = principal != null ? principal.userId() : null;
        this.userName = principal != null ? principal.email() : null;
        this.superAdmin = principal != null && "SUPER_ADMIN".equals(principal.role());
        this.eventSink = eventSink;
        this.workspaceRoot = null;
        this.jdbcUrl = null;
        this.dbUser = null;
        this.dbPassword = null;
    }

    public ToolContext withWorkspace(String workspaceRoot, String jdbcUrl,
                                     String dbUser, String dbPassword) {
        return new ToolContext(projectId, conversationId, null, eventSink,
                workspaceRoot, jdbcUrl, dbUser, dbPassword, userId, userName, superAdmin);
    }

    private ToolContext(Long projectId, Long conversationId, dev.aidev.security.AppPrincipal principal,
                        Consumer<Object> eventSink, String workspaceRoot, String jdbcUrl,
                        String dbUser, String dbPassword, Long userId, String userName,
                        boolean superAdmin) {
        this.projectId = projectId;
        this.conversationId = conversationId;
        this.userId = userId;
        this.userName = userName;
        this.superAdmin = superAdmin;
        this.workspaceRoot = workspaceRoot;
        this.jdbcUrl = jdbcUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.eventSink = eventSink;
    }

    public Long projectId() { return projectId; }
    public Long conversationId() { return conversationId; }
    public Long userId() { return userId; }
    public String userName() { return userName; }
    public boolean isSuperAdmin() { return superAdmin; }
    public String workspaceRoot() { return workspaceRoot; }
    public String jdbcUrl() { return jdbcUrl; }
    public String dbUser() { return dbUser; }
    public String dbPassword() { return dbPassword; }

    /** Fire an event object to the live UI stream (serialized as JSON). */
    public void publish(Object event) {
        if (eventSink != null) eventSink.accept(event);
    }
}
