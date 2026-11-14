# Handover 2026-11-14: interest settlement verified

Paste the shared context, then the prompt for your AI.

## Shared context
You are continuing MandiSamiti work for Vivek. Read vaults/Vivek-K/wiki/hot.md first, then projects/MandiSamiti/PROGRESS.md (last entry, 2026-11-14).

State:
- Feature money-slip-interest-settlement is now passing (FEATURES.json). Commit 6fd3206 on MandiSamiti main, local only, not pushed. Parent pointer in Vivek-K and hot.md are NOT updated yet, wait for Vivek's approval.
- Verified on emulator medium_phone (emulator-5554), staging http://64.227.142.180:8060, test shop Maa Sharda Traders. The Redmi d93c743 was not connected.
- Open items:
  1. Test loan "Rate Test" (9999900003, principal now Rs 5,000, 1.5%) is still on staging. Wipe it from the test shop only, other shops untouched. Ask Vivek before deleting.
  2. Minor bug: after typing Rs 100 in the repay dialog the interest field still showed 220 while the split line said 100. Saved result was correct. Investigate, add a regression test if it is real.
  3. Scratch screenshots in /private/tmp/claude-501/.../scratchpad can be deleted.
  4. Next features still failing: khata-chips-explained, khata-filter-explainer-card (needs Redmi or emulator check in Light, Dark, 390dp), offsite-backup-restore-drill, input-audit-medium-low.

Rules: device tests use the staging URL, never 10.0.2.2. Never print API keys. Ask Vivek one question at a time. Log evidence to PROGRESS.md and FEATURES.json before saying done. Run python3 scripts/vault_lint.py after vault edits.

## Claude Code
Start by asking Vivek whether to wipe the Rate Test loan on staging. Then use systematic-debugging on the stale interest field (open item 2). Use brainstorming before any new feature.

## Codex
You may set up and fix code but do not run roast-room councils. Start with open item 2: find where the repay dialog interest field default is computed in the Kotlin composeApp sources, write a failing jvmTest first, then fix. Run ./gradlew :composeApp:jvmTest :core-data:jvmTest before reporting.

## Antigravity / Gemini
Check the emulator with adb devices. If the Redmi d93c743 is connected, run the khata-filter-explainer-card device check (Light, Dark, 390dp, Clear filters tap) on a staging build and record evidence in PROGRESS.md and FEATURES.json. Delete only test-only screenshots when done.
