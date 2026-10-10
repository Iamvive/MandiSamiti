# MandiSamiti — Phase 4: Multi-Device Staff Access & Premium Roles (Design Spec)

**Date:** 2026-10-10  
**Status:** Draft · Behind Feature Flag (`FLAG_MULTI_DEVICE_STAFF`)  
**Context:** Expands MandiSamiti from single-device owner mode to multi-device operations with Owner (मालिक) vs. Munim (मुनीम) roles.

---

## 1. Executive Summary & Goals

### 1.1 The Real-World Problem
During peak morning auction rush hours (6:00 AM – 11:00 AM) in North Indian grain mandis:
- The **Aadhatiya (Owner)** sits at the galla (cash desk), handles settlements, collects buyer payments, and oversees balances.
- One or two **Munims / Weighing Staff** stand at the auction floor weighing bags, recording farmer crops, and issuing kachcha slips.
- Munims need fast entry on their own phones without seeing the shop's total net worth or having permission to delete/void historical books.

### 1.2 Core Architectural Principles
1. **Feature Flag Gated (`FLAG_MULTI_DEVICE_STAFF`):** Single-device shop operation remains the zero-overhead default.
2. **Local-First, Server-Authoritative:** Both Owner and Munim record entries offline instantly. The server enforces permissions upon sync push.
3. **Audit Attribution:** Every entry revision records `created_by_user_id` and `device_id` so the owner sees exactly who entered or modified each record.
4. **Zero SQLite Migration Risk:** Local staff metadata is stored via `syncMetadataEntity` and existing SQLDelight models without altering `1.sqm` or `2.sqm`.

---

## 2. Feature Flag Specification

### 2.1 Server-Side Flag
- In `ShopProfile` model (`backend/app/models/user.py`):
  ```python
  plan_tier = Column(String, nullable=False, default="STANDARD")  # "STANDARD" | "PREMIUM"
  is_multi_device_enabled = Column(Boolean, nullable=False, default=False)
  ```
- Checked during staff invitation and staff authentication. If `False`, staff login requests return `403 Forbidden` (`{"detail": "MULTI_DEVICE_NOT_ENABLED"}`).

### 2.2 Client-Side Flag
- In `core-domain` (`com.appwork.mandisamiti.domain.model.FeatureFlag`):
  ```kotlin
  object FeatureFlags {
      fun isMultiDeviceEnabled(profile: ShopProfile?): Boolean {
          return profile?.isMultiDeviceEnabled == true
      }
  }
  ```
- In UI: When flag is disabled, the **स्टाफ / मुनीम प्रबंधन** (Staff Management) card in `SettingsScreen` displays a sleek "प्रीमियम" badge and an upgrade prompt rather than active invite inputs.

---

## 3. Role & Permissions Matrix

| Permission | Owner (मालिक / ADMIN) | Munim (मुनीम / STAFF) |
|---|:---:|:---:|
| **Enter Deals (Kachcha & Pakka)** | Full | Full |
| **Record Cash In/Out (Galla Entry)** | Full | Full |
| **Share / Print WhatsApp Slips** | Full | Full |
| **View Party Ledgers & Balances** | Full | Full |
| **Edit/Void Today's Deals** | Full | Allowed with audit reason |
| **Void Historical / Closed Deals** | Full | ❌ Blocked (Requires Owner PIN) |
| **Edit Default Commission & Rates** | Full | ❌ Hidden |
| **View Overall Shop P&L / Net Worth** | Full | ❌ Masked |
| **Add / Remove Staff Devices** | Full | ❌ Hidden |
| **Close Daily Cash Drawer (Galla)** | Full | ❌ View-only / Owner action |

---

## 4. Backend Data Models & Endpoints

### 4.1 Data Models (`backend/app/models/staff.py`)
```python
class StaffRole(str, enum.Enum):
    OWNER = "OWNER"
    MUNIM = "MUNIM"

class StaffMembership(Base):
    __tablename__ = "staff_memberships"

    id = Column(String, primary_key=True)               # UUID
    shop_id = Column(String, ForeignKey("shop_profiles.id"), nullable=False, index=True)
    user_id = Column(String, ForeignKey("users.id"), nullable=False, index=True)
    role = Column(String, nullable=False, default=StaffRole.MUNIM)
    display_name = Column(String, nullable=False)        # e.g. "राधे मुनीम"
    phone_number = Column(String(10), nullable=False)
    is_active = Column(Boolean, nullable=False, default=True)
    created_at = Column(BigInteger, nullable=False)
    revoked_at = Column(BigInteger, nullable=True)

    __table_args__ = (
        UniqueConstraint("shop_id", "phone_number", name="uq_shop_staff_phone"),
    )
```

