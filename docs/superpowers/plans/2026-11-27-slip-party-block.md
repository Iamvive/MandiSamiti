# Slip Party Block Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to run this plan task by task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** On Buy and Sell slips, show a read-only line for a saved account and show name and village boxes only for a new one, without losing party data.

**Architecture:** All new rules and the three small composables live in one file, `ui/components/PartyBlock.kt`, shared by both screens. Form states and resolvers stay as they are; the screens only decide what to show and clear stale values.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Material 3, kotlin.test (jvmTest).

**Spec:** `docs/superpowers/specs/2026-11-27-slip-party-block-design.md`. Read it first.

## Global Constraints

- Repo root is `projects/MandiSamiti`. Read `CLAUDE.md` there first.
- Branch `feat/slip-party-block` from `origin/main`. Never push `main`. Never force-push. Do not merge the PR.
- Mobile number stays compulsory (10 digits). Name and village stay required.
- No schema, migration, sync, backend or receipt change. Udhaar and Lend screens do not change.
- Colours only from `ui/theme/Color.kt`. `ImageVector` icons. No emoji in UI. Text contrast 4.5:1 or higher.
- Every UI string has English and Hindi, chosen by `isEnglish`.
- Device tests use the staging API URL (`mandi.apiBaseUrl` in `local.properties`), never `10.0.2.2`. No audio. No real user data.
- Do not print or commit secrets. Do not commit `local.properties`.
- Do not update `vaults/Vivek-K/wiki/hot.md` or the submodule pointer. That waits for Vivek's approval.

Short paths below: `UI = composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui`.

---

### Task 0: Branch

- [ ] **Step 1**

```bash
cd /Users/appworx/Desktop/Vivek-K/projects/MandiSamiti
git status --short   # expect only the new spec and plan under docs/superpowers/
git fetch origin && git switch -c feat/slip-party-block origin/main
git add docs/superpowers/specs/2026-11-27-slip-party-block-design.md docs/superpowers/plans/2026-11-27-slip-party-block.md
git commit -m "docs: slip party block design and plan"
./init.sh
```

If the tree holds other changes you did not make, or tests fail, stop and tell Vivek.

---

### Task 1: Party block rules

**Files:**
- Create: `UI/components/PartyBlock.kt`
- Test: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/components/PartyBlockTest.kt`

**Interfaces (produced, used by Tasks 3 and 4):**
- `sealed interface PartyBlockState { Lookup; Saved(party: Party); New }`
- `partyBlockState(parties: List<Party>, selectedId: String?, phone: String, newRequested: Boolean): PartyBlockState`
- `isSelectionStale(parties: List<Party>, selectedId: String?, newPhone: String): Boolean`
- `samePersonCandidate(parties: List<Party>, selectedId: String?, phone: String, name: String, village: String): Party?`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.appwork.mandisamiti.ui.components

import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PartyBlockTest {
    private fun party(id: String, name: String, phone: String?, village: String? = "TEST गाँव") =
        Party(id, "shop", name, phone, village, PartyType.FARMER, createdAt = 1, updatedAt = 1)

    private val ramesh = party("p1", "TEST रमेश", "9876543210")
    private val noPhone = party("p2", "TEST सुरेश", null)
    private val all = listOf(ramesh, noPhone)

    @Test
    fun emptyFormShowsOnlyLookup() {
        assertEquals(PartyBlockState.Lookup, partyBlockState(all, null, "", false))
        assertEquals(PartyBlockState.Lookup, partyBlockState(all, null, "98765", false))
    }

    @Test
    fun matchingNumberOrPickShowsSaved() {
        assertEquals(PartyBlockState.Saved(ramesh), partyBlockState(all, null, "9876543210", false))
        assertEquals(PartyBlockState.Saved(noPhone), partyBlockState(all, "p2", "", false))
    }

    @Test
    fun unknownNumberOrNewRowShowsNewFields() {
        assertEquals(PartyBlockState.New, partyBlockState(all, null, "9111111111", false))
        assertEquals(PartyBlockState.New, partyBlockState(all, null, "", true))
    }

    @Test
    fun changingNumberMakesSelectionStale() {
        assertTrue(isSelectionStale(all, "p1", "987654321"))
        assertFalse(isSelectionStale(all, "p1", "9876543210"))
    }

    @Test
    fun addingMissingNumberKeepsSelection() {
        assertFalse(isSelectionStale(all, "p2", "9000000001"))
        assertFalse(isSelectionStale(all, null, "9000000001"))
    }

    @Test
    fun sameNameAndVillageOnNewNumberIsCandidate() {
        assertEquals(ramesh, samePersonCandidate(all, null, "9111111111", " test रमेश ", "test गाँव"))
    }

    @Test
    fun noCandidateWhenPickedKnownNumberOrDifferentVillage() {
        assertNull(samePersonCandidate(all, "p1", "9876543210", "TEST रमेश", "TEST गाँव"))
        assertNull(samePersonCandidate(all, null, "9876543210", "TEST रमेश", "TEST गाँव"))
        assertNull(samePersonCandidate(all, null, "9111111111", "TEST रमेश", "दूसरा"))
        assertNull(samePersonCandidate(all, null, "9111111111", "", ""))
    }
}
```

