# Galla-Rokad cash popup: kisan picker and name-only entries

Date: 2026-11-24
Status: design chosen by Vivek (option A), not built yet
Branch: feat/galla-rokad-editable

## Problem

The नकद जमा / नकद निकासी popup (`DailyCashRegisterScreen.kt`, the Direct Cash Dialog, about lines 592-732) shows a 120dp scroll list of every party. It has no search and no way to add a kisan. There is also no way to write a walk-in name without creating a khata.

Bug found while reading: when no khata is picked, the Save button uses `uiState.availableParties.firstOrNull()?.id`. A name-less entry is then booked into whichever party happens to be first. That party's khata gets money that does not belong to it.

## Chosen design (A: one smart name box)

```
┌ नकद जमा प्रविष्टि ─────────────┐
│ नाम या मोबाइल                  │
│ [ रमे|                     ]   │
│  (o) रमेश यादव · सिरसा         │  up to 5 matches
│  (o) रमेश चंद · हिसार          │
│  (+) नया किसान खाता: "रमे"     │
│ i सिर्फ नाम सहेजेगा, खाते में  │  live status line
│   नहीं जाएगा                   │
│ राशि ₹ [ 5000 ]                │
│ [नकद] [UPI] [RTGS] [NEFT] [चेक]│
│ नोट  [        ]                │
│           रद्द करें [सुरक्षित]  │
└────────────────────────────────┘
```

The field can be in one of three states, and the status line always says which:

| State | How you get there | Status line | What Save writes |
|---|---|---|---|
| Khata linked | Tap a match | "खाते में दर्ज होगा" + chip with x to unlink | `partyId = party.id` |
| New kisan | Tap "+ नया किसान खाता" | Phone (optional) and village fields appear under the name | Create FARMER party, then `partyId = newParty.id` |
| Name only | Type and save without tapping | "सिर्फ नाम सहेजेगा, खाते में नहीं जाएगा" | `partyId = ""`, name goes in remarks |
| Empty | Nothing typed | none | Same as today's no-name entry, `partyId = ""` (no first-party fallback) |

Typing again after linking a khata clears the link, so a changed name can never stay attached to the old khata.

## Data rules

- Name-only entries: `partyId = ""`. The name goes in `remarks`. If a note is also given, `remarks = "<name> · <note>"`. The bahi row already shows remarks when there is no party (`BahiTransactionItem`, line 948), so no display change is needed.
- Do not put the name in a `[TAG:...]`, because `RemarksFormatter.cleanForDisplay` strips tags.
- Server: `transactions.party_id` is nullable with a foreign key to `parties`. Postgres rejects `""`. The sync push must send `null` when `partyId` is blank. Check the client DTO mapping first; if it already does this, no change.
- Local SQLite does not enforce foreign keys (no `PRAGMA foreign_keys`), and `party_id TEXT NOT NULL` accepts `""`.
- New kisan creation reuses `LendPartyResolver.partyToSave(name, phone, village, shopId, existing = null, now)` and the same party repository insert that Lend uses. Local write first, sync later.

## Reuse

- Matching: `BuyPartyResolver.matches(parties, phone, name)`. It matches by phone if 3+ digits are typed, else by name, and returns the top 5.
- Party creation: `LendPartyResolver.partyToSave`.
- Tokens: `MandiAmberLight` / `MandiAmberDark` for the selected chip, as today. No new colours.

## Out of scope

- The three `firstOrNull() ?: "GALLA_DRAWER"` fallbacks in `setOrAdjustOpeningBalance`, cash-to-home and the other drawer entries have the same wrong-khata bug. Flag them and fix them in a separate small PR.
- Editing the party on an existing entry.

## Verification

- Unit test: Save with no party picked never uses `availableParties.first()` (regression for the bug).
- Unit test: name-only entry writes `partyId = ""` and `remarks = "name · note"`.
- Unit test: the sync mapping sends `party_id = null` for a blank id.
- Redmi d93c743 on staging, light and dark: pick khata, add kisan, name only, empty name. Check each row in the bahi and in the khata ledger.
