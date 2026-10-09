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

| Feature Module | Description | Age 25–65 UX Solution | Status | Phase |
| :--- | :--- | :--- | :--- | :--- |
| **NGDL v1.2 Design System** | Pure Alabaster / Space Black monochromatic theme | High contrast (≥ 4.5:1), pure vector icons, zero emoji clutter | ✅ **Live on Phone** | Phase 0 (Done) |
| **Paisa-Precise Math Core** | Decimal-free 64-bit integer arithmetic for all mandi calculations | 100% mathematical honesty, half-up rounding, signed farmer payables | ✅ **Live on Phone** | Phase 1A (Done) |
| **Audit Trail & Soft Void** | Schema v2 `entryRevisionEntity` append-only revisions with `VoidReason` | Mistake protection with transparent audit log & balance reversal | ✅ **Live on Phone** | Phase 1A (Done) |
| **Daily Cash Drawer (Galla)** | Carried-forward opening balance, cash-only mode, excluded voids | Accurate daily cash reconciliation, no all-time leakage | ✅ **Live on Phone** | Phase 1A (Done) |
| **Zero Demo Data Seeding** | Pure production cleanliness, removed fake demo party seeding | No unwanted demo parties polluting live farmer books | ✅ **Live on Phone** | Phase 1A (Done) |
| **Backend Schema v2 Parity** | FastAPI models aligned with 64-bit Long paisa/grams & `EntryRevision` sync | Multi-tenant shop isolation, no float conversion issues | ✅ **Verified (Pytest)** | Phase 1B (Done) |
| **Backend Auth Security** | OTP rate limiting (max 3 / 5m), 5-failure lockout, Pydantic BaseSettings | Protection against brute-force and credential abuse | ✅ **Verified (Pytest)** | Phase 1B (Done) |
| **Authentication & Sign Out** | OTP, MPIN login with attempts-left, server sessions, sign-out blocked while entries are unsynced | No fake OTP or plaintext MPIN; no silent data loss on sign-out | 🟡 **Phone-verified (static OTP, local server)**: signup, relaunch to Home, sign-out wipe, login and blocked sign-out all passed on the Redmi; open: sign-out Snackbar text contrast ~2:1 in light theme, dark theme not verifiable on MIUI, real OTP sender pending | Accounts plan (Task 11) |
| **Server-issued shop id** | Shop id is a server UUID returned by signup/login; `shop_default` removed | One shop identity across devices | ✅ **Phone-verified (static OTP, local server)**: 36-char UUID in `shopProfileEntity`, same id after re-login | Accounts plan (Task 11) |
| **KMP Offline Sync Engine** | Two-way Outbox sync (`SyncEngine`, `MandiSyncApiClient`, DTOs) | Seamless offline entries with automatic background sync | ✅ **Live in KMP** | Phase 1C (Done) |
| **Sync Status UI Indicators** | Single vector check (offline local), double check (synced cloud), pending badge | Instant visual trust without technical jargon | ✅ **Live in KMP** | Phase 1C (Done) |
| **Day Closing Reconciliation** | Galla Day Closing summary dialog (दैनिक रोज़नामा) with physical count & discrepancy calc | Clear tally ("हिसाब बराबर", "फालतू", "कमी") before closing | ✅ **Live in KMP** | Phase 2 (Done) |
| **WhatsApp Day Closing Share** | Pre-formatted Hindi daily register summary via `WhatsAppShareManager` | 1-tap sharing of daily business tally with owners/partners | ✅ **Live in KMP** | Phase 2 (Done) |
| **Soundbox Voice Playback** | Vernacular TTS voice announcement for deals and day closing totals | Audio reassurance for senior Aadhatis during busy rush | ✅ **Live in KMP** | Phase 2 (Done) |
| **Soundbox Dialects & Replay** | Multi-dialect voice templates (Hindi, Braj/Desi Mandi, English) + instant replay | Customized dialect preference & auditory verification | ⏳ **In Progress** | Phase 3 |
| **Multi-Device Staff Roles** | Owner (Full) vs Munim (Entry Only) vs Partner (Read Only) role guards | Premium multi-staff collaboration on separate devices | ⚪ **Planned** | Phase 4 |
| **Official Mandi Papers (J-Form)**| Generate and export official J-Form (नीलामी पर्ची) & mandi return PDFs | Legal compliance and formal proof for mandi samiti tax | ⚪ **Planned** | Phase 5 |
| **Camera Slip Attachment** | Attach or scan physical weighbridge slips (कांटा पर्ची) | Photo capture with thumbnail preview | ⚪ **Planned** | Phase 6 |

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
│ • Sync Status Ticks│ • Sync Status Ticks│ • Discrepancy Calc   │ • Multi-Staff Roles   │
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
4. **Append-Only Sync Engine:**
   - Revisions are tracked in `entryRevisionEntity` and synced as delta operations to backend `EntryRevision` table.

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
1. **Rule §1:** Run `./gradlew test` (or `./gradlew jvmTest`) before making assertions. All 55 test suites must pass.
2. **Rule §2:** Strictly adhere to NGDL v1.2 tokens in `ui/theme/Color.kt` (No raw emojis in UI strings/icons; use Compose vector ImageVectors).
3. **Rule §3:** Record newly completed features in this document under Section 2 and synchronize the Obsidian Second Brain (`vaults/Vivek-K/wiki/concepts/mandisamiti-product-review-2026-10-08.md` and `exports/context-pack.md`).