- [ ] **Step 2: Run it, expect a compile failure**

`./gradlew :composeApp:jvmTest --tests '*PartyBlockTest'` fails with unresolved reference `PartyBlockState`.

- [ ] **Step 3: Create `UI/components/PartyBlock.kt`**

```kotlin
package com.appwork.mandisamiti.ui.components

import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.ui.buy.BuyPartyResolver

/** What the party block of a slip shows. */
sealed interface PartyBlockState {
    data object Lookup : PartyBlockState
    data class Saved(val party: Party) : PartyBlockState
    data object New : PartyBlockState
}

fun partyBlockState(
    parties: List<Party>,
    selectedId: String?,
    phone: String,
    newRequested: Boolean,
): PartyBlockState {
    val saved = BuyPartyResolver.existing(parties, selectedId, phone)
    return when {
        saved != null -> PartyBlockState.Saved(saved)
        newRequested || phone.length == 10 -> PartyBlockState.New
        else -> PartyBlockState.Lookup
    }
}

/** True when the number no longer belongs to the picked account, so its details must be cleared. */
fun isSelectionStale(parties: List<Party>, selectedId: String?, newPhone: String): Boolean {
    val selected = parties.firstOrNull { it.id == selectedId } ?: return false
    return !selected.phone.isNullOrBlank() && selected.phone != newPhone
}

/** A saved account with this name and village while the slip has an unknown number. */
fun samePersonCandidate(
    parties: List<Party>,
    selectedId: String?,
    phone: String,
    name: String,
    village: String,
): Party? {
    if (BuyPartyResolver.existing(parties, selectedId, phone) != null) return null
    val n = name.trim()
    val v = village.trim()
    if (n.isEmpty() || v.isEmpty()) return null
    return parties.firstOrNull {
        it.name.trim().equals(n, ignoreCase = true) && it.village?.trim().equals(v, ignoreCase = true)
    }
}
```

- [ ] **Step 4: Run the test, expect PASS**
- [ ] **Step 5: Commit** `feat(slip): party block state rules`

---

### Task 2: Shared composables and the "+ New" row

**Files:**
- Modify: `UI/components/PartyBlock.kt` (append)
- Modify: `UI/components/MandiTextField.kt` (`MandiPartyLookupField`)

**Interfaces (produced):**
- `SavedPartyLine(name: String, village: String, isEnglish: Boolean, onEdit: () -> Unit)`
- `EditPartyDialog(name: String, village: String, isEnglish: Boolean, onDone: (String, String) -> Unit, onDismiss: () -> Unit)`
- `SamePersonDialog(candidate: Party, isEnglish: Boolean, onSamePerson: () -> Unit, onNewAccount: () -> Unit, onDismiss: () -> Unit)`
- `MandiPartyLookupField(..., newPartyLabel: String? = null, onNewParty: ((String) -> Unit)? = null)`

