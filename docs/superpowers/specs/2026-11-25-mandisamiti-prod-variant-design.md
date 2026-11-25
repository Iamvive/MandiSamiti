# MandiSamiti Android Prod & Staging Variants Design

Date: 2026-11-25
Scope: `projects/MandiSamiti/composeApp` and root `.gitignore`

## Goal
Introduce side-by-side `staging` and `prod` Android product flavors in `composeApp`, enable R8 minification and resource shrinking for release builds, and configure fail-closed release signing via a gitignored `keystore.properties` (or `MANDI_KEYSTORE_*` environment variables).

## 1. Product Flavors (`env` dimension)
Add `flavorDimensions += "env"` with two flavors in `composeApp/build.gradle.kts`:

- **`staging`**
  - `dimension = "env"`
  - `applicationIdSuffix = ".staging"` (effective ID `com.appwork.mandisamiti.staging`)
  - `resValue("string", "app_name", "मंडी समिति (Staging)")`
  - `buildConfigField("String", "API_BASE_URL", "\"$stagingUrl\"")` where `stagingUrl` reads `mandi.stagingApiBaseUrl` from `local.properties` or defaults to `http://64.227.142.180:8060`.
  - Uses a staging manifest / network security config allowing cleartext HTTP only for the staging host `64.227.142.180`.
- **`prod`**
  - `dimension = "env"`
  - Uses base `applicationId = "com.appwork.mandisamiti"`
  - `resValue("string", "app_name", "मंडी समिति")`
  - `buildConfigField("String", "API_BASE_URL", "\"https://mandi-api.appworx.co.in\"")`
  - No cleartext HTTP traffic allowed.

The `debug` buildType continues to honor `mandi.apiBaseUrl` from `local.properties` when set (and preserves `drawerReview` behavior). The `androidMain/AndroidManifest.xml` `android:label` references `@string/app_name` so each flavor shows its own launcher label.

## 2. Release Hardening (R8 / ProGuard)
In `composeApp/build.gradle.kts` under `buildTypes.release`:
- Set `isMinifyEnabled = true`
- Set `isShrinkResources = true`
- Reference `getDefaultProguardFile("proguard-android-optimize.txt")` and `"proguard-rules.pro"`

Create `composeApp/proguard-rules.pro` with keep rules for:
- `kotlinx.serialization` serializers and `@Serializable` classes
- Ktor client / engine classes
- Koin reflection/viewModel bindings if any
- SQLDelight generated database/query classes

## 3. Release Signing (Fail-Closed)
- Add `signingConfigs.create("release")` reading `storeFile`, `storePassword`, `keyAlias`, and `keyPassword` from `rootProject.file("keystore.properties")`, falling back to `MANDI_KEYSTORE_PATH`, `MANDI_KEYSTORE_PASSWORD`, `MANDI_KEY_ALIAS`, and `MANDI_KEY_PASSWORD` environment variables.
- Assign `signingConfig = signingConfigs.getByName("release")` to `release` when configured, and fail `prodRelease` / `stagingRelease` assembly tasks with a clear Gradle error if release signing credentials are missing (never falling back to the debug keystore for `prodRelease`).
- Add `keystore.properties`, `*.jks`, and `*.keystore` to `projects/MandiSamiti/.gitignore`.
- Add `projects/MandiSamiti/keystore.properties.example` with placeholder values only.

## 4. Verification
- `./gradlew :composeApp:testJvmTest` passes.
- `./gradlew :composeApp:assembleStagingDebug` and `./gradlew :composeApp:assembleProdDebug` succeed and produce distinct application IDs (`com.appwork.mandisamiti.staging` and `com.appwork.mandisamiti`).
- `./gradlew :composeApp:assembleProdRelease` fails clearly when `keystore.properties` is absent, and succeeds with R8 enabled once a release keystore is configured.
