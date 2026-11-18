# Udhaar Task 7b: review fixes for the short add form

Follows commit `3a5e3ee`. Brief wins over any earlier text. Same rules as the Task 7 brief (staging only, no sound, delete test data and test-only screenshots, log to PROGRESS.md and FEATURES.json).

## Decision from Vivek
A phone that matches a soft-deleted party is **restored and reused**. No block, no duplicate.

## Fix 1: deleted party by phone (edge case 13)
Problem: `getAllParties` filters `is_deleted = 0`, so `LendPartyResolver.existing` never sees a deleted party. The save inserts a new row and hits `idx_party_shop_phone` (unique on `shop_id, phone`). The sheet then shows a raw SQLite message.

Do:
1. Find how the repo can read a party by phone including deleted rows. `getPartyByPhone` exists in `OfflineFirstPartyRepository`; check whether it filters `is_deleted`. Add a query or flag only if none exists.
2. In `UdhaarLandingViewModel.saveLoan`, when no live party matches the phone, look up the phone including deleted rows. If found, restore it (`is_deleted = 0`, new revision, outbox entry, through the repository's normal save path so sync sees it), use the name the user typed, and reuse its id for the loan.
3. Restored party keeps `village` as stored. Do not null it.
4. The phone also matches the shop-owner or `khaate-%` rows in some cases. If the match is a `khaate-%` party, do not restore it. Block with a clear message in the current language ("This number belongs to a Khata account"). Flag it in your report.
5. Never show `e.message` raw. Map any failure to a fixed message in the current language. Log the real error.
6. Tests (write first, see FAIL): deleted party restored and reused, no second row for that phone, loan points at the restored id, name from the form wins, `khaate-%` match is blocked, live party still reused as before.
7. Emulator check on staging: create a party through the sheet, soft-delete it, add udhaar with the same phone, confirm one party row, restored, loan visible in the correct tab. Delete all test rows after.

## Fix 2: rename on existing party (edge case 2)
Confirm with a test what `partyToSave` does when `existing` is set and the typed name differs. Required: the notice shows, and the rename happens only because the user saved with the edited name (the notice is the warning). If the code silently keeps the old name or silently renames without the notice, fix it and say which.

## Fix 3: small
- Interest error text says "0 to 100" but zero is rejected. Change to "above 0 up to 100" in both languages.
- Report the real test count from the four modules' `build/test-results/jvmTest/*.xml`. Last report said 844; the XMLs show 660 (109 + 160 + 12 + 379). Expected now: 660 plus the new tests.

## Report back
Commit hash, real test counts per module, what `khaate-%` handling you chose, the answer to Fix 2, and anything unverified.