- [ ] **Step 1: Append to `PartyBlock.kt`** (add the imports)

```kotlin
@Composable
fun SavedPartyLine(name: String, village: String, isEnglish: Boolean, onEdit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = listOf(name, village).filter { it.isNotBlank() }.joinToString("  •  "),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MandiTextPrimary,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onEdit) { Text(if (isEnglish) "Edit" else "बदलें") }
    }
}

@Composable
fun EditPartyDialog(
    name: String,
    village: String,
    isEnglish: Boolean,
    onDone: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newName by remember { mutableStateOf(name) }
    var newVillage by remember { mutableStateOf(village) }
    val valid = newName.isNotBlank() && MandiValidators.sanitizeVillage(newVillage).isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEnglish) "Edit account" else "खाता बदलें") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MandiFlatTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = if (isEnglish) "Name" else "नाम",
                )
                MandiFlatTextField(
                    value = newVillage,
                    onValueChange = { newVillage = it },
                    label = if (isEnglish) "Village" else "गांव",
                )
                Text(
                    text = if (isEnglish) {
                        "Saved with this slip. Applies to this account's old slips too."
                    } else {
                        "पर्ची के साथ सेव होगा। इस खाते की पुरानी पर्चियों पर भी लागू होगा।"
                    },
                    fontSize = 12.sp,
                    color = MandiTextSecondary,
                )
            }
        },
        confirmButton = {
            Button(enabled = valid, onClick = { onDone(newName.trim(), newVillage) }) {
                Text(if (isEnglish) "Done" else "ठीक है")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (isEnglish) "Cancel" else "रद्द करें") } },
    )
}

@Composable
fun SamePersonDialog(
    candidate: Party,
    isEnglish: Boolean,
    onSamePerson: () -> Unit,
    onNewAccount: () -> Unit,
    onDismiss: () -> Unit,
) {
    val who = listOfNotNull(candidate.name, candidate.village, candidate.phone).joinToString("  •  ")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEnglish) "Same person with a new number?" else "वही व्यक्ति, नया नंबर?") },
        text = { Text(if (isEnglish) "Already saved: $who" else "पहले से सेव: $who") },
        confirmButton = {
            Button(onClick = onSamePerson) { Text(if (isEnglish) "Yes, same person" else "हाँ, वही है") }
        },
        dismissButton = {
            TextButton(onClick = onNewAccount) { Text(if (isEnglish) "No, new account" else "नहीं, नया खाता") }
        },
    )
}
```

`onDismiss` (back or outside tap) saves nothing.

- [ ] **Step 2: Add the "+ New" row to `MandiPartyLookupField`**

Add two parameters after `errorMessage`:

```kotlin
    newPartyLabel: String? = null,
    onNewParty: ((typedName: String) -> Unit)? = null
```

After `showDropdown`:

```kotlin
    val typedName = searchQuery.trim()
    val showNewRow = onNewParty != null && newPartyLabel != null && expanded &&
        typedName.length >= 2 && typedName.any { !it.isDigit() }
```

Change `if (showDropdown) {` to `if (showDropdown || showNewRow) {`, change `matches.forEachIndexed` to `(if (showDropdown) matches else emptyList()).forEachIndexed`, and add inside the `Column`, after that loop:

```kotlin
                    if (showNewRow) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expanded = false
                                    searchQuery = ""
                                    textMode = false
                                    onNewParty?.invoke(typedName)
                                }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MandiTextPrimary)
                            Text(
                                text = "$newPartyLabel: $typedName",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MandiTextPrimary
                            )
                        }
                    }
```

The Lend screen passes neither parameter, so it does not change.

- [ ] **Step 3:** `./gradlew :composeApp:jvmTest` passes.
- [ ] **Step 4: Commit** `feat(slip): saved party line, edit and same-person dialogs, new row`

---

### Task 3: Wire the Buy screen

**Files:** Modify `UI/buy/BuyEntryScreen.kt`

**Interfaces consumed:** everything from Tasks 1 and 2, `BuyPartyResolver.pick(form, party)`.

- [ ] **Step 1: State**, beside `newPartyCheck`:

