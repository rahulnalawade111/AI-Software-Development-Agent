# AI Software Development Agent — Master Spec

## Product
Web-based AI software developer. User states a requirement in natural language
(e.g. "Build a restaurant management system using React, Spring Boot and MySQL").
The agent understands it, asks clarifications only when necessary, produces a
technical plan + architecture, generates REAL source code into a per-project
workspace, runs installs/builds/tests, parses build errors, fixes its own code in
a BUILD→ERROR→ANALYZE→FIX loop until green, and shows the result in an embedded
preview. NOT a chatbot, NOT a mockup.

## Stack
- Frontend: React 18 + Vite (JS), HTML5, CSS3 (design tokens below)
- Backend: Java 17, Spring Boot 3, Spring Security (JWT, BCrypt, RBAC), REST, JPA/Hibernate, Flyway
- DB: MySQL (auto-provisioned; creds from get_project_details → env keys)
- AI: OpenAI-compatible API, tool/function calling, SSE streaming
- Editor: Monaco (js, jsx, html, css, java, sql, json, yaml, xml)
- Git: JGit (same JVM, safe subset)
- WS: Spring WebSocket (terminal + live agent events)

## Layout
- /workspace/frontend — React app (Vite, port 5173, Caddy → /)
- /workspace/backend — Spring Boot (port 8080, Caddy → /api)
- /workspace/storage/projects/<projectId>/ — per-project generated workspaces
- /workspace/storage/templates/ — starter templates

## Env keys (all is_secret, /workspace/backend/.env)
OPENAI_API_KEY (Drytis-managed mint), OPENAI_BASE_URL, OPENAI_MODEL (default
gpt-5.1), WORKSPACE_ROOT=/workspace/storage/projects, and DB_* via resolvers.
AI_BASE_URL / AI_MODEL are admin-editable copies (system_settings overrides).

## Design tokens
Dark: bg #0F172A / panel #111827 / elevated #1E293B; accents #2563EB (blue),
#7C3AED (purple), #22C55E (green). Light mode counterpart via CSS vars.
Dark/light toggle, responsive (desktop 3-panel, mobile tabs).

## Toolchain provisioning (CRITICAL — container has NO Java)
This container is a Node/PHP base image: NO JDK, NO Maven (verified). It has
passwordless sudo, internet (Maven Central + npm reachable), 9.2G disk,
4 cores, 6G RAM. Node 24.19, npm 11.17, Python 3.13.5, git 2.47.3 present.

The backend background service script MUST self-provision BEFORE launching:
1. JDK 21: download Temurin 21 (Adoptium API latest/21/ga/linux/x64/jdk) into
   /workspace/.tools/jdk (persistent volume — survives restarts; keep
   extracted dir, skip download if /workspace/.tools/jdk/bin/java exists).
2. Maven 3.9.9: apache-maven-3.9.9-bin.tar.gz from repo.maven.apache.org into
   /workspace/.tools/maven (same idempotency).
3. Export JAVA_HOME=/workspace/.tools/jdk, PATH=$JAVA_HOME/bin:/workspace/.tools/maven/bin:$PATH.
4. cd /workspace/backend && mvn -q -DskipTests package, then run the jar
   (or spring-boot:run). First build downloads all deps (cold ~/.m2) — allow
   several minutes; log progress.
5. generated-agent builds reuse the same JDK/Maven (runner prepends
   /workspace/.tools/*/bin to PATH).

Pinned versions (verified on registries today): Spring Boot 3.5.16, jjwt
0.12.6, JGit 7.3.0.202506031305-r, React 19.2.8, react-router-dom 7.18.3,
Vite 8.2.2, @vitejs/plugin-react 6.1.1, monaco-editor 0.56.0, @xterm/xterm
6.0.0, @stomp/stompjs 7.3.0, axios 1.20.0. Java target: 21 (Debian 13 default
JDK also 21). Do NOT use Spring Boot 4.x (4.2.0-M1 is a milestone).

DB (from project details): MySQL at 127.0.0.1:3306, database
u861p3480_ai_software_development_agent, user u861p3480_user — wired via
env resolvers, never hardcode.

## Phases
1. Foundations — backend skeleton + frontend shell + auth
2. Projects & workspaces
3. AI agent core (LLM client, streaming, tool calling, session loop)
4. Tool system (19 tools + permissions + approvals)
5. Build/test/fix loop (sandboxed runner, error parsing)
6. IDE UI (explorer, editor, terminal, git)
7. Preview & database panels
8. Super Admin console
9. Polish, responsive, docs, e2e

## Sandbox & security
Allowlist runner: npm/npx/node, mvn/mvnw/java, git, mkdir/ls/cat; denylist:
rm -rf /, mkfs, format, shutdown, dd, credential paths (/etc/shadow, ~/.ssh,
.env of THIS app), curl/wget piping to shell; 10-min timeout, output cap 2 MB,
kill process group on timeout. Path guard: tools may only touch
storage/projects/<projectId>/. AI tool calls require WRITE project permission.
Destructive tool calls (delete_file, run_command with writes, git reset) pause
agent and require [Approve]/[Cancel] in UI. Secrets only in env vars.

## Agent loop
OBSERVE → PLAN → TOOL CALL → EXECUTE → OBSERVE RESULT → ANALYZE → FIX → TEST →
FINAL RESPONSE. Persisted per conversation step in ai_actions. Max 3 fix
iterations before handing off to user with full context.
