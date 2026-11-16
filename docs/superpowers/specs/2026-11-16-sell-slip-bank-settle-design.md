# Sell Slip, Deal Labour and Galla Bank Tags: Design

Date: 2026-11-16. Source: Vivek's handwritten note, decisions taken one point at a time in a brainstorm session. Status: approved by Vivek, not planned or built.
Rough UI mock: `proposal.html` from the session (not committed). Layout is described below.

## 1. Deal screen: labour per quintal

**Today.** `DealEntryScreen` has one flat field "हम्माली / लेबर (₹)". It defaults from `defaultLabourPaisa` (the per-bag setting) and is stored as `labourChargePaisa`. It ignores weight.

**Change.**
- Relabel the field "हम्माली (₹ प्रति क्विंटल)". It stays editable on the Deal screen.
- Labour = rate x net quintals, using `MandiMathEngine.grossAmountPaisa(netGrams, ratePaisaPerQuintal)`.
- The default comes from `TradeSettings.sellLabourPaisaPerQuintal`. The Settings field "व्यापारी हम्माली (₹ प्रति क्विंटल)" is prefilled with ₹1 on a fresh install.
- The invoice row shows the working, for example "− हम्माली 50 क्विं × ₹1 = ₹50".
- Existing deals keep their stored `labourChargePaisa`. Editing one shows the stored total, not a recomputed rate.

## 2. Sell entry: Mandi shulk % field (Bill of Supply only)

**Today.** `BuySellMath.calculateSell` uses a fixed `MANDI_SHULK_BPS` (about 2%). Both receipt renderers print "मंडी प्रभार एवं उपकर (2%)".

**Change.**
- Add a "मंडी शुल्क %" field, default 2, in the charges row of `SellEntryScreen`, shown only when the slip is a Bill of Supply for a merchant.
- Layout: three equal fields in one row: कमीशन %, मंडी शुल्क %, मजदूरी (रु/क्विंटल). Cash Memo and customer slips keep two fields.
- `SellMathInput` gets `mandiShulkBps`. `calculateSell` uses it instead of the constant. The rate is stored on the sell entry so a reprint shows the rate used.
- Receipts print the entered rate, for example "मंडी शुल्क (1.5%)".

## 3. Bill-type tag and receipt title

**Today.** The list shows a "सप्लाई बिल" chip for a Bill of Supply and a party chip (व्यापारी or ग्राहक) for every other slip. The receipt title for other slips is "बिक्री पर्चा (Mandi Sale Bill)".

**Change.**
- Every slip in `SellTransactionsListScreen` shows a bill-type chip: "सप्लाई बिल" or "कैश मेमो". The party chip stays beside it.
- The non-Bill-of-Supply receipt title becomes "कैश मेमो (Cash Memo)" in `SellTransactionReceiptScreen` and `ReceiptHtmlRenderer`.

## 4. Settle with Cash or Bank

**Today.** `TraderSlipCashSettlementSheet` takes an amount only. `settleSellSlip` writes `JAMA_RECEIVED` with `PaymentMode.CASH`. `PaymentMode` already has CASH, UPI, RTGS, NEFT and CHEQUE.

**Change.**
- The settle sheet gets a switch नकद (Cash) / बैंक (Bank), default Cash. The same sheet is used from the parchi list and the party ledger.
- Cash behaves as today and adds to Galla.
- Bank shows: mode (NEFT, RTGS, Cheque, UPI; required), reference (UTR, cheque number or UPI ID; required), bank name (free text, optional).
- No bank account list and no Settings screen for banks.
- The cash row stores the real mode, never CASH, plus the buyer through `partyId`.

## 5. Data

Add to `cashTransactionEntity` and `CashTransaction`: `reference_no TEXT` and `bank_name TEXT`, both nullable. This needs a numbered `.sqm` migration, sync DTO fields (`reference_no`, `bank_name`, defaulting to null for old clients), and a backend migration. Check the Udhaar plan's migration `14.sqm` and `0007` first so numbers do not collide. `CashTransaction` already carries `lendId` from that plan.

Add `mandi_shulk_bps INTEGER NOT NULL DEFAULT 200` to the sell entry table with the same migration and sync care.

## 6. Galla and day close

- The drawer already counts only CASH rows (`CashDrawer`). Bank, cheque and UPI never change drawer cash.
- `BahiDayPageBuilder` already writes a non-cash entry on both sides with an ONLINE_COUNTERPART line, so left equals right. Keep this unchanged.
- Jama display for a bank settle: "[Buyer name] · NEFT · Ref 123456 · SBI", with the buyer's name always shown.
- `DailyCashRegisterScreen`: non-cash rows join the existing In and Out lists, each with a small mode tag (Cash, NEFT, RTGS, Cheque, UPI). No new section or tab.
- Under the cash total, add one small line: "Bank/Cheque/UPI: ₹X (not in cash)". The cash total itself excludes non-cash rows.

## 7. Tests

- Labour: 50 quintals at ₹1 gives ₹50. Rate 0 gives 0. An edited old deal keeps its stored amount.
- Mandi shulk: a Bill of Supply at 1.5% charges 1.5%. Cash Memo charges none. Default is 2%.
- Settle: a bank settle stores the chosen mode, reference and bank name, and leaves `CashDrawer` closing unchanged. A cash settle raises it.
- Bank settle without a reference is rejected.
- Bahi page: a bank settle gives a balanced day (difference 0).
- Galla screen state: non-cash rows appear with the right tag, and the cash total excludes them.

## 8. Out of scope

Bank account list, partial-bank split settles, buy-slip (farmer payment) bank settles, reconciling against bank statements.

## 9. Open items

- Confirm the settle sheet also covers the farmer payment side. This spec covers sell slips (buyer receipts) only.
- NGDL checks apply to every changed screen: tokens only, contrast 4.5:1 in light and dark, three fields fit at 390dp without horizontal scroll, no emoji.
