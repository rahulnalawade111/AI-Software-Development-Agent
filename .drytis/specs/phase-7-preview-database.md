# Phase 7 — Preview & Database Panels

## Goal
The agent (and user) can start the generated application, see it live in an
embedded preview, and inspect/operate the project's database from the DB panel.

## Backend
- PreviewService: start/stop preview per project. Detection:
  - frontend-only: npm run dev (Vite port 5173+auto) — run in workspace,
    parse port from output
  - fullstack: backend spring-boot:run (8080+offset) + frontend dev server;
    compose URL routes /api → backend
  Ports allocated from a per-project pool (e.g. base 41000 + projectId*10),
  bound 127.0.0.1 inside container, exposed via dedicated Caddy subdomain
  routes registered per project (add_caddy_proxy equivalent via reverse
  proxy API — implementation detail for coding agent).
- PreviewController: POST /api/projects/{id}/preview/start|stop, GET status
  (url, running, pid, last logs). preview_application tool wraps this.
- Health probe: poll http GET on detected port until 200/redirect (max 60 s)
  before reporting URL.
- Proxy hardening: preview URLs unguessable (project slug + random token);
  only project members can open the direct URL (token check) — iframe embed
  in panel for all.
- DatabaseController:
  - GET /schema → tables (name, columns w/ types, PKs, FKs, indexes) via
    information_schema of the project sandbox DB (aidev_p<id>)
  - POST /query (sql) → run via JDBC; SELECT/SHOW/DESCRIBE → rows+columns
    (cap 500 rows / 1 MB); anything else (DDL/DML) requires the user's own
    confirmation in UI (button "Run (writes)") and is audited; same policy
    for the agent's database_query tool (writes need approval).
  - Agent-created schema.sql is auto-applied to the sandbox DB on first
    preview start if the DB is empty.
- WS events for preview state changes; DB panel pushes query results over
  REST only.

## Frontend
- PreviewPanel (right panel tab): toolbar [Start] [Stop] [Refresh] [Open in
  new tab], URL bar, iframe with loading state, status dot (starting/running/
  stopped/error + port), last ~20 log lines.
- DatabasePanel: schema tree (tables → columns w/ type badges, FK arrows
  list), SQL editor (CodeMirror or Monaco SQL mode, Ctrl+Enter run), results
  grid (sortable columns, row count, truncated notice), query history
  (last 20), write-SQL confirmation modal.
- Chat integration: agent final report includes "Preview running at <url>"
  link when preview_application succeeded.

## Acceptance criteria
- [ ] For a generated React project, clicking Start in the Preview panel
      boots the dev server and the app renders inside the embedded iframe
- [ ] For a generated fullstack project, preview serves the frontend with
      /api hitting the Spring Boot backend (e.g. login flow works in the
      iframe)
- [ ] Preview status shows the detected port; Stop actually kills the
      process (no zombie after stop, verified via process list)
- [ ] The Database panel lists tables created by the agent's schema.sql with
      correct columns for a generated project
- [ ] Running `SELECT * FROM users` returns a paginated grid; running
      `DROP TABLE users` prompts confirmation first and is audited
- [ ] Preview URL is not reachable without the project token (404/401 for
      guessable variants)

## Tests
- PreviewServiceIT: start/stop a tiny static dev server fixture, port
  detection, health probe, cleanup
- DatabaseControllerIT: schema listing, select, write-requires-confirm path
- Token auth test for preview URL

## Edge cases
- Port already in use → allocate next in pool, update stored URL
- Dev server crashes on start → status error with captured log tail
- App listens on 0.0.0.0 vs localhost → bind/parse both
- Stop while health-probing → cancel probe, kill process
- Preview left running across sessions → auto-stop on project archive/
  delete and on container idle-resume (startup sweeper kills stale pids)
- SQL returning millions of rows → hard cap + notice
