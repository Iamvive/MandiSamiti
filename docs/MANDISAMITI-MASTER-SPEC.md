# MandiSamiti — Master Architecture, Feature Roadmap & Universal AI Specification
<!-- Conforms to NGDL v1.2, Cross-AI One-Brain Protocol, and VoltAgent/awesome-design-md -->

**Product:** MandiSamiti (मंडी समिति — आढ़त बही-खाता)  
**Package:** `com.appwork.mandisamiti`  
**Platforms:** Android (Primary), Desktop (JVM), iOS (KMP Framework), Web (FastAPI Backend)  
**Target Demographic:** Indian Agricultural Traders (Aadhatis) & Farmers (Age 25 – 65)  
**Design Standard:** NoGravity Design Language (NGDL v1.2)  
**Last Updated:** 2026-10-07  

---

## 1. Product Vision & Accessibility Guardrails (Age 25–65)

MandiSamiti is an enterprise-grade, high-trust accounting and trade execution system designed specifically for **Grain & Agricultural Markets (अनाज मंडी)** in India (e.g., Mathura, Aligarh, Hathras, Bharatpur).

### 🎯 25–65 Age Group Ergonomics:
1. **High-Legibility Typography:** Large, readable numbers and clear Devanagari labels (minimum 14–16sp for body, 22–28sp for financial balances).
2. **Generous Touch Targets:** All clickable cards, buttons, and keypad elements have a minimum **48dp × 48dp** touch area.
3. **Monochromatic NGDL Purity:** High contrast (≥ 4.5:1), eliminating confusing rainbow colors. Visual hierarchy relies on elevation, subtle borders, and semantic indicators (`#DC2626` for Receivable, `#16A34A` for Payable).
4. **Voice Soundbox Audio Feedback:** Real-time vernacular TTS announcements for every transaction so elderly Aadhatis have 100% confidence without squinting at the screen.
5. **Frictionless Navigation:** Zero hidden deep menus; a 4-tab bottom navigation with plain-language labels.

---

## 2. Universal Feature Matrix & Implementation Status

| Feature Module | Description | Age 25–65 UX Solution | Status | Target Phase |
| :--- | :--- | :--- | :--- | :--- |
| **NGDL v1.2 Design System** | Pure Alabaster / Space Black monochromatic theme | High contrast, zero emoji clutter | ✅ **Live on Phone** | Phase 0 (Done) |
| **Paisa-Precise Math Core** | Decimal-free integer arithmetic for all mandi calculations | 100% mathematical honesty | ✅ **Live on Phone** | Phase 0 (Done) |
| **Exact Money Math** | Half-up rounding, signed farmer payables, paisa-safe rate edits | No silent zeroing or lost paise | 🟡 **Done in code, phone test pending** | Phase 1A |
| **Voice Soundbox (TTS)** | Vernacular audio playback for settlements | Audio reassurance for busy/senior users | ✅ **Live on Phone** | Phase 0 (Done) |
| **WhatsApp Slip Export** | 1-tap instant WhatsApp bill & receipt delivery | Pre-formatted Hindi slips for farmers | ✅ **Live on Phone** | Phase 0 (Done) |
| **4-Tab Navigation Bar** | Screen decomposition (Dashboard, Khata, Galla, Settings) | Prevents single-screen overcrowding | ✅ **Live on Phone** | Phase 1 (Done) |
| **Language Switcher (हिन्दी/EN)** | Dynamic runtime toggle between Hindi & English | Easy switch for younger vs senior users | 🟡 **UI only** (not persisted) | Phase 1 |
| **Authentication & Sign Out** | Phone + 4-digit MPIN, Session Store & Sign Out | Simple PIN memory, no complex passwords | 🟡 **UI only** (fake OTP) | Plan 1C |
| **Multi-Shop Management** | Switch between multiple mandi licenses/firms | 1-tap dropdown in Settings | ⚪ **Planned** | Phase 2 |
| **Entry Deletion & Void Audit** | Soft-delete / Void with Reason & instant balance reversal | Mistake protection with audit trail | 🟡 **Done in code, phone test pending** | Phase 1A |
| **Camera Slip OCR / Attachment**| Attach or scan physical weighbridge slips (कांटा पर्ची) | Photo capture with thumbnail preview | 🟡 **Drafted** | Phase 3 |
| **FastAPI + PostgreSQL Sync** | VPS Backend synchronization (:8050) | Cloud backup for local SQLite DB | ⚪ **Not connected** | Plans 1B/1C |

