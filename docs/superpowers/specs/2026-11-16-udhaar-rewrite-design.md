# Design Spec: Udhaar Rewrite

**Date:** 2026-11-16
**Author:** Claude Code with Vivek Kumar
**Status:** Draft for Vivek's review. Every decision below was confirmed by Vivek in conversation on 2026-11-16.

## 1. Why

Testing on 2026-11-15 showed the udhaar flow is not what a trader needs. Reading the code showed why:

- Udhaar is five mechanisms (loan entries, `UDHAR_GIVEN` cash rows, loan-received cash rows, unpaid slip balances, discounts) summed into one party balance by `getPartyBalance` (10 terms).
- Double counting is avoided by matching Hindi text in remarks (`उधार वापसी%`, `[SETTLE:`, `[LEND:`). Renaming a string would silently change a balance.
- Interest is forced (rate required, default 2 percent, 0 rejected) and never part of the balance.
- Loan Received has no loan record, no interest, no repayment tracking.
- The entry form asks about ten fields. The repay and settle dialogs make the trader pick loans and slips.

## 2. Core rule

**A slip owns its own money story. Udhaar owns only udhaar.**

A buy slip of 10,000 with 4,000 paid on one day and 1,000 on another is entirely slip money. Those payments never show in the Udhaar screens or totals.

Udhaar is money lent or borrowed outside any slip, plus its repayments and interest.

## 3. What counts as udhaar

| Case | Udhaar? |
|---|---|
| Loan given or received from the Udhaar screen | Yes |
| Pesgi given before the farmer comes to sell | Yes, from the moment it is given. Tag "पेशगी" |
| Cash given on the day of the slip as part of that slip | No, slip payment |
| Plain cash entry in the Party Ledger with no slip ("रुपये दिए" / "रुपये मिले") | Yes. "दिए" creates उधार दिया. "मिले" repays what the party owes; if they owe nothing it becomes उधार लिया |
| Slip dues, slip payments, slip advances settled inside the slip | No |

**Loan cut on a buy slip:** shows on the slip as a deduction line and on the udhaar side as a repayment row tagged "पर्ची #... से कटौती". Both sides always agree.

**Pesgi adjusted on a slip:** same mechanism. The slip shows its अग्रिम line, the udhaar side gets a repayment row. When the udhaar balance reaches zero the person leaves the Udhaar list.

## 4. Screens

### 4.1 Udhaar landing page (replaces `LendList`, opens from Home)

- View pager, two tabs: **उधार दिया** and **उधार लिया**.
- Each tab lists people, one row each: name, amount still due, small "ब्याज" mark when interest is on.
- Add action on the same page. The separate `LendEntry` screen goes away.
- No new bottom-bar tab. Navigation changes only inside `Screen.LendList`.

### 4.2 Add form (one per tab)

- **Mobile** (required, first field). A saved number fills the name.
- **Name** (required for a new person).
- **Amount** (required).
- **Date** (today by default, past allowed, future blocked).
- **ब्याज** switch, **off by default**. When on, one rate field (percent per month).
- **उधार दिया only:** switch "पैसा किसी और को दिया?". When on, one name field for the receiver, plus an optional mobile. The receiver is a name-only note, not a party. The loan counts against the account holder. It shows on the loan row and ledger entry as "प्राप्तकर्ता: ...".
- Village and notes are optional and live on the person's profile, not on every loan.
- Removed: the "on behalf of" block, required village, required rate.

### 4.3 Person screen and repayment

- One balance: "बाकी ₹X", plus "+ ब्याज ₹Y" when interest is on.
- List of that person's entries and repayments.
- One button: **"पैसे मिले"** (उधार दिया) or **"पैसे दिए"** (उधार लिया). Amount and date only.
- The app applies it **oldest loan first, interest before principal**. The trader never picks a loan.
- Overpayment is **blocked** with a message ("बाकी सिर्फ ₹X है"). It no longer becomes a hidden general jama.
- Edit and void stay on each repayment row.

### 4.4 Kisaan khata

- Top: two balance cards side by side, **पर्चियों का बाकी** (slips) and **उधार बाकी** (udhaar). Never added together.
- Pager **दिया / लिया**: udhaar only, with the repayment button.
- Below: one ledger in date order. Every row carries a tag, either **"पर्ची #..."** or **"उधार"**, never both.

## 5. Data model direction

- Two balances per party: slip balance and udhaar balance, each with its own query.
- Every udhaar cash row carries a real link to its loan (loan id column), not text in remarks. The Hindi remark matching is removed from the balance query.
- A loan is created for loans received too (today they are plain cash rows), so they can hold interest and repayments.
- Interest is a per-loan option (rate null or 0 means off). Balance shows principal with interest as a separate line.
- Receiver note is a text column on the loan row.
- Slip-linked rows keep `sourceEntryType` and `sourceEntryId`.
- Test data only, so there is **no migration**. The local database is reset. The backend schema and sync payloads need the same change (see section 7).

## 6. Out of scope

- Grain udhaar. The model supports it today and the screen already excludes it. It stays excluded.
- Aging tags, the pending-collection list and WhatsApp statements from "Udhari Recovery v1". They can sit on top of this later.
- A saved list of past receivers.

## 7. Open items for the plan

- Backend schema and sync wire format for the loan link, receiver note and optional interest. Backend is not deployed without Vivek's approval.
- Exact split of work and the order of tasks (belongs in the implementation plan).
- Dark-mode and 390dp checks for the new screens (NGDL, section 5.7 of the design system).
- Tests: a regression test that a slip payment never changes the udhaar balance, and that a loan cut changes both sides by the same amount.
