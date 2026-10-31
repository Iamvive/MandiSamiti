# Cash Drawer two-day device review

Tested on the physical Redmi Note 7 (`d93c743`) on 2026-10-31. All rows are synthetic TEST data.

The side-by-side app is **Mandi Drawer Review**, package `com.appwork.mandisamiti.drawerreview`. It uses its own app storage and has no INTERNET permission. The pilot app and its data were not modified. The review harness freezes its clock at 2026-10-31 17:30 IST so the two calendar days remain reproducible.

## Results

| Date | Opening | Cash received | Cash paid | Expected closing | Rows |
| --- | ---: | ---: | ---: | ---: | ---: |
| 30 Oct | ₹20,000 | ₹47,000 | ₹14,500 | ₹52,500 | 7 |
| 31 Oct, seeded | ₹52,500 | ₹53,000 | ₹25,000 | ₹80,500 | 10 |
| 31 Oct, after two entries saved through the UI | ₹52,500 | ₹54,000 | ₹25,200 | ₹81,300 | 12 |

There is one additional opening-balance row on 29 Oct, making 20 cash-transaction records in the retained database. UPI and NEFT entries appear in the full list and do not change physical cash. Cash-only filtering shows five rows on day one and ten on day two after UI entry.

Passed on the phone:

- Date navigation and opening-balance carry-forward.
- List/Bahi and cash-only icon controls, with accessibility descriptions.
- A ₹1,000 receipt and ₹200 payment created using the actual cash-entry dialogs and numeric keyboard.
- Day-one physical count of ₹52,500, saved independently from day two.
- A matching ₹81,500 count invalidated after the ₹200 payment.
- Denomination count: 160 × ₹500 + 6 × ₹200 + 1 × ₹100 = ₹81,300.
- Matching day-two count and explicit close-day confirmation.
- Counts, closing state, and rows preserved through APK upgrade and force-stop/relaunch.
- Hindi/English and light/dark screen inspection. No audio played.

The run found and fixed received interest being placed on the wrong bahi side, internal expense tags appearing as row names, and bahi totals needing refresh after a count change. Regression coverage uses the same debug-only fixture as the device app. Final gate: 362 JVM tests passed; review APK assembled. Standard APK build is checked separately. NGDL token checks passed.

## Retained for approval

**Do not clear, uninstall, delete rows, or reset counts until Vivek approves cleanup.** The data is still on the phone.

- Review APK: `exports/mandisamiti-cash-drawer-review.apk` in the Vivek-K root.
- Read-only database backup: `exports/mandisamiti-drawer-review-data.sqlite` in the Vivek-K root. Integrity check returned `ok`.
- Rebuild the separate app: `./gradlew :composeApp:assembleDebug -PdrawerReview=true`.
- Open it: `adb -s d93c743 shell am start -n com.appwork.mandisamiti.drawerreview/com.appwork.mandisamiti.CashDrawerReviewActivity`.

Screenshots 01–07 capture the test progression; 08 shows the final count after restart; 09 confirms the cleaned expense label. Unverified: enlarged accessibility fonts, other phone models, and real shop sync. The review app intentionally cannot sync.
