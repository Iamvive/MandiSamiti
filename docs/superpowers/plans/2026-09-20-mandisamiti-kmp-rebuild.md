# MandiSamiti (Aadhat Cash Ledger) KMP + CMP Rebuild Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild MandiSamiti as an offline-first, scalable Kotlin Multiplatform (KMP) and Compose Multiplatform (CMP) aadhat ledger app with a low-literacy, high-contrast Devanagari UI, two-sided settlement, in-app soundbox announcements, and WhatsApp receipt generation.

**Architecture:** Clean architecture across 4 decoupled layers: `core-domain` (pure Kotlin math and models), `core-database` (SQLDelight offline-first with UUIDs & sync outbox), `core-data` (offline-first repositories), and `composeApp` (Compose Multiplatform UI with platform-specific expect/actual bridges for TTS, Camera, and WhatsApp sharing).

**Tech Stack:** Kotlin 2.0+, Compose Multiplatform 1.6+, SQLDelight 2.0+, Koin 3.5+, Kotlinx Coroutines & Serialization, Noto Sans Devanagari / Mukta typography.

## Global Constraints

- Storage: Stored as scaled integers (`Long` paisa for currency, `Long` grams for weights) to prevent floating-point rounding errors.
- UI Design: UI/UX Pro Max high-contrast Kiosk/Fintech theme (Deep Navy `#0F172A`, Crimson Red `#DC2626`, Forest Green `#15803D`, Pure White surface `#FFFFFF` with `#CBD5E1` border).
- Touch Targets: Minimum 56dp across all buttons, calculator keys, and list items.
- Typography: Minimum 16sp body text, 20sp–24sp headings, 28sp–34sp currency numbers.
- Localization: Devanagari (Hindi) default locale with English secondary, using Compose Multiplatform resources (`Res.string.*`).
- Soundbox: In-app standard conversational Hindi TTS announcement on transaction save with persistent top-bar 1-tap toggle.
- Offline-First: 100% functionality offline using SQLDelight; all records use UUIDs, UTC timestamps, soft deletes, and `sync_status` outbox column.

---

