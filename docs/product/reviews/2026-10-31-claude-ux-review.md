# MandiSamiti UX review (Claude), Phase 1

> Screenshots cited here were deleted on 2026-11-13. Recover them from git history with `git show 05f8301^:<path>`.

Status: Phase 1 finished on 2026-10-31 except for the gaps listed under "Not verified". Phase 2 direction chosen: B (तराजू), see "Phase 2 decision" at the end. Brief: `prompts/mandisamiti-ux-overhaul.md` in the Vivek-K repo. Screenshots are in `docs/testing/evidence/ux-review/` (115 files). Code paths below are relative to `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/` unless they start with another top-level folder.

## Decide before the pilot goes further

These four are product or data bugs found during the UX pass. None has been changed.

| # | Finding | Evidence | Code |
| :--- | :--- | :--- | :--- |
| P1 | Expenses never reach the server. The client pushes `expenses` and reads `synced_expenses`; the server reads `expense_entries` and returns `synced_expense_entries`, ignores the unknown field and answers 200. The local backend's `expense_entries` table had 0 rows after 14 successful pushes. Effects: the pending badge never clears, sign-out stays blocked with a message telling the trader to wait for network, and after a reinstall कुल खर्च reads ₹0 while Galla still lists the ₹350. Checked against the local backend built from this repo; the deployed server was not called. | `87-signout-blocked`, `90-after-relogin` | `core-data/.../data/sync/model/SyncDto.kt:220,234,253`, `core-data/.../data/sync/SyncEngine.kt:208,246,279`, `backend/app/schemas/sync.py:24,36,53` |
| P2 | Day close contradicts itself. A day that balances (₹7,000 = ₹7,000 on the Galla footer) opens with "अंतर: ₹47,885" and likely causes, because credit purchases are listed as naame. After counting, the same dialog says "हिसाब बिल्कुल बराबर है (₹0)". NGDL §5.6: never headline a distorted number. | `62-count`, `67-after-close-tap` | `ui/register/DailyCashRegisterScreen.kt:689-691,1061`, `ui/register/DailyRegisterViewModel.kt:258-259` |
| P3 | Slip numbers restart after reinstall. Two different farmers now both hold पर्चा #1. | `94-buy-list-slipnumbers` | Not pinned to a line |
| P4 | Typing a partial name without tapping a suggestion silently creates a new party. "TEST R" became its own khata beside "TEST Ramesh", with no prompt. A Buy against it is real money on the wrong account. | `91-repeat-1`, `94-buy-list-slipnumbers` | `ui/components/PartyAutocompleteField.kt` (line not pinned) |

## Money display questions (math untouched)

- Sell cash memo: the ₹10/bag मजदूरी field has no effect because `labourPaisa` is fixed at 0 (`ui/sell/SellEntryFormState.kt:187`). The receipt line "मजदूरी +₹2.15" is really tulak (`:204-207`, label at `ui/sell/SellTransactionReceiptScreen.kt:103,108`). Evidence: `25-sell-bottom`, `26-sell-receipt`.
- Buy: "सकल" on the lot means ₹24,000 and "कुल राशि (Gross)" means ₹23,160 (`ui/buy/BuyEntryScreen.kt:403,514`). The receipt shows both with no काट line, so the farmer sees ₹840 vanish. Evidence: `16-buy-ready`, `17-buy-after-save`. The tare decision itself stays out of scope.
- Buy commission shows ₹160 where 0.70% of ₹23,160 is ₹162.12. This is the deliberate nearest-₹10 rule (`ui/buy/BuyEntryFormState.kt:225-229`), but nothing on screen says so.
- A corrected slip keeps its number with no "corrected" mark, so two different totals can circulate as #1. Evidence: `17-buy-after-save`, `54-edit-after-save`.
- An absurd rate (₹24,00,24,000 total) keeps Save fully enabled, and the only warning has scrolled away. Evidence: `15-buy-absurd-rate`.
- Loan card still reads "उधार राशि: ₹5,000" after ₹2,000 is repaid, with no remaining amount (`ui/lend/LendTransactionsListScreen.kt:307`). Evidence: `33-after-repay`.
- Interest dialog opens with "मूलधन: ₹0" for a party holding a ₹3,000 open loan (`ui/ledger/PartyLedgerScreen.kt:919`). Evidence: `45-interest`.