```kotlin
    var newPartyRequested by remember(existingId) { mutableStateOf(false) }
    var showEditParty by remember { mutableStateOf(false) }
    var sameCandidate by remember { mutableStateOf<Party?>(null) }
```

- [ ] **Step 2: Old slips show the account's current details.** Replace the body of `LaunchedEffect(existingEntry, parties)` after the `party` lookup (the three `if` lines) with:

```kotlin
        // Only on first load: later party updates must not overwrite what the owner is editing.
        if (formState.farmerName == entry.farmerId) {
            val slipPhone = formState.phone
            val slipVillage = formState.village
            BuyPartyResolver.pick(formState, party)
            if (formState.phone.isBlank()) formState.updatePhone(slipPhone)
            if (formState.village.isBlank()) formState.village = slipVillage
        }
```

- [ ] **Step 3: Ask about a new number for a saved person.** Replace the `onSaveTriggered` block with:

```kotlin
            onSaveTriggered = {
                val candidate = if (formState.isKhaate) null else samePersonCandidate(
                    parties, formState.selectedFarmerId, formState.phone, formState.farmerName, formState.village
                )
                val typedCheck = if (formState.isKhaate) null else checkTypedParty(formState.farmerName, formState.selectedFarmerId, parties)
                if (candidate != null) {
                    sameCandidate = candidate
                } else if (typedCheck is TypedPartyCheck.NeedsConfirm) {
                    newPartyCheck = typedCheck
                } else {
                    performSave()
                }
            }
```

`performSave` must be the current lambda. The `actions` `remember` is already keyed on `parties`.

- [ ] **Step 4: Dialogs.** Next to the existing `newPartyCheck` dialog:

```kotlin
    sameCandidate?.let { candidate ->
        SamePersonDialog(
            candidate = candidate,
            isEnglish = isEnglish,
            onSamePerson = {
                sameCandidate = null
                formState.selectedFarmerId = candidate.id
                performSave()
            },
            onNewAccount = { sameCandidate = null; performSave() },
            onDismiss = { sameCandidate = null },
        )
    }
    if (showEditParty) {
        EditPartyDialog(
            name = formState.farmerName,
            village = formState.village,
            isEnglish = isEnglish,
            onDone = { name, village ->
                formState.farmerName = name
                formState.village = village
                showEditParty = false
            },
            onDismiss = { showEditParty = false },
        )
    }
```

- [ ] **Step 5: The block.** Replace everything inside `if (!state.isKhaate) { ... }` of "Section 1: Farmer block" with:

```kotlin
            val block = partyBlockState(parties, state.party.selectedPartyId, state.party.phone, newPartyRequested)

            MandiPartyLookupField(
                phoneValue = state.party.phone,
                onPhoneChange = { phone ->
                    if (isSelectionStale(parties, formState.selectedFarmerId, phone)) {
                        formState.selectedFarmerId = null
                        formState.farmerName = ""
                        formState.village = ""
                    }
                    actions.onPhoneChange(phone)
                },
                onPartySelected = { party ->
                    newPartyRequested = false
                    BuyPartyResolver.pick(formState, party)
                },
                parties = parties,
                label = if (isEnglish) "Mobile number or name" else "मोबाइल नंबर या नाम",
                placeholder = if (block is PartyBlockState.Saved && block.party.phone.isNullOrBlank()) {
                    if (isEnglish) "Enter 10-digit number" else "10 अंकों का नंबर डालें"
                } else null,
                isError = state.party.phoneError != null,
                errorMessage = state.party.phoneError,
                newPartyLabel = if (isEnglish) "New farmer" else "नया किसान",
                onNewParty = { typed ->
                    formState.selectedFarmerId = null
                    formState.updatePhone("")
                    formState.village = ""
                    formState.farmerName = typed
                    newPartyRequested = true
                }
            )

            val villageField: @Composable () -> Unit = {
                MandiFlatTextField(
                    value = state.party.village,
                    onValueChange = { actions.onVillageChange(it) },
                    label = if (isEnglish) "Village" else "गांव",
                    placeholder = if (isEnglish) "Type village" else "गांव लिखें",
                    isError = state.party.villageError != null,
                    errorMessage = state.party.villageError,
                    isTall = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            when (block) {
                is PartyBlockState.Saved -> {
                    SavedPartyLine(
                        name = state.party.name,
                        // A missing village is typed in the box below, so the line does not echo it.
                        village = if (block.party.village.isNullOrBlank()) "" else state.party.village,
                        isEnglish = isEnglish,
                        onEdit = { showEditParty = true },
                    )
                    if (block.party.village.isNullOrBlank()) villageField()
                }
                PartyBlockState.New -> {
                    Text(
                        text = if (isEnglish) "New account. It is created when the slip is saved." else "नया खाता। पर्ची सेव होते ही बन जाएगा।",
                        fontSize = 12.sp,
                        color = MandiTextSecondary
                    )
                    MandiFlatTextField(
                        value = state.party.name,
                        onValueChange = { actions.onNameChange(it) },
                        label = if (isEnglish) "Farmer name" else "किसान का नाम",
                        placeholder = if (isEnglish) "Type name" else "नाम लिखें",
                        isError = state.party.nameError != null,
                        errorMessage = state.party.nameError,
                        isTall = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    villageField()
                }
                PartyBlockState.Lookup -> Unit
            }
```