### 4.2 API Endpoints (`/api/v1/staff`)

1. **`POST /api/v1/staff/invite` (Owner only):**
   - Body: `{phone_number: "9876543210", display_name: "राधे मुनीम", role: "MUNIM"}`
   - Verifies owner privileges and `is_multi_device_enabled`.
   - Creates `StaffMembership` in pending state.

2. **`GET /api/v1/staff` (Owner only):**
   - Returns list of active and revoked staff members for the shop.

3. **`POST /api/v1/staff/{staff_id}/revoke` (Owner only):**
   - Sets `is_active = False`, revoking all active sessions for that Munim on that shop.

4. **Staff Login Flow (`/api/v1/auth/verify-otp`):**
   - When Munim verifies OTP with an existing staff membership:
   - Server returns JWT with claims:
     ```json
     {
       "sub": "user_id_123",
       "shop_id": "shop_srv_456",
       "role": "MUNIM",
       "staff_id": "staff_789"
     }
     ```

### 4.3 Sync Validation Rules on Server (`/api/v1/sync/push`)
When applying incoming revisions:
```python
if user_role == StaffRole.MUNIM:
    if incoming_revision.change_kind == "VOID":
        # Reject void on deal older than 24 hours
        if is_historical_entry(incoming_revision.entry_id):
            raise HTTPException(status_code=403, detail="MUNIM_CANNOT_VOID_HISTORICAL")
```

---

## 5. Client (KMP / Compose Multiplatform) Implementation

### 5.1 Local Session & Role Storage
- `Session`: Add `val role: UserRole = UserRole.OWNER` and `val staffDisplayName: String?`.
- `SessionStore`: Serializes `role` in secure keystore.

### 5.2 UI Adaptations in `composeApp`
1. **`SettingsScreen.kt`:**
   - Under "दुकान व खाता" (Shop & Account), add **"मुनीम व स्टाफ" (Staff Members)**:
     - Shows active munim list with revoke button.
     - "नया मुनीम जोड़ें" (Add Munim) opens dialog for Phone Number + Name.
2. **`DashboardScreen.kt` & `DailyCashRegisterScreen.kt`:**
   - When `session.role == UserRole.MUNIM`:
     - Hide gross profit / margin tiles.
     - Hide "दरें बदलें" (Edit Rates) in Settings.
3. **Audit Attribution Badge:**
   - On `PartyLedgerScreen` and `DailyCashRegisterScreen`:
   - If entry was created by Munim, display a subtle subtitle tag:
     `दर्जकर्ता: राधे मुनीम` (Created by: Radhe Munim).

---

## 6. Implementation Phases (Step-by-Step)

### Wave 1: Server Models & Flag (`backend/`)
- [ ] Add `plan_tier` and `is_multi_device_enabled` to `ShopProfile`.
- [ ] Create `StaffMembership` table and pytest unit tests.
- [ ] Add `/api/v1/staff/invite` and `/api/v1/staff/revoke` endpoints.
- [ ] Enforce role checks in `sync_push.py`.

### Wave 2: Domain & Data Contracts (`core-domain` & `core-data`)
- [ ] Add `UserRole` enum (`OWNER`, `MUNIM`) and `StaffMember` model.
- [ ] Update `Session` to hold `UserRole`.
- [ ] Add `StaffRepository` interface and offline-first implementation.

### Wave 3: Compose UI & Feature Flag Wiring (`composeApp`)
- [ ] Add `StaffManagementScreen` / Settings integration behind `FLAG_MULTI_DEVICE_STAFF`.
- [ ] Add role-based visual gates (masking sensitive financial metrics for Munim).
- [ ] Add creator attribution in ledger items.

### Wave 4: Integration & E2E Testing
- [ ] Test multi-device sync with 1 Owner device + 1 Munim device on test server.
- [ ] Verify Munim cannot void older deals.
- [ ] Verify revoking Munim immediately revokes device sync access.

---

## 7. Rollout Safety Guarantee
- **Feature Flag Default:** `FLAG_MULTI_DEVICE_STAFF = false`.
- **Zero Impact on Current Users:** Existing installations and the upcoming family shop trial in Mathura run as single-owner standard mode without any overhead or UI complexity.
