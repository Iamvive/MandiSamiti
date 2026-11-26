# MandiSamiti status

One board for every agent. If this file and GitHub disagree, GitHub wins. No secrets here.

## Last updated

| When | Who | Basis |
| --- | --- | --- |
| 2026-11-26 18:21 IST | Claude Code | Branch `mgr/2-backend-only-harden-post-api-v`: added a fixed-window assertion to the upload-limit expiry test. Backend pytest 178 passed. Not pushed, not deployed. |
| 2026-11-26 18:15 IST | Antigravity | Branch `mgr/2-backend-only-harden-post-api-v`: hardened `POST /api/v1/events` with per-shop rate limit (429) and shop existence check (409). Pytest was not run by me. |
| 2026-11-26 16:17 IST | Antigravity | Branch `fix/sentry-flavor-env`: flavor-tagged Sentry `environment` (`staging`/`prod`). Verified test crash on Redmi `d93c743` (`MANDISAMITI-ANDROID-1`), phone scrubbed, `:composeApp:jvmTest` and `:composeApp:installStagingDebug` passed. |
| 2026-11-26 15:24 IST | Claude Code | Branch `fix/deploy-local-files`: backend deploy packaging fix. pytest 172 passed. Staging deploy of `c26af63` ran clean. Production not deployed. |
| 2026-11-26 15:05 IST | Claude Code | PR #47 merged (`d4eec34`), deployed to staging, checked on Redmi `d93c743`. `clickstream-telemetry` passing. Deploy script needs a fix. |
| 2026-11-26 14:06 IST | Claude Code | Branch `feat/telemetry-pipeline`: clickstream telemetry. pytest 169 passed, jvmTest 660 passed, staging APK built. No device check, not deployed. |
| 2026-11-26 13:20 IST | Antigravity | Consolidated `fix/hide-voided-entries-from-ui` (`198dc31`) and `appworx/android-sentry-crash-reporting` (`7400a47`) into PR #45, merged to `main` (`eb35ced`). `./gradlew jvmTest :composeApp:assembleStagingDebug` passed (`BUILD SUCCESSFUL`). Deleted merged local branches. |
| 2026-11-25 IST | Claude Code (cloud) | Branch `appworx/android-sentry-crash-reporting`: opt-in Sentry crash reporting in the Android app. Backend pytest 140 passed. Gradle build and device check not run (no Android SDK in cloud). |
| 2026-11-19 23:10 IST | Antigravity (Claude Sonnet) | Integration branch `integration/open-prs-november` created from `main` (236f2c7). PRs #4, #5, #9, #10, #11 merged cleanly. FEATURES.json: 47 features, 42 passing, 5 failing. PROGRESS.md: all chronological session entries preserved. Plan: `docs/superpowers/plans/2026-11-20-mandi-open-prs-integration.md`. |
| 2026-11-19 21:05 IST | Gemini 3.8 Flash, Antigravity | Staging verification: main 236f2c7 deployed with deploy_staging.sh (pytest 140 passed, migrations and smoke tests passed), alembic current stamped 0001_baseline (head), health/ready confirmed ok. PR #3 (6ed6866) and PR #6 (29170e2) merged. Features alembic-single-baseline and staging-deploy-main-baseline marked passing. |
| 2026-11-19 20:55 IST | Gemini 3.8 Flash, Antigravity | Physical Redmi Note 7 (d93c743) checks passed against staging (http://64.227.142.180:8060). Features udhaar-round-trip-staging, buy-slip-advance-not-pesgi, flat-cards-dashboard-expense (PR #4), and flat-cards-lists (PR #5) verified on device at 392dp in Light + Dark themes and moved to passing. |
| 2026-11-19 18:35 IST | Gemini 3.8 Flash, Antigravity | PR #9 fixup: retook dark void dialog (audit-dark-buylist-void-390.png shasum differs from drawer), generated per-PR light & dark 390dp evidence for #4 (18dbde1) and #5 (33be63c) under throwaway shop 'Vivek Mandi Shop' with zero Omveer Singh & Brothers data. Moved merged #3 (6ed6866) out of Needs device check (noted evidence on e0557fa). Features stay failing until Redmi d93c743 check. |
| 2026-11-19 17:40 IST | Gemini 3.8 Flash, Antigravity | Device checks executed for #3 (e0557fa), #4 (18dbde1), #5 (33be63c) on Android 15 at 390dp in light + dark theme. Features remain failing until dated Redmi d93c743 check. |
| 2026-11-19 16:58 IST | Grok 4.7, Cursor cloud | `main` `f290971`. #7 is merged. Open PRs are #3–#6 and #8. `FEATURES.json`: 36 passing, 5 failing. |

## In flight

| Branch | PR | Agent | State |
| --- | --- | --- | --- |
| `mgr/2-backend-only-harden-post-api-v` | — | Antigravity | Hardened `POST /api/v1/events` with rate limit and shop existence check. Backend pytest 178 passed (Claude Code, 2026-11-26). Local only: not pushed, not deployed. |
| `fix/sentry-flavor-env` | — | Antigravity | Ready for review. `android-crash-reporting` passing on Redmi `d93c743`. |
| `fix/deploy-local-files` | this PR | Claude Code | Open for review. `deploy-excludes-local-venv` passing; on staging. |
| `integration/open-prs-november` | — | Antigravity | Integration branch containing PRs #4, #5, #9, #10, #11. Ready for Vivek's review and merge to main. 42 passing / 5 failing. |

### Merged into integration/open-prs-november

| Branch | PR | Agent | State |
| --- | --- | --- | --- |
| `cursor/flat-cards-dashboard-expense-4465` | [#4](https://github.com/Iamvive/MandiSamiti/pull/4) | Cursor cloud (Grok) | Merged into integration branch. Redmi evidence: `passing`. |
| `cursor/flat-cards-lists-4465` | [#5](https://github.com/Iamvive/MandiSamiti/pull/5) | Cursor cloud (Grok) | Merged into integration branch. Redmi evidence: `passing`. |
| `antigravity/device-audit-2026-11-19` | [#9](https://github.com/Iamvive/MandiSamiti/pull/9) | Antigravity | Merged into integration branch via PR #11 chain. |
| `antigravity/redmi-device-checks-2026-11-19` | [#10](https://github.com/Iamvive/MandiSamiti/pull/10) | Antigravity | Merged into integration branch via PR #11 chain. |
| `antigravity/staging-deploy-evidence-2026-11-19` | [#11](https://github.com/Iamvive/MandiSamiti/pull/11) | Antigravity | Merged into integration branch. |

## Waiting on Vivek

| Item | Owner | Link | Note |
| --- | --- | --- | --- |
| Staging deploy script | Vivek | `backend/scripts/deploy_staging.sh` | Fails at migration: exclude `venv/` and add `backend/.dockerignore`. |
| Backend observability go-live | Vivek | [owner actions](product/observability-owner-actions.md) | Telegram ops bot, Sentry backend DSN (`appworkx/mandisamiti-backend`), UptimeRobot monitor on `/health/ready`, digest cron, prod deploy. Code is on `main`, off until set. |
| Merge integration branch to main | Vivek | `integration/open-prs-november` | Merge this branch to main. Contains PRs #4, #5, #9, #10, #11. All conflicts resolved. 42 passing / 5 failing. |
| Close PRs #4, #5, #9, #10, #11 on GitHub | Vivek | — | Once integration branch merges to main, close the individual PRs. |
| Khata filter chips | Vivek | `khata-chips-explained`, `khata-filter-explainer-card` | Both stay failing. The screen is unreachable since Parchi replaced Khata. No product decision yet. |

## Needs review

Owner of these rows: Claude. No review on any open PR.

| Item | Link | What to check |
| --- | --- | --- |
| Buy-slip अग्रिम | [#3](https://github.com/Iamvive/MandiSamiti/pull/3) | Slip cash stays linked. It is not a second way to create a pesgi. |
| Flat cards, Home and Expenses | [#4](https://github.com/Iamvive/MandiSamiti/pull/4) | `MandiCard` only. No new shadow token. |
| Flat cards, lists and drawer | [#5](https://github.com/Iamvive/MandiSamiti/pull/5) | Border replaces elevation. Long-press void still opens. |
| Alembic baseline | [#6](https://github.com/Iamvive/MandiSamiti/pull/6) | Empty database reaches today's schema. Existing databases need the stamp above. |
| Status board after the merge | [#8](https://github.com/Iamvive/MandiSamiti/pull/8) | Confirms #7 is on `main` and #3–#6 are still open. Docs only. |

## Needs device check

Owner of these rows: Antigravity.

Staging URL only. Throwaway shop ("Vivek Mandi Shop"). Light and dark at 390dp. No real user data. Never point a device at `10.0.2.2`. A feature stays failing until a dated Redmi `d93c743` check, or a test run when the feature is tests only.

All open device check features verified on physical Redmi Note 7 (`d93c743`) on 2026-11-19:
- `udhaar-round-trip-staging`: PASSING on Redmi `d93c743` (2026-11-19).
- `buy-slip-advance-not-pesgi`: PASSING on Redmi `d93c743` (2026-11-19).
- PR #4 (`flat-cards-dashboard-expense`, build `18dbde1`): PASSING on Redmi `d93c743` (2026-11-19) in Light and Dark themes (`pr4-home-*-redmi-390.png`, `pr4-expense-*-redmi-390.png`).
- PR #5 (`flat-cards-lists`, build `33be63c`): PASSING on Redmi `d93c743` (2026-11-19) across all 6 screens in Light and Dark themes (`pr5-*-redmi-390.png`).

| Today | Link | What to tap | Status |
| --- | --- | --- | --- |
| Flat cards, Home and Expenses | [#4](https://github.com/Iamvive/MandiSamiti/pull/4) | Flat surface and a 1dp border. No raised card. | Build commit `18dbde1`: PASSING on Redmi `d93c743` (2026-11-19). Home (`pr4-home-light-redmi-390.png`, `pr4-home-dark-redmi-390.png`) and Expenses (`pr4-expense-light-redmi-390.png`, `pr4-expense-dark-redmi-390.png`) verified at 392dp in Light + Dark themes with 1dp border and zero elevation shadows. |
| Flat cards, lists and drawer | [#5](https://github.com/Iamvive/MandiSamiti/pull/5) | Same on buy, sell, parchi, stock, and the drawer. Long-press a buy row: void still opens. | Build commit `33be63c`: PASSING on Redmi `d93c743` (2026-11-19). Buy list (`pr5-buylist-light-redmi-390.png`, `pr5-buylist-dark-redmi-390.png`), Void dialog (`pr5-void-dialog-light-redmi-390.png`, `pr5-void-dialog-dark-redmi-390.png`), Sell list (`pr5-selllist-light-redmi-390.png`, `pr5-selllist-dark-redmi-390.png`), Parchi (`pr5-parchi-light-redmi-390.png`, `pr5-parchi-dark-redmi-390.png`), Stock (`pr5-stock-light-redmi-390.png`, `pr5-stock-dark-redmi-390.png`), and Drawer summary (`pr5-drawer-light-redmi-390.png`, `pr5-drawer-dark-redmi-390.png`) verified at 392dp in Light + Dark themes. Long-press on buy row opens VoidEntryDialog cleanly. |

## Needs deploy

Owner: Vivek. Agents never touch the VPS.

| Item | Link | State |
| --- | --- | --- |
| Baseline stamp & deploy | [#6](https://github.com/Iamvive/MandiSamiti/pull/6) | Deployed on staging. `alembic stamp 0001_baseline` executed, `alembic current` confirms `0001_baseline (head)`. Main `236f2c7` active. Pytest 140 passed. |
| Udhaar sync schema | `udhaar-sync-and-backend-schema` | On `main`. Tests passed. Backend deployed to staging, production not deployed. |
| Offsite backup drill | `offsite-backup-restore-drill`, `backend/scripts/backup_offsite.sh`, `backend/scripts/restore_test.sh` | Scripts are in the repo. Not run on the VPS. Cron not installed. Stays failing. |

## Next up

Top of the list first. Deploy steps stay with Vivek. Name files and variables only. Never copy a secret value.

1. Phase 0, security cleanup.
   - Committed backend database: `backend/mandi_backend.db`.
   - `backend/docker-compose.yml` contains literal `DATABASE_URL`, `SECRET_KEY`, `OTP_STATIC_CODE`, and `POSTGRES_PASSWORD`.
   - `backend/docker-compose.staging.yml` sets `OTP_STATIC_ENABLED` and reads `SECRET_KEY`, `OTP_STATIC_CODE`, and `POSTGRES_PASSWORD` from the environment.
   - Static OTP: `OTP_STATIC_ENABLED` and `OTP_STATIC_CODE` in `backend/app/config.py` and `backend/app/core/otp_sender.py`. [Known issues](docs/pilot/KNOWN-ISSUES.md) already calls them dead code.
   - No CI on pull requests. The repo has no workflow files.
   - Staging is plain HTTP. The pilot needs HTTPS. Agents do not change the server.
2. Pilot, from [the product spec](docs/PRODUCT_SPEC.md).
   - Per-person login. The shop shares one account today.
   - Show who entered what.
   - A galla tally that points at the missing entry.

Still failing, and not in that order: `input-audit-medium-low` (Group A of the core trade screens is unfinished).

## Parked / dropped

| Item | Reason |
| --- | --- |
| Grain udhaar | Out of the money udhaar rewrite. `GrainLendEntry` and `LendEntryScreen` stay, and the new Udhaar screens do not link to them. [Plan](docs/superpowers/plans/2026-11-16-udhaar-rewrite-plan.md). |
| Taken pesgi | The add sheet has no taken pesgi. The toggle is only on उधार दिया. [Task 15 brief](docs/superpowers/plans/2026-11-19-udhaar-task15-pesgi-toggle-brief.md). |
| Create-pesgi on the buy slip | A buy-slip अग्रिम is slip cash, not a new pesgi. Pesgi is created only on the Udhaar add sheet. [#3](https://github.com/Iamvive/MandiSamiti/pull/3). |

## Known risks

- #3, #4, #5, and #6 conflict with each other, and with the status-board entry now on `main`, on `PROGRESS.md`. They also conflict with each other on `FEATURES.json`. Keep all new rows.
- A staging upgrade after #6, without `alembic stamp 0001_baseline`, tries to recreate tables that already exist.
- The backend database and compose secrets are in git. Do not print them. A history rewrite needs Vivek.
- Recent udhaar tasks were marked passing from emulator walks. The passing rule for a device feature is a dated Redmi `d93c743` check.
- `docs/PRODUCT_SPEC.md` sections 2 to 5 are still open.

## Last handover

Each agent owns only their block.

### Cursor cloud (Grok 4.7)

**Status: ON LEAVE for the next few days.**

Done (all merged or on integration branch):
- PRs #3, #4, #5, #6, #7, #8 delivered and merged to main or integration branch.
- Operating contract `AGENTS.md`, product spec `docs/PRODUCT_SPEC.md`, buy-slip advance separation, Alembic single baseline, flat MandiCard styling, shared status board.

**Queue while on leave (Antigravity to execute):**
- Day 1: Fix Parchi-to-Khata back-navigation bug (back should return to Parchi list, not Home). Files: `ParchiListScreen.kt`, `KhataScreen.kt`.
- Day 1: Purge committed `backend/mandi_backend.db` from git tracking (`git rm --cached`). Update `.gitignore`.
- Day 2: Parameterize `backend/docker-compose.yml` plaintext credentials with environment variable references. Add `.env.staging.template`.
- Day 2: Delete static OTP bypass code from `backend/app/config.py` and `backend/app/core/otp_sender.py`.
- Day 3: Create `.github/workflows/ci.yml` running JVM tests and Pytest on every PR.

### Claude

Done: 2026-11-18, Task 12. Udhaar direction, kind, receiver, and cash `lend_id` sync both ways (`0008_udhaar_fields`). Tests passed. Not deployed. `udhaar-sync-and-backend-schema` stays failing until Vivek deploys and a device sync is checked.

Done: 2026-11-26, clickstream telemetry on `feat/telemetry-pipeline`. Verify: `cd backend && pytest`, then `./gradlew jvmTest :composeApp:assembleStagingDebug`. Merged as PR #47, deployed to staging and checked on Redmi on 2026-11-26. Open follow-ups: deploy script fix, per-shop upload limit, 500 when the shop row is missing.

Done: 2026-11-26, deploy packaging fix on `fix/deploy-local-files`. Verify: `cd backend && pytest`. `deploy_staging.sh` ran end to end on staging. Next: review and merge.

Done: 2026-11-26, review fixes on `mgr/2-backend-only-harden-post-api-v`. Verify: `cd backend && pytest`. Next: Vivek accepts the commits, then push and PR.

### Codex

Done: 2026-11-16, udhaar rewrite Tasks 0 and 1. No open Codex PRs. Udhaar model in `docs/decisions/2026-11-16-udhaar-model.md`.

### Antigravity

Done: 2026-11-26, on `mgr/2-backend-only-harden-post-api-v`: per-shop upload limit and the missing-shop 500 follow-ups from clickstream telemetry are addressed. Backend pytest 178 passed (Claude Code, 2026-11-26). Not checked on staging.

Done: 2026-11-19, Tasks 1-4 complete.
- PR #9 fixup: void dialog screenshot retaken, per-PR Redmi evidence organized, docs corrected (commit `ea5632d`).
- PR #10: Physical Redmi Note 7 (d93c743) audit — `udhaar-round-trip-staging`, `buy-slip-advance-not-pesgi`, flat cards #4/#5 all verified passing with 23 hardware screenshots (commit `a4cade9`).
- PR #11: Live staging deploy evidence, alembic baseline confirmed `0001_baseline (head)` (commit `a70a2fc`).
- Integration branch `integration/open-prs-november` created: PRs #4, #5, #9, #10, #11 merged, all conflicts resolved, 47 features (42 passing / 5 failing).

Next (Antigravity incharge while Grok is on leave):
1. Push `integration/open-prs-november` to remote for Vivek's review and merge.
2. Execute Grok's queue: navigation fix → security hygiene → CI workflow → pilot features.
3. Verify 725+ JVM tests and 140 Pytest tests pass on the integration branch before each PR.
