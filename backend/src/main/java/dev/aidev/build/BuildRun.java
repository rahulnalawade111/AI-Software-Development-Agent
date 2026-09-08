package dev.aidev.build;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "build_runs")
public class BuildRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "conversation_id")
    private Long conversationId;

    @Column(nullable = false)
    private int attempt = 1;

    @Column(nullable = false)
    private String target = "ALL";

    @Column
    private String command;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(columnDefinition = "LONGTEXT")
    private String output;

    @Column(columnDefinition = "LONGTEXT")
    private String errors;

    @Column(name = "exit_code")
    private Integer exitCode;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "started_by")
    private String startedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @PrePersist
    void prePersist() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public int getAttempt() { return attempt; }
    public void setAttempt(int attempt) { this.attempt = attempt; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    public String getErrors() { return errors; }
    public void setErrors(String errors) { this.errors = errors; }
    public Integer getExitCode() { return exitCode; }
    public void setExitCode(Integer exitCode) { this.exitCode = exitCode; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public String getStartedBy() { return startedBy; }
    public void setStartedBy(String startedBy) { this.startedBy = startedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
}
