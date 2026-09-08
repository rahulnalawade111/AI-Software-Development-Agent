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
    private final java.util.function.BiConsumer<String, String> emit; // (label, detail)

    public ToolContext(Long projectId, Long conversationId, dev.aidev.security.AppPrincipal principal,
                       Consumer<Object> eventSink) {
        this.projectId = projectId;
        this.conversationId = conversationId;
        this.userId = principal != null ? principal.getUserId() : null;
        this.userName = principal != null ? principal.getUsername() : null;
        this.superAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        this.eventSink = eventSink;
        this.emit = null;
    }

    public Long projectId() { return projectId; }
    public Long conversationId() { return conversationId; }
    public Long userId() { return userId; }
    public String userName() { return userName; }
    public boolean isSuperAdmin() { return superAdmin; }

    /** Fire an event object to the live UI stream (serialized as JSON). */
    public void publish(Object event) {
        if (eventSink != null) eventSink.accept(event);
    }
}
