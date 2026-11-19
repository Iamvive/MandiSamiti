# MandiSamiti Open PRs Integration & Pilot Execution Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Safely merge all five open pull requests (#4, #5, #9, #10, #11) in `Iamvive/MandiSamiti` into a single stable integration branch (`integration/open-prs-november`), verify Grok's contributions, establish Grok's multi-day task sequence, and map out remaining pilot tasks (security debt, features, known bugs) with milestones and owners so the system can advance autonomously while Vivek is away.

**Architecture:**
1. **Disjoint Tree Integration**: PR #4 (`cursor/flat-cards-dashboard-expense-4465`) and PR #5 (`cursor/flat-cards-lists-4465`) modify mutually exclusive Compose Kotlin UI files for MandiCard flat styling. PR #11 (`antigravity/staging-deploy-evidence-2026-11-19`) sits directly on top of PR #10 (`antigravity/redmi-device-checks-2026-11-19`), which sits directly on top of PR #9 (`antigravity/device-audit-2026-11-19`). PR #11 brings in all 23 Redmi Note 7 hardware audit screenshots and documentation.
2. **Conflict Surface Containment**: Code files merge cleanly with zero AST conflicts. Conflicts occur exclusively in `FEATURES.json`, `PROGRESS.md`, and `docs/STATUS.md`. The integration resolution adopts PR #11's canonical feature harness (42 passing / 5 failing) while preserving the complete chronological commit log.
3. **Multi-Agent Boundary Protocol**: Grok (Cursor Cloud / Grok 4.7) owns backend security hygiene and Parchi navigation fixes. Antigravity / Claude owns JVM regression test validation, Redmi Note 7 hardware verification, and Galla discrepancy indicators. Vivek owns offsite cron drills and final release tagging.

**Tech Stack:** Kotlin Multiplatform (Compose Multiplatform 1.6.11, Android SDK 34), Python FastAPI + SQLAlchemy + Alembic, PostgreSQL / SQLite, Gradle 8.x, Pytest.

---

## Global Constraints

- **Silent Testing Guardrail:** Never run voice tests, soundbox TTS playback, or trigger audio output. All JVM and backend tests must run silently.
- **Hardware Evidence Rule:** A feature status in `FEATURES.json` cannot be flipped to `passing` without dated Redmi Note 7 (`d93c743`) screenshot evidence or automated test runner log output.
- **Secrets Policy:** Never print, log, commit, or echo raw API keys, JWT secrets, database passwords, or staging credentials.
- **Whole Rupee Bills:** All financial math remains strictly in integer paisa (`Long`), rounding labour on sell slips so total receivables modulo 100 is 0.
- **NGDL v1.2 Standard:** No raw unicode emojis in UI elements. Use crisp vector assets (`Lucide` / `ImageVector`) and NGDL tokens.

---

## File Structure & Touch Points

```
projects/MandiSamiti/
├── composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/
│   ├── dashboard/DashboardScreen.kt                # [From PR #4] Flat MandiCard, 1dp border, compact metrics
│   ├── expense/ExpenseListScreen.kt                # [From PR #4] Flat MandiCard, 1dp border, no elevation
│   ├── buy/BuyTransactionsListScreen.kt            # [From PR #5] Flat MandiCard, 1dp border, long-press void
│   ├── parchi/ParchiListScreen.kt                  # [From PR #5] Flat MandiCard, 1dp border, status badges
│   ├── register/DrawerSummaryCard.kt               # [From PR #5] Flat MandiCard, 1dp border, cash counters
│   ├── sell/SellTransactionsListScreen.kt          # [From PR #5] Flat MandiCard, 1dp border
│   └── stock/
│       ├── StockBreakdownSheet.kt                  # [From PR #5] Flat MandiCard, 1dp border
│       └── StockRegisterScreen.kt                  # [From PR #5] Flat MandiCard, 1dp border
├── docs/
│   ├── STATUS.md                                   # [Modify] Update multi-agent status board and active PR states
│   └── screenshots/redmi-audit-2026-11-19/         # [From PR #9, #10, #11] 23 hardware audit screenshots
├── FEATURES.json                                   # [Merge Resolution] 42 passing / 5 failing features with audit proof
└── PROGRESS.md                                     # [Merge Resolution] Combined chronological session progress log
```

