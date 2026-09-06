# Phase 3 — AI Agent Core

## Goal
The heart of the product: an agent that takes a natural-language requirement,
streams its reasoning/plan, calls tools, executes them, observes results,
fixes errors and reports. Streaming over WebSocket (terminal + live events)
and SSE where useful.

## Backend
- Package ai/: LlmClient (OpenAI-compatible /chat/completions with
  tools + stream=true via OkHttp/SSE), AiAgentService (session loop),
  AgentSession (state machine: PLANNING → EXECUTING → AWAITING_APPROVAL →
  BUILDING → FIXING → DONE/FAILED/STOPPED), ToolRegistry (name→handler),
  ToolDefinitionFactory (JSON-schema tool specs exposed to the LLM).
- Agent loop: send conversation + project memory + tool defs → LLM returns
  assistant message with tool_calls → execute sequentially → append
  tool results as role=tool messages → loop until LLM produces final
  content with no tool calls, or safety limits hit (max 60 tool calls,
  max 3 fix iterations, 15-min wall clock, token budget).
- Every step persisted: ai_messages (role, content, tool_calls JSON,
  tool_call_id), ai_actions (type: THOUGHT/PLAN_STEP/TOOL_CALL/TOOL_RESULT/
  BUILD/ERROR/FIX/APPROVAL_REQUESTED/APPROVAL_RESULT/FILE_CHANGE, payload
  JSON, status PENDING/RUNNING/SUCCESS/FAILED/CANCELLED).
- Events pushed live over WebSocket topic /topic/project/{id}/agent
  (action created/updated, message delta tokens, plan step toggled, build
  status). Frontend AI Actions timeline + chat stream render from these.
- Conversations: AiConversationController — POST /api/projects/{id}/chat
  (user message → starts or continues session), GET conversations, GET
  messages, POST /{convId}/stop (cancels running session via flag).
- System prompt builder: role ("senior full-stack engineer"), project memory
  (stack/architecture/requirements), codebase context snapshot (package.json,
  pom.xml, application.yml, src tree summary), coding rules (real code not
  pseudo-code, follow existing architecture, reuse components, no unrelated
  changes, error handling + validation + security), current plan state.
- Clarification flow: LLM may emit a `ask_user` pseudo-tool → session pauses
  in AWAITING_USER state, UI shows question; next user message resumes.
- Approval flow: when a tool call is flagged destructive (registry metadata),
  session → AWAITING_APPROVAL, ai_action APPROVAL_REQUESTED with payload
  (tool, args, human-readable summary); POST /api/agent/actions/{id}/approve
  or /reject resumes or cancels. Safe ops (read_file, list_files,
  build_project, run_tests) never pause.
- LLM config resolution: system_settings (ai_base_url, ai_model,
  temperature) override env defaults; Super Admin edits these (Phase 8).
- Streaming chat: token deltas for assistant text; tool-call arguments
  stream into the action payload progressively.

## Frontend
- IDE chat panel: conversation list (per project), message stream with
  markdown rendering, streaming caret, [Start Building] button after plan
  message, stop button, approval cards ([Cancel] [Approve]) inline.
- Plan display: checklist rendered from PLAN_STEP actions ("✓ React
  application created"), progress %.
- AI Actions tab (right panel): vertical timeline of all actions with icons
  (🤖 analyzing → ✓ steps), statuses, expandable payloads; auto-scroll while
  running.
- "Currently creating: X" live status line from the newest RUNNING action.
- Errors tab/section: build errors grouped by file with line numbers.

## Acceptance criteria
- [ ] Sending "Build a restaurant management system with React + Spring
      Boot + MySQL" produces a plan message with feature checklist, then a
      [Start Building] button
- [ ] After clicking Start, actions stream live into the AI Actions timeline
      (files created, commands run) without page refresh
- [ ] Assistant chat text streams token-by-token, not in one lump
- [ ] When the agent calls a destructive tool (e.g. delete_file), the session
      pauses, an approval card appears in chat, Approve resumes and Reject
      cancels cleanly
- [ ] Stopping a running session cancels it within a couple of seconds and
      leaves the conversation resumable
- [ ] Asking a follow-up like "Use JWT authentication" then later "Add admin
      login" — the second reply correctly refers to the existing JWT setup
      (project memory working)
- [ ] Conversation history survives page reload and server restart (all in DB)
- [ ] Agent respects limits: a session that would loop forever instead stops
      with a clear FAILED report explaining why

## Tests
- LlmClient test against a stub SSE server (WireMock): streams deltas, tool
  call parsing
- AgentSessionTest with fake LLM script (in-order scripted responses):
  completes a create-file → build → fix cycle without real API
- ApprovalServiceTest: destructive tool pauses and resumes correctly
- ToolRegistryTest: every tool has schema + permission metadata

## Edge cases
- LLM returns malformed tool args → retry once with corrective feedback,
  then fail action with useful error
- SSE disconnect mid-stream → session continues, UI resyncs from DB on
  reconnect (idempotent replay of actions)
- Two users send messages to same conversation concurrently → serialized via
  per-conversation lock
- Wall-clock budget exceeded → graceful FAILED with summary of what was done