### Task 1: KMP Multi-Module Project Structure & Build Configuration

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`
- Create: `gradle/libs.versions.toml`
- Create: `core-domain/build.gradle.kts`
- Create: `core-database/build.gradle.kts`
- Create: `core-data/build.gradle.kts`
- Create: `composeApp/build.gradle.kts`

**Interfaces:**
- Produces: Multi-module Gradle build graph with `:core-domain`, `:core-database`, `:core-data`, and `:composeApp` configured for Android and JVM/iOS targets.

- [ ] **Step 1: Configure Version Catalog (`gradle/libs.versions.toml`)**
Define Kotlin, Compose Multiplatform, SQLDelight, Koin, Coroutines, Serialization, and AndroidX library coordinates and versions.

- [ ] **Step 2: Update `settings.gradle.kts` and root `build.gradle.kts`**
Include all 4 subprojects (`:core-domain`, `:core-database`, `:core-data`, `:composeApp`) and apply plugin management.

- [ ] **Step 3: Create module `build.gradle.kts` files**
Configure KMP targets (`androidTarget`, `iosX64`, `iosArm64`, `iosSimulatorArm64`, `jvm`) and source set dependencies.

- [ ] **Step 4: Verify build configuration**
Run: `./gradlew tasks --dry-run` to ensure all modules and plugins resolve without errors.

- [ ] **Step 5: Commit**
```bash
git add gradle/libs.versions.toml settings.gradle.kts build.gradle.kts core-domain/ core-database/ core-data/ composeApp/
git commit -m "build: scaffold KMP multi-module build configuration"
```

---

### Task 2: Core Domain Models & Precision Math Engine

**Files:**
- Create: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/model/Party.kt`
- Create: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/model/Commodity.kt`
- Create: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/model/Deal.kt`
- Create: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/model/CashTransaction.kt`
- Create: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/math/MandiMathEngine.kt`
- Create: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/math/RuralInterestEngine.kt`
- Create: `core-domain/src/commonTest/kotlin/com/appwork/mandisamiti/domain/MandiMathEngineTest.kt`
- Create: `core-domain/src/commonTest/kotlin/com/appwork/mandisamiti/domain/RuralInterestEngineTest.kt`

**Interfaces:**
- Produces: 
  - `MandiMathEngine.calculateSettlement(grossWeightGrams, cutWeightGrams, ratePaisaPerUnit, deductions)` $\rightarrow$ `SettlementCalculation`
  - `RuralInterestEngine.calculateAccruedInterest(principalPaisa, monthlyRatePercent, startDateEpoch, endDateEpoch)` $\rightarrow$ `InterestCalculation`

- [ ] **Step 1: Write failing unit test for Mandi calculation math**
Test gross weight, cut deductions, quintal-to-gram conversion, rate multiplication, farmer deductions, buyer commission, and net payouts with exact paise precision.

- [ ] **Step 2: Run test to verify failure**
Run: `./gradlew :core-domain:allTests` (Fails: Class not found).

- [ ] **Step 3: Implement `MandiMathEngine` & Domain Models**
Implement pure Kotlin domain models and arithmetic logic using `Long` operations without floating-point precision loss.

- [ ] **Step 4: Write failing unit test for Rural Interest Engine**
Test monthly simple interest calculation (*सैकड़ा दर math*: ₹1.50/₹100/month = 18% p.a., prorated by days elapsed).

- [ ] **Step 5: Implement `RuralInterestEngine`**
Write math calculating days elapsed, whole months, fractional days, and net interest amount in paise.

- [ ] **Step 6: Run tests to verify all pass**
Run: `./gradlew :core-domain:allTests` (All pass).

- [ ] **Step 7: Commit**
```bash
git add core-domain/
git commit -m "feat(domain): implement core models, mandi math engine, and rural interest engine with tests"
```

---

### Task 3: SQLDelight Database Schema & DAOs

**Files:**
- Create: `core-database/src/commonMain/sqldelight/com/appwork/mandisamiti/database/AppDatabase.sq`
- Create: `core-database/src/commonMain/kotlin/com/appwork/mandisamiti/database/DriverFactory.kt`
- Create: `core-database/src/androidMain/kotlin/com/appwork/mandisamiti/database/DriverFactory.android.kt`
- Create: `core-database/src/commonTest/kotlin/com/appwork/mandisamiti/database/DatabaseTest.kt`

**Interfaces:**
- Produces: `AppDatabase` with tables (`shop_profile`, `party`, `commodity`, `deal`, `cash_transaction`) and views (`v_party_balance`, `v_daily_cash_register`).

- [ ] **Step 1: Write SQLDelight schema `AppDatabase.sq`**
Include all tables with UUID primary keys, `shop_id` partition keys, `is_deleted` soft-deletes, `sync_status` flags, and running balance calculation queries.

- [ ] **Step 2: Write failing database unit test**
Create an in-memory SQLite driver test verifying insert, query, update (edit deal), soft-delete, and running balance aggregation for both Farmers and Buyers.

- [ ] **Step 3: Run database test to verify failure**
Run: `./gradlew :core-database:allTests` (Fails: DriverFactory/AppDatabase not generated).

- [ ] **Step 4: Implement platform `DriverFactory` and generate SQLDelight classes**
Provide `AndroidSqliteDriver` on Android and in-memory test driver for unit tests.

- [ ] **Step 5: Run database tests to verify all pass**
Run: `./gradlew :core-database:allTests` (All pass).

- [ ] **Step 6: Commit**
```bash
git add core-database/
git commit -m "feat(database): define SQLDelight schema, queries, views, and test driver"
```

---

### Task 4: Offline-First Repository Layer & Sync Engine Contract

**Files:**
- Create: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/repository/PartyRepository.kt`
- Create: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/repository/DealRepository.kt`
- Create: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/repository/CashTransactionRepository.kt`
- Create: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/repository/ShopProfileRepository.kt`
- Create: `core-data/src/commonMain/kotlin/com/appwork/mandisamiti/data/repository/OfflineFirstPartyRepository.kt`
- Create: `core-data/src/commonMain/kotlin/com/appwork/mandisamiti/data/repository/OfflineFirstDealRepository.kt`
- Create: `core-data/src/commonMain/kotlin/com/appwork/mandisamiti/data/repository/OfflineFirstCashTransactionRepository.kt`
- Create: `core-data/src/commonMain/kotlin/com/appwork/mandisamiti/data/sync/SyncEngine.kt`
- Create: `core-data/src/commonTest/kotlin/com/appwork/mandisamiti/data/RepositoryTest.kt`

**Interfaces:**
- Produces: Flow-based reactive repository APIs returning domain models, handling local insertions with `sync_status = PENDING`, and exposing `SyncEngine.getPendingSyncPayload()` for future remote cloud sync.

- [ ] **Step 1: Define repository interfaces in `core-domain`**
Expose Kotlin `Flow<List<Party>>`, `Flow<PartyBalance>`, `suspend fun saveDeal(deal: Deal)`, `suspend fun editDeal(deal: Deal)`, `suspend fun recordCash(tx: CashTransaction)`.

- [ ] **Step 2: Write failing unit test for repositories**
Test adding a deal, updating a rate, recording a cash advance, and asserting the updated balances via `Flow`.

- [ ] **Step 3: Implement `OfflineFirst*Repository` in `core-data`**
Implement DB mapping, UUID generation, monotonic timestamps, and soft-delete updates.

- [ ] **Step 4: Run repository tests to verify all pass**
Run: `./gradlew :core-data:allTests` (All pass).

- [ ] **Step 5: Commit**
```bash
git add core-data/ core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/repository/
git commit -m "feat(data): implement offline-first repositories and sync outbox engine"
```

---

### Task 5: Platform Expect/Actual Bridges (Soundbox TTS, Camera, WhatsApp Sharing)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/platform/SoundboxTtsManager.kt`
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/platform/CameraSlipPicker.kt`
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/platform/WhatsAppShareManager.kt`
- Create: `composeApp/src/androidMain/kotlin/com/appwork/mandisamiti/platform/SoundboxTtsManager.android.kt`
- Create: `composeApp/src/androidMain/kotlin/com/appwork/mandisamiti/platform/CameraSlipPicker.android.kt`
- Create: `composeApp/src/androidMain/kotlin/com/appwork/mandisamiti/platform/WhatsAppShareManager.android.kt`

