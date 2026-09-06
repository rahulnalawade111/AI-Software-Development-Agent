# Phase 6 — IDE UI (Explorer, Editor, Terminal, Git)

## Goal
The VS Code-like IDE experience: file explorer, Monaco editor with tabs and
diffs, interactive terminal with command policy, and a real git panel. The
AI Actions timeline lands here too since it shares the right-panel infra.

## Backend
- FileController: GET /api/projects/{id}/files?path= (content + metadata +
  language detection), PUT (save — records git-dirty state, updates
  project_files), POST (create file/dir), DELETE, GET /tree, GET /search
  (delegates to search_code), GET /diff?path= (working tree vs HEAD via JGit).
- TerminalController + WebSocket handler /ws/project/{id}/terminal: user
  types a command → server validates via CommandPolicy (same rules as agent;
  anything not allowlisted → blocked message; destructive-safe list mirrors
  agent safe list) → executes via SandboxedCommandRunner in workspace cwd →
  streams stdout/stderr lines back over WS, then exit code prompt. History in
  terminal_runs. Ctrl-C/stop button → kill process group.
- GitController: GET status (porcelain groups), GET diff, POST commit
  (message; adds all tracked changes), POST /branch (create/checkout), POST
  /rollback (checkout . — destructive, confirmation in UI), GET log (last
  50). JGit-based GitService (init on project create already done).
- File watch: lightweight polling watcher (2 s) on workspace roots excluding
  heavy dirs → WS file-change events so explorer/tabs stay fresh when the
  agent or terminal writes files.

## Frontend
- FileExplorer (right panel Files tab): collapsible tree, file/folder icons
  by type, context menu (New File, New Folder, Rename, Delete w/ confirm),
  dirty dot for modified files, filter input. Click → opens tab.
- MonacoEditor integration: multi-tab bar (close, close others, dirty
  indicator *), syntax highlighting for js/jsx/html/css/java/sql/json/yaml/
  xml, line numbers, search in file + find/replace panel, Ctrl/Cmd+S save,
  read-only mode for VIEWERs. Language auto-detect from extension.
- DiffViewer: side-by-side Monaco diff (agent update_file snapshot, git diff,
  rollback preview).
- TerminalPanel (right panel): xterm.js rendering, prompt with cwd, command
  history (↑/↓), ANSI colors, live build output reuses this stream when
  user-triggered. Blocked-command feedback inline.
- GitPanel: status groups (Modified/Added/Deleted/Untracked with counts),
  file list → click opens diff viewer, commit box (message + Commit button),
  branch switcher, log list, Rollback with confirm. Auto-checkpoint: before
  any AI modification batch, agent creates "checkpoint: before <task>"
  commit — surfaced in log.
- AIActionsPanel: timeline feed from WS + REST backfill (idempotent by action
  id), icon per type, expandable payload (tool args/results), running spinner,
  auto-scroll toggle.
- Editor tab ↔ chat: "show changed files" after a task lists modified paths,
  each opening its diff.

## Acceptance criteria
- [ ] File tree reflects real disk state: creating a file via context menu
      appears instantly; agent-created files appear within ~2 s without
      manual refresh
- [ ] Opening a .java and a .jsx file shows correct syntax highlighting,
      line numbers; Ctrl+S saves and clears the dirty indicator; reloading
      the page keeps the saved content
- [ ] Find/replace works within an open file; multiple tabs stay open and
      switchable, each keeping its own scroll/cursor
- [ ] Typing `npm run build` in the terminal runs it in the project workspace
      and streams colored output; typing `rm -rf /` is blocked with a policy
      message
- [ ] Git panel shows correct Modified/Untracked groups after edits; commit
      with message works; the commit then appears in the log; the file tree
      dirty markers clear
- [ ] After an AI modification batch, a checkpoint commit exists in the log
      and the diff of any changed file opens side-by-side
- [ ] VIEWER-role users get a read-only editor (no save) and a terminal that
      rejects commands

## Tests
- FileControllerIT: CRUD + traversal rejection + language detection
- TerminalPolicyIT: allowlisted command executes; denied command returns
  blocked status; no process spawned for denied
- GitServiceIT: status/diff/commit/branch/rollback on temp repo
- Frontend: FileExplorer renders fixture tree; tabs add/remove; terminal
  blocked-command rendering

## Edge cases
- File deleted on disk while tab open → tab marks missing, offers close
- Very large file (>1 MB) → editor refuses with notice, offers download
- Binary/asset files → icon + "preview not supported", no Monaco load
- Two sessions editing same file → optimistic concurrency (409 + reload
  prompt) via updatedAt check
- Renaming a file with open tab → tab retargets
