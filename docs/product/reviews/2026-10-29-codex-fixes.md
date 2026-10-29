# 2026-10-29 Codex PM review fixes

## F19: partial, waiting on database owner

- Fixed the day-closing result text for shortage, surplus, and exact match. The saved count now remains visible as `गिनती: ₹10,000 · कमी ₹200`, `अधिक`, or `मिलान सही`.
- Added the optional reason field to the count flow.
- Test: `DayClosingPresentationTest`.
- Filed the required `dayClosingEntity`, queries, repository flow, and revision contract in hub file `prompts/mandisamiti-requests.md`. Count/reason persistence and opening-cash audit cannot be completed without Claude's database/repository change.

## F20: not fixed

- The split bahi has two nested `LazyColumn`s inside a fixed-height screen. Replacing them safely requires converting the whole register body to one lazy list while retaining side-by-side aligned rows. This was not completed.
- Existing `DailyRegisterUiState.totalNameSidePaisa` includes the closing balance, and the closing row exists, but it can be hidden below the nested scroll.

## F22: needs device/emulator proof

- Code inspection found Home's loading state clears on the first `getPartiesStream` emission; dashboard/party balance observers do not gate it. No sequential `first()` exists in `HomeViewModel`.
- No emulator AVD is installed. A Redmi Note 7 is attached, but the task says Antigravity owns the phone, so this branch did not install or launch on it.
- Before/after `adb shell am start -W` numbers were therefore not recorded. Antigravity must capture them after integration.

## F23: fixed, visual proof pending

- Quick-action, KPI, and market/farmer metric rows now use intrinsic row height with `fillMaxHeight()` children.
- Shop name allows two lines while the text column yields space to status/actions.
- Commit: `15ef491`.
- No preview tooling is configured in `composeApp`; 390dp light/dark screenshots remain required on the integration/device branch.

## F24: partial due file ownership

- Added shared `SyncStatusPill`: clock + `फ़ोन में` for pending, cloud + `सर्वर पर` for synced, with English equivalents.
- Replaced the ambiguous tick in owned `PartyCard` UI.
- Test: `SyncStatusPresentationTest`.
- Remaining tick instances are in `ui/ledger/PartyLedgerScreen.kt`, which this prompt explicitly assigns away from Codex. Their owner must replace them with `SyncStatusPill` after merge.
- F08's session-expired text remains unchanged: `लॉगिन करें, फिर एंट्री अपलोड होंगी`.
- Commit: `b9d3ef4`.

## Verification

- Focused F19/F24 tests: `BUILD SUCCESSFUL`.
- Baseline/full JVM suite has the known Claude-owned F01 failure: `PartyLedgerBalanceF01Test.reproF01_partyBalance_exactScenario_gives13900ThenZero`.
- Still required after database branch merge: rebase, full `./gradlew :composeApp:jvmTest`, `tests/ngdl-tokens`, 390dp light/dark screenshots, cold-start timing, and device click-through.
