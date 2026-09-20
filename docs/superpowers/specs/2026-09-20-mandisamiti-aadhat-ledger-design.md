# MandiSamiti (मंडी समिति - आढ़त बही खाता) — Product & Technical Design Spec

**Date:** 2026-09-20  
**Target:** Kotlin Multiplatform (KMP) + Compose Multiplatform (CMP)  
**Primary Platforms:** Android (First), iOS & Desktop (Near-future ready)  
**Primary Persona:** Anaj Mandi Aadhat Shop Owner ("लाला जी") & Staff (40–60 years old, Mathura, India)

---

## 1. Executive Summary & Relationship-Driven Domain Context

**MandiSamiti** is a specialized, offline-first cash ledger and mandi settlement app built for commission agents (*Aadhatiyas*) in agricultural grain markets.

### 1.1 "संबंध और व्यवहार" (Relationship-Driven Business Rules)
Mandi aadhat is fundamentally built on multi-generational relationships (*व्यवहार और संबंध*), mutual trust, and negotiations:
- **Full Retrospective Editability**: Rates, weights, deductions, and payments can be edited, renegotiated, or adjusted at any point (e.g. buyer requests a quality concession after unloading at the flour mill). All running balances and daily summaries recalculate dynamically.
- **Rural Monthly Interest Calculation (देसी ब्याज / सैकड़ा दर)**: Advances given to farmers for seeds/fertilizers often carry a simple monthly interest rate (e.g., ₹1.50 or ₹2.00 per ₹100/month — *₹1.50 सैकड़ा प्रति माह*). The app automatically calculates accrued interest with 1-tap ledger addition or interest waiver/discount (*ब्याज छूट / समझौता*).
- **Audit & Version History**: Every edit updates `updated_at`, triggers a delta sync event, and re-announces the revised balance.

### 1.2 Persona Constraints & Behavioral Patterns
- **Primary User**: 40–60 year old shop owners with limited formal tech familiarity; deeply comfortable with physical calculators, paper *Bahi-Khatas*, and WhatsApp.
- **Physical Context**: High outdoor glare in open mandi sheds, dust on fingers, loud background noise during auctions, rush hours between 6 AM and 11 AM.
- **Visual & Motor Needs**: Extra large high-contrast text (accommodating reading glasses / presbyopia), minimum 56dp touch targets, zero tiny icons without text.
- **Zero Keyboard Disruption**: No soft QWERTY keyboard popups for calculations; uses an oversized bottom **Mandi Calculator Keypad**.
- **Auditory Trust ("App Soundbox")**: Spoken confirmation in clear, conversational Hindi upon saving transactions (similar to Paytm/PhonePe soundbox behavior) to reassure both the trader and the farmer standing at the desk.

---

## 2. Core User Experience & UI/UX Pro Max Design Principles

### 2.1 Visual Hierarchy & Palette
- **Brand Navy**: `#0F172A` (Header & Structure)
- **Market Credit (लेना है / बाकी / लाल)**: High-Visibility Crimson `#DC2626`
- **Farmer Deposit (देना है / जमा / हरा)**: Deep Emerald Green `#15803D`
- **Surface / Card**: Pure White `#FFFFFF` with `#CBD5E1` (1.5dp border) for maximum sunlight contrast.
- **Typography**: Bundled Google Fonts *Noto Sans Devanagari* & *Mukta*.
  - Body: Minimum 16sp.
  - Headings & Party Names: 20sp–24sp Bold.
  - Currency & Weight: 28sp–34sp Ultra-Bold.