## Findings by severity

### High

| Finding | Evidence | Code |
| :--- | :--- | :--- |
| Dashboard briefly shows ₹0 and "हिसाब चुकता" for a party who is owed money, on returning to it after a save | `19-dashboard-after-buy` vs `21-dashboard-recheck` | `ui/home/HomeViewModel.kt:155,247` |
| Day close: count field sits below the false warning, and "दिन बंद करें" is clipped by the fixed footer until scrolled further | `64-count-scrolled`, `68-close-scrolled` | `ui/register/DailyCashRegisterScreen.kt:1037,1101` |
| English and raw enum text inside Hindi day close: "Grain purchased", "Farmer receipt net", "HAMMALI" | `62-count` | `ui/register/DailyRegisterViewModel.kt:258-259` |
| App opens to all money data after a week with no MPIN prompt, and nothing mentions the elapsed time or last close | `77-week-later`, `78-week-galla` | Not pinned |
| Voice defaults on at first install and returns on after re-login, despite the mute | `04-after-create`, `90-after-relogin` | `core-database/.../AppDatabase.sq:11` |
| Status bar icons are white on near-white in light theme (measured 1.0:1), so clock and battery vanish; no status bar styling exists in `composeApp/src` | `00-first-launch`, `05-dashboard-empty-voice-tapped` | None to cite |
| Save is below the fold on Buy and Sell, needing a scroll past the deductions and preview every time | `10-buy-list-or-entry`, `14-buy-bottom` | `ui/buy/BuyEntryScreen.kt:594,601` |

### Medium

| Finding | Evidence | Code |
| :--- | :--- | :--- |
| Contrast below 4.5:1: Bill of Supply hint 1.41 light, 1.56 dark; "पेशगी" tile label 2.81 light; "मंडी व्यय" 3.84 light, 3.72 dark; inactive tabs and "0 खातों से" 3.12 light, 3.17 dark. The last pair is `--ngdl-text-tertiary` carrying meaningful text | `20-sell-entry`, `dark-sell`, `19-dashboard-after-buy`, `dark-dash`, `40-khata` | `ui/sell/SellEntryScreen.kt:242`, `ui/navigation/MandiBottomBar.kt:93` |
| Void exists only as an unlabelled long press | `55-void-dialog` | `ui/buy/BuyTransactionsListScreen.kt:135` |
| Autocomplete shows a check on every row and no balance when lending to a party | `29-lend-autocomplete` | `ui/components/PartyAutocompleteField.kt:140` |
| Sync banner reads the same online and offline, and says "इंटरनेट मिलते ही" while connected | `36-expense-saved`, `71-offline-dash`, `76-online-again` | `ui/dashboard/DashboardScreen.kt:485-487` |
| "डेटा इस फ़ोन में सुरक्षित है" with a green cloud while entries are unsynced | `46-settings-bottom` | `ui/settings/SettingsScreen.kt:450` |
| Closing the day gives only a small check icon, with no closed state and no moment of pride | `70-day-closed` | `ui/register/DailyCashRegisterScreen.kt:1101` |
| No save confirmation; the app jumps straight to the receipt | `17-buy-after-save` | Not pinned |
| Failed party search offers no way to add that party, and filter counts ignore the search. A Latin query did not find a Devanagari name | `82-khata-search-empty` | `ui/ledger/KhataLedgerTabScreen.kt:322` |
| New party requires mobile while Buy and Sell call it optional, and shows a red error before any typing | `83-new-party`, `10-buy-list-or-entry` | `ui/ledger/PartyLedgerScreen.kt:428` |
| Party ledger shows one loan as two rows (+₹5,000 and -₹5,000), has no running balance, and cash rows have no date | `44-party-ledger` | Not pinned |
| Crossed-out phone icon reads as a broken control on every party row | `40-khata` | `ui/components/PartyCard.kt:22,247` |
| Tutorial promises "5 सेकंड" and spoken confirmation | `80-tutorial` | `ui/tutorial/` (line not pinned) |
| A Dashboard Buy tile opens a blank form, while the KPI tile opens the list. Nothing signals that the KPI tile is tappable | `95-kpi-tap` | `ui/dashboard/DashboardRevampComponents.kt:38` |

### At 200% font

