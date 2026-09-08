package dev.aidev.terminal;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "terminal_runs")
public class TerminalRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "user_id")
    private Long userId;

    @Column
    private String command;

    @Column(columnDefinition = "LONGTEXT")
    private String output;

    @Column(name = "exit_code")
    private Integer exitCode;

    @Column(name = "exit_reason")
    private String exitReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    public Integer getExitCode() { return exitCode; }
    public void setExitCode(Integer exitCode) { this.exitCode = exitCode; }
    public String getExitReason() { return exitReason; }
    public void setExitReason(String exitReason) { this.exitReason = exitReason; }
    public Instant getCreatedAt() { return createdAt; }
}