### 2.2 Soundbox Engine & Audio Trust
- **In-App Text-To-Speech (TTS)**: Built-in multiplatform speech synthesis speaking aloud in clear standard conversational Hindi upon saving:
  - *Grain Settlement*: *"मंडी समिति: [किसान का नाम] का [वजन] क्विंटल [फसल], शुद्ध हिसाब [रकम] रुपये दर्ज हुआ।"*
  - *Cash Received (जमा)*: *"[पार्टी नाम] से [रकम] रुपये जमा प्राप्त हुए।"*
  - *Cash Advance (उधार)*: *"[पार्टी नाम] को [रकम] रुपये उधार दिए गए।"*
  - *Edit/Update*: *"[पार्टी नाम] का संशोधित हिसाब [रकम] रुपये अपडेट हुआ।"*
- **Persistent Top-Bar Speaker Toggle**: A prominent button on every top bar (`[ 🔊 आवाज़ चालू ]` / `[ 🔇 आवाज़ बंद ]`) for instant 1-tap toggling without opening settings.
- **Replay Audio**: A dedicated `[ 🗣️ दोबारा सुनें ]` button on the receipt screen.

---

## 3. Product Features & Workflows

### 3.1 Two-Stage Deal Lifecycle (आवक से पक्का पर्चा)
1. **Stage 1: आवक व तौल (Morning Arrival & Weigh-in)**
   - Select Farmer -> Select Commodity -> Enter Bags count -> Enter Weight (Quintals/Kg) -> Snap handwritten scale slip photo (`[ 📷 कच्ची पर्ची ]`).
   - Saved as `PENDING_SETTLEMENT` (कच्चा पर्चा).
2. **Stage 2: नीलामी व अंतिम पर्चा (Auction Settlement)**
   - Select Buyer (व्यापारी) -> Enter Auction Rate (₹/Quintal) -> Enter Manual Deductions (Commission, Labour, Weighing, Cut) -> Auto-computes net farmer payable and net buyer receivable -> Saved as `SETTLED` (पक्का पर्चा) + In-app voice announcement.
3. **Post-Settlement Adjustments / Edit Mode**:
   - Any completed deal can be opened and edited (adjust rate, change cut, edit weight, re-assign buyer).
   - Recalculates both parties' balances in real-time.

### 3.2 Two-Sided Party Ledger (किसान व व्यापारी खाता)
- Unified Party Model: `FARMER` (विक्रेता) and `BUYER` (खरीदार).
- Real-time running balance:
  - **🔴 लेना है (Receivable / Debit)**: Money owed to the shop.
  - **🟢 देना है (Payable / Credit)**: Money the shop owes the party.

### 3.3 Rural Interest & Settlement Engine (देसी ब्याज व छूट)
- **Monthly Simple Interest Rate (सैकड़ा दर %)**: Configurable per party (e.g., 1.5% or 2% per month on outstanding credit).
- **Accrued Interest Calculator**: Shows elapsed days/months and accumulated interest in real-time.
- **1-Tap Actions**:
  - `[ ➕ ब्याज खाते में जोड़ें ]` -> Posts an interest charge transaction.
  - `[ 🤝 छूट / समझौता (Waiver) ]` -> Records a discount/waiver to settle old accounts cleanly.

### 3.4 Quick Cash In / Out (रोकड़ लेन-देन)
- **🔴 रुपये दिए / उधार (Cash Out / Advance)**: 1-tap advance entry to a farmer with optional voice note.
- **🟢 रुपये मिले / जमा (Cash In / Payment)**: 1-tap settlement received from a buyer or farmer.

### 3.5 Daily Cash Register (दैनिक गल्ला रोकड़)
- 1-tap Day-End balance summary:
  - Total Cash Inflow (आज कुल नकद आया)
  - Total Cash Outflow (आज कुल नकद गया)
  - Total Volume Traded by Commodity (कुल बिका माल - क्विंटल/बोरी)
  - Net Physical Cash in Drawer (गल्ला रोकड़ शेष)

### 3.6 Branded WhatsApp Receipt Generator
- Generates high-contrast digital receipt images formatted like traditional mandi paper slips.
- 1-tap WhatsApp sharing directly to farmer/buyer phone numbers.
- Vernacular-ready architecture: bundled Hindi & English string resources (`Res.string.*`) structured for easy addition of regional Indian languages.