130% holds up (`m130-dash`, `m130-galla`). 200% breaks: chips split mid-word ("व्यापा/री", "(Mer/chan/t)", "अना/ज"), the "कस्टम तारीख" chip collapses to a sliver, the header takes about a quarter of the screen, the FAB covers an amount, Galla's entries are pushed out of view, and icons stay small while text grows. Evidence: `big-sell`, `big-lend`, `big-dash`, `big-khata`, `big-galla`.

### Already known, still true

Hindi avatar breaks conjuncts: क्षितिज shows क (`75-offline-dash-after`, `SoundboxTopBar.kt:57`). No ripple on tabs and no haptics anywhere. Bracketed English in Hindi labels, 4 hubs in the doc and 5 in the app. The Godown multi-crop headline was not reproduced because only one crop was entered.

## Inconsistencies across screens

| Area | What varies | Evidence |
| :--- | :--- | :--- |
| Spacing | 19 padding values, 21 font sizes from 6sp to 40sp, 9 corner radii | grep on `ui/` |
| Cards | White with border, grey elevated, heavy-shadow KPI, pastel tile. Buy's lot card is flat; Sell's has a thick grey frame around an inset panel | `13-buy-filled`, `24-sell-lot`, `19-dashboard-after-buy` |
| Headers | `SoundboxTopBar` on tabs, `TopAppBar` on the rest, a mute pill only on the party ledger; 4 header controls wrap the shop name in English | `40-khata`, `44-party-ledger`, `en-dash` |
| Chips | Lavender selected in forms, which is no NGDL colour; black selected on Khata and Godown; rectangular filters and pill filters | `28-lend-entry`, `40-khata`, `41-godown` |
| Buttons | Black pill for saves, green rounded in the repay dialog, full-width green and red in Galla, grey rectangle in Settings, outlined pill for add lot | `16-buy-ready`, `33-after-repay`, `42-galla`, `46-settings-bottom` |
| Empty states | Card with action on Dashboard, grey line on Khata search and Galla | `05-dashboard-empty-voice-tapped`, `82-khata-search-empty`, `78-week-galla` |
| Numbers | `Qtl`, `Q` and `क्विंटल` for one unit; receipts show gross weight on Buy lots and net on Sell lots; ₹83.85 paisa on Sell, whole rupees on Buy | `41-godown`, `17-buy-after-save`, `26-sell-receipt` |
| Terms | कुंतल and क्विंटल; मजदूरी, हम्माली and पल्लेदारी for overlapping charges; डाटा and डेटा; नांव is a typo; "1 खातों", "1 entries", "1 Commodities" | `14-buy-bottom`, `46-settings-bottom`, `83-new-party`, `en-dash`, `en-godown` |
| English mode | Hindi leaks: गेहूं default crop, "ग्राहक का नाम दर्ज करें", "₹10 / बोरी", "फ़ोन:" | `en-sell`, `en-settings` |
| Raw colours | 17 references outside `ui/theme` across 7 files | grep on `ui/` |

Dark theme is the most consistent of the variants.

## Journeys

Tap counts come from the steps actually driven through adb. They exclude typed characters. No human stopwatch was used, so no durations are claimed. The only measured time is cold start: 829 to 939 ms over 3 runs on the emulator.

| Journey | Taps and swipes | Notes |
| :--- | :--- | :--- |
| First install to first bill | 16 taps, 1 to 2 swipes | Includes OTP auto-submit and the share button. No first-run guidance on the empty Dashboard |
| Buy, repeat | 8 taps and 1 to 2 swipes per buy, 40 taps for five | Ran 3 saves and 1 duplicate stop. The duplicate dialog works well |
| Sell plus WhatsApp bill | 9 taps, 2 swipes | Opens the system chooser and sends text; WhatsApp itself is not installed on the emulator |
| Find a party's balance | 0 to 3 taps | Zero if on Dashboard's recent list |
| Correct an entry | 6 taps, 1 swipe | Via tile then back; 5 via the KPI tile. No reason asked and no mark on the slip |
| Lend and repay | 6 taps, then 3 | Works; remaining amount not shown |
| Day close | 5 taps, 3 swipes | See P2 |
| Network loss | 0 extra | Saved offline and synced on reconnect. Solid, apart from expenses |
| Return after a week | 0 | Opens instantly and says nothing about the gap |

