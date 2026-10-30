# Pilot install checklist (shop, by cable)

APK: `exports/mandisamiti-pilot-debug.apk` (Vivek-K repo root). Built from MandiSamiti `55ed30b` (shop-sheet math). Server: staging, `http://64.227.142.180:8060`.
The login code (static OTP) is in the private env file on the VPS. Ask Claude to show it in that message; it is not written here.

## Before you leave
- [ ] USB debugging on, phone connected, `adb devices` lists it
- [ ] Phone has internet (SIM data or shop Wi-Fi)

## Install
- [ ] `adb install -r exports/mandisamiti-pilot-debug.apk` (if upgrading over a pre-pilot build, run `adb shell pm clear com.appwork.mandisamiti` to prevent SQLite duplicate column error)
- [ ] Open the app, log in with the shop phone number and the private 6-digit code, set MPIN

## Smoke test (use names containing TEST)
- [ ] Create party "TEST Party"
- [ ] One buy entry, one sell entry
- [ ] One loan (lend) to TEST Party, then one repayment with interest
- [ ] Day page: repayment shows two lines (principal, interest); party balance falls by principal only
- [ ] Force-close the app, reopen: all entries still there
- [ ] Sync pending count is 0 (wait a minute on data/Wi-Fi if not)
- [ ] Light and dark mode both readable

**If any total looks wrong, stop and screenshot the day page and the party page. Do not enter real data.**

## Wipe test data
- [ ] Delete the TEST party and its entries from the app. If the app cannot delete them: Settings > Apps > MandiSamiti > Storage > Clear data, then log in again.
- [ ] Confirm the day page and party list are empty

## First real entry
- [ ] Enter one real entry, check the total against your paper bahi, then continue

## If it breaks
Screenshot, note the time, send to Claude. Pause real entries; keep the paper bahi going.
