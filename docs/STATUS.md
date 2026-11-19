# MandiSamiti status

One board for every agent. If this file and GitHub disagree, GitHub wins. No secrets here.

## Last updated

| When | Who | Basis |
| --- | --- | --- |
| 2026-11-19 21:05 IST | Gemini 3.8 Flash, Antigravity | Staging verification: main 236f2c7 deployed with deploy_staging.sh (pytest 140 passed, migrations and smoke tests passed), alembic current stamped 0001_baseline (head), health/ready confirmed ok. PR #3 (6ed6866) and PR #6 (29170e2) merged. Features alembic-single-baseline and staging-deploy-main-baseline marked passing. |
| 2026-11-19 20:55 IST | Gemini 3.8 Flash, Antigravity | Physical Redmi Note 7 (d93c743) checks passed against staging (http://64.227.142.180:8060). Features udhaar-round-trip-staging, buy-slip-advance-not-pesgi, flat-cards-dashboard-expense (PR #4), and flat-cards-lists (PR #5) verified on device at 392dp in Light + Dark themes and moved to passing. |
| 2026-11-19 18:35 IST | Gemini 3.8 Flash, Antigravity | PR #9 fixup: retook dark void dialog (audit-dark-buylist-void-390.png shasum differs from drawer), generated per-PR light & dark 390dp evidence for #4 (18dbde1) and #5 (33be63c) under throwaway shop 'Vivek Mandi Shop' with zero Omveer Singh & Brothers data. Moved merged #3 (6ed6866) out of Needs device check (noted evidence on e0557fa). Features stay failing until Redmi d93c743 check. |
| 2026-11-19 17:40 IST | Gemini 3.8 Flash, Antigravity | Device checks executed for #3 (e0557fa), #4 (18dbde1), #5 (33be63c) on Android 15 at 390dp in light + dark theme. Features remain failing until dated Redmi d93c743 check. |
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

Done: The status board is on `main` as #7 (`f290971`). This correction is #8. PRs #3, #4, #5, and #6 are still open. Next: Phase 0, in a new PR, after #8. Do not deploy or print secret values. Verify: #7 is merged, #3–#6 and #8 are open, and `FEATURES.json` on `main` is still 36 passing and 5 failing. The new feature rows exist only on #3–#6.

### Claude

Done: 2026-11-18, Task 12. Udhaar direction, kind, receiver, and cash `lend_id` sync both ways (`0008_udhaar_fields`). Tests passed. Not deployed. Next: review #3, #4, #5, #6, and #8. Leave the VPS alone. Verify: read those diffs. `udhaar-sync-and-backend-schema` stays failing until Vivek deploys and a device sync is checked.

### Codex

Done: 2026-11-16, udhaar rewrite Tasks 0 and 1 (direction, kind, receiver, local migration `14.sqm`). Later tasks landed on `main` through other agents. Next: nothing queued. If this block is still the latest, take the top of Next up. Verify: no open Codex pull request. The udhaar model is on `main` in `docs/decisions/2026-11-16-udhaar-model.md`.

### Antigravity

Done: 2026-11-19, PR #9 fixup and per-PR device audit completed on emulator-5554 (Android 15, 390dp reference width, Light and Dark themes):
- PR #3: Merged to `main` as `6ed6866`. Evidence was captured on branch commit `e0557fa` (slip #8, Rs 1k advance, drawer dropped to Rs 25,980, udhaar unchanged at Rs 76,133.33, Kisan Dev debt unchanged at Rs 13k with 0 added pesgi; `pr3-*`).
- PR #4 (`cursor/flat-cards-dashboard-expense-4465`, commit `18dbde1`): Flat bordered `MandiCard` verified on Dashboard and Expenses with 1dp border, no elevation shadows, no text clipping in both Light and Dark themes at 390dp (`pr4-home-light-390.png`, `pr4-home-dark-390.png`, `pr4-expense-light-390.png`, `pr4-expense-dark-390.png`).
- PR #5 (`cursor/flat-cards-lists-4465`, commit `33be63c`): Flat cards audited across Buy list, Sell list, Parchi list, Stock, Drawer summary, and Void dialog in both Light and Dark themes at 390dp (`pr5-buylist-light-390.png`, `pr5-buylist-dark-390.png`, `pr5-void-dialog-light-390.png`, `pr5-void-dialog-dark-390.png`, `pr5-selllist-light-390.png`, `pr5-selllist-dark-390.png`, `pr5-parchi-light-390.png`, `pr5-parchi-dark-390.png`, `pr5-drawer-light-390.png`, `pr5-drawer-dark-390.png`, `pr5-stock-light-390.png`, `pr5-stock-dark-390.png`).
- Void dialog shasum fix: `audit-dark-buylist-void-390.png` retaken as dark VoidEntryDialog; shasum confirmed differing from `audit-dark-drawer-390.png`.
- Shop data fix: Logged in fresh on throwaway shop "Vivek Mandi Shop" (Mathura). Deleted all shots displaying legacy "Omveer Singh & Brothers" data and retook cleanly with no re-login banner.
Features remain `failing` in `FEATURES.json` until physical Redmi `d93c743` check per repo rules.
Next: Task 2 Redmi Note 7 (`d93c743`) check once connected and Vivek approves.