## What already works

Offline save and recovery, the duplicate and discard dialogs, the receipt layout, long name truncation, dark theme, 130% font, and the sign-out guard against unsynced data.

## Not verified

- Human timing, and any look at the Redmi. It stayed untouched: `lastUpdateTime` is unchanged.
- The emulator is about 411dp wide, not 390dp.
- Screens missed: `FirstSync` (it flashed by), Bill of Supply save and receipt, `DealEntry` and `ReceiptPreview`, expense list, `StockSheet` and `StockBreakdownSheet`, custom date picker, voice settings dialog.
- Variant gaps: receipts and party ledger in dark, English and 200%; dark with English; dark with 200%. Lists with real volume, and multiple crops in Godown.
- Whether the deployed server behaves like the local one on P1.
- Lines marked "not pinned" above.

## Test setup, to rebuild for Phase 3 and 4

The scratchpad scripts are session-only, and the emulator and backend are stopped. AVD `medium_phone` remains, on Android 35 Google APIs arm64. Start it with `-no-audio`, because `ui/auth/RegisterViewModel.kt:273` speaks on login.

- Debug APK: `local.properties` points `mandi.apiBaseUrl` at a remote server. Swap it to `http://10.0.2.2:8000` for the build and restore it afterwards. It is currently restored.
- Backend: from `backend/`, set `APP_ENV=development`, a scratch SQLite `DATABASE_URL`, `OTP_STATIC_ENABLED=true`, a random `OTP_STATIC_CODE` and `SECRET_KEY`, and blank `SENTRY_DSN` and `OPS_TELEGRAM_*`. Create tables with `Base.metadata.create_all`, then run uvicorn on port 8000. fakeredis is picked up automatically.
- Never run `./gradlew installDebug` with the Redmi attached, because it installs on every device. Use `adb -s emulator-5554 install`.

## Secret to rotate

`docs/product/reviews/2026-10-31-pilot-rehearsal.md` is committed with a static staging OTP and an MPIN in plain text. Rotate the OTP and remove both from the doc.

## Next session

Phase 2 mockups are in `docs/product/ux-directions/` (open `index.html` through a local server, renders in `screens/`). Direction chosen, see below. "B final" in that page draws the four updated screens and is published at https://claude.ai/artifact/M6MyfPumoDKt6Sr1CU8vwc. Vivek approved those screens on 2026-11-01. Next: ask the one open question below (cash payment larger than Rokdi), then write the Phase 3 plan: fix P1 to P4 first, then shared pieces, then Buy. The party ledger keeps जमा मिला and भुगतान दिया next to हिसाब चुकता, as approved in the mockup. This file replaces re-running the review. Do not update `hot.md` or bump the submodule until Vivek has approved the feature.

## Phase 2 decision (Vivek, 2026-10-31)

Direction B (तराजू) is final. Exception: Galla day close uses direction A's layout plus a small तराजू from B.

Vivek's feedback, as given:

New entry:
- Phone number is the primary field. If the number is in the DB, the name autofills. If not, save it to the DB once the entry is created.
- Bag count, weight and extra weight sit on one line. Weight max 3 digits, extra weight less than bag weight, bag count max 4 digits.
- Rate goes on the next line, max 100000.
- Remove Tare/Bardana. Move advance after the final calculations.
- A separate section after the slip calculation of the commodity, where advance is handled, with an option showing the person's existing loan or deposit with us.

Dashboard, top to bottom:
1. Current balance in Rokdi.
2. Metrics with Net payable, excluding any advance: only the amount for the commodity.
3. Top 5 recent slips as a details card, with See all.
4. All-time metric.

Party ledger: a settlement option, partial or full.

Answered by Vivek on 2026-10-31:
- Scope: Buy, Sell and Lend. Lend has no bags or rate, so only the phone-first rule applies there.
- Tare: removed from the math too. New slips use the weight entered. Saved slips keep their totals and tare line.
- Phone: required on every entry.
- Old parties with no number: the phone field also finds parties by name. Picking one asks for the number before Save, and it is stored on the party.
- Autofilled name: editable, and an edit renames the party everywhere, including older slips.
- Bag weight: whole kg, max 999. Extra weight: kg with decimals, less than bag weight.
- Rate: whole rupees per quintal, max 1,00,000.

