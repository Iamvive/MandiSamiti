# 🌾 MandiSamiti (आढ़त रोकड़ बही)

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.7.3-purple.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Desktop%20(JVM)%20%7C%20iOS-green.svg)]()
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**MandiSamiti** is an offline-first, multiplatform financial ledger and transaction platform tailored for agricultural commission agents (*आढ़ती / मंडी व्यापारी*) across Indian agricultural mandis (APMC markets).

Engineered to FAANG Tier-1 enterprise fintech design standards, it replaces paper ledger books (*बही-खाता*) and clumsy spreadsheets with integer-precision arithmetic, real-time voice speech soundbox announcements, and 1-tap WhatsApp digital bills.

---

## 🌟 Core Features

### 1. ⚖️ 2-Stage Grain Deal Lifecycle (*कच्चा पर्चा ➔ पक्का सौदा*)
- **Stage 1 (आवक व धर्मकांटा तौल)**: Instant gross weight, bag count, and tare deduction (*बारदाना काट*) calculation in integer grams. Saved immediately as `PENDING_SETTLEMENT`.
- **Stage 2 (मंडी खुली बोली व नीलामी भाव)**: Settles auction winning bid (`₹/Qtl`), itemized commission (आढ़त 2%), Mandi cess (1.5%), and labour charges (*हम्माली / पल्लेदारी*).
- **Exact Integer Math**: Zero floating-point loss (`Long` paisa and gram representation).

### 2. 🔊 In-App Soundbox Voice Engine (*लाउडस्पीकर आवाज़*)
- Real-time Hindi voice announcements for every saved deal, weighment, cash in (*जमा*), and cash out (*भुगतान*).
- **1-Tap Top Bar Toggle**: `[ 🔊 वॉइस ऑन / 🔇 म्यूट ]` with persistent preference.
- Built-in native speech engines for Android (TTS), Desktop/Mac (native `say` / `Lekha` voice), and iOS (`AVSpeechSynthesizer`).

### 3. ⌨️ Dedicated Mandi Calculator Keypad
- Integrated bottom numerical keypad (`1-9, ., 00, C, OK`) eliminating intrusive OS QWERTY keyboard popups.

### 4. 📈 Desi Simple Interest Engine (*सैकड़ा दर ब्याज*)
- Calculates traditional monthly interest rates (e.g. ₹1.50 प्रति ₹100 प्रति माह) on a daily pro-rata basis (`Interest = Principal * (Rate/100) * (Days/30)`).

### 5. 📲 1-Tap WhatsApp Digital Bill Slip (*डिजिटल कच्ची/पक्की पर्ची*)
- Automatically formats Unicode-styled Hindi receipts with itemized weighment, deductions, and shop branding.

### 6. 💵 Daily Cash Register (*दैनिक गल्ला रोकड़ बही*)
- Real-time cash drawer tracking: `Opening Balance + Cash In (जमा) - Cash Out (निकासी) = In-Hand Cash Drawer`.

---

## 🏗️ Multiplatform Architecture

```
MandiSamiti/
├── core-domain/       # Pure Kotlin business rules, MandiMathEngine, RuralInterestEngine
├── core-database/     # SQLDelight offline SQLite schema, queries, multiplatform driver factories
├── core-data/         # Reactive Flow repositories, offline outbox synchronization (SyncEngine)
└── composeApp/        # Compose Multiplatform UI, ViewModels, Theme Tokens, Platform Bridges (TTS, Camera, Share)
    ├── commonMain/    # Shared Compose UI screens, components, viewmodels
    ├── jvmMain/       # Desktop JVM entry point & macOS native speech bridge
    ├── androidMain/   # Android Launcher Activity & Android TextToSpeech bridge
    └── iosMain/       # iOS UIViewController & AVSpeechSynthesizer bridge
```

---

## 🚀 Getting Started

### Prerequisites
- JDK 17 or higher
- Android Studio / IntelliJ IDEA with Compose Multiplatform Plugin

### Run on Desktop (Mac / Linux / Windows)
```bash
./gradlew :composeApp:run
```

### Run Live Interactive CLI Simulation
```bash
./gradlew -q :composeApp:runDemo
```

### Run on Android Device / Emulator
```bash
./gradlew :composeApp:installDebug
```

### Run Multiplatform Test Suite
```bash
./gradlew jvmTest testDebugUnitTest
```

---

## 🛡️ License
Distributed under the MIT License. Built with ❤️ for Indian Agricultural Mandis.
