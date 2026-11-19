# Decision: Udhaar Data Model & Ledger Rules

Date: 2026-11-16
Status: Accepted and Implemented in KMP (Phase 2 Udhaar Rewrite)

## Context

The legacy loan/advance implementation stored loans and settlements in ways that leaked into market slip balances, caused cash drawer discrepancy confusion, and lacked proper directionality (दिया vs लिया), kind categorization (LOAN vs PESGI vs CASH), and atomic loan cuts from buy slips.

## Core Rules

1. **Slip Balance and Udhaar Balance Never Add:**
   - Slip balance (पर्चियों का बाकी) and Udhaar balance (उधार बाकी) represent distinct accounting domains.
   - They are never netted or combined into a single balance on Home, Khata headers, or party lists.
   - Slip balances reflect commodity trade receivables/payables only.

2. **Oldest Loan First, Interest Before Principal:**
   - Loan repayments allocate strictly against the oldest open non-void loan first (`entryDate` ASC, `id` ASC).
   - For an individual loan with accrued interest, settlement pays accrued interest first (`interestPaisa`), then principal (`principalPaisa`).
   - Settlements are tracked via `LendSettlement` records with atomic SQLite transactions.

3. **Overpayment Blocked:**
   - Overpayment beyond outstanding principal and calculated interest is rejected with clear, localized validation messages.
   - Prevents phantom negative loans or distorted balance states.

4. **Buy Slip Loan Cut & Atomic Void Reversal:**
   - A loan deduction cut made on a buy checkout (`BOOK_ENTRY` cash transaction with `[BUYCUT:id]`) settles oldest open loans.
   - If the buy slip is voided, the loan cut, cash transaction, and settlements are atomically voided, restoring the original loan principal and status (`OPEN` / `PARTIAL`).

## Open Decision Outcomes

- **Open Decision 1 (DISCOUNT_GIVEN & INTEREST_ADDED buttons removed):**
  - Removed separate discount/interest adjustment buttons from the ledger (Task 9, accepted).
  - Cleaned up ledger interaction to avoid manual arbitrary ledger adjustments; settlements strictly record real payments.

- **Open Decision 2 (Home KPIs & Udhaar Card):**
  - Home screen's `totalMarketReceivablePaisa` and `totalFarmerPayablePaisa` reflect trade slip balances only (Task 11, accepted).
  - Home features a dedicated **Udhaar Card** showing live totals for "दिया: ₹X" and "लिया: ₹Y" calculated across active parties. Tapping the card opens the Udhaar landing page.

- **Open Decision 3 (Plain Cash Repayment Routing & Mirroring):**
  - In party cash flows (Task 3, accepted):
    - Plain "रुपये दिए" (cash out) repays outstanding "उधार लिया" (TAKEN) loans oldest-first. If no TAKEN loans exist, it records a new "उधार दिया" (GIVEN) loan.
    - Plain "रुपये मिले" (cash in) repays outstanding "उधार दिया" (GIVEN) loans oldest-first. If no GIVEN loans exist, it records a new "उधार लिया" (TAKEN) loan.
    - Overpayment against opposite loans is blocked.