**Interfaces:**
- Produces:
  - `SoundboxTtsManager.speak(text: String, isSoundEnabled: Boolean)`
  - `CameraSlipPicker.captureSlipPhoto() -> String?` (file URI)
  - `WhatsAppShareManager.shareReceiptImage(imageBytes: ByteArray, phoneNumber: String?, caption: String)`

- [ ] **Step 1: Declare common `expect` interfaces**
Define common signatures for Text-to-Speech synthesis, camera image capture, and WhatsApp image intent dispatcher.

- [ ] **Step 2: Implement Android `actual` for `SoundboxTtsManager`**
Wrap Android `TextToSpeech` engine configured with `Locale("hi", "IN")` and pitch/rate calibrated for clear speech.

- [ ] **Step 3: Implement Android `actual` for `CameraSlipPicker`**
Connect to Android `ActivityResultContracts.TakePicture` to store high-res slip photos in internal cache.

- [ ] **Step 4: Implement Android `actual` for `WhatsAppShareManager`**
Create `FileProvider` URI and launch `ACTION_SEND` targeting `com.whatsapp` or generic share sheet fallback.

- [ ] **Step 5: Verify compilation on Android**
Run: `./gradlew :composeApp:compileKotlinAndroid`

- [ ] **Step 6: Commit**
```bash
git add composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/platform/ composeApp/src/androidMain/
git commit -m "feat(platform): implement expect/actual bridges for TTS, Camera, and WhatsApp sharing"
```

---

### Task 6: UI Design System, Devanagari Strings & Custom Mandi Calculator Keypad

**Files:**
- Create: `composeApp/src/commonMain/composeResources/values/strings.xml` (Devanagari Default)
- Create: `composeApp/src/commonMain/composeResources/values-en/strings.xml` (English Secondary)
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/theme/Color.kt`
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/theme/Type.kt`
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/theme/Theme.kt`
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/components/MandiCalculatorKeypad.kt`
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/components/SoundboxTopBar.kt`
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/components/PartyCard.kt`

**Interfaces:**
- Produces: Reusable UI components conforming to UI/UX Pro Max tokens: `MandiCalculatorKeypad` with 56dp keys, `SoundboxTopBar` with 1-tap `[ 🔊 आवाज़ ]` toggle, `PartyCard` with village badges and high-contrast ₹ balance.

- [ ] **Step 1: Add Devanagari and English string resources**
Add all UI strings, labels, soundbox speech templates, and receipt templates.

