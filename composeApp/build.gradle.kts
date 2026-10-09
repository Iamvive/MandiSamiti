import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

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
    sourceSets.getByName("debug").manifest.srcFile("src/androidDebug/AndroidManifest.xml")

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", "\"https://mandi-api.appworx.co.in\"")
        }
        getByName("debug") {
            val localProps = Properties().apply {
                rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
            }
            val debugUrl = localProps.getProperty("mandi.apiBaseUrl") ?: "http://10.0.2.2:8000"
            buildConfigField("String", "API_BASE_URL", "\"$debugUrl\"")
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
