# MandiSamiti Complete Revamp Design & Multi-AI Spec

**Date:** 2026-10-27  
**Status:** Approved for Implementation Planning  
**Reference Repo:** `tmp/reference-mandi-mobile-app` ([https://github.com/brew-tech-app/Mandi-Mobile-App](https://github.com/brew-tech-app/Mandi-Mobile-App))  
**Target Codebase:** `projects/MandiSamiti`

---

## 1. Problem & Goals

`MandiSamiti` currently has a trustworthy ledger, append-only revisions, and daily cash closing, but only offers a single combined 2-stage auction deal entry.

Real grain mandis require distinct operations:
1. **Buy (खरीद / आवक):** Farmer purchasing with bags, weight per bag, extra loose weight, commission, labour per bag, and advance deductions.
2. **Sell (बिक्री / जावक):** Wholesale / retail sales with Normal trade vs Bill of Supply (with Arat %, Tulak, 1.5% Mandi Tax).
3. **Lend (उधार / कर्जा):** Lending money or grain bags with return date tracking.
4. **Expense (दैनिक खर्च):** Quick standard mandi operational expenses.
5. **Stock Inventory (स्टॉक बही):** Live warehouse quintals calculation (`Buy - Sell`).

---

## 2. Screen Inventory (11 Target Screens)

- **S1:** `BuyEntryScreen.kt` (Purchase form with bags, weight/bag, extra weight, advance deduction)
- **S2:** `BuyTransactionsListScreen.kt` (Purchase log with phone search & payment status)
- **S3:** `BuyTransactionReceiptScreen.kt` (Formatted farmer slip with WhatsApp share)
- **S4:** `SellEntryScreen.kt` (Sale form with Normal vs Bill of Supply and tax math)
- **S5:** `SellTransactionsListScreen.kt` (Sale log with credit balance tracking)
- **S6:** `SellTransactionReceiptScreen.kt` (Sale bill with tax breakdown)
- **S7:** `LendEntryScreen.kt` (Money / Grain loan entry with return date)
- **S8:** `LendTransactionsListScreen.kt` (Loan book & settlement)
- **S9:** `ExpenseEntryScreen.kt` (Expense entry with standard mandi chips)
- **S10:** `StockBreakdownSheet.kt` (Godown stock breakdown by commodity)
- **S11:** `HomeScreen.kt` / `DashboardScreen.kt` (Revamped with 4 KPI cards, godown stock tile, and speed-dial FAB)

---

## 3. Mathematical Specifications

- **Weight in Grams:** $(\text{Bags} \times \text{WeightPerBagKg} \times 1000) + (\text{ExtraWeightKg} \times 1000)$
- **Gross Paisa:** $\text{round}((\text{Grams} \times \text{RatePerQuintalPaisa}) / 100,000)$
- **Labour Paisa:** $\text{Bags} \times \text{LabourPerBagPaisa}$
- **Commission Paisa:** $(\text{Gross Paisa} \times \text{CommissionBps}) / 10,000$
- **Net Farmer Payable:** $\text{Gross} - \text{Commission} - \text{Labour} - \text{AdvancePaid}$
- **Bill of Supply Additions:** $\text{Gross} + \text{Arat} + \text{Tulak} + \text{MandiShulk}$

---

## 4. Multi-AI Work Allocation

- **Claude Code**: Master implementation plan in `docs/superpowers/plans/`, SQLDelight schemas, and KMP UseCases.
- **Gemini / Antigravity**: Compose Multiplatform UI screens, NGDL token compliance, and Redmi Note 7 hardware verification.
- **Codex / OpenCode**: JVM / Pytest unit test suites and sync DTO verifications.
- **Hermes / Terminal**: Gradle builds, test runs, and Telegram alerts.
