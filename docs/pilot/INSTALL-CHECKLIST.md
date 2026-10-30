# MandiSamiti pilot install checklist

Pilot date: 2026-10-31

Package: `com.appwork.mandisamiti`

Version: `2.0.0` (`versionCode 1`)

Build commit: `971f49d`

APK from the Vivek-K workspace root: `exports/mandisamiti-pilot-debug.apk`

Size: 20,890,987 bytes

SHA-256: `17536d7e39457d9a903fd5b3c20a08713fb983e4b91d3a31a7d172bc9a82e8a4`

Use this APK only after the D1 rehearsal passes. The pilot uses a debug APK installed by cable.

## Before leaving for the shop

- [ ] Charge the phone and laptop.
- [ ] Keep the APK on the laptop. From the Vivek-K workspace root, confirm its SHA-256:

  ```bash
  shasum -a 256 exports/mandisamiti-pilot-debug.apk
  ```

- [ ] Confirm Android Platform Tools are available: `adb version`.
- [ ] Read the private staging OTP directly from the approved local or VPS secret store. Do not copy it into chat, notes, screenshots, or this repository.
- [ ] Keep mobile data or shop Wi-Fi available for login and sync.
- [ ] Read `projects/MandiSamiti/docs/pilot/KNOWN-ISSUES.md`.

## Connect and install

1. Enable Developer options and USB debugging on the phone.
2. Connect the phone by cable, unlock it, and accept the USB debugging prompt.
3. Confirm that ADB sees one authorized device:

   ```bash
   adb devices
   ```

4. From the Vivek-K workspace root, install the pilot APK. Replace `<serial>` with the serial shown by `adb devices`:

   ```bash
   adb -s <serial> install -r exports/mandisamiti-pilot-debug.apk
   ```

5. Open MandiSamiti. Log in with the shop phone number, the private staging OTP, and the shop MPIN.
6. Do not enter real business data until the TEST flow below passes and its local data has been wiped.

## TEST flow at the shop

Use names beginning with `TEST`. Use small, unmistakable example values for buy, sell, and expense entries.

- [ ] Create a `TEST` farmer with a phone number that is not used by another party.
- [ ] Create a `TEST` buyer with a different phone number.
- [ ] Record one buy for the TEST farmer. Confirm the receipt has a slip number.
- [ ] Record one sell for the TEST buyer. Confirm the bill has a slip number.
- [ ] Record one money loan of Rs 10,000 for a TEST party, backdated 30 days at 2% monthly interest.
- [ ] Repay Rs 1,200. Confirm the preview shows Rs 200 interest, Rs 1,000 principal, and Rs 9,000 remaining principal.
- [ ] Record one TEST expense.
- [ ] Open the daily cash register and the day-closing view. Confirm the Left and Right totals are equal. Confirm the repayment appears as separate principal and interest lines.
- [ ] Force-close the app and reopen it. Confirm every TEST entry is still present.
- [ ] Check light and dark mode. Confirm labels remain readable and no content is clipped or scrolls horizontally.
- [ ] Wait for the amber pending-sync count in the top bar and the dashboard warning line to disappear. A missing amber count means zero entries are waiting to upload.
- [ ] Turn airplane mode on, add one more TEST entry, then turn airplane mode off. Confirm the amber count returns to zero and the entry appears only once.
- [ ] Take screenshots of any wrong total, missing row, duplicate row, crash, or sync count that does not clear.

If any money total is wrong or any entry is lost, stop the pilot. Do not enter real data.

## Wipe TEST data

Only wipe after the pending-sync count is zero.

1. Close the app.
2. Clear all local app data:

   ```bash
   adb -s <serial> shell pm clear com.appwork.mandisamiti
   ```

3. Open MandiSamiti and log in again.
4. Confirm the dashboard and account lists contain no TEST rows. If TEST rows return from the server after login, stop and clean the staging shop data before entering a real record.
5. Enter the first real record only after the empty state is confirmed. Check its total against the paper bahi before continuing.

## Pilot operating rule

- Wrong money or lost data: stop, take screenshots, and preserve the phone state for diagnosis.
- Cosmetic issue or a crash that loses no data: record it and continue.
- Do not uninstall, clear app data, sign out, or switch phones while the amber pending-sync count is visible.
- The pilot uses staging. There are no remote hot fixes while Vivek is away through 2026-11-17.
