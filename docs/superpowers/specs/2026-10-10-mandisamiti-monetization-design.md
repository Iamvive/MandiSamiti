# MandiSamiti — Monetization Design: बही सुरक्षा (Basic) + उधारी वसूली (Pro)

**Date:** 2026-10-10 · **Status:** approved in brainstorming, awaiting spec review · **Owner:** Vivek
**Related:** [pilot plan](../../product/pilot-plan.md) · [pilot cost](../../product/pilot-cost.md) · [Phase 4 staff roles](2026-10-10-mandisamiti-phase4-staff-roles-design.md) · [feature status](../../product/feature-status.md)

## 1. Goal and constraints
- **Goal:** ₹12 lakh/year profit from the Mathura mandi (≈400 Aadhati shops), and profitable from the first paid month.
- **Target:** ≈200 paying shops at an average of ≈₹500/month, with roughly half on Pro.
- **Revenue stays under ₹20L/year,** so no GST registration is needed at this target.
- **Constraints from the field** (Vivek and his father):
  - Aadhatis are hard to get money from.
  - There is no fixed payment rhythm, so monthly, seasonal and yearly payments must all be accepted.
  - They are comfortable with the app sending reminders to buyers.

## 2. The two pains we sell against
| Pain | Field evidence | Plan |
| :--- | :--- | :--- |
| **Udhari stuck with buyers** | Typically ₹5–20 lakh outstanding in season. At the ~1.5%/month rate Aadhatis themselves charge, that's ₹7,500–30,000/month of money lost to waiting. Getting ₹10L back one week sooner is worth ≈₹3,500/month | **Pro "उधारी वसूली"** |
| **Losing records** (fire, flood, theft, lost phone) | A paper bahi lost is the whole business history lost | **Basic "बही सुरक्षा"** |

## 3. Plans and prices (prices include everything)
| | Basic "बही सुरक्षा" | Pro "उधारी वसूली" |
| :--- | :--- | :--- |
| Monthly | ₹299 | ₹699 |
| Season (6 months) | ₹1,599 | ₹3,799 |
| Yearly | ₹2,999 | ₹6,999 |
| Includes | Khata, galla, day close, WhatsApp share, user manual, cloud backup + restore on a new phone, बही सुरक्षा card, monthly bahi PDF to own WhatsApp, export | Everything in Basic, plus aging tags, बाकी वसूली list, WhatsApp statements with UPI ID + QR, ROI meter, staff roles (Phase 4), priority support |

**Server values:** reuse `plan_tier` from the Phase 4 spec. `STANDARD` = Basic, `PREMIUM` = Pro. Pro sets `is_multi_device_enabled = true`.

**Guarantee (Pro):** if the amount that came in after reminders during the first 30 days is less than the fee paid, the shop gets a full refund on request.

