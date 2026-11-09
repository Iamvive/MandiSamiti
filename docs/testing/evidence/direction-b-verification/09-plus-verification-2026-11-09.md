# Direction B verification, Sell and Lend entry (2026-11-09)

Device: Redmi d93c743 (1080x2340). Build: debug APK assembled from `main` at `cdac6be`, installed 11:26. Backend: staging `http://64.227.142.180:8060`.
Method: real adb taps and typed input. Nothing was saved, no write reached staging. Test input was discarded through the unsaved-changes dialog.
Device state: the phone was on Light mode with Schedule Dark mode on. I switched to Dark mode for the dark pass and set it back to Light mode.

## Pass / fail

| # | Check | Sell | Lend | Evidence |
|---|-------|------|------|----------|
| 1 | Mobile mandatory | PASS | PASS | 20, 16, 17 |
| 2 | No kaat / bardaana field on the entry form | PASS | n/a | 10, 14 |
| 3 | Bori / bajan / extra on one line | PASS | n/a | 10, 14 |
| 4 | Phone-first party block (phone, then name, then village) | PASS | PASS | 09, 15, 19 |
| 5 | Sticky action dock | PASS | FAIL | 10, 14 (Sell); 17, 18 (Lend) |
| 6 | Light theme renders | PASS | PASS | 09-11, 19 |
| 7 | Dark theme renders | PASS | PASS | 13-15 |
| 8 | Unsaved-changes guard on back | PASS | not tapped | seen on Sell |
| 9 | Settings rates: farmer 0.7%, labour Rs 10 | PASS on existing shop | | 12 |

Notes per check:
1. Sell: with name and village filled and no phone, the dock stays disabled and reads "enter 10-digit mobile number" (20). Lend: 9 digits shows a red error "must be 10 digits (starts 6-9)" (16); 10 digits shows "new number, account created on save" (17).
5. Lend FAIL: the Save button is an inline item at the end of the scroll. After a scroll it moved up the screen (17 vs 18) and sits clipped at the bottom edge when the form grows. Sell's dock (total plus Save) stays fixed while the form scrolls, in both themes.
9. This reads the existing pilot shop (farmer 0.70%, merchant 1.50%, labour Rs 10 per bori, default kaat / bardaana 0.35 Qtl). It is not a clean install.

## Other findings

- The mobile field on Sell and Lend opens the full text keyboard, not a numeric pad. Label on both reads "mobile number or name".
- Sell is mobile-mandatory even though its label says "mobile or name".
- Dashboard still shows Rokad Rs 0 (known open bug, not retested here).

## Unverified

- Clean-install defaults (farmer 0.7%, labour Rs 10 on a fresh install). Not done: the phone holds real shop data (bills #4 to #8 worth lakhs, a GSTIN), and clearing app data would wipe the local copy. Needs Vivek's approval.
- Lend Grain mode, Merchant buyer type and Bill of Supply on Sell.
- Save paths (blocked on purpose, no writes).
- 390px width and contrast ratios were not measured. The screens were only viewed.
- Sell below the receipt block (bottom of the form past "Total Receivable").
- Light and dark were both checked on the new-entry screens only, not on the rest of the app.

## Screenshots

09 Sell light top, 10 Sell light scrolled, 11 Sell light phone typed, 12 Settings rates (existing shop), 13 Sell dark top, 14 Sell dark scrolled, 15 Lend dark top, 16 Lend dark 9 digits, 17 Lend dark 10 digits (dock clipped), 18 Lend dark scrolled (button moved), 19 Lend light top, 20 Sell light name and village without phone.
