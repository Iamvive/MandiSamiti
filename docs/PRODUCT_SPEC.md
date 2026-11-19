# MandiSamiti product spec

Living spec.
Confirmed facts stay as written.
Open sections stay marked until a later session fills them.

## 1. Users & Problem

Confirmed by Vivek on 2026-11-19.

### Who it is for

The target user is every aadhati.
An aadhati is a commission agent in a grain mandi.

On the premium plan, the munim also uses the app.
A munim is the shop accountant.

A shop has more than one staff member.
They share one shop account.

### Pilot shop

The pilot shop is Vivek's family shop.
His father and two brothers run it.

Any one of them may pay cash.
The person who pays must create an entry.

### Crops and a busy day

The shop deals in wheat (gehu), bajra, and maize (makka).
It also deals in many other grains.

On a busy day the shop may see up to about 50 farmers (kisaan).
On a busy day the shop may see up to about 50 deals.

### Who does not use the app

Farmers do not use the app.
Buyers do not use the app.
They only receive bills.

### The biggest pain

Wrong totals and missing entries make the cash and the bill disagree.
The daily cash drawer (galla) does not match the bahi (the ledger book).

Today the family starts the sum again from scratch.
They ask the person who forgot to write an entry.
They can spend days on one gap.

Example.
The shop bought Diwali sweets several times.
One entry of Rs 2,500 was missed.
That one miss caused days of worry.

### What they need because of this

Any person in the shop must be able to make an entry fast.
The app must show who entered what.
The daily galla tally must point to the mismatch.
The app must make zero calculation errors.
The app must generate every kind of bill.

### How they read and type

The brothers type English or Hinglish.
They do not type in Hindi script.

The father is less comfortable with phones.
He will still use the app himself.
One example is checking totals.

The app needs big, simple buttons.
The app needs Hinglish input.

### What success looks like

The app is easy to use.
It makes zero calculation mistakes.
It generates all kinds of bills.
It takes this daily headache off the aadhati.

### What the code already has (inferred)

This note comes from the README.
It was checked against the code on 2026-11-19.
It is not an owner-confirmed rule.
Money rules stay open until a later session.

A deal has two stages.
Stage 1 saves the weighment as pending settlement.
Stage 2 settles the auction rate and the buyer.
See `DealStatus` in `core-domain/.../domain/model/Deal.kt`.

Commission is a shop setting, with its own default for each side.
Farmer commission defaults to 0.70%.
Buyer commission defaults to 1.50%.
The README line "commission 2%" does not match these defaults.
Mandi shulk (the cess) on a bill of supply is 1.50%, which matches the README.
Hammali is in the code as slip labour.
The labour default is Rs 10 per bag.
Hammali is also an expense category.

Rural interest is simple interest.
A new loan starts at 2% per month.
Interest uses days divided by 30.
A payment clears interest first, then principal.

The app has a daily cash register for the galla.
Bills can be sent on WhatsApp.
Voice uses Hindi (`hi-IN`).

## 2. Core Flows

TBD, session pending.

## 3. Money & Data Rules

TBD, session pending.

## 4. Built vs Planned (gaps)

TBD, session pending.

## 5. Priorities

TBD, session pending.

## Later ideas

- Suggest the crop or item the shop enters most often.

## Change log

- 2026-11-19: session 1. Users and the problem, confirmed by Vivek. Sections 2 to 5 left open.