---

## 3. Information Architecture (4-Hub Decomposition)

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                     MANDISAMITI CORE                                   │
├───────────────────┬────────────────────┬───────────────────────┬───────────────────────┤
│   📊 Dashboard    │   👥 Khata Ledger  │   💵 Galla Register   │   ⚙️ Settings/Profile │
│   (आज का व्यापार)  │    (पार्टी बहीखाता)  │     (रोकड़ बही)       │    (प्रोफाइल व भाषा)  │
├───────────────────┼────────────────────┼───────────────────────┼───────────────────────┤
│ • Today's Volume  │ • Farmer Accounts  │ • Cash in Drawer      │ • Language (HI / EN)  │
│ • Total Turnover  │ • Buyer Accounts   │ • Cash In (जमा)       │ • Sign Out / Lock     │
│ • Net Receivable  │ • Search & Filter  │ • Cash Out (निकासी)   │ • Voice Soundbox Vol  │
│ • Net Payable     │ • WhatsApp Khata   │ • Day Closing Tally   │ • Switch Shop License │
│ • Quick New Deal  │ • Add Party (+FAB) │ • Petty Cash History  │ • Cloud Backup Status │
└───────────────────┴────────────────────┴───────────────────────┴───────────────────────┘
```

---

## 4. Entry Deletion & Correction Standard (Audit Integrity)

In traditional Mandi Aadhat, transactions cannot simply "disappear" without an audit record:

1. **Soft-Delete / Void Mechanism:**
   - Any transaction (Deal, Cash In, Cash Out, Settlement) has a `isVoid: Boolean` and `voidReason: String?`.
   - When user taps **"हटाएं / रद्द करें"**, a bottom sheet requests confirmation and a simple reason (*तौल त्रुटि*, *गलत प्रविष्टि*, *सौदा निरस्त*).
2. **Instant Balance Recalculation:**
   - The party's running balance (`balancePaisa`) and daily cash drawer (`inHandCashDrawerPaisa`) are automatically recalculated immediately via SQLDelight reactive queries.
3. **Visual Distinction:**
   - Voided entries appear in ledger with strikethrough and a muted status tag (`रद्द प्रविष्टि`), or are hidden under an "Unvoid / View Cancelled" toggle.

---

## 5. Domain Mathematical Formulas (Exact Standards)

All calculations in `com.appwork.mandisamiti.domain.math.MandiMathEngine` use 64-bit integer Paisa (`Long`):

$$\text{Net Weight (Quintals)} = \text{Gross Weight} - \left(\frac{\text{Bags} \times \text{Cut per Bag (Kg)}}{100}\right)$$

$$\text{Gross Crop Value} = \frac{\text{Net Weight in Kg} \times \text{Rate per Quintal}}{100}$$

$$\text{Farmer Final Payable} = \text{Gross Crop Value} - \text{Mandi Labor} - \text{Bardana} - \text{Advance (बयाना)}$$

$$\text{Buyer Final Receivable} = \text{Gross Value} + \text{Aadhat Commission (e.g. 2.5\%)} + \text{Mandi Tax (e.g. 1.5\%)}$$

---

## 6. Multi-AI Collaboration Protocol

When any AI assistant (Antigravity, Claude Code, Codex, OpenCode, Hermes) works on MandiSamiti:
1. **Rule §1:** Run `./gradlew test` before making assertions.
2. **Rule §2:** Strictly adhere to NGDL v1.2 tokens in `ui/theme/Color.kt` (No raw emojis in UI strings/icons; use Compose vector ImageVectors).
3. **Rule §3:** Record newly completed features in this document under Section 2 and synchronize the Obsidian Second Brain (`python3 scripts/project_update.py --path projects/MandiSamiti`).
