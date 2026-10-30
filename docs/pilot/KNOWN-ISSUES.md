# MandiSamiti pilot known issues

Applies to the 2026-10-31 debug pilot build from commit `971f49d`.

## Pause the pilot

Pause before entering more real data when any of these occurs:

- A displayed money total is wrong.
- An entry disappears after save, restart, upgrade, or sync.
- A synced entry appears more than once.
- The amber pending-sync count stays above zero after the phone has a working internet connection.
- An upgrade fails or the app cannot open the existing shop ledger.

Keep the phone unchanged, take screenshots, and note the last action. Do not clear data or reinstall until the local rows have been inspected.

## Known risks before D1

| Issue | What may happen | Pilot action |
| --- | --- | --- |
| D1 rehearsal is pending | The exact exported APK has not completed its clean-install, upgrade, offline-sync, reboot, and bilingual rehearsal on the Redmi. | Do not install at the shop until D1 reports PASS. |
| End-to-end sync round trip is pending | Repository round-trip tests pass, but the rebuilt staging service has not yet received and returned the full TEST flow from this APK. | D1 and the shop TEST flow must confirm zero pending entries and no duplicates. |
| Weekly Google Drive copy is not configured | Nightly VPS dumps run and keep 14 days, but there is no off-VPS weekly copy yet. | Record as an infrastructure risk. The nightly backup and restore test already pass. |
| Staging uses plain HTTP | Pilot sync traffic is not protected by TLS on the current staging endpoint. | Use only the private TEST and shop pilot flow. Move staging behind HTTPS before wider use. |
| Login has no phone allow-list | Anyone who knows the private static OTP and a phone number can attempt to log in. | Keep the OTP private. Rotate it if it is exposed. |
| Static OTP has a development fallback | `config.py` contains a fallback code if `OTP_STATIC_CODE` is missing. Staging currently sets a private value. | Every deployed environment must set `OTP_STATIC_CODE`; never rely on the fallback. |
| Old secrets remain in Git history | The exposed staging credentials were rotated, but the old values remain in the public history. | Treat every old value as burned. Never reuse it. |
| Duplicate phone migration | Schema version 7 adds a unique index on shop and phone. An older install with two parties using the same phone can fail during upgrade. | D1 must run the no-clear-data upgrade. Repair duplicate phones before upgrading any affected shop. |
| Backend migration comparison is shallow | The baseline test compares table and column names, not every SQL type or index. | Avoid backend schema changes during the pilot unless a numbered migration and focused review are added. |

## Expected pilot limitations

- The APK is debug-signed. A later release-signed APK may require uninstalling the debug build, which clears local data. Sync must reach zero before that switch.
- `versionCode` is still `1`. Identify this pilot artifact by its commit and SHA-256, not by the Android version number alone.
- The app shows an amber sync count only while local entries are waiting to upload. There is no detailed sync-history screen.
- Sign out wipes local rows after a successful sync. The app blocks sign-out while local changes remain pending.
- A re-login banner can appear after the session expires. Logging in again preserves local data.
- Legacy repayments without a principal and interest split count in full against the balance.
- Existing compiler and backend deprecation warnings remain cleanup work after the pilot.

## Verified before D1

- Client schema is frozen at SQLDelight version 7 with an additive upgrade test.
- Party balance uses repayment principal only. Interest no longer reduces the principal balance.
- The daily page shows repayment principal and interest as separate lines.
- JVM tests and debug assembly pass.
- Backend tests pass: 102 tests, 0 failures.
- Staging uses Alembic, the reset and smoke test passed, and a nightly dump was restored successfully.
