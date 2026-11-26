import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Opt-in side-by-side review APK. Its manifest has no network permission or production launcher.
val drawerReview = providers.gradleProperty("drawerReview").orNull == "true"

kotlin {
    jvmToolchain(17)
    jvm()
    androidTarget()
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        matching { it.name == "androidDebug" }.configureEach { kotlin.srcDir("src/reviewFixtures/kotlin") }
        getByName("jvmTest").kotlin.srcDir("src/reviewFixtures/kotlin")
        commonMain.dependencies {
            implementation(projects.coreDomain)
            implementation(projects.coreDatabase)
            implementation(projects.coreData)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.koin.android)
            implementation(libs.sentry.android)
            implementation(libs.kotlinx.coroutines.android)
        }
        jvmTest.dependencies {
            implementation(libs.ktor.client.mock)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}

android {
    namespace = "com.appwork.mandisamiti"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.appwork.mandisamiti"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "2.0.0"
    }

    buildFeatures {
        buildConfig = true
    }

    // KMP maps androidDebug -> android "debug" too late for the manifest merger; set the overlay explicitly.
    sourceSets.getByName("debug").manifest.srcFile(
        if (drawerReview) "src/drawerReview/AndroidManifest.xml" else "src/androidDebug/AndroidManifest.xml"
    )

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    val localProps = Properties().apply {
        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
    }
    // Crash reporting is off unless a DSN is set: local.properties `mandi.sentryDsn` or env SENTRY_DSN. Never commit it.
    val sentryDsn = localProps.getProperty("mandi.sentryDsn") ?: System.getenv("SENTRY_DSN") ?: ""
    val keystoreProps = Properties().apply {
        rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
    }
    val releaseStoreFile = keystoreProps.getProperty("storeFile") ?: System.getenv("MANDI_KEYSTORE_PATH")
    val releaseStorePassword = keystoreProps.getProperty("storePassword") ?: System.getenv("MANDI_KEYSTORE_PASSWORD")
    val releaseKeyAlias = keystoreProps.getProperty("keyAlias") ?: System.getenv("MANDI_KEYSTORE_ALIAS")
    val releaseKeyPassword = keystoreProps.getProperty("keyPassword") ?: System.getenv("MANDI_KEYSTORE_PASSWORD")
    val hasReleaseSigning = !releaseStoreFile.isNullOrBlank() &&
        !releaseStorePassword.isNullOrBlank() &&
        !releaseKeyAlias.isNullOrBlank() &&
        !releaseKeyPassword.isNullOrBlank()

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    flavorDimensions += "env"
    productFlavors {
        create("staging") {
            dimension = "env"
            applicationIdSuffix = ".staging"
            resValue("string", "app_name", "मंडी समिति (Staging)")
            val stagingUrl = localProps.getProperty("mandi.stagingApiBaseUrl") ?: "http://64.227.142.180:8060"
            buildConfigField("String", "API_BASE_URL", "\"$stagingUrl\"")
        }
        create("prod") {
            dimension = "env"
            resValue("string", "app_name", "मंडी समिति")
            buildConfigField("String", "API_BASE_URL", "\"https://mandi-api.appworx.co.in\"")
        }
    }

    buildTypes {
        getByName("release") {
            buildConfigField("String", "SENTRY_DSN", "\"$sentryDsn\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        getByName("debug") {
            buildConfigField("String", "SENTRY_DSN", "\"$sentryDsn\"")
            if (drawerReview) applicationIdSuffix = ".drawerreview"
            val debugUrl = localProps.getProperty("mandi.apiBaseUrl")
            if (!debugUrl.isNullOrBlank()) {
                buildConfigField("String", "API_BASE_URL", "\"$debugUrl\"")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.desktop {
    application {
        mainClass = "com.appwork.mandisamiti.MainKt"
    }
}

tasks.register<JavaExec>("runDemo") {
    group = "application"
    description = "Runs the MandiSamiti live interactive scenario demo"
    val jvmTarget = kotlin.targets.getByName("jvm") as org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
    val compilation = jvmTarget.compilations.getByName("main")
    classpath = compilation.output.allOutputs + compilation.runtimeDependencyFiles
    mainClass.set("com.appwork.mandisamiti.DemoSimulator")
}

tasks.withType<Test>().configureEach {
    systemProperty("mandi.sound.disabled", "true")
}

gradle.taskGraph.whenReady {
    val needsReleaseSigning = allTasks.any { task ->
        task.project == project &&
            (task.name.contains("Release") && (task.name.startsWith("assemble") || task.name.startsWith("bundle")))
    }
    if (needsReleaseSigning) {
        val keystoreProps = Properties().apply {
            rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
        }
        val configured = (!keystoreProps.getProperty("storeFile").isNullOrBlank() ||
            !System.getenv("MANDI_KEYSTORE_PATH").isNullOrBlank()) &&
            (!keystoreProps.getProperty("storePassword").isNullOrBlank() ||
                !System.getenv("MANDI_KEYSTORE_PASSWORD").isNullOrBlank()) &&
            (!keystoreProps.getProperty("keyAlias").isNullOrBlank() ||
                !System.getenv("MANDI_KEYSTORE_ALIAS").isNullOrBlank()) &&
            (!keystoreProps.getProperty("keyPassword").isNullOrBlank() ||
                !System.getenv("MANDI_KEYSTORE_PASSWORD").isNullOrBlank())
        if (!configured) {
            throw GradleException(
                "Release signing is not configured. Copy keystore.properties.example to keystore.properties " +
                    "(or set MANDI_KEYSTORE_PATH, MANDI_KEYSTORE_PASSWORD, MANDI_KEYSTORE_ALIAS)."
            )
        }
    }
}