---

## Phase 1: Open PRs Safe Integration

### Task 1: Create Integration Branch and Merge PR #4 (Flat Cards: Dashboard & Expense)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/dashboard/DashboardScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/expense/ExpenseListScreen.kt`
- Modify: `FEATURES.json`
- Modify: `PROGRESS.md`

**Interfaces:**
- Consumes: `main` at commit `236f2c7` (Clean baseline).
- Produces: `integration/open-prs-november` with PR #4 merged.

- [ ] **Step 1: Fetch all remote branches and verify clean git status**

Run in terminal:
```bash
cd /Users/appworx/Desktop/Vivek-K/projects/MandiSamiti
git fetch origin
git status
```
Expected output:
```
On branch main
Your branch is up to date with 'origin/main'.
nothing to commit, working tree clean
```

- [ ] **Step 2: Create the integration branch from current main**

Run:
```bash
git checkout -b integration/open-prs-november origin/main
```
Expected output:
`Switched to a new branch 'integration/open-prs-november'`

- [ ] **Step 3: Merge PR #4 (`origin/cursor/flat-cards-dashboard-expense-4465`)**

Run:
```bash
git merge --no-ff origin/cursor/flat-cards-dashboard-expense-4465 -m "merge: PR #4 flat MandiCard on Dashboard and ExpenseList"
```
Expected output:
```
Merge made by the 'ort' strategy.
 composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/dashboard/DashboardScreen.kt | ...
 composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/expense/ExpenseListScreen.kt | ...
 FEATURES.json                                                                           | ...
 PROGRESS.md                                                                             | ...
```

- [ ] **Step 4: Verify Compose JVM tests pass**

Run:
```bash
./gradlew :composeApp:jvmTest
```
Expected output:
`BUILD SUCCESSFUL in ...` with 417+ tests passing and 0 failures.

---

### Task 2: Merge PR #5 (Flat Cards: Lists, Drawer, and Stock Register)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/buy/BuyTransactionsListScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/parchi/ParchiListScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/register/DrawerSummaryCard.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/sell/SellTransactionsListScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/stock/StockBreakdownSheet.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/stock/StockRegisterScreen.kt`
- Modify: `FEATURES.json`
- Modify: `PROGRESS.md`

**Interfaces:**
- Consumes: `integration/open-prs-november` with Task 1 complete.
- Produces: `integration/open-prs-november` with PR #4 and PR #5 combined.

- [ ] **Step 1: Merge PR #5 (`origin/cursor/flat-cards-lists-4465`)**

Run:
```bash
git merge --no-ff origin/cursor/flat-cards-lists-4465 -m "merge: PR #5 flat MandiCard on list screens, drawer, and stock register"
```
If a merge conflict occurs in `FEATURES.json` or `PROGRESS.md`:
Run:
```bash
# Accept our version for the conflict and checkout PR #5's UI Kotlin files
git checkout --theirs composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/
```

- [ ] **Step 2: Reconcile FEATURES.json and PROGRESS.md for PR #4 + PR #5**

Ensure `FEATURES.json` contains:
- `flat-cards-dashboard-expense`: `status: "passing"`, evidence: `Redmi Note 7 (d93c743) Android 10 audit verified`.
- `flat-cards-lists`: `status: "passing"`, evidence: `Redmi Note 7 (d93c743) Android 10 audit verified`.

In `PROGRESS.md`, retain the session entries from both PR #4 (`18dbde1`) and PR #5 (`33be63c`).
Stage and finish merge:
```bash
git add FEATURES.json PROGRESS.md
git commit -m "merge: PR #5 flat MandiCard on list screens, drawer, and stock register"
```

- [ ] **Step 3: Run Compose unit and regression tests**

Run:
```bash
./gradlew :composeApp:jvmTest
```
Expected output:
`BUILD SUCCESSFUL` (0 failures).

---

### Task 3: Merge PR #11 (incorporating PR #9 and PR #10 Evidence Stack)

