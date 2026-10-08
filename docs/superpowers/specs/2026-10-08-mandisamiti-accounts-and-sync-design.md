# MandiSamiti — Accounts & Cloud Sync Design

**Date:** 2026-10-08 · **Status:** Approved in brainstorming (Vivek) · **Part of:** Sell-ready core, piece 1 of 4. The others are charge settings and the Hindi/English switch, each with its own spec.

## Goal
A shop can sign up, log in on any phone, and keep working with no network. Every entry reaches the server in the background when signal returns. A phone never shows another shop's books.

## Decisions (from brainstorming)
| Topic | Decision |
|---|---|
| OTP delivery | **Fixed code for now** (`OTP_STATIC_CODE`), behind a swappable `OtpSender`. WhatsApp OTP is the cheapest real option (≈ ₹0.115/OTP) and is the likely next sender; an SMS fallback can follow after DLT registration. |
| MPIN | Asked only at login (new phone, or after logout). **No app lock.** The MPIN is stored and checked on the server only, never on the phone. |
| Logout | Sync first. If anything is unsent, block logout with a message. When fully synced, wipe the local database and tokens. |
| Existing test data on the Redmi (`shop_default`) | **Wiped** on the first signup on that phone. |
| Network model | Local-first: saving an entry never waits for the network. The server holds the official books. |

## 1. Server (FastAPI, `backend/`)

### 1.1 OTP
- `OtpSender` protocol with `send(phone, code)`. The only implementation is `StaticOtpSender`. It sends nothing; the code is `settings.OTP_STATIC_CODE`.
- Settings: `OTP_STATIC_ENABLED` (default true) and `OTP_STATIC_CODE` (default `"123456"`). When static mode is off and no other sender is configured, `/auth/otp/send` returns 503.
- `GET /health` includes `"otp_mode": "static"` so the mode is visible.
- OTP records move from the in-process dict to **Redis**, keyed `otp:{phone}` with a 5-minute TTL. Keep the existing limits: max 3 sends per 5 minutes per phone, and lockout after 5 wrong attempts.

### 1.2 Auth endpoints (`/api/v1/auth`)
| Endpoint | Body | Response |
|---|---|---|
| `POST /otp/send` | `{phone}` | `{sent: true, cooldown_s}` |
| `POST /otp/verify` | `{phone, otp}` | `{status: "NEW", signup_pass}` or `{status: "EXISTING", login_pass}` |
| `POST /signup` | `{signup_pass, shop_name, owner_name, mandi_name, mpin}` | `{access_token, refresh_token, shop}` |
| `POST /login` | `{login_pass, mpin}` | `{access_token, refresh_token, shop}` |
| `POST /refresh` | `{refresh_token}` | `{access_token, refresh_token}` (rotated) |
| `POST /logout` | `{refresh_token}` | `204`; the refresh token is revoked |

- `signup_pass` and `login_pass` are short-lived (10 minutes), single-use JWTs bound to the phone number.
- `/signup` creates the shop with a **server-generated UUID** `shop_id`, then creates the user with the MPIN bcrypt-hashed.
- `/login`: after 5 wrong MPINs, the `login_pass` is burned and a new OTP is required.
- Access token: 1 hour. Refresh token: 90 days, rotated on every use. Refresh tokens are tracked in a Postgres `refresh_token` table (jti, user_id, expires_at, revoked_at). A revoked or already-rotated token is rejected.
- The existing `/mpin/setup` and `/mpin/verify` are replaced by `/signup` and `/login`, then removed.

### 1.3 Sync (`/api/v1/sync`)
- **Server change counter:** a Postgres sequence `server_seq`. Every accepted revision row and every party upsert gets the next value.
- `POST /push`:
  - Body: `{revisions: [...], parties: [...]}`, at most 100 revisions per request, sent gzip-compressed.
  - Each revision is identified by `(entry_id, revision)`. A duplicate with the same content returns `ALREADY_HAVE`. A duplicate key with different content stores both versions and flags the entry `CONFLICT` for the owner. A new revision is applied to the entry's current row and stored.
  - Parties: latest `updated_at` wins.
  - Response: per item `ACCEPTED | ALREADY_HAVE | CONFLICT`, plus the assigned `server_seq`.
- `GET /pull?after_seq=N&limit=500` returns revisions and parties with `server_seq > N`, plus `next_seq` and `has_more`. The timestamp parameter `since` is removed.
- Every query is scoped to `shop_id` from the access token. Keep the existing isolation tests.

### 1.4 HTTPS
- Serve the API at `mandi-api.appworx.co.in` behind the existing Caddy edge proxy on `appworx-core-vps`.
- Close direct public access to ports 8050/8060.
- Vivek adds a DNS A record for the subdomain if a wildcard record doesn't already cover it.

## 2. App (KMP, Android first)