### 3.7 Shop Setup & Security
- Shop Name + Mandi Location + Owner Phone Number + 4-digit numeric Security PIN.

---

## 4. Technical Architecture & Scalability

### 4.1 Module Structure
```
MandiSamiti/
├── composeApp/                 # Compose Multiplatform UI (Android + iOS + Desktop)
│   ├── commonMain/             # Shared UI, ViewModels, Theme, Devanagari Strings
│   │   ├── tts/                # Expect SoundboxManager (Text-to-Speech)
│   │   ├── camera/             # Expect CameraPicker (Paper slip capture)
│   │   └── share/              # Expect WhatsAppShareManager (Receipt image sharing)
│   ├── androidMain/            # Android TTS, CameraX / ActivityResult, SpeechRecognizer
│   └── iosMain/                # iOS AVSpeechSynthesizer, UIImagePickerController
├── core-domain/                # Pure Kotlin domain logic (Models, UseCases, Scaled Math, Byaj Engine)
├── core-database/              # SQLDelight schema, DAOs, DriverFactory
└── core-data/                  # Repository implementations & SyncEngine outbox
```

### 4.2 Distributed Data & Scalability Contract
- **Local-First & Offline-Ready**: SQLDelight handles all queries locally with zero latency.
- **Client-Generated UUIDs**: Prevents ID collisions when syncing across multiple devices in the future.
- **Monotonic UTC Timestamps**: `created_at` and `updated_at` (epoch ms) for delta sync (`GET /sync?since=...`).
- **Soft Deletes**: `is_deleted INTEGER DEFAULT 0` to propagate deletes across distributed clients.
- **Outbox Sync Engine**: Mutations marked with `sync_status = 0 (PENDING)`. Background worker pushes batches to remote backend and marks `1 (SYNCED)`.
- **Multi-Tenant Ready**: All tables partitioned by `shop_id`.
- **Precision Scaled Arithmetic**: Currency stored in **Paisa (Int/Long)** and weight in **Grams (Int/Long)** to eliminate floating-point rounding bugs.

---

## 5. SQLDelight Database Schema

