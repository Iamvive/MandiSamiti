# WhatsApp Direct Send Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task by task. Steps use checkbox (`- [ ]`) syntax. Tick them as you go.

**Goal:** The main share button on buy, sell and preview slips sends to the party's WhatsApp number, with "Other contact" below it.

**Architecture:** Pure helpers in `commonMain` decide the number and build the `wa.me` link. `WhatsAppShareManager` gains an app list and an app argument. One composable, `SlipSendBar`, draws both states and the two-app sheet, and the three screens use it.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, `kotlin.test`.

**Spec:** `docs/superpowers/specs/2026-11-27-whatsapp-direct-send-design.md`

## Global Constraints

- Work only in `/Users/appworx/Desktop/Vivek-K/.claude/worktrees/ms-whatsapp-direct`, branch `feat/whatsapp-direct-send` (from `origin/main` `3fac745`). Read `CLAUDE.md` there first. Never edit the main checkout.
- No push to `main`, no merge, no deploy. Do not push the branch or open a PR until Vivek says so.
- Do not edit `vaults/Vivek-K/wiki/hot.md` and do not bump the hub submodule pointer. Vivek approves first.
- Device tests use the staging build only. Never `10.0.2.2`, never prod.
- **Never tap Send inside WhatsApp.** Stop at the pre-filled message and press back. No real person may receive a test message.
- Do not run voice or soundbox playback. Do not make sound.
- Slip text and print HTML do not change.
- Colours come from `ui/theme/Color.kt` tokens. No new `Color(...)` literals, no `Color.White`. No emoji in new UI.
- Product rule: build first (Tasks 1 to 4), check on device (Task 5), then write tests (Task 6).
- Touch only the files named here.

Path prefix used below: `M = composeApp/src/commonMain/kotlin/com/appwork/mandisamiti`.

## Files

| File | Change |
|---|---|
| `M/platform/WhatsAppLink.kt` | new: app enum, URL encoding |
| `M/ui/slip/SlipRecipient.kt` | new: number rules, preview recipient |
| `M/platform/WhatsAppShareManager.kt` + android, ios, jvm actuals | app list, app argument |
| `M/ui/slip/SlipSendBar.kt` | new: send buttons and app sheet |
| `M/ui/buy/BuyTransactionReceiptScreen.kt` | use the bar |
| `M/ui/sell/SellTransactionReceiptScreen.kt`, `M/ui/components/ReceiptParts.kt` | use the bar |
| `M/ui/slip/ReceiptPreviewScreen.kt` | use the bar |
| `M/App.kt` | wiring |

---

### Task 0: Baseline

- [ ] **Step 1:** `git -C . status -sb` shows `feat/whatsapp-direct-send` and a clean tree. `local.properties` exists (already copied; do not print it).
- [ ] **Step 2:** Run each and write down pass or fail before changing code:

```bash
./gradlew :composeApp:jvmTest
./gradlew :composeApp:assembleStagingDebug
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

If `jvmTest` or the staging build fails here, stop and report. If only the iOS compile fails on untouched code, record it, continue, and say iOS is unverified.

---

### Task 1: Pure helpers

**Files:** Create `M/platform/WhatsAppLink.kt`, `M/ui/slip/SlipRecipient.kt`

**Interfaces produced:**
`enum class WhatsAppApp(packageName, label)`; `whatsAppSendUrl(phone: String?, text: String): String`; `whatsAppPhone(raw: String?): String?`; `slipRecipientPhone(accountPhone: String?, slipPhone: String?): String?`; `receiptPreviewRecipient(farmer: Party, buyer: Party?, ledgerPartyId: String?): Party`

- [ ] **Step 1:** Create `M/platform/WhatsAppLink.kt`:

```kotlin
package com.appwork.mandisamiti.platform

enum class WhatsAppApp(val packageName: String, val label: String) {
    PERSONAL("com.whatsapp", "WhatsApp"),
    BUSINESS("com.whatsapp.w4b", "WhatsApp Business")
}

/** Click-to-chat link. [phone] is a 10-digit Indian mobile, or null to pick the contact inside WhatsApp. */
fun whatsAppSendUrl(phone: String?, text: String): String =
    "https://wa.me/${phone?.let { "91$it" }.orEmpty()}?text=${percentEncode(text)}"

