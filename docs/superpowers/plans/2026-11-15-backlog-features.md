# MandiSamiti: 2026-11-15 Feature Backlog Execution Plan

Audience: Claude Code and Antigravity (Gemini), so either can pick up any milestone.

## Context
On 2026-11-15 Vivek logged six features in `vaults/Vivek-K/wiki/hot.md` (lines 101-107). A grill-me interview settled the open questions. This plan holds the decisions and the build order. Repo: `projects/MandiSamiti` (KMP, main at `db86398`). Per the MandiSamiti Feature Velocity Rule, build first and add tests once the feature is final. Money math is the exception: it gets tests in the same change, because a wrong bill means a wrong ledger.

On approval, copy this file to `projects/MandiSamiti/docs/superpowers/plans/2026-11-15-backlog-features.md` and add a pointer in `hot.md` so Antigravity can find it.

## Decisions (locked)
| # | Feature | Decision |
|---|---|---|
| D1 | Buy labour leftover rule | Extra weight of **20 kg or less is free**. Above 20 kg it is billed as +1 bori. This replaces today's 5 kg threshold. **Buy only.** |
| D2 | Extra weight on bills | Show the extra kg on Buy and Sell bills. **Sell math now bills the full weight** (bags × wt + extra). Today it passes `0L`. |
| D3 | Stock check on Sell | **Warn, allow override.** Show the shortfall; the user confirms and stock may go negative. |
| D4 | Trader (Vyapaari) Sell labour | **New separate charge**, per quintal on the full weight. Tulak stays as it is. Rate comes from **Settings** (no hardcoded default; 0 or empty means no labour line). **Hide the prefilled labour chips** for trader sales. Paise in the total get rounded into Labour to the **nearest rupee**. |
| D5 | Print | **System print / PDF** via the Android print framework. Bluetooth thermal is deferred. |
| D6 | Shop profile edit | Editable: shop name, owner name, mandi name, **plus address, GSTIN, licence no.** (new fields). Phone stays read-only. |

## Milestones (in order)

### M1: Labour math (D1, D2, D4 core); domain only, tests required
Files: `core-domain/.../domain/math/BuySellMath.kt`, `domain/model/TradeSettings.kt`, `core-domain/src/commonTest/.../BuySellMathTest.kt`
- `EXTRA_BAG_THRESHOLD_GRAMS`: `5_000L` → `20_000L` (rule: `extra > 20 kg` adds a bori).
- `calculateSell`: pass the lot's `extraWeightGrams` to `totalWeightGrams` (D2). Add `extraWeightGrams` and `traderLabourPaisaPerQuintal` to `SellMathInput`. Add `labourPaisa` to `SellMathResult`.
- Trader labour = `grossAmountPaisa(grams, rate)`. After the existing arat, tulak and shulk logic, round the total to the nearest rupee and put the difference into labour. Cash Memo today floors via tulak, so decide the order: labour absorbs the rounding and tulak stays at its default for trader sales. Test both bill modes.
- `TradeSettings`: add `traderLabourPaisaPerQuintal: Long = 0L`.
- Tests: Buy with 10 bags plus 4, 12, 20 and 21 kg → 10, 10, 10 and 11 bori. Sell 10 bags + 21 kg = 5.21 qtl. Trader rounding cases: x.49 rounds down, x.50 rounds up.
- Check: do saved Buy entries recompute on read? If yes, old bills would change. Confirm that saved entries store their computed labour (look at `BuyEntry.kt` and `BuyTransactionUseCase.kt`) before shipping.

### M2: Sell form, trader labour and stock warning (D3, D4 UI)
Files: `composeApp/.../ui/sell/SellEntryFormState.kt`, the Sell screen/VM, the settings screen (`TradeSettingsFormDefaultsF05Test` shows where defaults live)
- Settings: a per-quintal trader labour rate field.
- Sell form: when the party is a trader, show the Labour line (auto-calculated) and hide the labour chips.
- Stock: on save, compare requested quantity with available stock per crop. If short, show a dialog: "Stock X qtl, sale Y qtl. Save anyway?" Find the existing stock query first (grep the `core-data` repositories for stock or inventory). If none exists, compute stock as sum(buy) − sum(sell) per crop for the shop.

### M3: Bills show the extra weight (D2 display)
Files: the slip equation formatter from today's session (`SlipEquation*`, see `composeApp/.../ui/receipts/`), `BuyTransactionReceiptTest`, `ReceiptChargesAndDefaultsTest`
- The slip line already reads `bags × wt + extra = qtl × rate`. Make sure Sell uses it with the real extra value, and add the trader Labour line to the Sell receipt.
- Update the WhatsApp share text the same way.

### M4: Shop profile edit (D6)
Files: `domain/model/ShopProfile.kt`, the `core-database` schema plus a migration (follow the `PilotSchemaMigrationTest` pattern), the sync DTO and backend columns, Settings UI
- Add `address`, `gstin`, `licenceNo` (nullable). Add an "Edit shop profile" screen in Settings with the phone shown read-only.
- Validate GSTIN format (15 characters) when it is filled in. Set `updatedAt` and `syncStatus` so the change syncs.
- The bill header shows the new fields when present.
- Backend: the sync push must accept the new columns. Check `backend/` for the shop_profile schema.

### M5: Print (D5)
- Android `actual`: render the receipt as an HTML document or PDF and hand it to `PrintManager` (WebView `createPrintDocumentAdapter` is the cheapest option). Add an expect/actual `ReceiptPrinter`. On iOS, `UIPrintInteractionController` or a no-op stub for now.
- Add a Print button next to Share on Buy, Sell and the other receipts. Reuse the same receipt model that share uses, so there is a single source of truth.

## Guardrails
- NGDL tokens only, no emoji, check at 390px in light and dark (CLAUDE.md §5). Read `DESIGN.md`.
- Store money as paisa `Long`. No doubles.
- After each milestone: `./gradlew :core-domain:allTests :composeApp:jvmTest`, then update `FEATURES.json` and `PROGRESS.md` (the seven backlog entries already exist; update their status) and commit.

## Verification
1. Unit tests: all the M1 cases pass. The full jvmTest run passes, except the known flaky `HomeViewModelTest`, which should be rerun in isolation.
2. On the emulator, debug APK: Buy 10 bags + 20 kg shows 10 bori labour. A trader sale of 10 bags + 21 kg shows a 5.21 qtl line, a labour line and a whole-rupee total. A sale over stock shows the warning and saves after you confirm. Editing the profile changes the bill header. Print opens the Android print dialog and the PDF matches the share text.
3. Sync: an edited profile and a new sale both push to staging without errors.

## Still open (ask when reached)
- M2: trader sales get no labour chips. Do farmer and customer sales keep any labour chips? (Default: unchanged.)
- M5: paper size for PDF: A5 or A4? (Default: A5.)
