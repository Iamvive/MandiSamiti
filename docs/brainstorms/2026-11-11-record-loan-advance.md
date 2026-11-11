# Record Loan / Advance redesign

Status: Option 2 implemented in the app. Interview decisions are captured; the separate HTML file remains a preview only.

## Agreed structure

- Use a ViewPager with visible, tappable tabs for **Loan Received** and **Loan Given**; allow swipe navigation too.
- Open on **Loan Given** by default.
- Prefill transaction date with today. Permit past dates and block future dates.
- Record whole rupees only.
- Put the mobile-number field before the person's name in every contact section. Use mobile number as the primary lookup field.

## Loan Received

- Capture lender mobile number, lender name, village, amount, and date. Put mobile number before name and use it for saved-lender lookup. Do not add interest rate or repayment fields.
- Require village and phone. A saved lender fills their details; a new lender and their details are saved.
- If saved lender village or phone details are updated here, update the saved lender too.

## Loan Given

- **Given to** is the person physically receiving cash. Capture their mobile number first, then name, village, amount, and date. Village and phone are required.
- Use mobile number as the primary lookup for **Given to**. When it matches a saved person, autofill their name and village. Do not save a new recipient as a saved person.
- **On behalf of** is the person whose account should carry the advance. This is optional. When used, capture mobile number before name, then village; require all three. A saved mobile match fills name and village; save new beneficiary details.
- Validate Indian mobile numbers as 10 digits.

## Village entry

- Use free text with saved village suggestions. Save newly entered villages for future suggestions.

## Scope note

- The two cash flows described are shown. Existing grain lending is not addressed by this redesign.