```sql
-- Shop Profile / Multi-Tenancy
CREATE TABLE shop_profile (
    id TEXT PRIMARY KEY NOT NULL,
    shop_name TEXT NOT NULL,
    owner_name TEXT NOT NULL,
    mandi_name TEXT NOT NULL,
    shop_number TEXT,
    phone_number TEXT NOT NULL,
    pin_hash TEXT NOT NULL,
    default_monthly_interest_rate REAL NOT NULL DEFAULT 1.5, -- % प्रति माह (सैकड़ा)
    is_sound_enabled INTEGER NOT NULL DEFAULT 1, -- Soundbox toggle state
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    sync_status INTEGER NOT NULL DEFAULT 0
);

-- Party Master (Farmers & Buyers)
CREATE TABLE party (
    id TEXT PRIMARY KEY NOT NULL,
    shop_id TEXT NOT NULL,
    name TEXT NOT NULL,
    phone TEXT,
    village TEXT,
    party_type TEXT NOT NULL, -- 'FARMER' | 'BUYER'
    monthly_interest_rate REAL, -- Party-specific override %
    photo_uri TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    sync_status INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(shop_id) REFERENCES shop_profile(id)
);

-- Commodity Master
CREATE TABLE commodity (
    id TEXT PRIMARY KEY NOT NULL,
    shop_id TEXT NOT NULL,
    name_hi TEXT NOT NULL,
    name_en TEXT NOT NULL,
    default_unit TEXT NOT NULL DEFAULT 'QUINTAL',
    is_active INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    sync_status INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(shop_id) REFERENCES shop_profile(id)
);

-- Deal / Crop Sale (Two-Stage Settlement with Full Editability)
CREATE TABLE deal (
    id TEXT PRIMARY KEY NOT NULL,
    shop_id TEXT NOT NULL,
    farmer_id TEXT NOT NULL,
    buyer_id TEXT,               -- Nullable in Stage 1 (Pending Settlement)
    commodity_id TEXT NOT NULL,
    deal_status TEXT NOT NULL,   -- 'PENDING_SETTLEMENT' | 'SETTLED' | 'CANCELLED'
    deal_date INTEGER NOT NULL,
    
    -- Weight details
    bags_count INTEGER NOT NULL,
    gross_weight_grams INTEGER NOT NULL,
    cut_weight_grams INTEGER NOT NULL DEFAULT 0,
    net_weight_grams INTEGER NOT NULL,
    
    -- Pricing details (Stage 2)
    rate_paisa_per_unit INTEGER, 
    gross_amount_paisa INTEGER NOT NULL DEFAULT 0,
    
    -- Manual Deductions (Paisa)
    farmer_commission_paisa INTEGER NOT NULL DEFAULT 0,
    buyer_commission_paisa INTEGER NOT NULL DEFAULT 0,
    labour_charge_paisa INTEGER NOT NULL DEFAULT 0,
    weighing_charge_paisa INTEGER NOT NULL DEFAULT 0,
    other_deductions_paisa INTEGER NOT NULL DEFAULT 0,
    
    -- Final Settlements
    net_farmer_payable_paisa INTEGER NOT NULL DEFAULT 0,
    net_buyer_receivable_paisa INTEGER NOT NULL DEFAULT 0,
    
    receipt_photo_uri TEXT,      -- Camera photo of physical handwritten slip
    voice_note_uri TEXT,
    remarks TEXT,
    
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    sync_status INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(farmer_id) REFERENCES party(id),
    FOREIGN KEY(buyer_id) REFERENCES party(id),
    FOREIGN KEY(commodity_id) REFERENCES commodity(id)
);

-- Direct Cash, Interest & Discount Transactions (उधार / जमा / ब्याज / छूट)
CREATE TABLE cash_transaction (
    id TEXT PRIMARY KEY NOT NULL,
    shop_id TEXT NOT NULL,
    party_id TEXT NOT NULL,
    deal_id TEXT,                -- Optional link to a specific deal
    -- transaction_type:
    -- 'UDHAR_GIVEN'   (रुपये दिए / पेशगी)
    -- 'JAMA_RECEIVED' (रुपये मिले / अदायगी)
    -- 'INTEREST_ADDED'(ब्याज जोड़ा)
    -- 'DISCOUNT_GIVEN'(छूट / रियायत / समझौता)
    transaction_type TEXT NOT NULL, 
    amount_paisa INTEGER NOT NULL,
    payment_mode TEXT NOT NULL DEFAULT 'CASH', -- 'CASH', 'UPI', 'BANK', 'BOOK_ENTRY'
    transaction_date INTEGER NOT NULL,
    voice_note_uri TEXT,
    remarks TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    sync_status INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(party_id) REFERENCES party(id),
    FOREIGN KEY(deal_id) REFERENCES deal(id)
);
```

---

## 6. Verification & Testing Plan

1. **Unit & Domain Tests (`core-domain`)**:
   - Mandi mathematical calculation engine (Bags, Grams, Paisa, Gross, Net Payouts).
   - Rural Simple Monthly Interest Calculator (सैकड़ा दर math across calendar days/months).
   - Running ledger balance computations for both farmers and buyers across partial payments, discounts, interest, and edited deals.
2. **Database Integration Tests (`core-database`)**:
   - SQLDelight migration tests, CRUD queries, soft-delete filtering, and running balance views.
3. **UI / Compose Multiplatform Tests (`composeApp`)**:
   - Calculator keypad input state machine (bags -> weight -> rate -> deductions).
   - Deal edit and recalculation workflows.
   - Localization rendering across Hindi and English locales.
   - Text-to-Speech soundbox voice prompt builder and top-bar toggle state.
   - WhatsApp slip image composition and sharing intent.