- [ ] **Step 2: Implement UI/UX Pro Max Color & Typography Theme**
Configure Slate Navy (`#0F172A`), Crimson (`#DC2626`), Emerald (`#15803D`), 16sp body, 28sp–34sp bold amounts.

- [ ] **Step 3: Implement `MandiCalculatorKeypad`**
Create a 4x4 grid of oversized tactile buttons (`1-9`, `0`, `00`, `.`, `C`, `⌫`, `अगला / Submit`) with haptic click feedback on press.

- [ ] **Step 4: Implement `SoundboxTopBar` and `PartyCard`**
Build top app bar with shop title and instant audio toggle; build large 80dp+ list cards with village tag.

- [ ] **Step 5: Verify Compose components build**
Run: `./gradlew :composeApp:compileKotlinAndroid`

- [ ] **Step 6: Commit**
```bash
git add composeApp/src/commonMain/composeResources/ composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/
git commit -m "feat(ui): implement design system tokens, devanagari strings, and calculator keypad component"
```

---

### Task 7: Home / Khata Dashboard Screen

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/home/HomeViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/home/HomeScreen.kt`
- Create: `composeApp/src/commonTest/kotlin/com/appwork/mandisamiti/ui/home/HomeViewModelTest.kt`

**Interfaces:**
- Produces: `HomeScreen` displaying master balances (🔴 लेना है ₹ / 🟢 देना है ₹), voice-enabled party search, quick filter tabs, farmer list, and persistent 64dp primary action button **"⚡ नया पर्चा / बिक्री दर्ज करें"**.

- [ ] **Step 1: Write unit test for `HomeViewModel`**
Test state emission for aggregated totals, party list filtering by name or village, and soundbox toggle state change.

- [ ] **Step 2: Implement `HomeViewModel`**
Connect to `PartyRepository` and `ShopProfileRepository` using `StateFlow`.

- [ ] **Step 3: Implement `HomeScreen` Compose UI**
Build the single-canvas scrolling dashboard with large stats cards, instant voice filter mic, and giant bottom action button.

- [ ] **Step 4: Run test to verify passes**
Run: `./gradlew :composeApp:allTests`

- [ ] **Step 5: Commit**
```bash
git add composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/home/ composeApp/src/commonTest/
git commit -m "feat(ui): build Home / Khata dashboard screen and ViewModel"
```

---

### Task 8: Two-Stage Deal & Calculator Flow Screen (नया पर्चा व संपादन)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/entry/DealEntryViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/entry/DealEntryScreen.kt`
- Create: `composeApp/src/commonTest/kotlin/com/appwork/mandisamiti/ui/entry/DealEntryViewModelTest.kt`

**Interfaces:**
- Produces: `DealEntryScreen` with two-stage support (Stage 1: Arrival/Weigh-in slip $\rightarrow$ Stage 2: Auction rate, buyer, and manual deductions entry), full edit mode for past deals, photo slip attachment, and real-time live total calculation.

- [ ] **Step 1: Write unit test for `DealEntryViewModel`**
Test keypad input state machine transitions (Bags $\rightarrow$ Weight $\rightarrow$ Rate $\rightarrow$ Deductions $\rightarrow$ Final Amount), deal saving, and edit deal balance recalculation.

- [ ] **Step 2: Implement `DealEntryViewModel`**
Integrate with `MandiMathEngine`, `DealRepository`, `CameraSlipPicker`, and `SoundboxTtsManager`.

- [ ] **Step 3: Implement `DealEntryScreen` Compose UI**
Build pinned input form on top half, fixed `MandiCalculatorKeypad` on bottom half, camera slip attachment button, and giant submit button.

- [ ] **Step 4: Run tests to verify all pass**
Run: `./gradlew :composeApp:allTests`

- [ ] **Step 5: Commit**
```bash
git add composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/entry/
git commit -m "feat(ui): build two-stage deal calculator entry screen with photo slip capture and edit support"
```

---

