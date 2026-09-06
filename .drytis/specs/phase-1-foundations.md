# Phase 1 — Foundations

## Goal
Spring Boot 3 backend skeleton + React/Vite frontend shell with working
authentication and the three-panel IDE layout. Everything later builds on this.

## Toolchain prerequisite (container has NO Java — verified)
Before anything runs, provision into /workspace/.tools (persistent volume):
- JDK 21 (Temurin, via Adoptium API latest/21/ga/linux/x64/jdk) → /workspace/.tools/jdk
- Maven 3.9.9 (apache-maven-3.9.9-bin.tar.gz from repo.maven.apache.org) → /workspace/.tools/maven
- Idempotent: skip downloads if /workspace/.tools/jdk/bin/java and
  /workspace/.tools/maven/bin/mvn already exist. Export JAVA_HOME and PATH in
  the backend service script BEFORE mvn runs. Passwordless sudo available;
  internet verified working. First mvn build has a cold ~/.m2 — expect minutes.
- Register backend + frontend background services (add_background_service)
  that provision (if needed), build and run; logs to /var/log/services/.

## Pinned versions (verified on registries)
Spring Boot 3.5.16 · Java 21 · jjwt 0.12.6 · JGit 7.3.0.202506031305-r ·
React 19.2.8 · react-router-dom 7.18.3 · Vite 8.2.2 · @vitejs/plugin-react
6.1.1 · axios 1.20.0 · monaco-editor 0.56.0 (later phases) · @stomp/stompjs
7.3.0 (later phases). Never Spring Boot 4.x.

## Deliverables

### Backend (/workspace/backend)
- Spring Boot 3.5.16 (Java 21), Maven. `spring-boot-starter-web`, `data-jpa`,
  `security`, `validation`, `websocket`, `mysql-connector-j`, `flyway-core`,
  `flyway-mysql`, `jjwt 0.12.6` (api/impl/jackson), `org.eclipse.jgit` 7.3.0.
- Flyway V1 migration with: users, roles, permissions, users_roles,
  users_permissions, projects, project_members, project_files,
  ai_conversations, ai_messages, ai_actions, build_runs, test_runs,
  terminal_runs, project_versions, git_commits, system_settings, audit_logs.
  Column details: standard BIGINT PKs, created_at/updated_at TIMESTAMP,
  projects.owner_id FK users, project_members (project_id, user_id, role enum
  OWNER/DEVELOPER/VIEWER), ai_messages (role enum, tool_call_id nullable,
  action_id nullable), build_runs (status enum PENDING/RUNNING/SUCCESS/FAILED,
  output LONGTEXT, exit_code), terminal_runs (command, output, exit_code,
  exit_reason), system_settings (setting_key UNIQUE, setting_value TEXT),
  audit_logs (user_id nullable, action, entity_type, entity_id, details JSON).
- Entities + repositories for all tables above.
- JWT auth: AuthController (/api/auth/register, /login, /me), JwtService
  (HS256, secret from env, 24h access tokens), JwtAuthFilter, SecurityConfig:
  /api/auth/** + /ws/** permitAll, everything else authenticated; stateless;
  BCryptPasswordEncoder; CORS allow frontend origin.
- Seed on first run (Flyway afterMigrate or CommandLineRunner):
  admin@aidev.local / Admin#12345 (SUPER_ADMIN), demo@aidev.local / Demo#12345
  (USER).
- DTOs + GlobalExceptionHandler (@RestControllerAdvice) with consistent
  { status, message, timestamp } error shape. ApiError record.
- application.yml: port 8080, context-path none, datasource from env
  (DATABASE_HOST etc. via Spring placeholders), JPA ddl-auto: validate,
  flyway enabled, multipart limits, async executor config.
- Backend .env keys registered via add_environment_key (DB_*, JWT_SECRET
  random static, WORKSPACE_ROOT).

### Frontend (/workspace/frontend)
- Vite 8.2.2 + React 19.2.8, JavaScript, react-router-dom 7.18.3, axios
  1.20.0 (baseURL /api, JWT from localStorage, 401 → login).
- Auth: Login + Register pages, AuthContext (user, login, logout, register).
- AppShell: LEFT SIDEBAR (New Project, Projects, Recent Projects, Templates,
  Settings; user card + logout; collapse), MAIN AREA (route outlet),
  RIGHT PANEL (tab strip: Files | Preview | Terminal | Database | Git | AI
  Actions; collapsible). CSS Grid layout, design tokens as CSS variables,
  dark default + light theme via [data-theme], persisted.
- Pages (stubbed): Projects (grid), NewProject (form: name, description,
  tech chips React/Spring Boot/Node.js/Python/Java/MySQL, AI model Default/
  Advanced, CREATE WITH AI), Settings (profile, theme, AI defaults).
- Vite dev proxy /api → http://localhost:8080.

## Acceptance criteria
- [ ] Backend starts: `./mvnw spring-boot:run` boots clean, Flyway migrates,
      /api/auth/login with seeded admin returns a JWT
- [ ] /api/auth/me with that JWT returns the admin user; without token → 401
- [ ] Register creates a USER-role account that can then log in
- [ ] Frontend `npm run dev` serves the app; login as admin lands on the IDE
      shell with all three panels visible (desktop width)
- [ ] Sidebar navigation works: Projects, New Project, Templates, Settings
      render their pages; Settings theme toggle switches dark/light live
- [ ] Seeded accounts work: admin@aidev.local / Admin#12345 and
      demo@aidev.local / Demo#12345
- [ ] Right panel tabs are present and switchable (content may be placeholders
      in this phase)

## Tests
- Backend: AuthServiceTest (BCrypt round-trip, JWT generate/parse),
  AuthControllerIT (register/login/me, 401 path) with @SpringBootTest +
  Testcontainers MySQL or H2-mysql profile fallback.
- Frontend: render smoke tests (Vitest) for AppShell, Login.

## Edge cases
- Duplicate email registration → 409 with friendly message
- Expired/invalid JWT → 401 (not 500)
- Frontend served with missing token → redirect to /login
- Theme choice survives reload (localStorage)
