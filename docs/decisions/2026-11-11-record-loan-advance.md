# Decision: Record Loan / Advance entry design

Date: 2026-11-11
Status: Implemented in the app; build and device UI checked in Light mode. Dark-mode visual check remains unverified.

## Decision

Use two separate entry flows in a ViewPager with visible tabs: **Loan Received** and **Loan Given**. Tapping tabs and swiping both change pages. Open on **Loan Given**.

## Loan Received fields and behavior

- Lender name, village, phone number, whole-rupee amount, and transaction date are required.
- Do not capture interest rate or repayment terms in this entry flow.
- Suggest saved lenders. Selecting one fills their village and phone number.
- Save a new lender with their details. Editing a saved lender's village or phone updates the saved lender record.
- Put lender mobile number before name and use the number as the primary saved-lender lookup field.

## Loan Given fields and behavior

- Given to: the person who physically received the cash. Put their mobile number before their name; require both, plus village. Use mobile number as the primary lookup field. When it matches a saved person, autofill name and village. Do not save new recipients.
- On behalf of: optional beneficiary whose account receives the advance. When enabled, put mobile number before name and village; require all three. A saved mobile match fills name and village; save new beneficiary details.
- Validate phone numbers as 10-digit Indian mobile numbers.

## Shared behavior

- Amounts are whole rupees.
- Prefill the date with today. Allow earlier dates and block future dates.
- Villages use free text with suggestions from saved villages. Save new village names for later suggestions.

## Scope

This decision covers the two cash flows described by the user. Grain lending is outside this cash-entry screen.

## App integration

- Loan Received is saved as a `JAMA_RECEIVED` cash transaction linked to the lender, tagged `LOAN_RECEIVED`; this increases cash and records the amount owed to the lender in the party balance.
- Loan Given uses the existing lend entry and linked `UDHAR_GIVEN` cash transaction. The ledger party is the beneficiary when one is supplied; otherwise the physical recipient is the account holder and is saved as a party. The physical recipient's details remain on the entry when a different beneficiary is selected.
- The Loan / Advance screen does not include a grain-lending action.
- New villages are suggested from saved parties and saved recipient details on existing loan entries.
