package dev.aidev.build;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "test_runs")
public class TestRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "build_run_id")
    private Long buildRunId;

    @Column(nullable = false)
    private String target = "ALL";

    @Column(nullable = false)
    private String status = "PENDING";

    @Column
    private Integer total;

    @Column
    private Integer passed;

    @Column
    private Integer failed;

    @Column
    private Integer skipped;

    @Column(columnDefinition = "LONGTEXT")
    private String results;

    @Column(columnDefinition = "LONGTEXT")
    private String output;

    @Column(name = "exit_code")
    private Integer exitCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @PrePersist
    void prePersist() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getBuildRunId() { return buildRunId; }
    public void setBuildRunId(Long buildRunId) { this.buildRunId = buildRunId; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }
    public Integer getPassed() { return passed; }
    public void setPassed(Integer passed) { this.passed = passed; }
    public Integer getFailed() { return failed; }
    public void setFailed(Integer failed) { this.failed = failed; }
    public Integer getSkipped() { return skipped; }
    public void setSkipped(Integer skipped) { this.skipped = skipped; }
    public String getResults() { return results; }
    public void setResults(String results) { this.results = results; }
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    public Integer getExitCode() { return exitCode; }
    public void setExitCode(Integer exitCode) { this.exitCode = exitCode; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
}
