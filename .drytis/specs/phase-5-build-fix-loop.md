# Phase 5 — Build / Test / Fix Loop

## Goal
The signature feature: run real builds and tests for generated projects,
capture structured errors, feed them back to the agent, and loop
BUILD → ERROR → ANALYZE → FIX → BUILD until success or the retry budget is
exhausted. Includes the sandboxed command runner used by run_command.

## Backend (packages build/, terminal/)
- SandboxedCommandRunner: ProcessBuilder in workspace cwd, per-user env
  (PATH incl. node, mvn), working dir enforced under project workspace,
  10-min default timeout (configurable per call), output streamed line-by-line
  to a queue → persisted to terminal_runs/build_runs + pushed over WS,
  2 MB output cap (head+tail), exit code + killed/timeout reason.
- CommandPolicy (allow/deny): allowlist npm, npx, node, yarn, mvn, mvnw
  (./mvnw), java, javac, git, mkdir, ls, cat, grep, find, cp, mv (within
  workspace), touch, echo, python3, pip (project-local), spring-boot CLI if
  present. Denylist patterns (regex): rm -rf /, rm -rf ~, mkfs*, format,
  shutdown, reboot, halt, poweroff, dd if=, :(){ fork bombs, curl|sh,
  wget|sh, > /dev/sd*, chmod 777 /, sudo, su, chown root, /etc/shadow,
  ~/.ssh, cat backend/.env, env, printenv, set, history, credentials.
  Compound commands: split on &&, ;, | (pipes allowed only between allowlisted
  binaries, e.g. npm run build | tail). Approval escalation: any not-explicitly-
  allowed command → approval card instead of silent rejection.
- BuildService.build(projectId, target): detects frontend (package.json with
  build script) / backend (pom.xml or build.gradle) / both.
  - frontend: npm install (if no node_modules or package.json changed) then
    npm run build
  - backend: ./mvnw -q -DskipTests package (uses wrapper if present else mvn)
  Each attempt = build_runs row (attempt#, status, output, exit_code,
  started/finished). WS events for start/finish.
- ErrorParser: extracts structured BuildError { file, line, column?, symbol?,
  severity, message, raw } from:
  - Maven/Java: `[ERROR] /path/File.java:[45,12] cannot find symbol`, BUILD
    FAILURE summary, test failures (Surefire)
  - npm/Vite/webpack: `Module not found: Error: Can't resolve './x' in
    /src`, TS/ESLint diagnostics, `SyntaxError: Unexpected token`
  - Gradle: `e: file.kt: (3, 5): ...`, task :failed
  Returns grouped-by-file list + counts; attaches to build_runs.errors JSON.
- FixLoop (inside AiAgentService, but factored as BuildFixService): after a
  failed build → parse errors → feed compact error digest + relevant file
  excerpts to the LLM as tool results → agent read_file/update_file →
  build again. Max 3 fix iterations (configurable in system_settings),
  each iteration logged as FIX action with what changed. Success → green
  ✓ report; exhausted → FAILED report listing remaining errors + files
  touched + suggestion for the user.
- TestService.run(projectId, target): frontend `npm test -- --run` (vitest/
  jest detection), backend `./mvnw test` (Surefire report parsed into
  test_runs with per-test results JSON). WS progress.
- Build #1 ❌ / #2 ❌ / #3 ✓ display: build_runs history endpoint + UI.

## Frontend
- Terminal panel already exists (Phase 6 delivers shell); here add build
  output viewer: run cards with attempt number, duration, status icon
  (❌/✓), collapsible output, "Errors (n)" chips that jump to error lines.
- Errors section in main area lists grouped errors; clicking one opens the
  file at that line in the editor.

## Acceptance criteria
- [ ] A generated React project reaches `npm run build` success on the
      container, with build output visible line-by-line as it runs
- [ ] A generated Spring Boot project compiles via ./mvnw package; output
      streams live
- [ ] Deliberately breaking a generated file (agent or user) makes the next
      build fail with a parsed error showing file + line in the UI, and the
      agent's next fix iteration repairs it — build #2 shows ✓
- [ ] `rm -rf /`, `sudo`, `cat backend/.env` typed into run_command (agent or
      user terminal) are blocked with a clear policy message; allowlisted
      commands run fine
- [ ] A command that hangs is killed at its timeout and reported as
      TIMEOUT, not left running
- [ ] Build attempt history (Build #1 ❌, #2 ❌, #3 ✓) is visible per project
      and survives reload

## Tests
- CommandPolicyTest: denylist matrix, compound command splitting, pipe rules
- ErrorParserTest: fixture outputs for Maven, npm/vite, gradle → correct
  file/line/message extraction
- BuildFixServiceIT with scripted LLM: inject broken file → loop converges
  by iteration 2
- RunnerTest: timeout kill, output cap

## Edge cases
- No build system detected → friendly "nothing to build" result
- npm install network failure → surfaced as actionable error
- Out-of-memory / killed build (exit 137) → parsed as resource failure with
  hint
- Simultaneous builds for same project → serialized, second waits
- Extremely verbose output → head+tail truncation with "… N lines hidden …"
