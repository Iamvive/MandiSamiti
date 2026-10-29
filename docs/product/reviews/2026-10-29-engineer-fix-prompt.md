You are fixing MandiSamiti (KMP Android, `projects/MandiSamiti`) based on a PM end-to-end review done on a Redmi Note 7 on 2026-10-29 (build 2.0.0, branch `feat/milestone-a-trust` @ 5996a23). The full review with screenshots is in `docs/product/reviews/2026-10-29-pm-e2e-review.md` (screenshots in `docs/testing/evidence/pm-review-2026-10-29/`).

## Rules
- Branch `fix/pm-review-2026-10-29` off `feat/milestone-a-trust`. Never commit to `main`. One commit per finding ID (`fix(ledger): F01 ...`).
- Work in priority order: P0, then P1, then P2, then P3. Do not start P2 until every P0 has a passing test and device proof.
- Money is integer paisa via `MandiMathEngine`. No floats, no DB schema break without a migration, and no API contract change without updating the backend DTOs.
- NGDL tokens only (`docs/design-system/DESIGN-SYSTEM.md` §2.1, §5), no emoji, no horizontal overflow at 390dp. Hindi-first copy that follows the app language toggle.
- Each logic fix gets a regression test in `composeApp/src/jvmTest` or `core-domain` tests that fails before the fix. Each visual fix gets a device screenshot in `docs/testing/evidence/pm-review-fixes/`.
- If a finding turns out to be wrong or needs a product call, stop on that item, write why in the PR, and continue with the next.

## P0: money shown wrong (fix first)
F01 Party balance double-counts loans and ignores voided loans.
- Repro: new party X. Buy 10 bags @ ₹2,500 (net ₹12,200). Lend ₹5,000 to X. Repay ₹7,000. Expense ₹300 "किसे दिया" = X. Ledger shows ₹20,900 देना है (expected ₹13,900). Void every X entry → shows ₹7,000 देना है (expected ₹0).
- Known cause: `core-database/.../AppDatabase.sq` party balance (around :360-395). Term 10 sums `lendSettlementEntity` without `l.is_void = 0 AND l.is_deleted = 0`. Repay writes both a `JAMA_RECEIVED` cash row (term 3) and a settlement (term 10), and Lend similarly writes a cash row plus a lend entry. Pick one source of truth for loans in the balance (recommended: lend + settlement rows; cash rows linked to a lend are excluded from terms 1-3), and make void cascade: voiding a lend voids its settlements and linked cash rows; voiding a linked cash row voids its settlement.
- Accept: the repro gives ₹13,900 then ₹0; the Loan Book, party ledger and Dashboard agree. Test covers lend, partial repay, full repay, void lend, void repay.

F02 Over-repayment accepted.
- Repro: loan ₹5,000 → Repay ₹7,000 → saved, loan "पूर्ण चुकता", extra ₹2,000 invisible.
- Fix: `LendTransactionUseCase.repay()` (core-domain, :33) rejects amount > outstanding with a typed error; the repay sheet (`LendTransactionsListScreen.kt` RepaymentBottomSheet, :309) shows "बकाया ₹X" and an inline error. Product decision on advances is pending; until then, block.

F03 Dashboard "Total Outstanding" counts settled loans (₹10,000 → ₹15,000 after a full repay). Outstanding = sum(lend) − sum(non-void settlements) over non-void lends.

F04 Voided expense still counted in "Total Expenses". Add `is_void = 0` to the expense KPI query; test void → KPI drops.

F05 Forms ignore Settings rates. `BuyEntryFormState.kt:93-94` hardcodes 2% / ₹5, `SellEntryFormState.kt:84` hardcodes 0%. Seed from the shop's saved rates (farmer commission, buyer commission, labour, tare) and apply tare in weight. Clarify the labour unit in Settings ("₹ प्रति बोरी"). Accept: change Settings → new form shows the new defaults.

F06 Wrong numbers get into entries.
- Closing the keyboard shifts the form, so the next tap misses and digits land in the previous field (bags became 10,92,500; real data has village "Khedae10n2800"). Set `imeAction = Next` with focus moving to the next field, keep the focused field visible (`bringIntoViewRequester` / `imePadding`), and use numeric keyboards on numeric fields.
- Add sanity limits with a confirm dialog (like the existing rate outlier guard): bags > 2,000 per lot, weight per bag outside 20-120 kg, rate outside the commodity band. Never show Net Payable while rate is empty; show "भाव दर्ज करें".
- Format large numbers in Indian grouping everywhere (10,92,500), and money as −₹X consistently (one place shows ₹-54,62,500).

