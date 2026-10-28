# MandiSamiti Revamp Runbook (for every AI)

Read this file, then work out which AI you are, then do only that AI's tasks. You do not need Vivek to tell you what to do next. This file tells you when to start, what you own, when to wait and when to stop.

Master plan with all code: `projects/MandiSamiti/docs/superpowers/plans/2026-10-27-mandisamiti-complete-revamp.md` (call it THE PLAN). Spec: `projects/MandiSamiti/docs/superpowers/specs/2026-10-27-mandisamiti-revamp-design.md`. Hub rules: `/Users/appworx/Desktop/Vivek-K/CLAUDE.md`.

## 1. Who are you?

| If you are | Your name here | Your tasks (numbers from THE PLAN) | Your worktree |
|---|---|---|---|
| Claude Code | `claude` | 2, 3, then 1, 4, 5, 6, 7, then integrate, 17, 18 | `projects/MandiSamiti` (main checkout), branch `feat/mandisamiti-revamp` |
| Codex or OpenCode | `codex` | 8, 9, 10 | `projects/MandiSamiti-tests`, branch `feat/mandisamiti-revamp-tests` |
| Gemini or Antigravity | `gemini` | 11, 12, 13, 14, 15, then 16 | `projects/MandiSamiti-ui`, branch `feat/mandisamiti-revamp-ui` |

Hermes is not part of this plan.

If you are none of these, stop and ask Vivek.

## 2. Gates (git tags tell you when to start)

Tags are shared by all worktrees. Check one with `git -C /Users/appworx/Desktop/Vivek-K/projects/MandiSamiti tag -l <name>`. Empty output means the gate is not open yet.

| Tag | Set by | Meaning | Who waits for it |
|---|---|---|---|
| `revamp-contracts` | claude, after Tasks 2 and 3 | Models, repository interfaces and `BuySellMath` signatures are frozen | codex, gemini |
| `revamp-data-ready` | claude, after Tasks 5 and 7 | Real repositories, sync and backend are on `feat/mandisamiti-revamp` | codex (to run Tasks 9, 10 green), gemini (to swap fakes for real wiring) |
| `revamp-integrated` | claude, after merging all three branches and passing the gates | One branch holds everything | gemini (Task 16) |

If your gate is closed, do not guess. Either do the allowed pre-work in section 4 or write your status as `WAITING` and check the tag again later. Do not poll more often than every few minutes.

## 3. Setup (do it once, skip if the folder exists)

Run from `/Users/appworx/Desktop/Vivek-K/projects/MandiSamiti` (the main checkout).

- claude: `git checkout -b feat/mandisamiti-revamp` (if the branch exists, check it out).
- codex, after `revamp-contracts` exists: `git worktree add ../MandiSamiti-tests -b feat/mandisamiti-revamp-tests revamp-contracts`
- gemini, after `revamp-contracts` exists: `git worktree add ../MandiSamiti-ui -b feat/mandisamiti-revamp-ui revamp-contracts`

Work only inside your own worktree from then on.

## 4. What each AI does

### claude
1. Task 2, then Task 3, in that order (TDD steps in THE PLAN). Commit the plan, spec and this runbook in the first commit.
2. `git tag revamp-contracts`. Write your status as `CONTRACTS COMMITTED <hash>`.
3. Tasks 1, 4, 5, 6, 7 in order. Each ends green and committed.
4. `git tag revamp-data-ready`.
5. When `/Users/appworx/Desktop/Vivek-K/tmp/revamp-status/codex.md` and `gemini.md` both say `DONE` (or Vivek says "integrate"): merge `feat/mandisamiti-revamp-tests` and `feat/mandisamiti-revamp-ui` into `feat/mandisamiti-revamp`, resolve conflicts, run the Task 17 gates, then `git tag revamp-integrated`.
6. Task 18. No push and no deploy without Vivek's approval.

You must not edit `composeApp/.../ui/**`, `App.kt`, or the Task 8/9/10 test files. The only exception is the five new `wipe*` calls in `LogoutUseCase`.