private fun percentEncode(text: String): String = buildString {
    for (byte in text.encodeToByteArray()) {
        val code = byte.toInt() and 0xFF
        val char = code.toChar()
        if (char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char in "-_.~") {
            append(char)
        } else {
            append('%').append(code.toString(16).uppercase().padStart(2, '0'))
        }
    }
}
```

- [ ] **Step 2:** Create `M/ui/slip/SlipRecipient.kt`:

```kotlin
package com.appwork.mandisamiti.ui.slip

import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.ui.ledger.isValidPartyPhone

/** The 10-digit mobile in [raw], or null when it is not clearly one Indian mobile number. */
fun whatsAppPhone(raw: String?): String? {
    if (raw == null || raw.any { it !in '0'..'9' && it !in " +-()" }) return null
    val digits = raw.filter { it in '0'..'9' }
    val local = when {
        digits.length == 12 && digits.startsWith("91") -> digits.drop(2)
        digits.length == 11 && digits.startsWith("0") -> digits.drop(1)
        else -> digits
    }
    return local.takeIf(::isValidPartyPhone)
}

/** The party account number wins; the number stored on the slip is the fallback. */
fun slipRecipientPhone(accountPhone: String?, slipPhone: String?): String? =
    whatsAppPhone(accountPhone) ?: whatsAppPhone(slipPhone)

/** Opened from the buyer's ledger it goes to the buyer; in every other case to the farmer. */
fun receiptPreviewRecipient(farmer: Party, buyer: Party?, ledgerPartyId: String?): Party =
    if (buyer != null && buyer.id == ledgerPartyId) buyer else farmer
```

- [ ] **Step 3:** `./gradlew :composeApp:compileKotlinJvm` passes. Commit:

```bash
git add composeApp/src/commonMain
git commit -m "feat(share): WhatsApp link and slip recipient rules"
```

---

### Task 2: Share manager

**Files:** Modify `M/platform/WhatsAppShareManager.kt` and `composeApp/src/{androidMain,iosMain,jvmMain}/kotlin/com/appwork/mandisamiti/platform/WhatsAppShareManager.*.kt`

**Interfaces produced:** `installedApps(): List<WhatsAppApp>`; `shareText(text: String, phoneNumber: String?, app: WhatsAppApp? = null)`. `phoneNumber` is the 10-digit result of `whatsAppPhone`.

`shareReceiptImage` has no callers. Leave it as it is.

- [ ] **Step 1:** In the common file, keep `shareReceiptImage` and replace the `shareText` declaration with:

```kotlin
    /** WhatsApp apps the user can choose between. Empty when the platform picks for itself or none is installed. */
    fun installedApps(): List<WhatsAppApp>

    /** [phoneNumber] is a 10-digit mobile, or null to choose the contact in WhatsApp. */
    fun shareText(
        text: String,
        phoneNumber: String?,
        app: WhatsAppApp? = null
    )
```

- [ ] **Step 2:** Android. Add imports `android.content.ActivityNotFoundException` and `android.net.Uri`. Replace the whole `shareText` function with:

```kotlin
    actual fun installedApps(): List<WhatsAppApp> = WhatsAppApp.entries.filter { app ->
        runCatching { context.packageManager.getApplicationInfo(app.packageName, 0).enabled }
            .getOrDefault(false)
    }

    actual fun shareText(
        text: String,
        phoneNumber: String?,
        app: WhatsAppApp?
    ) {
        val target = app ?: installedApps().firstOrNull()
        if (target != null) {
            val intent = if (phoneNumber != null) {
                Intent(Intent.ACTION_VIEW, Uri.parse(whatsAppSendUrl(phoneNumber, text)))
            } else {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
            }
            intent.setPackage(target.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return
            } catch (e: ActivityNotFoundException) {
                // fall through to the system share sheet
            }
        }
        val genericIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startChooser(genericIntent, "पर्ची शेयर करें")
    }
```

The manifest already declares both packages under `<queries>`. Do not change it.

- [ ] **Step 3:** iOS. Replace `shareText` with the code below. The old one-argument `openURL` returns false for apps built with the iOS 18 SDK, so use the options form.

```kotlin
    // iOS opens whichever WhatsApp app owns the link, so there is nothing to choose.
    actual fun installedApps(): List<WhatsAppApp> = emptyList()

    actual fun shareText(
        text: String,
        phoneNumber: String?,
        app: WhatsAppApp?
    ) {
        val url = NSURL.URLWithString(whatsAppSendUrl(phoneNumber, text)) ?: return
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
    }
```

- [ ] **Step 4:** JVM:

```kotlin
    actual fun installedApps(): List<WhatsAppApp> = emptyList()

    actual fun shareText(
        text: String,
        phoneNumber: String?,
        app: WhatsAppApp?
    ) {
        // No-op on JVM desktop testing
    }