### Task 9: Party Ledger (Khata), Direct Cash & Rural Interest Screen

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/party/PartyLedgerViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/party/PartyLedgerScreen.kt`
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/party/CashEntryDialog.kt`
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/party/RuralInterestDialog.kt`
- Create: `composeApp/src/commonTest/kotlin/com/appwork/mandisamiti/ui/party/PartyLedgerViewModelTest.kt`

**Interfaces:**
- Produces: `PartyLedgerScreen` showing chronological transaction history, running balance, quick buttons for **🔴 रुपये दिए / उधार** and **🟢 रुपये मिले / जमा**, and rural monthly interest calculator dialog with 1-tap **"➕ ब्याज जोड़ें"** and **"🤝 छूट / समझौता"**.

- [ ] **Step 1: Write unit test for `PartyLedgerViewModel`**
Test transaction timeline loading, recording cash advance, applying accrued interest, and applying discount/waiver.

- [ ] **Step 2: Implement `PartyLedgerViewModel`**
Integrate with `PartyRepository`, `CashTransactionRepository`, `RuralInterestEngine`, and `SoundboxTtsManager`.

- [ ] **Step 3: Implement `PartyLedgerScreen`, `CashEntryDialog`, and `RuralInterestDialog`**
Build the two-column transaction list, prominent balance banner, and quick action dialogs with large numeric inputs.

- [ ] **Step 4: Run tests to verify all pass**
Run: `./gradlew :composeApp:allTests`

- [ ] **Step 5: Commit**
```bash
git add composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/party/
git commit -m "feat(ui): build party ledger screen, cash in/out dialogs, and rural interest calculator"
```

---

### Task 10: WhatsApp Digital Slip Generator, Soundbox & Daily Cash Register

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/receipt/DigitalSlipRenderer.kt`
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/receipt/ReceiptPreviewScreen.kt`
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/dayclosing/DailyCashRegisterViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/dayclosing/DailyCashRegisterScreen.kt`
- Create: `composeApp/src/commonTest/kotlin/com/appwork/mandisamiti/ui/dayclosing/DailyCashRegisterViewModelTest.kt`

**Interfaces:**
- Produces: 
  - `DigitalSlipRenderer.renderReceiptBitmap(deal: Deal, shop: ShopProfile, farmer: Party)` $\rightarrow$ Image ByteArray
  - `ReceiptPreviewScreen` with 1-tap WhatsApp share and `[ 🗣️ दोबारा सुनें ]` soundbox replay button.
  - `DailyCashRegisterScreen` showing 1-tap day-end balance (Cash In, Cash Out, Total Traded Volume, Drawer Balance).

- [ ] **Step 1: Implement `DigitalSlipRenderer`**
Draw high-contrast, branded traditional mandi slip using Compose Canvas / Skiko Bitmap rendering.

- [ ] **Step 2: Implement `ReceiptPreviewScreen`**
Display the rendered slip, attach 1-tap WhatsApp sharing via `WhatsAppShareManager`, and speech replay button via `SoundboxTtsManager`.

- [ ] **Step 3: Implement `DailyCashRegisterScreen` & ViewModel**
Aggregate daily cash in, cash out, commodity volume, and drawer balance.

- [ ] **Step 4: Run tests to verify all pass**
Run: `./gradlew :composeApp:allTests`

- [ ] **Step 5: Commit**
```bash
git add composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/receipt/ composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/dayclosing/
git commit -m "feat(ui): implement digital receipt renderer, WhatsApp sharing, and daily cash register"
```

---

### Task 11: End-to-End Flow Verification & Walkthrough

**Files:**
- Create: `composeApp/src/commonTest/kotlin/com/appwork/mandisamiti/e2e/MandiLedgerE2ETest.kt`

**Interfaces:**
- Produces: Automated end-to-end integration test exercising the full aadhat workflow: Shop onboarding $\rightarrow$ Farmer arrival $\rightarrow$ Weigh-in $\rightarrow$ Auction rate settlement $\rightarrow$ Edit deal $\rightarrow$ Cash advance $\rightarrow$ Monthly interest accrued $\rightarrow$ WhatsApp slip generation $\rightarrow$ Daily cash closing.

- [ ] **Step 1: Write comprehensive end-to-end integration test**
Simulate complete daily mandi cycle across repository, domain math, and viewmodels.

- [ ] **Step 2: Run all module tests and build verification**
Run: `./gradlew check allTests`

- [ ] **Step 3: Commit**
```bash
git add composeApp/src/commonTest/kotlin/com/appwork/mandisamiti/e2e/
git commit -m "test(e2e): verify complete offline mandi ledger workflow and calculations"
```
