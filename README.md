# AI DevAgent — AI Software Development Agent

A web-based AI coding agent that acts like an AI software developer. Give it a
natural-language requirement (e.g. *"Build a restaurant management system using
React, Spring Boot and MySQL"*) and it understands the requirement, creates a
technical plan, generates complete real source code, writes the files into a
project workspace, runs builds, parses errors, fixes its own code and repeats
until the project builds successfully.

**This is not a chatbot or mockup** — the agent actually operates on the project
workspace end-to-end through a tool system.

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | React 19, Vite, Monaco editor, SSE streaming |
| Backend | Java 21, Spring Boot 3.5, Spring Security, JPA/Hibernate |
| Database | MySQL (platform DB + one dedicated schema per project) |
| AI | OpenAI-compatible LLM API with tool/function calling and streaming |

## How it works

```
USER REQUIREMENT → AI UNDERSTANDS → AI PLANS → AI CREATES FILES →
AI WRITES CODE → AI RUNS BUILD → AI DETECTS ERRORS → AI FIXES CODE →
AI TESTS → AI SHOWS WORKING PROJECT
```

The agent loop is OBSERVE → PLAN → TOOL CALL → EXECUTE → OBSERVE RESULT →
ANALYZE → FIX → TEST → FINAL RESPONSE, executed server-side with each tool
call streamed live to the UI (AI Actions timeline).

### The 19 tools the AI actually calls

`create_file` `update_file` `delete_file` `read_file` `list_files`
`search_code` `create_directory` `run_command` `run_tests` `build_project`
`install_dependencies` `get_build_errors` `get_git_status` `git_diff`
`create_git_commit` `database_query` `database_schema` `preview_application`

Every tool execution is permission-checked; destructive actions
(`delete_file`, non-read-only `run_command`, write SQL) pause for user
approval with **[Cancel] / [Approve]** before running.

## Feature map

- **Three-panel IDE** — left: projects/recent/templates/settings; center: AI
  chat + code editor; right: Files, Preview, Terminal, Database, Git, AI Actions.
- **VS Code-like file explorer** with a Monaco editor: syntax highlighting,
  line numbers, multiple tabs, unsaved-changes indicator, Ctrl+S save,
  find/replace (JS/JSX/TS/HTML/CSS/Java/SQL/JSON/YAML/XML).
- **Sandboxed terminal** — allowlist/denylist blocks `rm -rf /`, `format`,
  `shutdown`, disk and credential-extraction commands.
- **Git integration** — status (added/modified/deleted), diff, commit,
  history; automatic checkpoints via the agent.
- **Build loop UI** — `Build #1 ❌ / Build #2 ❌ / Build #3 ✓` strip with
  parsed errors from npm and Maven output.
- **Preview** — starts the generated app, detects the port, verifies it
  serves HTTP, embeds it behind authenticated proxying.
- **Per-project databases** — each project gets a dedicated MySQL schema
  (`proj_<id>`) the agent can query and inspect.
- **Super Admin console** — users, projects, AI usage overview, AI action log.
- **Security** — JWT auth, BCrypt, RBAC (USER / SUPER_ADMIN), permission
  checks on every tool, secrets in environment variables only.

## Running

```bash
# backend (Spring Boot, port 8080)
cd backend && ./mvnw spring-boot:run

# frontend (Vite, port 5173)
cd frontend && npm install && npm run dev
```

Environment (backend `.env`): `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`,
`DB_PASSWORD`, `JWT_SECRET`, `WORKSPACE_ROOT`, `OPENAI_API_KEY`,
`OPENAI_BASE_URL`, `OPENAI_MODEL`.

Default seeded accounts (dev only):

| Role | Email | Password |
|---|---|---|
| SUPER_ADMIN | admin@aidev.local | Admin#12345 |
| USER | user@aidev.local | User#12345 |

## Project layout

```
backend/src/main/java/dev/aidev/
├── ai/            # agent loop, prompts, conversations, LLM client
│   ├── llm/       # OpenAI-compatible client (chat + streaming + tools)
│   └── tools/impl # the 19 tools
├── admin/         # Super Admin API
├── auth/          # register / login / me
├── build/         # sandbox runner, build/test services, preview registry+proxy
├── common/        # error handling, health
├── config/        # seed data
├── git/           # JGit service + panel API
├── project/       # projects, members, templates
├── security/      # JWT filter, Spring Security config
├── terminal/      # sandboxed terminal service
├── user/          # users, roles
└── workspace/     # file services, file/terminal/database controllers

frontend/src/
├── auth/          # AuthContext (JWT in localStorage)
├── ide/           # Chat, FileExplorer, CodeEditor, Terminal, Git, Preview,
│                  # Database, AI Actions, BuildStrip panels
├── lib/           # typed API client
├── pages/         # Login, Register, Projects, NewProject, Templates,
│                  # Settings, AdminConsole, ProjectWorkspace
├── shell/         # AppShell with left sidebar
└── styles/        # design tokens (#0F172A #111827 #1E293B #2563EB #7C3AED
                   # #22C55E), light/dark, responsive
```