### codex
1. Wait for `revamp-contracts`. Then do the setup in section 3.
2. Task 8 now. It runs green without waiting: `./gradlew :core-domain:jvmTest`.
3. Tasks 9 and 10: write every test now and commit it. Tests that cannot compile or pass until `revamp-data-ready` go in `docs/handoffs/tests-blockers.md` with the reason. Never delete, skip or weaken a test to get green.
4. After `revamp-data-ready`: `git merge feat/mandisamiti-revamp`, run `./gradlew :core-data:jvmTest` and `cd backend && .venv/bin/python -m pytest -q`. Report each failure with its assertion text in your status file. Do not edit `src/commonMain` or `backend/app`. Claude fixes code bugs.
5. Status `DONE` when everything is green or every remaining failure is reported.

### gemini
1. Wait for `revamp-contracts`. Then do the setup in section 3.
2. Read `docs/design-system/DESIGN-SYSTEM.md` section 2.1 and section 5, the project `DESIGN.md`, and the existing `DealEntryScreen.kt`, `ReceiptPreviewScreen.kt`, `DashboardScreen.kt` for layout and theme patterns.
3. Tasks 11 to 15. Build against the frozen interfaces. Until `revamp-data-ready`, wire `App.kt` with in-memory fakes of the repository interfaces (in `composeApp/src/jvmTest` or one debug-only file) and keep the real wiring as one marked block.
4. After `revamp-data-ready`: `git merge feat/mandisamiti-revamp`, switch to the real repositories, run `./gradlew :composeApp:jvmTest`. Status `DONE`.
5. Task 16 only after `revamp-integrated`. Device serial `d93c743`, mirror with `scrcpy -s d93c743`. Throwaway shop for writes; on real data only look. List everything you could not verify.

You must not edit `core-domain`, `core-database`, `core-data` or `backend`.

## 5. Rules for everyone

- **Frozen contracts.** After `revamp-contracts`, the type names and signatures in Tasks 2, 3 and 4 do not change. If one is wrong, write `BLOCKED: <what and why>` in your status file and continue with other work. Only claude changes a contract, and then it retags `revamp-contracts` and says so in its status file.
- **One owner per file.** If you need a change in a file you do not own, write it in your status file under `REQUESTS`. Do not edit it.
- **TDD.** Failing test, run it, minimal code, run it, commit. Never claim done without running the command and reading the result.
- **Money.** `Long` paisa and `Long` grams only. Half-up rounding through `MandiMathEngine`. No `Double` in money paths.
- **Schema.** Never edit `1.sqm`, `2.sqm` or an existing `CREATE TABLE`.
- **NGDL UI.** Tokens from `ui/theme/Color.kt`, no hex, no emoji, vector icons, contrast 4.5:1 in light and dark, cards elevated, no horizontal scroll at 390dp, 48dp touch targets.
- **Secrets.** Never print, log, write or commit a key, token, password or `.env` value. Never paste raw API errors or request URLs. Telegram alerts only through `/Users/appworx/Desktop/Vivek-K/scripts/notify_telegram.py`, with short text and no log bodies.
- **Git.** Commit often on your own branch. Never push, never force, never touch another AI's branch except the merge steps above. Commit trailers: claude uses `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`; others use their own name.
- **Real data.** Do not write to a real shop's data. Use a throwaway shop.
- **Session length.** If your context gets long (about 150k tokens or 100 turns), finish the current step, write a handoff into your status file, and tell Vivek to start a fresh session. The new session reads this runbook and your status file and continues.

## 6. Status files (how AIs talk to each other)

Each AI keeps its own file, so nobody edits the same file. Path: `/Users/appworx/Desktop/Vivek-K/tmp/revamp-status/<name>.md` (create the folder if needed; it is outside git on purpose). Update it after every task.

```
# <name> status
Updated: <date time>
State: WAITING | WORKING | BLOCKED | DONE
Current task: <number and name>
Done: <task numbers with commit hashes>
Open: <what is left>
REQUESTS: <changes you need from another AI, with file and reason>
BLOCKED: <what and why, or none>
Unverified: <anything you could not test>
```

At the start of every session and before each new task: read this runbook, your own status file, then the other two status files.

## 7. Done means

- Every test command in THE PLAN for your tasks was run and passed, and you can quote the result.
- Your status file says `DONE` with the commit hashes.
- Anything you could not verify is listed under `Unverified`.