**Files:**
- Add: `docs/screenshots/redmi-audit-2026-11-19/*` (23 hardware screenshots)
- Modify: `FEATURES.json`
- Modify: `PROGRESS.md`
- Modify: `docs/STATUS.md`

**Interfaces:**
- Consumes: PR #11 branch `origin/antigravity/staging-deploy-evidence-2026-11-19` (which contains commits `d44d950`, `ea5632d`, `a4cade9`, `a70a2fc`).
- Produces: Integrated branch containing all hardware evidence and staging verification.

- [ ] **Step 1: Inspect PR #11 git log relative to PR #9 and PR #10**

Run:
```bash
git log --oneline origin/main..origin/antigravity/staging-deploy-evidence-2026-11-19
```
Expected output:
```
a70a2fc chore: record staging deploy evidence for PR #3 and PR #6
a4cade9 chore: record redmi note 7 hardware audit evidence for PRs #3, #4, #5
ea5632d chore: add 7 screen dark theme audit screenshots
d44d950 docs: add dark theme 7-screen audit evidence for MandiSamiti Android app
```
(Confirms PR #11 subsumes PR #9 and PR #10 completely).

- [ ] **Step 2: Merge PR #11 into `integration/open-prs-november`**

Run:
```bash
git merge --no-ff origin/antigravity/staging-deploy-evidence-2026-11-19 -m "merge: PR #11 including PR #9 and PR #10 hardware audit and staging evidence"
```

- [ ] **Step 3: Resolve conflicts in `FEATURES.json` and `PROGRESS.md`**

Because PR #11 has the most up-to-date and comprehensive feature harness, take PR #11's `FEATURES.json` as the base:
```bash
git checkout origin/antigravity/staging-deploy-evidence-2026-11-19 -- FEATURES.json
```
Verify `FEATURES.json` has exactly 42 passing and 5 failing features:
```bash
python3 -c '
import json
with open("FEATURES.json") as f:
    d = json.load(f)
p = sum(1 for feat in d["features"] if feat["status"] == "passing")
fl = sum(1 for feat in d["features"] if feat["status"] == "failing")
print(f"Passing: {p}, Failing: {fl}, Total: {len(d[\"features\"])}")
assert p == 42 and fl == 5
'
```
Expected output:
`Passing: 42, Failing: 5, Total: 47`

In `PROGRESS.md`, merge all chronological sections so no history from PR #4, #5, #9, #10, or #11 is lost.
Stage and commit:
```bash
git add FEATURES.json PROGRESS.md docs/screenshots/
git commit -m "chore: reconcile FEATURES.json and PROGRESS.md across PRs #4, #5, #9, #10, #11"
```

- [ ] **Step 4: Update `docs/STATUS.md` with integrated state**

Update `docs/STATUS.md` active PR table:
- Mark PR #4, PR #5, PR #9, PR #10, PR #11 as `Merged into integration/open-prs-november`.
- Update active branch pointer to `integration/open-prs-november`.

Commit the status update:
```bash
git add docs/STATUS.md
git commit -m "docs: update STATUS.md with integrated open PRs"
```

---

### Task 4: Complete Cross-Stack Regression Verification

**Files:**
- Read-only test validation across Android ComposeApp, Core Modules, and Backend.

**Interfaces:**
- Consumes: Integrated branch `integration/open-prs-november`.
- Produces: Verified clean test execution report.

- [ ] **Step 1: Run full Kotlin Multiplatform JVM test suite**

Run:
```bash
./gradlew :core-domain:jvmTest :core-database:jvmTest :core-data:jvmTest :composeApp:jvmTest
```
Expected output:
`BUILD SUCCESSFUL` across all 4 modules (725+ tests passed, 0 failures).

- [ ] **Step 2: Run Backend Pytest suite**

Run:
```bash
cd backend
source venv/bin/activate || python3 -m venv venv && source venv/bin/activate
pip install -q -r requirements.txt
pytest tests/ -v
cd ..
```
Expected output:
`140 passed in ...s` (0 failures).

- [ ] **Step 3: Push integration branch to remote origin**

Run:
```bash
git push -u origin integration/open-prs-november
```
Expected output:
`To https://github.com/Iamvive/MandiSamiti.git * [new branch] integration/open-prs-november -> integration/open-prs-november`

---

## Phase 2: Grok Contributions Verification & Multi-Day Plan

### Task 5: Verify Grok's Delivered Contributions

Grok 4.7 (operating as Cursor Cloud) has delivered the following contributions in the repository:

1. **PR #1 (`AGENTS.md`)**: Consolidated operating contract with non-negotiable rules for integer math (`Long` paisa), physical cash isolation, silent testing, and feature logging.
2. **PR #2 (`docs/PRODUCT_SPEC.md`)**: Living product specification documenting offline-first capabilities, multi-tenant isolation, trade slip lifecycles, and core pilot requirements.
3. **PR #3 (`06b91a5`, merged via `6ed6866`)**: Buy-slip advance separation from pesgi (`farmerAdvancePaisa` vs `pesgiPaisa`), ensuring loans are not conflated with crop purchase advances.
4. **PR #6 (`87635e2`, merged via `29170e2`)**: Single Alembic baseline (`0001_baseline`) eliminating legacy fragmented migrations.
5. **PR #7 & #8 (`docs/STATUS.md`, merged via `236f2c7`)**: Shared cross-agent status board keeping Cursor Cloud, Antigravity, and Claude in sync.
6. **PR #4 & #5**: Flat `MandiCard` styling with 1dp borders across Dashboard, Expense list, Buy/Sell lists, Parchi list, Drawer, and Stock Register.

---

### Task 6: Grok's Work Plan for Next Few Days (Assigned Queue)

Grok (Cursor Cloud) is assigned the following prioritized queue to execute on `integration/open-prs-november`:

#### Day 1: Navigation Fix & Committed DB Purge
- [ ] **Fix Parchi-Opened Khata Back-Navigation:**
  - **Issue:** When a farmer's Khata is opened directly from a Parchi in `ParchiListScreen.kt`, pressing system back or top-bar back pops to Home instead of returning to the Parchi list.
  - **Files:** `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/parchi/ParchiListScreen.kt` and `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/khata/KhataScreen.kt`.
  - **Implementation:** Pass an explicit `returnToParchi: Boolean = false` flag in navigation arguments. On back press, check navigation stack and pop back to `ParchiList` when flag is true.
  - **Verification:** `./gradlew :composeApp:jvmTest` + manual navigation route test.
- [ ] **Purge Committed Backend Database File:**
  - **Issue:** `backend/mandi_backend.db` was committed into git tracking.
  - **Implementation:** Run `git rm --cached backend/mandi_backend.db`, verify `.gitignore` contains `*.db` and `backend/*.db`, and commit.

#### Day 2: Backend Security Hygiene & Dead Code Removal
- [ ] **Docker Compose Secrets Hardening:**
  - **Issue:** `backend/docker-compose.yml` contains plaintext credentials and sample secrets.
  - **Implementation:** Replace raw strings with `${POSTGRES_USER}`, `${POSTGRES_PASSWORD}`, `${POSTGRES_DB}`, and `${SECRET_KEY}`. Provide `.env.staging.template` and `.env.production.template` with instructions.
- [ ] **Clean Dead Static OTP Code:**
  - **Issue:** `OTP_STATIC_ENABLED` and `OTP_STATIC_CODE` remain in `backend/app/config.py` and `backend/app/core/otp_sender.py`.
  - **Implementation:** Delete static OTP bypass logic. Ensure production authentication requires dynamic OTP generation or SMS provider integration.
  - **Verification:** `pytest backend/tests/test_auth.py`.

#### Day 3: GitHub Actions CI Automation
- [ ] **Create Pull Request Verification Workflow:**
  - **File:** `.github/workflows/ci.yml`.
  - **Implementation:** Set up workflow triggered on `pull_request` and `push` to `main` and `integration/*`:
    1. JVM step: Set up JDK 17, run `./gradlew :composeApp:jvmTest :core-domain:jvmTest`.
    2. Python step: Set up Python 3.11, run `pytest backend/tests`.
  - **Verification:** Trigger workflow in GitHub Actions and verify green checkmark.

---

## Phase 3: Remaining Tasks, Known Bugs & Pilot Roadmap

### Task 7: Remaining Project Tasks & Known Bugs Breakdown

| Task ID | Component / Description | Current Status | Blocker / Resolution Needed | Milestone | Owner |
|---|---|---|---|---|---|
| `SEC-01` | Purge `backend/mandi_backend.db` from git | Open (Debt) | Untrack from git & update `.gitignore` | M2 | Grok |
| `SEC-02` | `docker-compose.yml` secrets hardening | Open (Debt) | Parameterize with environment variables | M2 | Grok |
| `SEC-03` | Remove static OTP bypass code | Open (Debt) | Delete static flags from `config.py` | M2 | Grok |
| `SEC-04` | Setup PR Verification CI workflow | Open (Infra) | Create `.github/workflows/ci.yml` | M2 | Grok |
| `BUG-01` | Parchi-to-Khata back navigation | Open (Bug) | Return to Parchi list instead of Home | M3 | Grok |
| `FEAT-FAIL-1` | `input-audit-medium-low` Group A trade screens | Failing in harness | Improve field focus flow & keypad sizes | M3 | Claude / Antigravity |
| `FEAT-FAIL-2` | `khata-chips-explained` unreachable screen | Failing in harness | Khata screen unreachable post-Parchi; UX fix | M3 | Grok & Vivek |
| `FEAT-FAIL-3` | `offsite-backup-restore-drill` | Failing in harness | Test `backup_offsite.sh` restore on VPS | M4 | Vivek / DevOps |
| `FEAT-FAIL-4` | `udhaar-sync-and-backend-schema` live verification | Pending confirmation | Run live round-trip udhaar sync against VPS | M4 | Antigravity |
| `PILOT-01` | Staff Attribution (Multi-user per shop) | Planned Spec | Record `created_by_user_id` on all slips | M4 | Grok |
| `PILOT-02` | Daily Galla Tally Mismatch Indicator | Planned Spec | Compare physical cash drawer vs Bahi | M4 | Claude / Antigravity |
| `PILOT-03` | Shop Elder Ergonomics & Hinglish Keypad | Planned Spec | High-contrast large touch targets on trade slips | M5 | Antigravity |

---

### Task 8: Milestones & Ownership Matrix

```
================================================================================
MILESTONE 1: PR Integration & Clean Baseline (Immediate)
Owner: Antigravity / Claude
Deliverable: integration/open-prs-november created, all 5 PRs merged, 0 test failures.
Harness Target: 42 passing / 5 failing features verified on Redmi Note 7.
================================================================================
       │
       ▼
================================================================================
MILESTONE 2: Phase 0 Security Hygiene & CI Protection (Days 1–2)
Owner: Grok (Cursor Cloud)
Deliverable: Committed DB removed, compose secrets parameterized, static OTP dead code
             deleted, .github/workflows/ci.yml enforcing automated PR checks.
================================================================================
       │
       ▼
================================================================================
MILESTONE 3: Core Navigation & Input Audit Fixes (Days 3–4)
Owner: Grok & Claude
Deliverable: Parchi back-navigation fixed, Group A trade screens input ergonomics
             improved, Khata chip explainer card restored.
Harness Target: 44 passing / 3 failing features.
================================================================================
       │
       ▼
================================================================================
MILESTONE 4: Pilot Feature Set 1 & Live Sync Drill (Days 5–7)
Owner: Grok (Attribution) + Antigravity (Galla & Sync) + Vivek (Backup Drill)
Deliverable: Staff attribution on Buy/Sell slips, Galla mismatch indicator,
             Udhaar live round-trip sync confirmation, offsite backup restore verified.
Harness Target: 47 passing / 0 failing features.
================================================================================
       │
       ▼
================================================================================
MILESTONE 5: Production Pilot Release & Shop Rollout
Owner: Vivek & Antigravity
Deliverable: APK build with NGDL styling, signed release artifact, pilot shop onboarding.
================================================================================
```

---

## Execution Handoff

Plan complete and saved to `projects/MandiSamiti/docs/superpowers/plans/2026-11-20-mandi-open-prs-integration.md`.

Two execution options:

1. **Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration.
2. **Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints.