### 2.1 Network client
- Add Ktor (`ktor-client-core`, `content-negotiation`, kotlinx JSON). The engine is OkHttp on Android and Darwin on iOS.
- `AuthApi` covers the six auth calls. `KtorMandiSyncApiClient` implements the existing `MandiSyncApiClient`.
- The base URL comes from build config: `https://mandi-api.appworx.co.in` for release, configurable for debug.
- When the server says the access token has expired (HTTP 401), the client refreshes once and retries. If the refresh fails, the session becomes `NEEDS_LOGIN`.

### 2.2 Session
- `SessionStore` (an expect/actual class) keeps the access and refresh tokens. Android uses EncryptedSharedPreferences (Android Keystore). iOS uses the Keychain, later.
- The shop profile from the server is saved in `shopProfileEntity` under the server `shop_id`.
- **The hard-coded `shop_default` is removed everywhere:**
  - `App.kt`
  - `RegisterViewModel`
  - the `comm_wheat` fallback in `DealEntryViewModel`
  All of these read the shop id from the session.
- `NEEDS_LOGIN` never deletes unsynced data. The user logs in again with the same phone number, and pending entries still go up.

### 2.3 Screens and flow
- Reuse the `RegisterScreen` steps: phone → OTP → (NEW) shop details + set MPIN, or (EXISTING) enter MPIN.
- Remove the fake `verifyOtp`; the OTP step now calls the server.
- **App start:**
  - Session present → Home. This needs no network.
  - No session → Login.
- **First signup on a phone that has local data:** wipe the local database, then save the new shop.
- **Login on a new phone:** run the first download with a progress line: "खाता लोड हो रहा है… X / Y" ("loading the khata… X / Y"). It resumes from the last saved `next_seq` if interrupted.
- **Logout (Settings):**
  1. Run sync.
  2. If the pending count is above 0, show "N प्रविष्टियाँ अभी सर्वर पर नहीं गईं — नेटवर्क मिलने पर लॉगआउट करें" ("N entries haven't reached the server yet; log out when you have network") and stay logged in.
  3. Otherwise call `/logout`, wipe the database and the `SessionStore`, and go to the Login screen.

### 2.4 Background sync
- **Outbox:** pending `entryRevisionEntity` rows (`sync_status = 0`) and pending parties. They are acknowledged one revision at a time with `(entry_id, revision)` (already in place since 1A F5).
- **Pull cursor:** `last_server_seq`, stored locally.
- Android WorkManager runs sync in three ways:
  - a unique one-time job, enqueued after every local save, with a network-connected constraint
  - a periodic job every 15 minutes
  - exponential backoff on failure
- Each sync run does push (in batches) then pull (in pages). Pulled rows are applied in one transaction per page.
- The existing sync ticks and pending badge now reflect real server acks.
- iOS background sync is out of scope. iOS syncs only while the app is open.

## 3. Error handling
| Case | Behaviour |
|---|---|
| No network | Entries save locally, the tick stays single, and the pending count rises. No error dialogs. |
| The connection drops mid-push | The batch is retried and the server's duplicate check skips what already arrived. |
| Refresh token expired or revoked | `NEEDS_LOGIN` banner: "दोबारा लॉगिन करें" ("log in again"). Data is kept. |
| Wrong OTP or MPIN | An inline error in Hindi with the attempts left. A lockout message gives the wait time. |
| Server 5xx | Backoff retry. A banner shows only after 24 hours without a successful sync. |
| Same entry and revision with different content | Kept and flagged `CONFLICT`. Showing it to the owner comes in a later spec. |

## 4. Testing
- **Server (pytest):**
  - signup and login happy paths
  - OTP limits and lockout
  - MPIN lockout burning the pass
  - refresh rotation and replay rejection
  - a pushed duplicate returns `ALREADY_HAVE`
  - a conflicting duplicate is flagged
  - `server_seq` paging
  - shop isolation
- **App (JVM tests):**
  - Ktor `MockEngine` for every API call
  - 401 → refresh → retry
  - refresh failure → `NEEDS_LOGIN` with data kept
  - push resumes after a mid-batch failure
  - pull resumes from the cursor
  - logout blocked while entries are pending, and wipes the data when there are none
  - first-signup wipe
  - no `shop_default` left anywhere (grep test)
- **Device (Redmi Note 7):**
  - signup
  - logout blocked while offline
  - login on a second phone or after a reinstall downloads the khata
  - airplane-mode rush-hour test: entries made offline all reach the server after reconnect, and totals match

## 5. Out of scope
- Real WhatsApp or SMS OTP sender.
- App lock and biometric unlock.
- Showing conflicts to the owner.
- Background sync on iOS.
- Staff roles and multi-device per shop (premium, Phase 4).
- Charge settings and the Hindi/English switch (separate specs).

## 6. Delivery
Two implementation plans, in order:
1. **Accounts:** server §1.1, §1.2 and §1.4; app §2.1, §2.2 and §2.3 (without the first download).
2. **Sync:** server §1.3; app §2.4, the first download and the logout sync.
