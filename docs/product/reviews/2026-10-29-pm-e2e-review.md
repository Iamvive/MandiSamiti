# MandiSamiti Android: PM end-to-end review (2026-10-29)

Build: `com.appwork.mandisamiti` 2.0.0, branch `feat/milestone-a-trust` @ `5996a23`, Redmi Note 7 (MIUI), light theme, English app language, test data.
Screenshots: `docs/testing/evidence/pm-review-2026-10-29/NN-name.png` (Evidence column = NN). Engineer fix prompt: `docs/product/reviews/2026-10-29-engineer-fix-prompt.md`.

## Verdict
Not pilot-ready. Buy/sell math is correct to the paisa, but the party khata, which is the product's core promise, shows wrong balances after an ordinary lend → repay → void sequence, and some dashboard KPIs ignore voids. Fix P0 before any more UI work.

## P0: money shown wrong
| ID | Finding | Evidence | Suspected cause |
|---|---|---|---|
| F01 | Party balance wrong after lend/repay/void. PM TEST: buy ₹12,200 + lend ₹5,000 + repay ₹7,000 + hammali ₹300 → app ₹20,900 देना है, expected ₹13,900. After voiding every PM TEST entry → ₹7,000 देना है, expected ₹0. Voiding the repay cash row → ₹2,000 देना है, expected ₹5,000 लेना है. | 22, 35, 37, 38 | `AppDatabase.sq:393` term 10 sums `lendSettlementEntity` without `l.is_void = 0`; Repay writes both a `JAMA_RECEIVED` cash row (term 3) and a settlement (term 10); voids don't cascade lend → settlement or cash row ↔ settlement. |
| F02 | Over-repayment accepted: ₹7,000 against a ₹5,000 loan, no warning; loan marked "पूर्ण चुकता" and the extra ₹2,000 is shown nowhere on the loan. Repay sheet doesn't show outstanding. | 15, 16, 17 | `LendTransactionUseCase.kt:33` `repay()` has no cap. |
| F03 | Dashboard "Total Outstanding" counts a fully repaid loan (₹10,000 → ₹15,000 after full repay). | 20 | Outstanding KPI ignores settlements. |
| F04 | Voided expense still in "Total Expenses" (₹1,800 after voiding the ₹300 hammali; was ₹1,500). | 40 | Expense KPI query misses `is_void = 0`. |
| F05 | Buy/Sell forms ignore Settings. Settings: farmer 1.5%, buyer 1.5%, labour ₹150, tare 0.35 qtl. Buy form: 2% / ₹5 per bag, no tare. Sell form: 0%. | 05, 10, 29 | `BuyEntryFormState.kt:93-94`, `SellEntryFormState.kt:84` hardcoded. |
| F06 | No sanity limits on bags/weight; closing the keyboard shifts the layout so the next tap misses and digits go into the previous field (bags became 10,92,500; Net Payable ₹-54,62,500 shown before a rate existed). Real data shows the same: village "Khedae10n2800", a 20 qtl wheat buy for ₹12.01 Cr. | 05, 08b | Form IME/scroll handling; no `imeAction = Next`; no max bags/rate validation. |

## P1: trust and core flow
| ID | Finding | Evidence |
|---|---|---|
| F07 | Data honesty: ₹12.26 Cr purchases and ₹12.02 Cr payable are headlined; the only flag is a grey footnote "Purchases exceed 100x recorded sales". The single outlier entry is not named or linked. | 01b, 02 |
| F08 | Sync story contradicts itself: session expired, yet the banner says "upload when the internet is on" while Wi-Fi is on. Re-login card has no visual affordance (works on tap). Login screen doesn't prefill the shop's phone; Back from login exits the app. Pending count rose 24 → 34 for 5 user actions. | 01b, 20, 31, 32 |
| F09 | Loan Book and party ledger disagree: Kailash has ₹20,000 "जमा" against two ₹5,000 loans, ledger says we owe him ₹10,000, Loan Book still shows both loans "Open". Two homes for one entity. | 14, 23 |
| F10 | Void behaviour inconsistent: dialog says "रद्द प्रविष्टि खाते में दिखेगी", but voided buy/sell vanish from the ledger while voided cash rows stay with "रद्द". Voiding the ₹5,000 loan cash row didn't change the balance. | 34, 35, 36 |
| F11 | Settings shows "SQLite encrypted database active"; there is no SQLCipher or encryption in the codebase. False security claim. | 30, `SettingsScreen.kt:456` |
| F12 | Back navigation: Back from a party ledger goes to a stale Loan Book in one case and exits the app in another. Bottom-nav tabs don't reset the back stack. | 23, after 38 |
| F13 | Stock split by variety: sales tagged "गेहूं लोकवान/शरबती" show −5 / −10 qtl while "गेहूं" shows 72 qtl and 0 sold. Selling beyond stock gives no warning at entry time. | 41 |
| F14 | Expense "किसे दिया" auto-links to a party by name: a shop hammali expense was posted into farmer PM TEST's khata and changed his balance. | 22 |
| F15 | No party autocomplete in Buy/Lend (typing "PM TEST" showed no match), so typos create duplicate parties. Identical duplicate buys exist (RadheyShyam 30 bags ×2) with no duplicate-save guard. | 12b, 08b |
| F16 | Lend "expected return date" is free text and accepted "abc". The due date isn't shown in the Loan Book, which blocks the planned Udhari Recovery aging. | 13, 14, `LendEntryScreen.kt:167` |

