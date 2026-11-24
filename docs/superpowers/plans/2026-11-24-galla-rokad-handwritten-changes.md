# Galla-Rokad changes from Vivek's handwritten note (2026-11-24)

Source: handwritten note headed "Mandi App", photo shared in Claude Code session. Scope is the Galla-Rokad (cash drawer) screen, `ui/register/DailyCashRegisterScreen.kt`.

| # | Request | Status |
|---|---|---|
| 1 | Galla-Rokad entries must be editable | In progress on branch `feat/galla-rokad-editable`. Compiles (`:composeApp:compileKotlinJvm`). Not yet owner-tested. |
| 2 | A direct Galla-Rokad deposit or withdrawal must not attach a transaction | Open. Unconfirmed reading: remove the forced link to a sale or purchase transaction. Waiting on Vivek. |
| 3 | Withdrawal (निकासी) form fields: name, mobile no., type (पर्ची or उधार निकासी), payment type (Cash, UPI, NEFT, Cheque), note | Not started |
| 4 | Deposit (जमा) form fields: name, mobile no., type (पर्ची जमा or उधार जमा), payment type (Cash, UPI, NEFT, Cheque), note | Not started |

## Point 1 design
- `CashTransactionRepository.editTransaction` already exists (appends an EDIT revision, rejects voided rows and rows tagged `[LEND:`, `[EXPENSE:` or `[SETTLE:`).
- `DailyRegisterViewModel.editDailyEntry(transaction, amountRs, remarks)` calls it and triggers sync.
- `BahiTransactionItem`: the details dialog shows an Edit button for editable rows; it opens an amount + note dialog. Time is unchanged. Payment mode chips mirror the add-entry dialog (hidden for credit rows). Switching a cash row to UPI or bank moves it out of the drawer cash total.
- Linked rows stay non-editable here (edit them from their own lend, expense or settlement screen).

## Open questions
- Should edit also change name, payment mode and type once points 3 and 4 add those fields?
- Point 2 meaning (see table).

## Verification still to do
Light and dark theme, 390px phone, real taps on staging, edit then check drawer totals and revision history. Tests after the flow is final (feature-velocity rule).
