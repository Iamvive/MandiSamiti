# MandiSamiti: Device Proof of Merged Fixes (2026-10-29)

Tested on physical device: Redmi Note 7 (adb id `d93c743`), Android 10 (MIUI).
Build: v2.0.0 debug (`com.appwork.mandisamiti`) at commit `fe603f3` on `main`.

## Verification Matrix

| Check ID | Description | Status | Evidence Screenshot | Notes |
|---|---|---|---|---|
| **R1** | Over-repayment rejected in Loan Book | **PASS** | `11-r1-over-repayment-rejected-loan-book.png` | Entering ₹7,000 on a ₹5,000 loan shows inline error `वापसी राशि बकाया (₹5,000) से अधिक नहीं हो सकती`, confirm button remains disabled. |
| **R1** | Over-repayment rejected in Party Ledger | **PASS** | `08-r6-deposit-split-dialog.png` | In Party Ledger, attempting to deduct a deposit exceeding the loan balance gives warning card `दर्ज राशि इस ऋण के बकाया से अधिक है` and forces split / general jama. |
| **R4** | Loan cash row void cascades; party balance updates | **PASS** | `03-pm-test-ledger.png`, `12-cash-drawer.png` | PM TEST party shows balance `₹0 (हिसाब चुकता)`. Voided entries remain struck through with `रद्द · गलत प्रविष्टि`. Cash drawer reflects VOID without double-counting. |
| **R6** | Deposit larger than outstanding loan splits correctly; back button preserves form | **PASS** | `08-r6-deposit-split-dialog.png`, `09-r6-form-reopened-with-amount.png` | Split dialog offers clear choices (`उधार में काटें`, `सामान्य जमा करें`, `वापस जाएँ`). Tapping `वापस जाएँ` reopens the cash deposit form with ₹50,000 intact without losing state. |
| **F19** | Day-closing reconciliation count and reason | **PASS** | `13-f19-day-closing.png`, `14-f19-count-saved-diff-shown.png` | Count dialog accepts physical count and optional reason ("Chai pani"). After save, Cash Drawer displays `Diff: -₹200` alongside closing figures. |
| **F22** | Cold start under 4s | **PASS** | TotalTime: `3910ms`, WaitTime: `3914ms` via `am start -W` | Cold start clears skeleton quickly on Redmi Note 7 hardware. |
| **F23** | Equal-height action tiles and KPI tiles | **PASS** | `01-home-screen.png` | Action tiles (Buy, Sell, Lend, Expense) and KPI cards use intrinsic row heights and fill max height cleanly. |
| **F24** | Clear sync status pills | **PASS** | `15-f24-sync-pills-party-cards.png` | Party cards display `फ़ोन में` (local pending) with clock icon rather than bare checkmark. Header shows 21 unsynced entries with session expired guidance. |