## P1: trust and core flow
F07 Outlier data is headlined. When an entry trips the outlier rule, show a warning card on Home naming the entry ("RadheyShyam, 20 qtl @ ₹60 लाख/qtl, जाँचें") that opens it, instead of the grey footnote. KPIs stay honest but flagged.
F08 Sync messaging. If the session is expired, the sync banner must say "लॉगिन करें, फिर एंट्री अपलोड होंगी", not "internet is on". Give the re-login card a button and chevron. Prefill the shop's phone on login. Back from login returns to Home. Pending count = number of user entries, not outbox rows (it rose by 10 for 5 actions).
F09 Loan Book and party ledger must read from the same numbers (follows from F01). A "जमा मिला" from a party with open loans asks whether to apply it to a loan.
F10 Void visibility. Make it consistent with the dialog text: every voided entry stays in the ledger, struck through, with "रद्द · reason", and is excluded from totals.
F11 Remove the false "SQLite encrypted database active" text (`SettingsScreen.kt:456`). Either ship SQLCipher or say "डेटा इस फ़ोन में सुरक्षित है" with no encryption claim.
F12 Back stack. Bottom-nav tabs use `popUpTo(start) { saveState = true }; launchSingleTop`; Back from a party ledger returns to the screen that opened it; Back on a root tab goes to Dashboard before exiting.
F13 Stock by commodity, with variety as a sub-row. Warn at sell entry when the quantity exceeds stock.
F14 Expense "किसे दिया" must not auto-link to a party. It is a free-text payee unless the user explicitly picks a party.
F15 Party picker with search/autocomplete in Buy, Sell and Lend (name, village, phone). Duplicate-save guard: same party, same amount and same lots within 2 minutes asks "यह एंट्री पहले से है, फिर भी सुरक्षित करें?".
F16 Lend return date: date picker with chips "15 दिन / 30 दिन / 60 दिन", stored as a date, shown on Loan Book cards as "वापसी: 28 Nov (30 दिन बाकी)".

## P2
F17 Language toggle applies to every screen including forms, receipts and chips. No English inside Hindi strings ("Multi-Crop Lots", "qtl", "Gross"). The skeleton uses the saved language. (Same root as known item A4; remarks stored as codes, and drop the `'उधार ऋण:%'` filter at `AppDatabase.sq:366`.)
F18 Receipts show shop name, mandi, date and time, village. One human slip number (sequential per shop per year, e.g. "ख-2026-0142") used identically on receipt, list, ledger and WhatsApp text. Stop mixing `takeLast(6)` (receipts) and `takeLast(5)` (`PartyLedgerScreen.kt:618/867/967`).
F19 Day Closing: after Save Count, the Cash Drawer shows "गिनती: ₹10,000 · कमी ₹200" for the day, with an optional reason. Edits to opening cash create an audit revision.
F20 Cash Drawer split bahi: no nested scroll; the closing-balance row is visible so both sides visibly total the same.
F21 No trailing colon when a remark is empty.
F22 Cold start: Home shows data under 1.5s on the Redmi Note 7, or the skeleton has a clear loading label.

## P3
F23 Equal-height action tiles and KPI tiles (`Modifier.height(IntrinsicSize.Min)` row + `fillMaxHeight`). Shop name never truncates to an ambiguous word (2 lines or a smaller sync pill). Chips don't wrap mid-word ("किसान", "चेक").
F24 Sync tick: replace the bare tick with a clear "फ़ोन में" / "सर्वर पर" icon + label, or a clock icon for pending.
F25 Update `docs/product/feature-status.md` for every finding you change (Quick Expense Chips are live).

## Definition of done (NGDL §5.7)
- All new tests pass: `./gradlew :composeApp:jvmTest :core-domain:allTests`, and `tests/ngdl-tokens` passes.
- Re-run the review repro on the Redmi: the F01 sequence gives ₹13,900 → ₹0, F02 is blocked, Dashboard KPIs match the ledgers after voids. Screenshots for each P0 and P1 item in `docs/testing/evidence/pm-review-fixes/`.
- Light theme at phone width checked; dark theme can't be forced on MIUI, so check it on an emulator or add an in-app theme switch and verify there. List anything you could not verify.
- Data on the test phone: void or delete the PM TEST and PM TEST BUYER residue once F01 is fixed (the ghost ₹7,000 should disappear by itself).
- PR description lists each finding ID with status fixed / skipped (why) / needs product call.
