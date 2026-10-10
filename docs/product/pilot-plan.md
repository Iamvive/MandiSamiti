# Pilot Plan: Running, Ending Access, and User Data

← [Docs index](../MANDISAMITI-MASTER-SPEC.md) · Cost: [pilot cost](pilot-cost.md) · Status: [feature status](feature-status.md)

Drafted 2026-10-10. **Status: proposal.** Decisions marked *Open* need Vivek's call.

Scope: 21 shops, 1 month, at most 50 bills/shop/day. Free for pilot shops. Vivek and his father onboard every shop in person.

## 1. Principles
1. **The books belong to the shop, not to us.** Never wipe a shop's phone remotely, and never delete its data before it has had a way to export it. Indian traders must keep books of account for years (income-tax and GST rules; general knowledge, not legal advice).
2. **Ending the pilot ends writing, not reading.** After the pilot the app turns read-only. The shop can still see and export everything.
3. **Say it up front.** Each shop agrees to the dates, the end-of-pilot rules and the data policy before the first entry.

## 2. Before go-live (gates)
All must be true before the first pilot shop enters real data.

| Gate | Why |
| :--- | :--- |
| Static OTP off; WhatsApp OTP live | With static OTP anyone can log in as any phone, or lock it out for 24h |
| Old `SECRET_KEY` and DB password rotated, `APP_ENV=prod` | Both are in git history |
| Prod deployed behind HTTPS at `mandi-api.appworx.co.in` | No plain HTTP for shop data |
| Daily backups, off-site `pg_dump`, **one restore drill done** | The server is the book of record |
| Uptime ping and error alert to Telegram | The free minimum of the [observability goal](feature-status.md) |
| In-app user manual (मदद) | Shops get help without calling us |
| **Pilot access control** (§4) and **export** (§5) built and tested | Without them the pilot can't end cleanly |
| Pilot agreement accepted by each shop (§6) | Consent for dates and data use |

## 3. Running the pilot
**Timeline** (D0 = first day of the paid-server month):

| When | What |
| :--- | :--- |
| D−7 → D0 | Dry run in Vivek's father's shop only. Fix what breaks |
| Week 1 | 5 friendly shops |
| Week 2 | +8 shops (13 total) |
| Week 3 | All 21 |
| Week 4 | Steady state; collect feedback |
| End − 7 days | In-app banner and WhatsApp message: "pilot ends on <date>; choose continue or leave" |
| End | Access ends for shops that chose to leave (§4) |

**Distribution:** Play Console internal testing (up to 100 testers by Gmail). Removing a tester only stops updates; it does **not** remove the installed app. That's why access is controlled on the server and in the app (§4).

**Support:** one WhatsApp group for all pilot shops, plus a daily 10-minute check by Vivek.

**Daily check** (SQL until the observability goal is built):
- active shops today
- bills per shop
- shops with unsynced entries older than 24h
- server errors

**Success metrics:**
- shops active ≥5 days/week
- median bills per shop per day
- sync failures
- support questions per shop
- day-closing usage
- shops willing to pay after the pilot (and how much)

**Feedback:** a short visit or call with each shop at the end of week 2 and at pilot end.

## 4. Ending access after the pilot
**Mechanism** (to build; not in code yet):
1. **Server:** each shop gets an `access_until` date and a `plan` (`PILOT`, `PAID`, `ENDED`) on `shop_profiles`. Both come back in login, refresh and sync responses. After `access_until`, sync **push** is refused (`403 ACCESS_ENDED`). Login, refresh and sync **pull** keep working, so the phone can still fetch and export everything.
2. **App:** stores `access_until` locally, so the gate also works offline.
   - From 7 days before the end: a countdown banner.
   - After the end: **read-only mode.** New deals, cash entries and edits are disabled. Viewing, search, khata statements and export still work.
   - Unsynced entries made before the end still sync.
3. **Admin:** a CLI script to extend, convert to paid, or end a shop's access, with an audit line for every change.

**Why not `is_active = false`:** it blocks login and refresh, so the shop could no longer pull its data to export it. And because the app is offline-first, the phone would keep accepting entries that never sync. Keep `is_active` for abuse and lost phones only.

**Known limit:** a shop could wind the phone clock back to dodge the offline gate. Acceptable for a free pilot: they still can't sync, and the next online check uses server time.

## 5. User data: retained or not
| Shop's choice at pilot end | What happens to its data |
| :--- | :--- |
| **Continue** (paid or extended) | Kept as is. Same account, same books, no migration |
| **Leave** | Read-only for **30 days**. Then server data is deleted at **90 days** after pilot end (*Open:* 90 days, or shorter) |
| **Leave and asks for deletion now** | Delete from the server within 7 days after an export is offered. Confirm on WhatsApp |
| **No answer** | Treated as *Leave*. Two WhatsApp reminders before deletion |

**On the phone:** the app never wipes itself because the pilot ended. Local data stays until the owner signs out or uninstalls. Sign-out stays blocked while entries are unsynced.

**Export:** khata per party and galla history as PDF and CSV, shared through WhatsApp or saved to the phone. Built on the phone, so it costs nothing on the server.

**What we keep after deletion:** only anonymous aggregate counts for pilot learnings (e.g. "21 shops, median 32 bills/day"). No party names, phone numbers or amounts per shop.

**Farmers' and buyers' data:** party names and phone numbers are personal data of people who are not our users. Collect only what the ledger needs, use it only for that shop's books, and delete it with the shop's data. India's DPDP Act 2023 applies; check the current rules before charging money (not legal advice).

## 6. Pilot agreement (one page, Hindi, accepted on WhatsApp or signed)
- **Dates:** pilot start and end; free during the pilot.
- **After the pilot:** continue at a price to be agreed, or leave. If they leave, the app turns read-only for 30 days and they can export everything.
- **Their data:** the data is theirs. We use it only to run their books and to improve the app. We never sell it or share it.
- **Deletion:** deleted from our server 90 days after they leave, or sooner on request.
- **Backups:** we back up daily, but they should also export at day-close each week.

## 7. Engineering work this needs
Tracked as rows in [feature status](feature-status.md):
- **Pilot access control:** `access_until` + `plan` on the server, push refusal, app read-only mode, countdown banner, admin CLI. Must have tests, including "pull still works after end".
- **Khata & galla export:** PDF/CSV on the phone, shared through WhatsApp.
- **Shop deletion script:** deletes one shop's rows across all tables, writes an audit log line, and is run only after an export is offered.

## Open decisions (Vivek)
- [ ] Pilot dates, and whether a shop's month starts when that shop joins or for all shops together.
- [ ] Grace period (30 days proposed) and deletion delay (90 days proposed).
- [ ] Price after the pilot, if any. Infra alone is ~₹89/shop/month ([pilot cost](pilot-cost.md)).
- [ ] Whether pilot shops that continue get a discount.
