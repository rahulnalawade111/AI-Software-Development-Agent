# Phase 8 — Super Admin Console

## Goal
Full admin area for the platform: users, projects, AI usage/models/keys,
workspaces, permissions, logs, AI action feed, system settings.

## Backend
- AdminGuard: @PreAuthorize("hasRole('SUPER_ADMIN')") on all /api/admin/**.
- AdminUserController: list (paged, search, filter role/status), create user
  (with role), update (role, enabled), deactivate, reset password (returns
  one-time password shown once), force logout (JWT denylist by user id).
- AdminProjectController: list all projects (owner, tech, status, size,
  builds count), open/inspect (read-only view of any project), archive/
  delete, transfer ownership, manage project_members (add/remove/role).
- AdminAiController:
  - usage stats: tokens/requests/cost per day, per user, per model (from
    ai_messages/ai_actions metadata), charts endpoints (aggregates)
  - models: list configured models (system_settings ai_models JSON), set
    default, add/remove
  - keys: CRUD for provider API keys (stored encrypted AES-GCM with key from
    env ADMIN_ENC_KEY; never returned in full — masked preview), test
    connection button (tiny completion ping)
- AdminWorkspaceController: list workspaces (disk usage, project, last
  activity), cleanup orphaned workspaces, quota per user (storage + projects
  count) enforcement hooks in ProjectService.
- AdminLogController: audit_logs feed (filter user/action/entity/date),
  ai_actions global feed (filter project/user/type/status), terminal_runs
  and build_runs browsing.
- AdminSettingsController: system_settings CRUD (ai_base_url, ai_model,
  temperature, max_fix_iterations, build timeout, quotas, feature flags)
  with validation + audit.
- All admin mutations write audit_logs.

## Frontend
- Admin area route /admin (visible in sidebar only for SUPER_ADMIN), its own
  sub-nav: Overview, Users, Projects, AI Usage, Models & Keys, Workspaces,
  Logs, Settings.
- Overview: stat cards (users, projects, builds today, AI requests today,
  tokens, active previews) + 14-day sparkline charts.
- Users: table w/ role badges, enabled toggle, create/edit modal, reset
  password reveal-once modal, force logout.
- Projects: global table, actions (inspect, transfer, archive, delete),
  member management modal.
- AI Usage: charts per day/user/model + table of recent AI actions.
- Models & Keys: model cards (default badge), add model form; keys list
  (masked), add key, test connection, revoke.
- Workspaces: usage table with disk sizes, cleanup button, quota editor.
- Logs: filterable audit + AI action feeds with detail drawer.
- Settings: forms bound to system_settings with validation feedback.

## Acceptance criteria
- [ ] demo@aidev.local (USER role) cannot reach any /admin page or /api/admin
      endpoint (403 + redirected to IDE)
- [ ] Admin can create a user with a chosen role; that user can log in with
      the one-time password and is forced to change it on first login
- [ ] Admin can archive/transfer/delete any project; deletion removes the
      workspace from disk
- [ ] AI Usage page shows request/token counts that increase after running
      an agent session (numbers are real aggregates, not fixtures)
- [ ] Admin can change the default AI model in Settings; the next agent
      session uses the new model (visible in the session's first action)
- [ ] API keys are stored encrypted and only ever shown masked; test
      connection reports success/failure honestly
- [ ] Every admin mutation appears in the audit log with user + action +
      entity

## Tests
- SecurityIT: role matrix for /api/admin/** endpoints
- AdminUserControllerIT: create/reset/deactivate flows
- EncryptionTest: key round-trip, masked preview never contains plaintext
- Usage aggregation test with seeded ai_messages

## Edge cases
- Demoting the last SUPER_ADMIN → blocked with clear error
- Deleting a user with owned projects → requires transfer choice first
- Key rotation while session running → applies to next session only, no leak
- Quota exceeded → project creation returns friendly 402-style error in UI