## P2: friction and clarity
| ID | Finding | Evidence |
|---|---|---|
| F17 | Language mixing: app set to English, but Buy/Sell/Lend/Expense forms, receipts, party chips, re-login card are Hindi; Hindi screens carry English ("Multi-Crop Lots", "qtl", "Gross"); cold-start skeleton is Hindi then flips to English. | 01, 03 |
| F18 | Receipt lacks date/time, shop name, mandi, village. Slip IDs are hex and differ by screen: receipt `#793f5a` vs ledger `#93f5a` (`takeLast(6)` in receipts, `takeLast(5)` in `PartyLedgerScreen.kt:867/967`). | 07, 11, 22 |
| F19 | Day Closing: after "Save Count" (₹10,000 vs expected ₹10,200), the count and ₹200 shortage appear nowhere; no reason field; Opening cash is freely editable with no audit trail shown. | 27, 28 |
| F20 | Cash Drawer split bahi scrolls inside the page; "Total Naame Side ₹15,500" vs visible ₹5,300 because the closing-balance row is hidden. | 24 |
| F21 | Empty remarks render with a trailing colon: "उधार वापसी जमा:", "उधार ऋण:". | 22 |
| F22 | Cold start shows a skeleton for 4+ seconds. | 01 |

## P3: polish
| ID | Finding | Evidence |
|---|---|---|
| F23 | Layout: Buy tile taller than the others ("From Farmer" wraps); KPI tiles unequal heights; shop name truncated to "Maa Sharda" beside the sync pill; "किसान" chip wraps to "कि/सान" beside a large amount; "चेक (Cheque)" chip wraps over 3 lines with an orphan ")". | 01b, 02, 19 |
| F24 | Single tick beside party names means "Pending Local", but users read a tick as done/sent. | 02 |
| F25 | `feature-status.md` drift: Quick Expense Chips are live but listed as "Brainstorming". | 18 |

## Product decisions for Vivek (not engineer work)
- Sell has no mandi fee/cess line. Is it needed for UP mandi compliance in the pilot?
- Is a repayment above the outstanding amount ever valid (advance from farmer)? If yes, it must post as a separate advance, visibly.
- Should Home show today's figures first instead of all-time totals?

## What works (don't break)
Buy/sell math exact (₹12,500 − 2% − ₹5×10 = ₹12,200; ₹14,000 + ₹50 = ₹14,050). Live hisab preview. Save blocked until rate is entered; empty expense save disabled. Discard-draft dialog. Void with reasons. Galla arithmetic and Day Closing shortage calc. Buyer ledger after void returns to ₹0 "हिसाब चुकता".

## Not verified
Dark theme (MIUI ignores `cmd uimode night yes`; the app has no in-app theme switch). Offline/airplane mode. Actual sync upload (session expired; did not log in). WhatsApp share text, soundbox audio, rotation, signup/OTP.

## Test residue on the phone
Parties PM TEST and PM TEST BUYER remain. PM TEST shows a ghost ₹7,000 देना है (F01) that the UI can't clear. Total Expenses carries +₹300 (F04). A day-close count of ₹10,000 was saved for 29 Oct.

## Vivek's notes (no action yet)
- N1 Round-off: final transaction amounts round to the nearest ₹10 (₹10,988 → ₹10,990). Open: rule for amounts ending in 5, whether it applies only to net payable/receivable or to every line, and whether the slip shows a separate round-off line. Likely touches `MandiMathEngine.calculateSettlement`.
- N2 Paper khata (photo of a real roznamcha page, 28-10-26; not stored in repo, private data). What it shows, anonymised:
  - Page = one day's रोकड़ with date + weekday in the header. Two money columns, amount written first, then name + village (name alone is not unique; village or a "(पहले)" tag tells people apart).
  - Banks are parties too: district co-op bank and another bank appear as rows (with account no.), plus a bank opening balance. The app only models one cash galla.
  - Corrections and settlements are shown by striking through, and amounts are overwritten (1500 → 2000). Matches F10: keep voided rows visible, struck through.
  - A side column of small amounts (₹1,000 to ₹5,000) against names, struck off once paid; braces group several rows under one subtotal.
- N3 Printed sale bill from the same desk: "Bill of Supply" under GST composition scheme, with shop GSTIN, state code, bill no., date, HSN code, quantity in quintals with 2 decimals (e.g. 15.68 qtl), rate per qtl, and amount in rupees + paise. Implications: sell bill needs GSTIN, HSN, sequential bill no. (F18); quantity is often a weighed quintal figure, so the form must accept direct weight entry and not only bags × kg per bag; N1 round-off must not break paise on GST bills (round the khata total, keep the bill exact, or show a round-off line).
- N4 Farmer explainer: at the desk the Aadhati talks the farmer through the hisab: how many bori, the weight, the rate, then the total (and what was cut). The app should support this moment: a simple, large-type "किसान को समझाएँ" view of the buy slip (bori → वजन → भाव → कुल → कटौती → शुद्ध देय), readable across the table, with the soundbox optionally speaking the same lines in Hindi. Builds on the existing receipt + voice features; fixes in F18 (date, shop, slip no.) apply here too.
