# Phase 9 — Polish, Responsive, Docs & End-to-End

## Goal
Ship quality: responsive layout, theme polish, README, seed demo content, and
a verified end-to-end run of the full agent journey.

## Work
- Responsive: desktop three-panel; ≤1024px collapses to main + right panel
  toggle; mobile (<768px) tab bar switching Chat | Files | Preview | Terminal
  | Git | Actions (panels render in fullscreen sheets), sidebar becomes a
  drawer. Touch targets, safe areas.
- Theme: audit contrast in both modes; consistent cards, borders, subtle
  animations (action check-pop, streaming caret, status dots), professional
  typography (Inter/system), scrollbar styling. Loading skeletons and empty
  states everywhere (no blank panels).
- Onboarding/Docs: README.md at repo root (what it is, quickstart: start
  backend + frontend, env vars incl. AI key setup, seeded logins, how the
  agent loop works, tool list, security model, screenshots section);
  in-app Help modal with the same quickstart + a sample prompt.
- Demo content: seed script creating one showcase project (from
  fullstack-react-springboot template) with a sample conversation + actions
  so a fresh install isn't empty; toggleable via env SEED_DEMO=true.
- E2E verification: scripted full journey — new project → requirement →
  plan → start building → files appear → build fails once (inject error) →
  agent fixes → build ✓ → preview runs → git checkpoint visible. Captured as
  a repeatable script under scripts/e2e-demo.md + automated Vitest Playwright
  smoke if feasible; documented results.
- Final sweep: dead links, console errors, WS reconnect handling (exponential
  backoff, resync), unsaved-changes guards on navigation, 404 page, favicon,
  page titles.

## Acceptance criteria
- [ ] At mobile width the app is fully usable via tabs — chat, file tree,
      editor, terminal and preview all reachable and functional
- [ ] Dark and light themes both look consistent (no unreadable text /
      invisible borders on any panel)
- [ ] README covers quickstart, seeded credentials, env vars, agent loop,
      tools and security model well enough for a new dev to run it
- [ ] With SEED_DEMO=true a fresh database shows the showcase project with
      its conversation and actions; with false, lists are empty but UI shows
      friendly empty states
- [ ] The documented end-to-end journey runs green: requirement → plan →
      files → build ❌ → fix → build ✓ → preview renders the generated app
- [ ] No console errors during a normal session; WebSocket reconnects after
      a backend restart and resyncs agent action state

## Edge cases
- Flipping theme mid-agent-run doesn't interrupt streams
- Mobile rotation keeps current panel and scroll position
- Very long file paths/names truncation with tooltips
- Browser back/forward navigates IDE tabs sanely
