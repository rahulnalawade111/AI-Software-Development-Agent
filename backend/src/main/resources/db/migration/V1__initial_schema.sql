-- AI Software Development Agent — initial schema
-- All 17 domain tables. Enums stored as VARCHAR for forward-compat.

-- ---------- users / roles / permissions ----------
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    must_change_password TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE -- SUPER_ADMIN | USER
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE -- e.g. PROJECT_WRITE, AI_RUN, ADMIN
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE users_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE users_permissions (
    user_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, permission_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- projects ----------
CREATE TABLE projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id BIGINT NOT NULL,
    tech_stack JSON,
    architecture JSON,
    requirements JSON,
    file_tree JSON,
    dev_history JSON,
    ai_model VARCHAR(100) DEFAULT 'default',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE | ARCHIVED
    preview_token VARCHAR(64),
    preview_port INT,
    last_activity_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE project_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'DEVELOPER', -- OWNER | DEVELOPER | VIEWER
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_user (project_id, user_id),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE project_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    path VARCHAR(1024) NOT NULL,
    language VARCHAR(50),
    size_bytes BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_path (project_id, path(768)),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- AI conversations ----------
CREATE TABLE ai_conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(255),
    state VARCHAR(30) NOT NULL DEFAULT 'IDLE', -- IDLE | PLANNING | EXECUTING | AWAITING_APPROVAL | AWAITING_USER | BUILDING | FIXING | DONE | FAILED | STOPPED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL, -- user | assistant | tool
    content LONGTEXT,
    tool_calls JSON,
    tool_call_id VARCHAR(255),
    action_id BIGINT,
    prompt_tokens INT,
    completion_tokens INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES ai_conversations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_actions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    user_id BIGINT,
    type VARCHAR(40) NOT NULL, -- THOUGHT | PLAN_STEP | TOOL_CALL | TOOL_RESULT | BUILD | ERROR | FIX | APPROVAL_REQUESTED | APPROVAL_RESULT | FILE_CHANGE
    label VARCHAR(500),
    payload JSON,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING | RUNNING | SUCCESS | FAILED | CANCELLED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES ai_conversations(id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- builds & tests ----------
CREATE TABLE build_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    conversation_id BIGINT,
    attempt INT NOT NULL DEFAULT 1,
    target VARCHAR(30) NOT NULL DEFAULT 'ALL', -- FRONTEND | BACKEND | ALL
    command VARCHAR(1024),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING | RUNNING | SUCCESS | FAILED | TIMEOUT
    output LONGTEXT,
    errors JSON,
    exit_code INT,
    duration_ms BIGINT,
    started_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE test_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    build_run_id BIGINT,
    target VARCHAR(30) NOT NULL DEFAULT 'ALL',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total INT DEFAULT 0,
    passed INT DEFAULT 0,
    failed INT DEFAULT 0,
    skipped INT DEFAULT 0,
    results JSON,
    output LONGTEXT,
    exit_code INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (build_run_id) REFERENCES build_runs(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- terminal ----------
CREATE TABLE terminal_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id BIGINT,
    command VARCHAR(2048),
    output LONGTEXT,
    exit_code INT,
    exit_reason VARCHAR(30), -- NORMAL | TIMEOUT | KILLED | BLOCKED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- versioning ----------
CREATE TABLE project_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    kind VARCHAR(30) NOT NULL DEFAULT 'CHECKPOINT', -- CHECKPOINT | COMMIT | SNAPSHOT
    label VARCHAR(500),
    commit_hash VARCHAR(64),
    details JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE git_commits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    commit_hash VARCHAR(64) NOT NULL,
    message VARCHAR(1000),
    author_name VARCHAR(255),
    author_email VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    UNIQUE KEY uk_project_commit (project_id, commit_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- settings & audit ----------
CREATE TABLE system_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(150) NOT NULL UNIQUE,
    setting_value TEXT,
    description VARCHAR(500),
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(60),
    entity_id VARCHAR(100),
    details JSON,
    ip VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- indexes ----------
CREATE INDEX idx_projects_owner ON projects(owner_id);
CREATE INDEX idx_projects_status ON projects(status);
CREATE INDEX idx_members_user ON project_members(user_id);
CREATE INDEX idx_files_project ON project_files(project_id);
CREATE INDEX idx_conversations_project ON ai_conversations(project_id);
CREATE INDEX idx_messages_conversation ON ai_messages(conversation_id);
CREATE INDEX idx_actions_conversation ON ai_actions(conversation_id);
CREATE INDEX idx_actions_project ON ai_actions(project_id);
CREATE INDEX idx_builds_project ON build_runs(project_id);
CREATE INDEX idx_tests_project ON test_runs(project_id);
CREATE INDEX idx_terminal_project ON terminal_runs(project_id);
CREATE INDEX idx_versions_project ON project_versions(project_id);
CREATE INDEX idx_commits_project ON git_commits(project_id);
CREATE INDEX idx_audit_created ON audit_logs(created_at);
CREATE INDEX idx_settings_key ON system_settings(setting_key);
