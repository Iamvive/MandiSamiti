# Handover 2026-11-14 (Claude Code, evening): input audit and phone-or-name lookup

## Done this session (all committed, not pushed)
- f4f9696: RepayPreviewTest (3 cases). The repay dialog interest field showing 220 after typing 100 is not a bug: the field is the interest owed, the split line shows how the payment lands.
- b50b0e1: khata-chips-explained and khata-filter-explainer-card stay failing. KhataLedgerTabScreen has no caller since 3b9b704 (Parchi replaced Khata), so the chips and the explainer card are unreachable. Vivek said leave it for now.
- 6021ec0: Ascii keyboard and autoCorrect=false on Sell buyer GSTIN, Party ledger GSTIN and Mandi licence. 304 jvm tests pass. Sell GSTIN checked on emulator-5554 (QWERTY, Next, no suggestion strip). Party ledger fields not opened on the emulator.
- Rate Test loan was already gone from staging. Nothing deleted.

## Open
1. Phone-or-name lookup, MandiTextField.kt:197. Vivek chose option 2 (phone or name). Not started. Run brainstorming first: switch to a text keyboard when the first character is not a digit, pass names through as a search instead of a phone. Callers: Buy, Sell, Lend. onPhoneChange currently receives only digits.
2. 48 Low input-audit items deferred. input-audit-medium-low stays failing.
3. Party ledger GSTIN and licence fields still need an emulator look.
4. offsite-backup-restore-drill: run backup_offsite.sh on the VPS, add the Sunday cron line.
5. Khata filters: product decision pending (filters on Parchi, or restore the Khata tab).
6. hot.md and the submodule pointer: not touched, need Vivek's approval.
7. Scratch screenshots (e1.png, s1 to s4.png) in the session scratchpad can be deleted.

Rules: emulator only (Vivek, this session), staging URL, never print keys, one question at a time, log to PROGRESS.md and FEATURES.json before saying done.

## Claude Code
Start with open item 1. Use the brainstorming skill, ask Vivek one question at a time, then test first and fix MandiTextField.kt and its Buy, Sell, Lend callers. Verify on the emulator, log to PROGRESS.md, commit.

## Codex
Do not run roast-room councils. Take open item 1 only after Vivek approves a design. Otherwise take open item 2: batch the Low audit items from audits/2026-11-09-mandisamiti-input-ux-audit.md in Vivek-K, skipping rows in KhataLedgerTabScreen.kt. Run ./gradlew :composeApp:jvmTest :core-data:jvmTest before reporting.

## Antigravity / Gemini
On emulator-5554 only: open Party ledger, edit a merchant party, and check that the GSTIN and Mandi licence fields show a plain QWERTY keyboard with no suggestion strip. Do not save. Record evidence in PROGRESS.md and delete only test screenshots.
