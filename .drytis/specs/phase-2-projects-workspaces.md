# Phase 2 — Projects & Workspaces

## Goal
Full project lifecycle: create, list, rename, duplicate, archive, delete, open.
Each project gets an on-disk workspace directory under storage/projects/, and
project memory (stack, architecture, requirements, file tree, dev history)
persists in MySQL.

## Backend
- ProjectController REST under /api/projects:
  POST / (name, description, techStack[], aiModel) — creates project +
  workspace dir + empty git repo (JGit init) + initial commit "chore: init"
  POST /{id}/duplicate, PATCH /{id} (rename/description), PATCH /{id}/archive,
  DELETE /{id} (owner or SUPER_ADMIN only)
  GET / (paged, filter status/tech/search), GET /{id}, GET /{id}/file-tree,
  GET /recent (last 5 opened by user)
- ProjectService with ownership checks: owner or SUPER_ADMIN can mutate;
  project_members (OWNER/DEVELOPER/VIEWER) gate read/write; VIEWER read-only.
- WorkspaceService: resolveAndValidate(projectId, relPath) — canonical path
  must stay under storage/projects/<id>/ (reject ../, absolute, symlinks);
  ensureWorkspace(projectId); deleteWorkspace.
- ProjectMemoryService: get/update project memory JSON columns on projects
  (technology_stack, architecture, requirements, file_tree snapshot, dev
  history entries). AI later reads this instead of rescanning.
- FileTreeService: walk workspace (skip node_modules, target, .git, dist,
  build; depth limit 12) → nested tree DTO for the explorer.
- Templates: 5 starter templates stored under storage/templates/
  (react-starter, spring-boot-starter, fullstack-react-springboot,
  node-express-starter, python-flask-starter) each with a manifest.json
  (name, description, stack, files). GET /api/templates lists them;
  ProjectService.createFromTemplate copies template into workspace.
- project_files table tracks metadata (path, size, language, updated_at) —
  updated via file events from Phase 4 tools.
- audit_logs entries for create/rename/archive/delete/duplicate.

## Frontend
- Projects page: table/grid with Project Name, Technology badges, Status
  (Active/Archived), Last Modified, Owner, Actions (Open, Rename, Duplicate,
  Archive, Delete w/ confirm).
- Recent Projects list on Dashboard/home section of sidebar.
- New Project page wired: creates project (blank or template) → navigates to
  IDE route /project/:id.
- Templates page: gallery cards (name, description, stack chips, files count)
  with "Use template" → New Project prefilled.
- IDE route /project/:id loads project + file tree (Phase 6 fills panels).

## Acceptance criteria
- [ ] Creating a project makes it appear in Projects with owner, stack badges
      and Active status; workspace directory exists on disk with a .git repo
- [ ] Rename updates the name everywhere (list + IDE header); Duplicate
      creates a copy with " (copy)" suffix and its own workspace + git history
- [ ] Archive hides it from the default list (filterable via status toggle);
      Delete removes project + workspace and it disappears from the list
- [ ] Reopening a project from Recent Projects works after a full page reload
- [ ] Template gallery shows 5 templates; creating from
      fullstack-react-springboot produces the expected folders in the file tree
- [ ] A VIEWER-role member can open the project read-only; write endpoints
      return 403 for them
- [ ] Path traversal attempts (…/… in any file API) are rejected with 400

## Tests
- ProjectServiceIT: lifecycle + permission matrix (owner/member/viewer/
  superadmin × read/write)
- WorkspaceServiceTest: traversal rejection, symlink rejection
- FileTreeServiceTest: ignores node_modules/.git, depth cap
- Frontend: Projects page renders seeded data; New Project happy path

## Edge cases
- Duplicate project name allowed (unique per owner not enforced globally)
- Deleting a project with running builds → refuse (409) until stopped
- Workspace dir deleted out-of-band → auto-recreated empty + warn
- Template copy fails midway → cleanup partial dir
