# MandiSamiti status

One board for every agent. If this file and GitHub disagree, GitHub wins. No secrets here.

## Last updated

| When | Who | Basis |
| --- | --- | --- |
| 2026-11-19 16:58 IST | Grok 4.7, Cursor cloud | `main` `f290971`. #7 is merged. Open PRs are #3–#6 and #8. `FEATURES.json`: 36 passing, 5 failing. |

## In flight

Owner of these rows: Cursor cloud.

| Branch | PR | Agent | State |
| --- | --- | --- | --- |
| `cursor/buy-slip-advance-not-pesgi-4465` | [#3](https://github.com/Iamvive/MandiSamiti/pull/3) | Cursor cloud | Buy-slip अग्रिम stays on the bill and out of the pesgi list. JVM test green. No review yet. |
| `cursor/flat-cards-dashboard-expense-4465` | [#4](https://github.com/Iamvive/MandiSamiti/pull/4) | Cursor cloud | Home and Expenses use flat `MandiCard`. No review yet. |
| `cursor/flat-cards-lists-4465` | [#5](https://github.com/Iamvive/MandiSamiti/pull/5) | Cursor cloud | Lists and the drawer use a flat border. Long-press void kept. No review yet. |
| `cursor/alembic-baseline-4465` | [#6](https://github.com/Iamvive/MandiSamiti/pull/6) | Cursor cloud | `0002`–`0008` folded into `0001_baseline`. Backend tests green. Needs a stamp on the next deploy. |
| `cursor/status-board-pr7-merged-45f5` | [#8](https://github.com/Iamvive/MandiSamiti/pull/8) | Cursor cloud | Marks #7 merged and leaves #3–#6 as the open pulls. Docs only. |

## Waiting on Vivek

| Item | Owner | Link | Note |
| --- | --- | --- | --- |
| Merge the open PRs | Vivek | [#3](https://github.com/Iamvive/MandiSamiti/pull/3) [#4](https://github.com/Iamvive/MandiSamiti/pull/4) [#5](https://github.com/Iamvive/MandiSamiti/pull/5) [#6](https://github.com/Iamvive/MandiSamiti/pull/6) | #7 is merged. #3–#6 each still add a `FEATURES.json` row and a `PROGRESS.md` entry, and those entries also meet the status-board entry now on `main`. Keep every new row. |
| Stamp, then upgrade | Vivek | [#6](https://github.com/Iamvive/MandiSamiti/pull/6), [migrations README](backend/migrations/README.md) | After #6, the next staging deploy must `alembic stamp 0001_baseline` before upgrade. The live version is still `0008_udhaar_fields`. |
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

Staging URL only. Throwaway shop. Light and dark at 390dp. No real user data. Never point a device at `10.0.2.2`. A feature stays failing until a dated Redmi `d93c743` check, or a test run when the feature is tests only.

| Today | Link | What to tap |
| --- | --- | --- |
| Buy-slip अग्रिम drawer flow | [#3](https://github.com/Iamvive/MandiSamiti/pull/3) | अग्रिम stays on the bill, leaves the drawer, and does not show as an unlinked pesgi. |
| Flat cards, Home and Expenses | [#4](https://github.com/Iamvive/MandiSamiti/pull/4) | Flat surface and a 1dp border. No raised card. |
| Flat cards, lists and drawer | [#5](https://github.com/Iamvive/MandiSamiti/pull/5) | Same on buy, sell, parchi, stock, and the drawer. Long-press a buy row: void still opens. |

## Needs deploy

Owner: Vivek. Agents never touch the VPS.

| Item | Link | State |
| --- | --- | --- |
| Udhaar sync schema | `udhaar-sync-and-backend-schema` | On `main`. Tests passed. Backend not deployed. Stays failing. |
| Baseline stamp | [#6](https://github.com/Iamvive/MandiSamiti/pull/6) | After merge only. `alembic stamp 0001_baseline`, then upgrade. |
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

Done: The status board is on `main` as #7 (`f290971`). This correction is #8. PRs #3, #4, #5, and #6 are still open. Next: Phase 0, in a new PR, after #8. Do not deploy or print secret values. Verify: #7 is merged, #3–#6 and #8 are open, and `FEATURES.json` on `main` is still 36 passing and 5 failing. The new feature rows exist only on #3–#6.

### Claude

Done: 2026-11-18, Task 12. Udhaar direction, kind, receiver, and cash `lend_id` sync both ways (`0008_udhaar_fields`). Tests passed. Not deployed. Next: review #3, #4, #5, #6, and #8. Leave the VPS alone. Verify: read those diffs. `udhaar-sync-and-backend-schema` stays failing until Vivek deploys and a device sync is checked.

### Codex

Done: 2026-11-16, udhaar rewrite Tasks 0 and 1 (direction, kind, receiver, local migration `14.sqm`). Later tasks landed on `main` through other agents. Next: nothing queued. If this block is still the latest, take the top of Next up. Verify: no open Codex pull request. The udhaar model is on `main` in `docs/decisions/2026-11-16-udhaar-model.md`.

### Antigravity

Done: 2026-11-19, Task 15. Pesgi is created from the Udhaar add sheet. Emulator walk, light and dark, 390dp, 725 JVM tests. Not a Redmi check. Next: the three rows in Needs device check. Staging URL, throwaway shop, light and dark at 390dp. Verify: long-press void on a buy row still opens. Do not write real shop data. Do not use `10.0.2.2`.
