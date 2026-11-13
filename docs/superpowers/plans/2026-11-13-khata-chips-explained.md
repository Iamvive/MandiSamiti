# Khata Chips Explained — Feature Plan (2026-11-13)

## Goal
Help users understand what the party type and account status filter chips mean by showing contextual explanations when they filter to no results.

## Context
- **Chips on Khata Ledger Screen:**
  - Party type: "All" | "किसान (Kisaan/Farmer)" | "व्यापारी (Vyapari/Buyer)"
  - Account status: "सभी (All)" | "लेना है (Receivable)" | "देना है (Payable)"
- **Current state:** No explanation shown; users who filter too narrowly see only "कोई खाता नहीं मिला" (No accounts found)
- **Desired UX:** When a filter yields 0 results, show an explainer card that teaches what each filter means, with a reset button

## Implementation Tasks

### Task 1: Build Explainer Card Component
**File:** `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/ledger/KhataFilterExplainer.kt`

**What it does:**
- Composable `KhataFilterExplainerCard(isEnglish: Boolean, onResetFilters: () -> Unit)`
- Shows a clean card with:
  - Heading: "Filters Explained" / "फ़िल्टर समझें"
  - Two sections: Party Type and Account Status
  - Each with 3 bullet points (All, Kisaan/Vyapari OR Receivable/Payable)
  - "Clear filters" button at the bottom
- Uses design tokens: MandiSurface, MandiSurfaceElevated shadow, MandiPrimaryAction button
- No horizontal scroll at 390dp

**Design:**
```
┌─────────────────────────────────┐
│ फ़िल्टर समझें                   │ (heading)
│                                 │
│ किसान या व्यापारी:              │ (section title)
│ • सभी (All): सभी खाते            │
│ • किसान: फसल बेचने वाले          │
│ • व्यापारी: फसल खरीदने वाले       │
│                                 │
│ कितना देना है/लेना है:          │ (section title)
│ • सभी (All): सभी खाते            │
│ • लेना है: हमें पैसे मिलने हैं     │
│ • देना है: हमें देने हैं          │
│                                 │
│         फ़िल्टर साफ़ करें         │ (button)
└─────────────────────────────────┘
```

**Bilingual:** Every label and explanation in both Hindi and English, toggled by `isEnglish` parameter.

### Task 2: Integrate into KhataLedgerTabScreen
**File:** `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/ledger/KhataLedgerTabScreen.kt`

**Changes:**
- At line ~318-333 (the `if (uiState.filteredParties.isEmpty() && !uiState.isLoading)` block):
  - Check if any filter is active (not ALL + ALL): `isFilterActive(uiState.activeFilter, uiState.activeTypeFilter)`
  - If active, show `KhataFilterExplainerCard` instead of just the "No accounts" text
  - Pass `onResetFilters = { onFilterSelected(PartyFilter.ALL); onTypeFilterSelected(PartyTypeFilter.ALL) }`
  - If no filter active (both are ALL), show the original "No accounts found" text

### Task 3: Helper Function
**Add to KhataLedgerTabScreen.kt:**
```kotlin
private fun isFilterActive(statusFilter: PartyFilter, typeFilter: PartyTypeFilter): Boolean {
    return statusFilter != PartyFilter.ALL || typeFilter != PartyTypeFilter.ALL
}
```

### Task 4: Unit Tests
**File:** `composeApp/src/commonTest/kotlin/com/appwork/mandisamiti/ui/ledger/KhataLedgerTabScreenTest.kt`

**Tests:**
- Empty state + ALL filters → "No accounts" text shown
- Empty state + status filter RECEIVABLE → explainer card shown
- Empty state + type filter KISAAN → explainer card shown
- Reset button clears both filters and calls `onFilterSelected(ALL)` + `onTypeFilterSelected(ALL)`
- Explainer is not shown when `filteredParties` is not empty (even if filters are active)

### Task 5: Device Verification
**Redmi d93c743, staging:**
- Login with dummy shop, create a filter combination that yields 0 results
- Verify explainer card appears (not blank screen)
- Tap "Clear Filters" button → returns to full list
- Check Light and Dark themes
- Check at 390dp width (1072x2340): no horizontal scroll, text readable, card visible

## Acceptance Criteria
- [ ] Explainer card renders with correct Hindi and English text
- [ ] Card appears only when filters are active AND result is empty
- [ ] Card does NOT appear when no filters are active OR when results exist
- [ ] Reset button works: clears filters and re-shows all parties
- [ ] No overflow at 390dp in Light and Dark
- [ ] Unit tests pass: `./gradlew jvmTest`
- [ ] Device check on Redmi `d93c743` (Light, Dark, both filters)
- [ ] `FEATURES.json` marked `passing` with device date

## Notes
- Use NGDL tokens only (`MandiSurface`, `MandiSurfaceElevated`, `MandiPrimaryAction`)
- Explanations are not verbose; 1–2 lines per bullet
- If a user has no parties at all, this card won't show (empty list check happens first in the `if` statement)