The old hint lines and the `existingParty` / `isOldPartyWithoutPhone` values go away. Remove imports that become unused. `BuyEntryActions.onPickParty` may now be unused by this screen; leave the interface as it is.

Saving an edited name on a saved account must still persist through `commit`: `BuyPartyResolver.partyToSave` already handles it.

- [ ] **Step 6: Discard dialog** `onConfirmDiscard`: add `newPartyRequested = false`.

- [ ] **Step 7: Save button text.** A partial number must not ask for boxes that are hidden. In the disabled branch change

```kotlin
                        } else if (state.party.needsPhonePrompt || state.party.phone.isBlank()) {
```

to

```kotlin
                        } else if (state.party.needsPhonePrompt || state.party.phone.length < 10) {
```

- [ ] **Step 8:** `./gradlew :composeApp:jvmTest` passes. Commit `feat(buy): collapse party block for saved accounts`.

---

### Task 4: Wire the Sell screen

**Files:** Modify `UI/sell/SellEntryScreen.kt`

**Interfaces consumed:** Tasks 1 and 2, `SellPartyResolver.pick(form, party)`.

- [ ] **Step 1: State**, beside `newPartyCheck`: the same three variables as Task 3 Step 1.

- [ ] **Step 2: Old slips.** In `LaunchedEffect(existingEntry, parties)`, replace the lines after the `party` lookup with:

```kotlin
        if (formState.buyerName == entry.buyerId) {
            val slipPhone = formState.phone
            val slipVillage = formState.village
            val slipGstin = formState.gstin
            val slipKind = formState.partyKind
            val slipStateCode = formState.stateCode
            val slipCommission = formState.commissionPercent
            SellPartyResolver.pick(formState, party)
            // The slip's own category, rate and tax fields win over what pick() derives.
            formState.partyKind = slipKind
            formState.commissionPercent = slipCommission
            if (formState.phone.isBlank()) formState.updatePhone(slipPhone)
            if (formState.village.isBlank()) formState.village = slipVillage
            if (slipGstin.isNotBlank()) {
                formState.gstin = slipGstin
                formState.stateCode = slipStateCode
            }
        }
```

`pick` can switch the category to Merchant and with it the default commission, so both are restored. Check on device (Task 5, check 11) with an old Retail slip and an old Merchant slip.

- [ ] **Step 3:** In `triggerSave`, same logic as Task 3 Step 3 using `formState.selectedBuyerId` and `formState.buyerName`:

