# Known issues for the pilot (2026-10-31)

## Pauses the pilot (stop and screenshot)
- Any wrong total on the day page or party balance.
- App crash or a row that disappears after reopen.
- Sync pending count stuck above 0 for more than a few minutes with internet on.

## May show at the shop
- Debug build, larger than a release APK. Talks to staging over plain HTTP.
- Re-login banner can appear if the session expires; log in again, data is kept.
- Old repayments without a principal/interest split count in full against the balance.

## Security gaps accepted for the pilot
- No phone allow-list on login: anyone with the static code and a phone number can log in. Keep the code private.
- Static OTP code length: client UI (`RegisterViewModel.kt`) and backend Pydantic schema (`VerifyOtpRequest`) enforce exactly 6 digits. Staging env code must be 6 digits, not 8.
- `config.py` defaults `OTP_STATIC_CODE` to `123456` if the env var is unset. Staging sets it; any new environment must too.
- Old secrets stay in public git history (rotated, history not rewritten).

## Technical debt
- Pre-pilot upgrade crash (`duplicate column name: slip_no`): Upgrading over a pre-pilot build that already had `slip_no` added in SQLite fails in `6.sqm`. Clean install or `adb shell pm clear com.appwork.mandisamiti` is required for v7.
- Unique index `idx_party_shop_phone`: the client migration fails on an old install that has two parties with the same phone.
- B1 migration test compares table and column names only, not types or indexes.
- Off-VPS backup (weekly Drive copy) not set up yet. Nightly on-VPS backup runs at 02:30, kept 14 days.
- Sync round trip with a TEST party has not been run on a device yet (D1 covers it).