- Commission % and labour per bag: stay as always-visible editable fields above the हिसाब पर्ची, filled from Settings.
- Loan or deposit section: Buy only for now. Show and adjust: a loan cut reduces the payout, a deposit used adds to the payout. A loan cut takes interest first, then principal, same as `RepayLoanDialog.kt`. Sell shows the balance with no adjustment. A deposit today is a general जमा in `cashTransactionEntity`, and loans are `lendEntryEntity` with `lendSettlementEntity`.

- Dashboard Rokdi: Galla book cash now, updating with every entry.
- Dashboard Net payable: Buy slip value only. Advance and later payments are both ignored.
- Dashboard dated block: Net payable, weight bought and sold per crop, expenses. Opens on today, with the existing date chips.
- Dashboard Vyapari receivable card, added after the first mockup review: what is still to collect on Sell slips in the selected dates, after money received (₹5,590 slip with ₹2,000 paid shows ₹3,590). It follows the date chips, so older dues don't show on आज. Sits below Net payable.
- Dashboard cards all open a screen: Rokdi to Galla, Net payable to the Buy list for the selected dates, व्यापारी से लेना to unpaid Sell slips for the selected dates, crop weight to Godown, खर्च to the expense list. The all-time cards open the same lists undated, and कुल उधार opens the Lend list.
- Village: required, below name on the entry form, autofilled from the saved party. Editing updates the party. Save stays off until it is filled, including for old parties without one.
- हिसाब पर्ची: मजदूरी कटौती sits above कमीशन कटौती.
- Slip payments: a payment shows a link to its slip, the slip shows paid and left, and Galla lists it against the slip. Paid through हिसाब चुकता only.
- Interest money, separate from slips: जमा मिला records a kisan's deposit with us, भुगतान दिया records our loan. Each has an optional interest rate and no slip link, and both move Galla cash. Interest we owe on a deposit is new: it needs its own calculation and may need a schema change.
- Recent slips: last 5 Buy slips, newest first. See all opens the existing Buy list.
- All-time block: total purchases, total sales, outstanding loans, total expenses.
- Settlement: the operator picks one slip or loan, then pays full or partial against it. Always cash, so it moves Galla and Rokdi. A bank payment made this way would make Rokdi wrong.
- Galla: small तराजू inside direction A's बही / गिनती card, shown only after the count. Level if they match, tilted if not.

- Bugs: P1, P2, P3 and P4 are all fixed before the redesign starts.
- Pilot: stays on the current build. Each finished flow goes out as an update only after Vivek has tested it on test data.
- Dashboard actions: Buy, Sell, Lend and Expense in a fixed dock above the tabs, as in direction B.
- Mockups: update Buy, Dashboard, party ledger and Galla in `docs/product/ux-directions/` for approval before the implementation plan is built.

Latest feedback, not yet in the mockup (Vivek, 2026-11-01). It replaces the separate ₹70 row in "Slip payments" above:
- Party ledger does not show a slip payment as its own row. It shows one entry per bill with the total, then the paid amount if any, like the mockup's last card.
- After Save on New Purchase, the next screen offers paying the kisan a partial amount. It then creates an updated bill that includes the paid amount, for example ₹70.

Answered by Vivek on 2026-11-01:
- Advance: the अग्रिम / पेशगी field stays on the form and still changes the final payable. The post-save payment is recorded on top, so a bill can show both.
- Updated bill: keeps the same number, adds paid and left lines, and carries a "संशोधित" mark with date and time. Share always sends the latest version.
- Later payment: हिसाब चुकता can add a partial payment from the ledger. It updates the same bill, with the same mark, and moves Galla cash.
- Loans: one ledger entry per loan showing principal, interest, repaid and left, with repayments listed inside it. The +/- pair goes away.

These four are drawn in B final, published to the same artifact link (version `1793472977-e1bf`) and approved by Vivek on 2026-11-01, along with these mockup choices:
- The ledger's देना है stays ₹20,560 (slip left ₹23,560 − loan principal ₹3,000) and says the ₹200 interest is not included. The running बाकी is dropped from B final.

Answered 2026-11-01 (D1): Vivek says a cash payment larger than Rokdi will never happen, so no warning or block is built. The sample पूरा amounts on the payment screens being larger than the ₹1,580 in Rokdi is only mock data.