```kotlin
    val triggerSave: () -> Unit = {
        val candidate = if (formState.isKhaate) null else samePersonCandidate(
            parties, formState.selectedBuyerId, formState.phone, formState.buyerName, formState.village
        )
        val typedCheck = if (formState.isKhaate) null else checkTypedParty(formState.buyerName, formState.selectedBuyerId, parties)
        if (candidate != null) {
            sameCandidate = candidate
        } else if (typedCheck is TypedPartyCheck.NeedsConfirm) {
            newPartyCheck = typedCheck
        } else {
            performSave()
        }
    }
```

- [ ] **Step 4: Dialogs**, next to the `newPartyCheck` dialog:

```kotlin
    sameCandidate?.let { candidate ->
        SamePersonDialog(
            candidate = candidate,
            isEnglish = isEnglish,
            onSamePerson = {
                sameCandidate = null
                formState.selectedBuyerId = candidate.id
                performSave()
            },
            onNewAccount = { sameCandidate = null; performSave() },
            onDismiss = { sameCandidate = null },
        )
    }
    if (showEditParty) {
        EditPartyDialog(
            name = formState.buyerName,
            village = formState.village,
            isEnglish = isEnglish,
            onDone = { name, village ->
                formState.buyerName = name
                formState.village = village
                showEditParty = false
            },
            onDismiss = { showEditParty = false },
        )
    }
```

`SellPartyResolver.partyToSave` keeps `existing.gstin` when the form's GSTIN is blank, so "same person" does not erase a saved GSTIN.

- [ ] **Step 5: The block.** Replace from `// Section 1: Buyer block` down to just before `// BOS specific details` with:

```kotlin
            // Section 1: Buyer block
            val block = partyBlockState(parties, formState.selectedBuyerId, formState.phone, newPartyRequested)

            MandiPartyLookupField(
                phoneValue = formState.phone,
                onPhoneChange = { phone ->
                    if (isSelectionStale(parties, formState.selectedBuyerId, phone)) {
                        formState.selectedBuyerId = null
                        formState.buyerName = ""
                        formState.village = ""
                        formState.gstin = ""
                        formState.stateCode = "23"
                    }
                    formState.updatePhone(phone)
                },
                onPartySelected = { party ->
                    newPartyRequested = false
                    SellPartyResolver.pick(formState, party)
                },
                parties = parties,
                label = if (isEnglish) "Mobile number or name" else "मोबाइल नंबर या नाम",
                placeholder = if (block is PartyBlockState.Saved && block.party.phone.isNullOrBlank()) {
                    if (isEnglish) "Enter 10-digit number" else "10 अंकों का नंबर डालें"
                } else null,
                isError = formState.phone.errorIfFilled(formState.phoneValidation.errorOrNull) != null,
                errorMessage = formState.phone.errorIfFilled(formState.phoneValidation.errorOrNull),
                newPartyLabel = if (isEnglish) "New buyer" else "नया खरीदार",
                onNewParty = { typed ->
                    formState.selectedBuyerId = null
                    formState.updatePhone("")
                    formState.village = ""
                    formState.gstin = ""
                    formState.stateCode = "23"
                    formState.buyerName = typed
                    newPartyRequested = true
                }
            )

            val villageField: @Composable () -> Unit = {
                MandiFlatTextField(
                    value = formState.village,
                    onValueChange = { formState.village = it },
                    label = if (isEnglish) "Village / City" else "गाँव / शहर",
                    isError = formState.village.errorIfFilled(formState.villageValidation.errorOrNull) != null,
                    errorMessage = formState.village.errorIfFilled(formState.villageValidation.errorOrNull),
                    isTall = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            when (block) {
                is PartyBlockState.Saved -> {
                    SavedPartyLine(
                        name = formState.buyerName,
                        village = if (block.party.village.isNullOrBlank()) "" else formState.village,
                        isEnglish = isEnglish,
                        onEdit = { showEditParty = true },
                    )
                    if (block.party.village.isNullOrBlank()) villageField()
                }
                PartyBlockState.New -> {
                    Text(
                        text = if (isEnglish) "New account. It is created when the slip is saved." else "नया खाता। पर्ची सेव होते ही बन जाएगा।",
                        fontSize = 12.sp,
                        color = MandiTextSecondary
                    )
                    MandiFlatTextField(
                        value = formState.buyerName,
                        onValueChange = { formState.buyerName = it },
                        label = if (isEnglish) {
                            "Buyer Name"
                        } else if (formState.partyKind == SellPartyKind.MERCHANT) {
                            "व्यापारी / फर्म का नाम"
                        } else {
                            "ग्राहक का नाम"
                        },
                        isError = formState.buyerName.errorIfFilled(formState.buyerNameValidation.errorOrNull) != null,
                        errorMessage = formState.buyerName.errorIfFilled(formState.buyerNameValidation.errorOrNull),
                        isTall = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    villageField()
                }
                PartyBlockState.Lookup -> Unit
            }
```