## 4. Selling and collecting
- **Pilot = first paid month.** 21 shops pay the real price. Each shop's 30 days start on its own start day, and the guarantee applies.
- **Collection is in person:** UPI to Vivek's account, or cash with a receipt. No payment gateway in v1, so no ~2% fee. The admin CLI records the payment and sets `access_until`.
- **Renewal:** in-app banner and WhatsApp message 7 days before `access_until`. Unrenewed shops go **read-only**; they are never locked out of their books ([pilot plan §4](../../product/pilot-plan.md#4-ending-access-after-the-pilot)).
- **Referral:** 1 free month to a shop for each new paying Aadhati it brings in. Recorded by the admin CLI.
- **Sales demo for Basic:** log into a second phone at the counter and show every entry restored.

## 5. Parts (each gets its own implementation plan; built in this order)

### Part 1 — Plans and access (paywall)
- **Server:**
  - Add `access_until` (BigInteger ms, nullable) to `shop_profiles`. `plan_tier` and `is_multi_device_enabled` come from the Phase 4 spec.
  - Login, refresh and sync responses return `plan_tier` and `access_until`.
- **Server rules once `now > access_until`:**
  - Sync **push** returns `403 {"code":"ACCESS_ENDED"}`.
  - Login, refresh and sync **pull** keep working.
  - Pushes of revisions whose `created_at ≤ access_until` are still accepted, so entries made before the end and synced late are not lost.
- **App:**
  - Caches `plan_tier` and `access_until` in the session, so the gates work offline.
  - A countdown banner shows from 7 days before the end.
  - After the end: **read-only mode.** New deal, cash entry, edit and void are disabled. View, search, statements and export still work.
  - Pro-only screens show a "Pro में उपलब्ध" card with the guarantee text.
- **Admin CLI** (`backend/scripts/shop_admin.py`):
  - commands: `list`, `mark-paid --shop --plan --period monthly|season|yearly`, `extend --days`, `end`, `referral --from --to`
  - every change appends an audit line (who, when, old → new)
- `is_active` is not used for billing. It stays for abuse and lost phones only.

### Part 2 — Record safety and export
- **Settings → बही सुरक्षा card:**
  - "आखिरी बैकअप: <relative time>" (last backup)
  - count of entries on the cloud
  - error state if any entry has been unsynced for more than 24h
- **Export:** party khata statement and galla history as PDF and CSV, built on the phone and shared through the Android share sheet. Available in every plan state, including read-only.
- **Monthly bahi PDF:** on the first app open of each month, a prompt to send last month's full PDF to the owner's own WhatsApp. One tap to open WhatsApp; no auto-send.

### Part 3 — Udhari recovery v1 (Pro)
- **Aging:** for each BUYER party, match payments to the oldest deals first and skip voided entries. Tag the buyer **7+ / 15+ / 30+ दिन** by the age of the oldest unpaid amount.
- **बाकी वसूली list:** buyers with a balance due, sorted by amount × days overdue. Shows the total outstanding at the top.
- **Statement message:**
  - polite Hindi text with open deals (date, commodity, amount), the total due, the shop's UPI ID and the amount
  - a QR image built on the phone (`upi://pay?pa=…&pn=…&am=…&tn=…`)
  - sent from the **Aadhati's own WhatsApp:** "सबको भेजें" (send to all) steps through buyers with one tap each
- **Reminder log:** each send records `{party_id, amount_due, sent_at}` locally and syncs it, because the ROI meter needs it.
- **Shop UPI ID:** a new field on the shop profile, set in Settings. It's required before the first statement.
- **Out of v1:** automatic sending from a MandiSamiti WhatsApp Business number (≈₹0.12/message, ≈₹15/shop/month). Reconsider only if shops ask.

### Part 4 — ROI meter and 30-day check (can ship in pilot week 2–3)
- **Attribution rule:** a buyer payment counts as "came after reminder" if it is recorded for that buyer within **15 days** after a reminder to that buyer and is not voided. Each payment counts at most once.
- **Home card:** "रिमाइंडर के बाद आया: ₹X (इस महीने)" (came in after reminders, this month). The wording never claims the app recovered it.
- **Day-30 check (Pro):** shows "रिमाइंडर के बाद आया ₹X · फीस ₹Y". If X < Y, it tells the owner they can ask for a refund. The refund itself is handled by Vivek through the admin CLI.

## 6. Error handling
- Missing UPI ID → the statement action opens the UPI ID field first.
- WhatsApp not installed → fall back to the share sheet.
- Phone clock behind the server → the server's `access_until` check wins on the next sync. Offline, the gate uses the cached date (accepted limit, see pilot plan §4).
- `ACCESS_ENDED` on push → the app switches to read-only and keeps any unsynced pre-expiry entries queued (they will be accepted under the `created_at ≤ access_until` rule).
- Plan downgrade (Pro → Basic) → Pro screens show the upgrade card. Reminder history and data are kept.

## 7. Testing (TDD)
- **Aging:** oldest-first matching with part-payments, overpayment across deals, voided deals and voided payments; boundaries at 7/15/30 days.
- **Attribution:** a payment on day 15 counts and day 16 does not; one payment is counted once even after two reminders; voided payments are excluded.
- **Gates:** offline gate uses the cached `access_until`; push after expiry → 403; pull after expiry works; a pre-expiry revision pushed late is accepted.
- **Admin CLI:** each command updates the shop and writes an audit line.
- **Export:** PDF/CSV totals equal the khata and galla balances.
- **Real phone (light + dark, NGDL §5.7):** read-only mode, the Pro card, a real WhatsApp statement with QR, restore on a second phone, and the बही सुरक्षा card in its error state.

## 8. Pilot measures (decides whether the ₹12L plan holds)
| Measure | Goal |
| :--- | :--- |
| Shops that pay after the first month | ≥10 of 21 |
| Pro share among paying shops | ≥40% |
| Payment rhythm chosen (monthly / season / yearly) | Observe |
| Reminders sent per Pro shop per week | Observe |
| Amount that came in after reminders, per Pro shop | > fee for most shops |
| Refunds claimed under the guarantee | ≤20% of Pro shops |

## 9. Out of scope
- Payment gateway / UPI autopay.
- Automatic WhatsApp sending from a business number.
- Success-fee pricing (approaches B and C). Possible later, using the reminder log as data.
- Photographing old paper bahi pages (ties to Phase 6 camera; storage cost).
- Discount for pilot shops that continue (decide later).
