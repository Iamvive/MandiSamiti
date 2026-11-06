# Known issues for the pilot (2026-10-31)

## Pauses the pilot (stop and screenshot)
- Any wrong total on the day page or party balance.
- App crash or a row that disappears after reopen.
- Sync pending count stuck above 0 for more than a few minutes with internet on.

## May show at the shop
- Debug build, larger than a release APK. Talks to staging over plain HTTP.
- Re-login banner can appear if the session expires; log in again, data is kept.
- Old repayments without a principal/interest split count in full against the balance.

- Buy and sell math now follows the shop sheets (gross cut to the rupee, billed-bag labour, final rounded to Rs 10, Cash Memo charges arat and tulak). Advance comes off after rounding and ties at Rs 5 round up: both are assumptions, the sheets do not show them.
- Buyer GSTIN and licence fields are in this build. The party import tool exists but has not run on real data; staging needs Alembic `0002_party_tax_fields` deployed first.

## Security gaps accepted for the pilot
- No phone allow-list on login: anyone with the static code and a phone number can log in. Keep the code private.
- OTP is gone from the app and from the auth routes (2026-11-02). `OTP_STATIC_ENABLED` / `OTP_STATIC_CODE` in `backend/app/config.py` and `core/otp_sender.py` are dead code to delete.
- Login is phone + 4-digit MPIN with no second factor. Anyone who knows a registered phone can try MPINs (5 failures lock it for 24h, so they can also lock the owner out). Decide on a real verification step before real shop data.
- Old secrets stay in public git history (rotated, history not rewritten).

## Technical debt
- Pre-pilot upgrade crash (`duplicate column name: slip_no`): Upgrading over a pre-pilot build that already had `slip_no` added in SQLite fails in `6.sqm`. Clean install or `adb shell pm clear com.appwork.mandisamiti` is required for v7.
- Unique index `idx_party_shop_phone`: the client migration fails on an old install that has two parties with the same phone.
- B1 migration test compares table and column names only, not types or indexes.
- Off-VPS backup (weekly Drive copy) not set up yet. Nightly on-VPS backup runs at 02:30, kept 14 days.
- Sync round trip with a TEST party has not been run on a device yet (D1 covers it).