- [ ] **Step 6: Validation that points at a hidden box.** A saved merchant account can have a name that `buyerNameValidation` rejects. The save button already shows `validationReason`, and Edit fixes the name. In `saveButtonHint`, check the number first so an empty form does not ask for name and village before those boxes exist:

```kotlin
                    val saveButtonHint = if (!formState.isKhaate && formState.phone.length < 10) {
                        if (isEnglish) "Enter 10-digit mobile number" else "10 अंकों का मोबाइल नंबर डालें"
                    } else if (!formState.isKhaate && (formState.buyerName.isBlank() || formState.village.isBlank())) {
                        if (isEnglish) "Add name and village to save" else "नाम और गांव डालने पर सुरक्षित होगा"
                    } else {
                        validationReason ?: (if (isEnglish) "Save" else "सुरक्षित करें")
                    }
```

- [ ] **Step 7:** Remove the values that are now unused (`existingParty`, `isOldPartyWithoutPhone`) and unused imports. The Sell discard flow only calls `onBack()`, so it needs no change.
- [ ] **Step 8:** `./gradlew :composeApp:jvmTest` passes. Commit `feat(sell): collapse party block for saved accounts`.

---

### Task 5: Verify and open the PR

- [ ] **Step 1:** `./gradlew jvmTest :composeApp:assembleStagingDebug`. Fix any failing test by checking the spec first; do not delete or weaken a test.
- [ ] **Step 2:** Confirm `local.properties` points at staging, then `./gradlew :composeApp:installStagingDebug` on Redmi `d93c743`.
- [ ] **Step 3:** With TEST-prefixed accounts only, check on Buy and on Sell, in light and dark theme, with a screenshot of each:

| # | Check | Expected |
|---|---|---|
| 1 | Open a new slip | Only the search box |
| 2 | Type a saved 10-digit number | Saved line with name and village, no boxes |
| 3 | Delete one digit | Line gone, old name and village gone |
| 4 | Type a new number | New-account hint, name and village boxes |
| 5 | Type a new name, tap the "+ New" row | Name prefilled, number keyboard, number required |
| 6 | Pick an account with no number | Line shows, search box asks for number, Save blocked |
| 7 | Pick an account with no village | Village box under the line, Save blocked until filled |
| 8 | Edit, change village, Cancel, then Done, then save | Cancel changes nothing; Done updates account and receipt |
| 9 | New number with a saved name and village, answer Yes | One account, new number, old slips still attached |
| 10 | Same, answer No | Second account created |
| 11 | Open an old slip and save it unchanged | Same account, same amounts, commission, category and GSTIN |
| 12 | Khaate slip | Unchanged |
| 13 | Keyboard open on name, village and Edit dialog | Nothing hidden behind the keyboard |

- [ ] **Step 4:** Add a `slip-party-block` entry at the end of `FEATURES.json` (`passing` only if Step 3 ran on the Redmi, with the date), append to `PROGRESS.md`, update your rows in `docs/STATUS.md`.
- [ ] **Step 5:** Push `feat/slip-party-block`, open a PR with a plain-English summary and the mermaid flow from the spec. Report the out-of-scope Merchant switch from the spec in the PR. Do not merge.
- [ ] **Step 6:** Send Vivek a short Telegram note with `python3 /Users/appworx/Desktop/Vivek-K/scripts/notify_telegram.py "Slip party block" "<PR link, test result, what is unverified>"` and leave a handover for Claude Code review.