```

- [ ] **Step 5:** Run `./gradlew :composeApp:compileKotlinJvm :composeApp:compileStagingDebugKotlinAndroid :composeApp:compileKotlinIosSimulatorArm64` (use the Android compile task name Gradle lists if it differs). Expected: pass. Commit `feat(share): send to a number with WhatsApp or WhatsApp Business`.

---

### Task 3: SlipSendBar

**Files:** Create `M/ui/slip/SlipSendBar.kt`

**Interfaces produced:**
`SlipSendBar(recipientName: String, recipientPhone: String?, isEnglish: Boolean, installedApps: () -> List<WhatsAppApp>, onSend: (phone: String?, app: WhatsAppApp?) -> Unit, modifier: Modifier = Modifier, primary: Boolean = true)`

- [ ] **Step 1:** Create the file:

```kotlin
package com.appwork.mandisamiti.ui.slip

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.platform.WhatsAppApp
import com.appwork.mandisamiti.ui.components.MandiSecondaryButton
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiBtnSuccessBg
import com.appwork.mandisamiti.ui.theme.MandiBtnSuccessFg
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

private data class PendingSend(val phone: String?, val apps: List<WhatsAppApp>)

/**
 * Slip sharing controls. With a number: send to the party, then "Other contact".
 * Without one: a single button that opens WhatsApp's contact picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlipSendBar(
    recipientName: String,
    recipientPhone: String?,
    isEnglish: Boolean,
    installedApps: () -> List<WhatsAppApp>,
    onSend: (phone: String?, app: WhatsAppApp?) -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true
) {
    var pending by remember { mutableStateOf<PendingSend?>(null) }

    // Asked on each tap, so an app installed or removed meanwhile is picked up.
    fun send(phone: String?) {
        val apps = installedApps()
        if (apps.size > 1) pending = PendingSend(phone, apps) else onSend(phone, apps.firstOrNull())
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (recipientPhone == null) {
            Text(
                text = if (isEnglish) "No valid mobile number for $recipientName"
                else "$recipientName का सही मोबाइल नंबर नहीं है",
                fontSize = 12.sp,
                color = MandiTextSecondary,
                textAlign = TextAlign.Center
            )
        }
        Button(
            onClick = { send(recipientPhone) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (primary) MandiBtnSuccessBg else MandiSurface,
                contentColor = if (primary) MandiBtnSuccessFg else MandiTextPrimary
            ),
            border = if (primary) null else BorderStroke(1.dp, MandiBorder)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when {
                    recipientPhone == null -> if (isEnglish) "Choose contact to send" else "कॉन्टैक्ट चुनकर भेजें"
                    isEnglish -> "Send to $recipientName"
                    else -> "$recipientName को भेजें"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (recipientPhone != null) {
            Text(
                text = "${recipientPhone.take(5)} ${recipientPhone.drop(5)}",
                fontSize = 12.sp,
                color = MandiTextSecondary
            )
            TextButton(onClick = { send(null) }) {
                Text(
                    text = if (isEnglish) "Other contact" else "दूसरा कॉन्टैक्ट",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MandiTextPrimary
                )
            }
        }
    }

    pending?.let { request ->
        ModalBottomSheet(onDismissRequest = { pending = null }, containerColor = MandiSurface) {
            Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isEnglish) "Send with which app?" else "किस ऐप से भेजें?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MandiTextPrimary
                )
                request.apps.forEach { app ->
                    MandiSecondaryButton(
                        text = app.label,
                        onClick = {
                            pending = null
                            onSend(request.phone, app)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2:** `./gradlew :composeApp:compileKotlinJvm` passes. Commit `feat(slip): send bar with party number and other contact`.

---

### Task 4: Wire the three screens

Line numbers are from `origin/main` `3fac745`.

- [ ] **Step 1: Buy screen** (`M/ui/buy/BuyTransactionReceiptScreen.kt`).

Replace the `onShareWhatsApp` parameter (line 87) with:

```kotlin
    partyPhone: String? = null,
    whatsAppApps: () -> List<WhatsAppApp> = { emptyList() },
    onShareWhatsApp: ((text: String, phone: String?, app: WhatsAppApp?) -> Unit)? = null,
```

After `val slipNum = ...` add:

```kotlin
    // buyFarmerPhone is null for khaate slips; the account number is ignored for them too.
    val recipientPhone = if (entry == null || entry.isKhaate) null
    else slipRecipientPhone(partyPhone, buyFarmerPhone(entry))
```

In the `bottomBar`, `if (hasPendingDue)` branch: keep the "Pay Cash from Galla" button. Directly after it add the bar, and delete the `if (onShareWhatsApp != null) { MandiSecondaryButton(... "Share" ...) }` block from the row below, keeping Print and New:

```kotlin
                    if (onShareWhatsApp != null) {
                        SlipSendBar(
                            recipientName = displayName,
                            recipientPhone = recipientPhone,
                            isEnglish = isEnglish,
                            installedApps = whatsAppApps,
                            onSend = { phone, app -> onShareWhatsApp(slipText, phone, app) },
                            primary = false
                        )
                    }
```

Replace the whole `else` branch (the `if (entry != null) { Row { Print, Share } }` block plus the full-width "New purchase" button) with:

```kotlin
                } else {
                    if (entry != null && onShareWhatsApp != null) {
                        SlipSendBar(
                            recipientName = displayName,
                            recipientPhone = recipientPhone,
                            isEnglish = isEnglish,
                            installedApps = whatsAppApps,
                            onSend = { phone, app -> onShareWhatsApp(slipText, phone, app) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (entry != null && onPrint != null) {
                            MandiSecondaryButton(
                                text = if (isEnglish) "Print" else "प्रिंट करें",
                                leadingIcon = Icons.Default.Print,
                                height = 48.dp,
                                fontSize = 14.sp,
                                fullWidth = false,
                                onClick = { onPrint(slipHtml) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        MandiSecondaryButton(
                            text = if (isEnglish) "New purchase" else "नई खरीद",
                            height = 48.dp,
                            fontSize = 14.sp,
                            fullWidth = false,
                            onClick = { onNewBuy?.invoke() ?: onBack() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
```

Add imports for `WhatsAppApp`, `SlipSendBar`, `slipRecipientPhone`. Remove `MandiPrimaryButton` or `Icons.Default.Share` imports only if nothing else uses them.

- [ ] **Step 2: Scaffold** (`M/ui/components/ReceiptParts.kt`, used only by the sell screen). Replace the parameters `shareLabel: String,` and `onShare: (() -> Unit)?,` with `shareContent: (@Composable () -> Unit)? = null,` placed after `deleteContentDescription`. Replace everything after `content()` inside the column (the whole `if (onPrint != null && onShare != null) ... else if` chain) with:

```kotlin
            shareContent?.invoke()
            if (onPrint != null) {
                Button(
                    onClick = onPrint,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MandiSurface,
                        contentColor = MandiTextPrimary
                    ),
                    border = BorderStroke(1.dp, MandiBorder)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(printLabel, modifier = Modifier.padding(start = 8.dp))
                }
            }
```

Update the KDoc to "Shared frame for the sell memo: top bar, one card, share and print controls." Remove the imports this made unused (`Icons.Default.Share`, `Row`, `MandiPrimaryAction`, `MandiPrimaryActionText`, `TextOverflow` only if truly unused; the top bar title still uses `TextOverflow`).

- [ ] **Step 3: Sell screen** (`M/ui/sell/SellTransactionReceiptScreen.kt`). Replace the `onShareWhatsApp` parameter with:

```kotlin
    whatsAppApps: () -> List<WhatsAppApp> = { emptyList() },
    onShareWhatsApp: ((text: String, phone: String?, app: WhatsAppApp?) -> Unit)? = null,
```

In the `ReceiptScaffold(` call delete the `shareLabel =` and `onShare =` lines and add:

```kotlin
        shareContent = if (entry != null && onShareWhatsApp != null) {
            {
                SlipSendBar(
                    recipientName = name,
                    recipientPhone = if (entry.isKhaate) null
                    else slipRecipientPhone(partyPhone, entry.vehicleNumber),
                    isEnglish = isEnglish,
                    installedApps = whatsAppApps,
                    onSend = { phone, app -> onShareWhatsApp(billText, phone, app) }
                )
            }
        } else null,
```

`partyPhone` still feeds the bill text and print HTML as before. Add the three imports.

- [ ] **Step 4: Preview** (`M/ui/slip/ReceiptPreviewScreen.kt`). Add parameters after `deal: Deal,`:

```kotlin
    ledgerPartyId: String? = null,
    isEnglish: Boolean = false,
```

Before `Scaffold(` add `val recipient = receiptPreviewRecipient(farmer, buyer, ledgerPartyId)`.

Replace the content of the bottom bar `Box` (the whole `if (onPrint != null) { Row ... } else { Button ... }`) with:

```kotlin
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SlipSendBar(
                        recipientName = recipient.name,
                        recipientPhone = whatsAppPhone(recipient.phone),
                        isEnglish = isEnglish,
                        installedApps = whatsAppShareManager::installedApps,
                        onSend = { phone, app ->
                            whatsAppShareManager.shareText(formattedSlipText, phone, app)
                            onShareSuccess?.invoke()
                        }
                    )
                    if (onPrint != null) {
                        Button(
                            onClick = onPrint,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MandiSurface,
                                contentColor = MandiTextPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MandiBorder)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(text = "प्रिंट करें", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
```

Replace the text `"यह संदेश सीधे किसान / व्यापारी के फोन पर भेजा जाएगा:"` with `if (isEnglish) "Check the message before sending:" else "भेजने से पहले संदेश देख लें:"`. Remove unused imports (`Send` icon, `MandiBtnSuccessBg`, `Color`, `Row` if unused).

- [ ] **Step 5: App.kt** (`M/App.kt`).

1. Line 90: `data class ReceiptPreview(val deal: Deal, val farmer: Party, val buyer: Party?, val ledgerPartyId: String? = null) : Screen`
2. Line 483 (ledger `onShareWhatsAppReceipt`): `currentScreen = Screen.ReceiptPreview(deal, farmer, buyer, ledgerPartyId = screen.partyId)`. Leave line 443 (after a deal save) unchanged.
3. `ReceiptPreviewScreen(` call (line 518): add `ledgerPartyId = screen.ledgerPartyId,` and `isEnglish = tradeSettings?.isEnglish ?: false,`.
4. Lines 808 and 1037: change `var receiptParty by remember {` to `var receiptParty by remember(screen.id) {` so one slip can never show the previous slip's number.
5. `BuyTransactionReceiptScreen(` call: add `partyPhone = receiptParty?.phone,` and `whatsAppApps = whatsAppShareManager::installedApps,`; change the share lambda's first two lines to:

```kotlin
                    onShareWhatsApp = { text, phone, app ->
                        whatsAppShareManager.shareText(text, phone, app)
```

6. `SellTransactionReceiptScreen(` call: add `whatsAppApps = whatsAppShareManager::installedApps,` and make the same lambda change. `partyPhone = receiptParty?.phone` is already there.

Lines 376 and 580 (day report) stay as they are.

- [ ] **Step 6:** Run:

```bash
./gradlew :composeApp:jvmTest
./gradlew :composeApp:assembleStagingDebug
./gradlew :composeApp:compileKotlinIosSimulatorArm64
grep -rn "shareText(" composeApp/src/commonMain
```

Expected: tests and builds pass as in Task 0. `grep` shows no `shareText(text, null)` on a slip; only the two day report calls pass `null` literally. Commit `feat(slip): send buy, sell and preview slips to the party's WhatsApp`.

---

### Task 5: Device check on staging (Redmi `d93c743`)

- [ ] **Step 1:** `adb devices` lists `d93c743`; `./gradlew :composeApp:installStagingDebug`. Confirm the app talks to staging. Check which apps exist: `adb -s d93c743 shell pm list packages | grep -E "^package:com\.whatsapp(\.w4b)?$"`. Do not install or remove apps on the phone.
- [ ] **Step 2:** Use staging data only. For direct-send checks use a test party; ask Vivek for a number that is safe to open if you need one that is on WhatsApp. Never tap Send.
- [ ] **Step 3:** Record pass, fail or unverified for each:

| # | Check | Expected |
|---|---|---|
| 1 | Paid buy slip, party with number | "{name} को भेजें", number below, opens that chat with the full text filled in |
| 2 | Buy slip with money due | Pay Cash first and primary; send button below it in the secondary (outlined) style; both work |
| 3 | Sell slip, party with number | opens the buyer's chat |
| 4 | "दूसरा कॉन्टैक्ट" | WhatsApp contact picker with the text |
| 5 | खाते buy and sell slip | reason line and single "कॉन्टैक्ट चुनकर भेजें" |
| 6 | Slip whose party has no valid number | same as 5 |
| 7 | Change a party's number in the ledger, reopen an old slip | new number shown |
| 8 | Long Hindi buy slip (several crops and payments) containing `&` or `#` in a name | last line "धन्यवाद" or footer reaches WhatsApp intact. If text is cut, **stop and report**; do not invent a workaround |
| 9 | English setting | new labels in English |
| 10 | Both apps installed | sheet appears on both buttons; each choice opens that app; dismiss opens nothing. If only one app is installed, mark unverified |
| 11 | Preview from farmer ledger and from buyer ledger | each targets that party. Needs an older deal record; if staging has none, mark unverified |
| 12 | Preview after saving a deal edit | targets farmer |
| 13 | Day report share | opens the picker as before |
| 14 | Light and dark theme, long party name | readable, nothing clipped, slip content still scrolls above the bottom bar |
| 15 | Print button on buy, sell, preview | opens the print dialog (do not print) |

- [ ] **Step 4:** Fix failures, rebuild, recheck the failed rows. Delete screenshots that show contacts or phone numbers when done.

---

### Task 6: Tests and records

- [ ] **Step 1:** Create `composeApp/src/commonTest/kotlin/com/appwork/mandisamiti/ui/slip/SlipRecipientTest.kt`:

```kotlin
package com.appwork.mandisamiti.ui.slip

import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.platform.whatsAppSendUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SlipRecipientTest {

    private fun party(id: String, type: PartyType) = Party(
        id = id, shopId = "s", name = id, partyType = type, createdAt = 0L, updatedAt = 0L
    )

    @Test
    fun acceptsCommonWaysOfWritingAnIndianMobile() {
        listOf("9876543210", "+91 98765-43210", "919876543210", "09876543210", " (98765) 43210 ")
            .forEach { assertEquals("9876543210", whatsAppPhone(it), it) }
    }

    @Test
    fun rejectsAnythingThatIsNotClearlyOneMobile() {
        listOf(null, "", "98765", "1234567890", "98765432101", "9876543210 / 9123456780", "UP80AB1234", "UP 9876543210")
            .forEach { assertNull(whatsAppPhone(it), "$it") }
    }

    @Test
    fun accountNumberWinsAndSlipNumberIsTheFallback() {
        assertEquals("9876543210", slipRecipientPhone("9876543210", "9123456780"))
        assertEquals("9123456780", slipRecipientPhone(null, "9123456780"))
        assertEquals("9123456780", slipRecipientPhone("123", "9123456780"))
        assertNull(slipRecipientPhone(null, null))
    }

    @Test
    fun previewGoesToTheLedgerPartyOrTheFarmer() {
        val farmer = party("farmer", PartyType.FARMER)
        val buyer = party("buyer", PartyType.BUYER)
        assertEquals(buyer, receiptPreviewRecipient(farmer, buyer, "buyer"))
        assertEquals(farmer, receiptPreviewRecipient(farmer, buyer, "farmer"))
        assertEquals(farmer, receiptPreviewRecipient(farmer, buyer, null))
        assertEquals(farmer, receiptPreviewRecipient(farmer, null, "buyer"))
    }

    @Test
    fun linkEncodesHindiAndReservedCharacters() {
        assertEquals(
            "https://wa.me/919876543210?text=A%20%26%20B%23%2B%25%0A%E0%A4%95",
            whatsAppSendUrl("9876543210", "A & B#+%\nक")
        )
        assertEquals("https://wa.me/?text=ok-_.~", whatsAppSendUrl(null, "ok-_.~"))
    }
}
```

- [ ] **Step 2:** `./gradlew :composeApp:jvmTest` passes. If an iOS simulator is available, `./gradlew :composeApp:iosSimulatorArm64Test --tests '*SlipRecipientTest'`; otherwise say it was not run. In `BuyTransactionReceiptTest.verifyHindiAndEnglishLabels`, remove "पर्ची भेजें" and "Share slip" from the two label comments.
- [ ] **Step 3:** Append to the end of `features` in `FEATURES.json` an entry `{"id": "whatsapp-direct-send", "description": "Buy, sell and preview slips send to the party's WhatsApp number, with Other contact below", "status": ..., "evidence": ...}`. Status is `passing` only for what a test run or the dated Redmi check proves; if any Task 5 row failed, use `failing`. Name every unverified row (and iOS) in `evidence`.
- [ ] **Step 4:** Append one `PROGRESS.md` entry (date, runtime, changes, evidence, next step) and update your rows in `docs/STATUS.md`, with "push and PR" under Waiting on Vivek. Commit `test(share): slip recipient rules and verification record`.
- [ ] **Step 5:** Stop. Report in chat: commits, the Task 5 table with results, and what is unverified. Do not push, merge, deploy or touch the hub vault `hot.md`.
