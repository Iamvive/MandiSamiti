# MandiSamiti — rules for AI coding agents

This file is the only source of truth for coding agents in this repo.
Read it before you change anything.
`CLAUDE.md` and `GEMINI.md` are symlinks to this file. Edit this file only.

## Shared status board

`docs/STATUS.md` is the one board for every agent.

- Read `docs/STATUS.md` before starting any work, and pick up items in your queue.
- Before you stop, update your rows in the same PR. Move items between sections, add your handover, and bump Last updated.
- Edit only the rows you own, so merge conflicts stay small.
- Never put secrets in it.
- Anything waiting on Vivek goes in Waiting on Vivek.
- If the board disagrees with GitHub, GitHub wins. Fix the board.

## Core rules

### Think before coding

- State your assumptions before you write code.
- If the request is unclear, ask one question. Wait for the answer.
- Name the tradeoffs when more than one design fits.
- Name inconsistencies between the request, the code, and these rules.
- Push back when the request looks wrong, or when a smaller change would do.
- Stop when you are confused. Say what is unclear.

### Simplicity first

- Build the smallest thing that works.
- Do not add abstractions, options, or error handling that the task does not need.
- Do not add features that nobody asked for.
- Delete dead code that your change created.

### Surgical changes

- Touch only the files and lines the task needs.
- Do not refactor, reformat, or clean up code outside the task.
- Do not remove comments or code you do not understand.
- If you see unrelated dead code, mention it. Leave it in place.
- Remove imports, variables, and functions that your change made unused.
- Every changed line must trace to the request.

### Goal-driven execution

- Define success as a check you can run.
- For a bug or a finished behavior, write or update the test first. Then make it pass.
- Loop until the checks pass.
- Write the naive correct version first.
- Optimize only after that version is correct. Keep the same results.
- Plan each step as: do the step, then run the check.
- For a trivial one-line fix, use judgment. Skip the ceremony.
- During feature discovery, follow the product testing rule below.

### Declarative over imperative

- Describe the outcome you want.
- Do not prescribe keystrokes or line-by-line edits unless a check depends on them.

## Working style

- Describe outcomes, not steps.
- Own the whole job, including deploy and hardening.
- Leave account, key, and domain steps to the owner.
- Filter with cheap deterministic code first.
- Call an LLM only after that filter is not enough.
- Aim at problems a test or a clear check can verify.
- Humans review. Agents write.
- Keep each diff small enough for a person who reviews about 30 minutes a day.
- Ship every pull request with a short plain-English summary.
- Add a mermaid diagram or a short walkthrough of what changed and why.
- Throwaway internal tools are allowed. Keep production code simple.

## MandiSamiti guardrails

### Git and secrets

- Never push to `main`.
- Work on a branch and open a pull request.
- Never force-push.
- Never rewrite git history unless Vivek explicitly approves that rewrite.
- Never commit, print, or log secrets, keys, passwords, tokens, or real user data.
- Never commit a `.db` file or a `.env` file.
- Read secrets from environment variables.
- Put placeholders only in `backend/.env.example`.

### Money and weights

- Store money as `Long` paisa. 1 INR = 100 paisa.
- Store weight as `Long` grams. 1 quintal = 100,000 g.
- Never use floating point for money or weight.

### Product testing (Vivek)

- While a feature is still being discovered, build the UI and the behavior first.
- Do not block that work on strict test-driven development.
- Write unit and regression tests when the flow and the design are final.
- A feature is done only after owner-level testing.
- Check light theme and dark theme, on a phone-sized screen, with real clicks.
- Do not write to real user data.
- Point device and emulator tests at the staging API URL.
- Never point a device or emulator test at `10.0.2.2`.
- Mark a feature `passing` only with a test run, or a Redmi `d93c743` check that includes the date.
- If you cannot run that check, leave the feature `failing` and say what is unverified.

### Voice

- Do not run voice tests, soundbox playback, or any audio unless Vivek asks for it.
- Do not make sound.
- In production, sound and voice follow the user setting only.

### UI

- Use design tokens from `ui/theme/Color.kt`.
- Keep text contrast at 4.5:1 or higher.
- Use `ImageVector` icons.
- Do not put Unicode emoji in the UI.

### Data and sync

- Commit every write to local SQLDelight first.
- Keep revisions append-only.
- Sync pending rows asynchronously. Do not write to the server before the local commit.

### Session tracking

- At the start of a product session, read `vaults/Vivek-K/wiki/hot.md`, then `PROGRESS.md`, then `git log -5`.
- Run `./init.sh`.
- If tests fail, or the tree is dirty from work you did not make, fix that first.
- Pick one `failing` feature from `FEATURES.json`. Work on that one only.
- In `FEATURES.json`, change only `status` and `evidence` on an existing entry.
- Do not delete or reword an existing feature entry. Add new entries at the end.
- When a feature status changes, update `FEATURES.json` and append one `PROGRESS.md` entry.
- That entry includes the date, the AI runtime, what changed, the evidence, and the next step.
- Update `vaults/Vivek-K/wiki/hot.md`, and bump the submodule pointer, only after the feature is tested and Vivek has approved it.
- At every stop, leave a paste-ready handover: what you did, what is next, and how to verify it.

### Verify

- After the code is final, run `./gradlew test` or `./gradlew jvmTest`.

## Stack

Verified against this repo on 2026-11-19.

- The app is Kotlin Multiplatform and Compose Multiplatform.
- Targets are Android, desktop JVM, and iOS. Android is the pilot target.
- Local data is SQLDelight on SQLite.
- Writes land locally first. `SyncEngine` pushes and pulls pending rows through `KtorMandiSyncApiClient`.
- The backend is FastAPI, SQLAlchemy, and Alembic.
- Deployed services are Postgres and Redis, as documented in `backend/.env.example`.
- If `DATABASE_URL` is unset, the backend code default is SQLite. Set `DATABASE_URL` for any shared or deployed run.

## How this file evolves

- The CTO (Goat CTO) reviews this file weekly.
- Change it only through a pull request.
- Vivek approves that pull request.
- The Head of Quality checks other pull requests against this file.
