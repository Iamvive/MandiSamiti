# MandiSamiti Prod & Staging Variants Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `staging` and `prod` Android product flavors to `composeApp`, enable R8 shrinking/obfuscation for release builds, and wire fail-closed release signing via a gitignored `keystore.properties` (or env vars).

**Architecture:** Configure an `env` flavor dimension in `composeApp/build.gradle.kts` (`staging` with `.staging` applicationIdSuffix and staging cleartext host config; `prod` with base `com.appwork.mandisamiti`, HTTPS-only, and R8 enabled). Keep signing credentials out of git using `.gitignore` and `keystore.properties.example`.

**Tech Stack:** Kotlin Multiplatform, Android Gradle Plugin 8.x, R8/ProGuard.

## Global Constraints
- Never push to `main`; work on a branch (`feat/prod-variant-signing`).
- Never commit `keystore.properties`, `*.jks`, `*.keystore`, or real secrets.
- `prod` must not allow cleartext HTTP traffic.
- `prodRelease` must fail cleanly if release signing config is absent (never fall back to debug keystore).

---

### Task 1: Branch & Secret-Hygiene Setup

**Files:**
- Modify: `projects/MandiSamiti/.gitignore`
- Create: `projects/MandiSamiti/keystore.properties.example`

- [ ] **Step 1:** Create branch `feat/prod-variant-signing` from current `HEAD`.
- [ ] **Step 2:** Append `keystore.properties`, `*.jks`, and `*.keystore` to `.gitignore`.
- [ ] **Step 3:** Create `keystore.properties.example` with placeholder keys (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`).

### Task 2: Staging Network Config & Launcher Labels

**Files:**
- Modify: `projects/MandiSamiti/composeApp/src/androidMain/AndroidManifest.xml:14`
- Create: `projects/MandiSamiti/composeApp/src/staging/AndroidManifest.xml`
- Create: `projects/MandiSamiti/composeApp/src/staging/res/xml/network_security_config_staging.xml`

- [ ] **Step 1:** Update `android:label` in `androidMain/AndroidManifest.xml` from `"मंडी समिति"` to `"@string/app_name"`.
- [ ] **Step 2:** Create `src/staging/res/xml/network_security_config_staging.xml` permitting cleartext only for `64.227.142.180` (base-config `cleartextTrafficPermitted="false"`).
- [ ] **Step 3:** Create `src/staging/AndroidManifest.xml` setting `android:networkSecurityConfig="@xml/network_security_config_staging"` (keeping `androidDebug` overlay for local dev).

### Task 3: Product Flavors, R8, and Fail-Closed Release Signing

**Files:**
- Create: `projects/MandiSamiti/composeApp/proguard-rules.pro`
- Modify: `projects/MandiSamiti/composeApp/build.gradle.kts:72-117`

- [ ] **Step 1:** Create `composeApp/proguard-rules.pro` with keep rules for `kotlinx.serialization`, Ktor, Koin, and SQLDelight.
- [ ] **Step 2:** Add `flavorDimensions += "env"` and `productFlavors` (`staging`, `prod`) in `composeApp/build.gradle.kts`, setting `applicationIdSuffix`, `resValue("string", "app_name", ...)`, and `API_BASE_URL` (preserving `local.properties` `mandi.apiBaseUrl` override on `debug` and `drawerReview`).
- [ ] **Step 3:** Add `signingConfigs.create("release")` reading `keystore.properties` or `MANDI_KEYSTORE_*` env vars, enable `isMinifyEnabled = true` and `isShrinkResources = true` on `release`, and add a task-graph check so `prodRelease` / `stagingRelease` fail with an actionable error if signing config is missing.

### Task 4: Verification & Tracking

- [ ] **Step 1:** Run `./gradlew :composeApp:testJvmTest` and verify all tests pass.
- [ ] **Step 2:** Run `./gradlew :composeApp:assembleStagingDebug :composeApp:assembleProdDebug` and verify both APKs build with distinct application IDs.
- [ ] **Step 3:** Verify `./gradlew :composeApp:assembleProdRelease` fails with the expected missing-signing message when `keystore.properties` is absent.
- [ ] **Step 4:** Commit spec, plan, and implementation on `feat/prod-variant-signing` and update `docs/STATUS.md` / `vaults/Vivek-K/wiki/log.md`.
