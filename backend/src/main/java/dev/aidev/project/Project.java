package dev.aidev.project;

import com.fasterxml.jackson.annotation.JsonRawValue;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    // Raw JSON strings (MariaDB stores JSON as LONGTEXT)
    @Column(name = "tech_stack", columnDefinition = "LONGTEXT")
    private String techStack;

    @Column(name = "architecture", columnDefinition = "LONGTEXT")
    private String architecture;

    @Column(name = "requirements", columnDefinition = "LONGTEXT")
    private String requirements;

    @Column(name = "file_tree", columnDefinition = "LONGTEXT")
    private String fileTree;

    @Column(name = "dev_history", columnDefinition = "LONGTEXT")
    private String devHistory;

    @Column(name = "ai_model")
    private String aiModel = "default";

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "preview_token", length = 64)
    private String previewToken;

    @Column(name = "preview_port")
    private Integer previewPort;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<ProjectMember> members = new ArrayList<>();

    @PrePersist
    void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; lastActivityAt = createdAt; }
    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public boolean isArchived() { return "ARCHIVED".equals(status); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    @JsonRawValue
    public String getTechStack() { return techStack; }
    public void setTechStack(String techStack) { this.techStack = techStack; }

    @JsonRawValue
    public String getArchitecture() { return architecture; }
    public void setArchitecture(String architecture) { this.architecture = architecture; }

    @JsonRawValue
    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }

    @JsonRawValue
    public String getFileTree() { return fileTree; }
    public void setFileTree(String fileTree) { this.fileTree = fileTree; }

    @JsonRawValue
    public String getDevHistory() { return devHistory; }
    public void setDevHistory(String devHistory) { this.devHistory = devHistory; }

    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPreviewToken() { return previewToken; }
    public void setPreviewToken(String previewToken) { this.previewToken = previewToken; }
    public Integer getPreviewPort() { return previewPort; }
    public void setPreviewPort(Integer previewPort) { this.previewPort = previewPort; }
    public Instant getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(Instant lastActivityAt) { this.lastActivityAt = lastActivityAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<ProjectMember> getMembers() { return members; }
}
