# MandiSamiti Phase 1: 4-Tab Navigation & Dynamic Language Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Decompose the overcrowded single screen into a modern 4-Tab NGDL Bottom Navigation Bar (Dashboard, Khata Ledger, Daily Galla, Settings & Profile) and implement a dynamic runtime Language Switcher (Hindi/English) and Sign Out session management.

**Architecture:** 
- Central `AppScaffold` managing bottom navigation state (`NavigationTab`: `DASHBOARD`, `KHATA`, `GALLA`, `SETTINGS`).
- `DashboardScreen`: Daily turnover metrics, net receivables/payables, recent 5 live transactions, and quick new deal CTA.
- `KhataLedgerScreen`: Dedicated search, filter chips (सभी, लेना है, देना है, किसान, व्यापारी), and virtualized party accounts with WhatsApp share.
- `SettingsProfileScreen`: Shop profile, language switcher (हिन्दी ⇋ English), voice soundbox controls, and Sign Out.
- `LocaleManager`: Runtime locale persistence and multiplatform string bundle selection.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, SQLDelight, Koin, NGDL v1.2.

## Global Constraints
- High contrast (≥ 4.5:1), zero emoji clutter, strict NGDL tokens from `ui/theme/Color.kt`.
- Minimum 48dp touch targets for age 25–65 accessibility.
- Zero breaking changes to `MandiMathEngine` or SQLDelight database schemas.

---

### Task 1: Navigation State & Bottom Navigation Component

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/navigation/NavigationTab.kt`
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/navigation/MandiBottomBar.kt`
- Test: `composeApp/src/commonTest/kotlin/com/appwork/mandisamiti/ui/navigation/NavigationTabTest.kt`

**Interfaces:**
- `enum class NavigationTab { DASHBOARD, KHATA, GALLA, SETTINGS }`
- `MandiBottomBar(currentTab: NavigationTab, onTabSelected: (NavigationTab) -> Unit, modifier: Modifier = Modifier)`

- [x] **Step 1: Write unit tests for NavigationTab definitions**
- [x] **Step 2: Run test to verify test execution**
- [x] **Step 3: Implement `NavigationTab` and `MandiBottomBar` with vector icons (Dashboard, People, AccountBalanceWallet, Settings)**
- [x] **Step 4: Run `./gradlew test` to verify build & pass**
- [x] **Step 5: Commit changes**

---

### Task 2: Dedicated Dashboard Screen Component

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/dashboard/DashboardScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/home/HomeScreen.kt`

**Interfaces:**
- Consumes: `HomeViewModel`, `MandiMathEngine`
- Produces: `DashboardScreen(uiState: HomeUiState, onNewDealClick: () -> Unit, onPartyClick: (Party) -> Unit, onOpenGallaClick: () -> Unit)`

- [x] **Step 1: Create `DashboardScreen.kt` with clean Daily Turnover banner, 2 KPI tiles, and Recent Activity list**
- [x] **Step 2: Ensure all touch targets are ≥ 48dp and use NGDL elevated cards**
- [x] **Step 3: Verify preview & `./gradlew test`**
- [x] **Step 4: Commit changes**

---

### Task 3: Dedicated Khata Ledger Screen

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/ledger/KhataLedgerTabScreen.kt`

**Interfaces:**
- Consumes: `HomeViewModel`, `PartyCard`
- Produces: `KhataLedgerTabScreen(uiState: HomeUiState, onSearchChanged: (String) -> Unit, onFilterSelected: (PartyFilter) -> Unit, onPartyClick: (Party) -> Unit, onAddNewPartyClick: () -> Unit)`

- [x] **Step 1: Implement `KhataLedgerTabScreen` with sticky search bar, horizontal filter chips, and full party list**
- [x] **Step 2: Add "+ नया खाता जोड़ें" floating action button**
- [x] **Step 3: Verify list rendering and `./gradlew test`**
- [x] **Step 4: Commit changes**

---

### Task 4: Settings & Profile Screen with Language Switcher & Sign Out

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/settings/SettingsScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/home/HomeViewModel.kt`

**Interfaces:**
- `SettingsScreen(shopName: String, mandiLocation: String, isSoundEnabled: Boolean, currentLanguage: String, onToggleSound: () -> Unit, onLanguageSelected: (String) -> Unit, onSignOutClick: () -> Unit)`

- [x] **Step 1: Create `SettingsScreen` with Shop Profile card, Language Toggle (`हिन्दी` / `English`), Soundbox controls, and Red Sign Out CTA**
- [x] **Step 2: Connect language selection handler to ViewModel**
- [x] **Step 3: Verify `./gradlew test`**
- [x] **Step 4: Commit changes**

---

### Task 5: Master Scaffold Integration & Live Phone Deployment

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/App.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/home/HomeScreen.kt`

- [x] **Step 1: Wire `MandiBottomBar` into `HomeScreen` / `AppScaffold` with smooth tab switching**
- [x] **Step 2: Run `./gradlew test` to ensure all 152+ test tasks pass**
- [x] **Step 3: Deploy to physical Redmi Note 7 phone via `./gradlew :composeApp:installDebug`**
- [x] **Step 4: Capture screenshot via ADB and verify visual quality**
- [x] **Step 5: Run `python3 scripts/project_update.py --path projects/MandiSamiti` to sync across all AIs**
