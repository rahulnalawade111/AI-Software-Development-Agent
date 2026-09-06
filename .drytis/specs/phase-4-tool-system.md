# Phase 4 — Tool System

## Goal
The complete internal tool system the agent uses to operate on the workspace:
all 19 tools from the spec, permission-checked, logged, with destructive
actions gated behind user approval.

## Backend (package ai/tools/)
Each tool = one @Component implementing Tool { name, description, jsonSchema,
Permission required, destructive flag, execute(ctx, args) → ToolResult }.
Registered in ToolRegistry; ctx carries projectId, workspace root, userId,
permissions, action recorder.

Tools:
- FileTools: create_file(path, content), update_file(path, content — full
  replacement; records previous content for diff), delete_file(path),
  read_file(path, maxLines), list_files(path, pattern), create_directory(path)
- SearchTools: search_code(query, glob) — workspace-scoped text search,
  excludes node_modules/target/.git/dist, returns path:line:text matches
  (simple, fast; no external index)
- CommandTools: run_command(command, timeoutSec) — via sandboxed runner
  (Phase 5 runner; introduced here with basic allowlist, full parser next
  phase)
- BuildTools: build_project(target: frontend|backend|all), run_tests(target),
  install_dependencies(target), get_build_errors() — thin wrappers over
  runner + last build run state
- GitTools: get_git_status(), git_diff(path?), create_git_commit(message),
  using JGit; status maps to porcelaine → M/A/D/?? lists
- DatabaseTools: database_query(sql, targetEnv) — runs against the project's
  own configured DB if the generated app defines one (parse its
  application.yml/properties or .env for a sandbox DB), read-only unless
  explicitly approved; database_schema() → tables/columns/FKs of that DB.
  Projects get a sandbox MySQL database (aidev_p<id>) provisioned on demand;
  credentials never returned to the client — only results.
- PreviewTools: preview_application(start: boolean) — starts the generated
  app's dev server via runner (npm run dev / spring-boot:run), detects the
  bound port from output, registers it with the preview proxy (Phase 7);
  returns URL.

Semantics:
- All path args validated by WorkspaceService.resolveAndValidate (traversal
  proof). Content size caps (1 MB/file write).
- Every execution writes an ai_actions row + updates project_files metadata
  + appends to project dev history.
- update_file preserves a snapshot (for undo/diff display); create_file on
  existing path → error (use update_file) unless overwrite=true.
- delete_file / run_command (non-read-only) / database_query (write SQL) /
  git rollback are destructive → approval flow.
- run_tests / build_project / read/list/search = safe, no approval.

## Frontend
- File change notifications via WS: toast + file tree refresh + open tab
  refresh when the agent modifies an open file (with "file changed by agent"
  banner + diff view).
- Approval card renders tool name + human-readable summary of args
  (e.g. "Delete 4 files: a.jsx, b.css…").

## Acceptance criteria
- [ ] Agent-created files really appear in the file explorer without refresh
      while the session is running
- [ ] If the agent updates a file the user has open, the tab shows a changed
      indicator and offers to view the diff; the diff renders old vs new
- [ ] delete_file pauses for approval; approving deletes the file for real,
      rejecting cancels that tool call only (session continues with the
      rejection as tool result)
- [ ] search_code("UserService") in a Spring project returns file:line hits
      from src/main/java only
- [ ] git_diff after agent changes shows a real unified diff in the Git panel
- [ ] database_query against the sandbox DB returns rows (e.g. after agent
      runs schema.sql); the DB password is never present in any API response
      or log line

## Tests
- Unit test per tool (happy + permission-denied + traversal attempt)
- ToolRegistryIT: agent script that calls create_file twice with same path →
  second fails; then update_file succeeds
- GitToolsIT against a real temp repo

## Edge cases
- Binary file read → returns "binary file" marker, no corruption
- Very large file read → truncated with notice + line count
- Concurrent tool calls to same file → serialized per project (lock)
- Approval timeout (10 min) → auto-reject with session continuing
