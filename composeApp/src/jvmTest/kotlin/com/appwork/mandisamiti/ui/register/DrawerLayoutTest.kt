package com.appwork.mandisamiti.ui.register

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.scene.MultiLayerComposeScene
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformRootForTest
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.appwork.mandisamiti.data.repository.*
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.*
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import com.appwork.mandisamiti.ui.theme.MandiSamitiTheme
import kotlinx.coroutines.*
import kotlinx.datetime.*
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import javax.swing.SwingUtilities
import kotlin.test.*

/** Real Compose layout regression with demo data; no network, phone, disk database, or audio. */
@OptIn(ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)
class DrawerLayoutTest {
    @Test fun readableHindiAndEnglishAtPhoneAndDesktopWidths() {
        var error: Throwable? = null
        SwingUtilities.invokeAndWait {
            try {
                for (width in listOf(390, 1000)) for (dark in listOf(false, true)) for (english in listOf(false, true)) {
                    renderAndCheck(width, dark, english)
                }
            } catch (failure: Throwable) { error = failure }
        }
        error?.let { throw it }
    }

    private fun renderAndCheck(width: Int, dark: Boolean, english: Boolean) = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val db = createTestDatabase()
        val cash = OfflineFirstCashTransactionRepository(db, Dispatchers.Unconfined)
        val parties = OfflineFirstPartyRepository(db, Dispatchers.Unconfined)
        val shops = OfflineFirstShopProfileRepository(db, Dispatchers.Unconfined)
        val now = Instant.parse("2026-10-31T10:00:00Z")
        shops.saveShopProfile(ShopProfile("demo", "DEMO Shop", "Demo Owner", "Mathura", phoneNumber = "9000000000",
            pinHash = "demo", isSoundEnabled = false, createdAt = 1, updatedAt = 1))
        val name = if (english) "Lalit Kumar Sharma" else "ललित कुमार शर्मा"
        parties.saveParty(Party("party", "demo", name, village = "Mathura", partyType = PartyType.FARMER, createdAt = 1, updatedAt = 1))
        cash.recordTransaction(CashTransaction("cash", "demo", "party", transactionType = TransactionType.JAMA_RECEIVED,
            amountPaisa = 15_850_000, transactionDate = now.toEpochMilliseconds(), createdAt = 1, updatedAt = 1))
        val vm = DailyRegisterViewModel("demo", cash, parties, shops, SoundboxTtsManager(),
            viewModelScope = scope, clock = object : Clock { override fun now() = now }, timeZone = TimeZone.UTC,
            drawerCountRepository = OfflineFirstDrawerCountRepository(db, Dispatchers.Unconfined))
        val roots = mutableSetOf<PlatformRootForTest>()
        val platform = object : PlatformContext by PlatformContext.Empty {
            override val windowInfo = object : androidx.compose.ui.platform.WindowInfo {
                override val isWindowFocused = true
                override val containerSize = IntSize(width, 900)
            }
            override val rootForTestListener = object : PlatformContext.RootForTestListener {
                override fun onRootForTestCreated(root: PlatformRootForTest) { roots += root }
                override fun onRootForTestDisposed(root: PlatformRootForTest) { roots -= root }
            }
        }
        val scene = MultiLayerComposeScene(density = Density(1f), size = IntSize(width, 900),
            coroutineContext = Dispatchers.Unconfined, composeSceneContext = object : ComposeSceneContext {
                override val platformContext = platform
            })
        val surface = org.jetbrains.skia.Surface.makeRasterN32Premul(width, 900)
        scene.setContent {
            MandiSamitiTheme(darkTheme = dark) {
                DailyCashRegisterScreen(vm, isEnglish = english, showBackButton = false)
            }
        }
        try {
            fun frame() { scene.render(surface.canvas.asComposeCanvas(), System.nanoTime()) }
            repeat(3) { frame() }
            val nodes = roots.flatMap { flatten(it.semanticsOwner.unmergedRootSemanticsNode) }
            val countText = if (english) "Count cash · Close day" else "नकद गिनें · दिन बंद करें"
            assertTrue(nodes.any { it.texts().contains(countText) }, "Counting action missing")
            assertTrue(nodes.any { it.texts().contains(name) }, "Party identity missing")
            val summary = nodes.first { it.config.getOrNull(SemanticsProperties.TestTag) == "drawer-summary" }
            assertTrue(summary.boundsInRoot.height <= 190f, "Summary card (with count button) too tall; actual=${summary.boundsInRoot.height}")
            val ledger = nodes.first { it.config.getOrNull(SemanticsProperties.TestTag) == "drawer-ledger" }
            assertTrue(ledger.boundsInRoot.height >= 900f * 0.60f,
                "Bahi needs at least 60% of screen height; actual=${ledger.boundsInRoot.height}")
            val output = File("build/drawer-review/${width}-${if (dark) "dark" else "light"}-${if (english) "en" else "hi"}.png")
            output.parentFile.mkdirs()
            surface.makeImageSnapshot().use { image ->
                output.writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
            nodes.forEach { node ->
                val layouts = mutableListOf<TextLayoutResult>()
                node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
                layouts.forEach { layout ->
                    // Skia's fractional paragraph height may exceed its rounded IntSize by <1 pixel.
                    assertFalse((0 until layout.lineCount).any { line ->
                        layout.getLineRight(line) - layout.getLineLeft(line) > layout.size.width + 1 ||
                            layout.getLineBottom(line) > layout.size.height + 1 || layout.isLineEllipsized(line)
                    }, "Text overflow at width=$width dark=$dark: ${layout.layoutInput.text}; size=${layout.size}, lines=${layout.lineCount}, lastBottom=${layout.getLineBottom(layout.lineCount - 1)}")
                }
                if (node.texts().isNotEmpty() && node.boundsInRoot.width > 0) {
                    assertTrue(node.boundsInRoot.left >= -1 && node.boundsInRoot.right <= width + 1,
                        "Horizontal overflow: ${node.texts()} ${node.boundsInRoot}")
                }
            }
            fun click(label: String) {
                val current = roots.flatMap { flatten(it.semanticsOwner.rootSemanticsNode) }
                val match = current.lastOrNull { it.texts().contains(label) && it.config.getOrNull(SemanticsActions.OnClick) != null }
                if (match == null) {
                    val available = current.map { it.texts() to (it.config.getOrNull(SemanticsActions.OnClick) != null) }
                    error("Cannot find clickable element with label: '$label'. Available nodes: $available")
                }
                assertEquals(true, match.config[SemanticsActions.OnClick].action?.invoke())
                repeat(3) { frame() }
            }
            click(name)
            click(if (english) "Close details" else "विवरण बंद करें")
            click(countText)
            // If already in counter mode (useDenominations == true), clicking the active tab is disabled.
            // Tap "2" on built-in pad for selected ₹500
            click("2")
            repeat(3) { frame() }
            val dialogOutput = File(output.parentFile, "${width}-${if (dark) "dark" else "light"}-${if (english) "en" else "hi"}-count.png")
            surface.makeImageSnapshot().use { image -> dialogOutput.writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes) }
            click(if (english) "Save count" else "गिनती सहेजें")
            assertEquals(100_000L, vm.uiState.value.physicalCountPaisa)
            assertFalse(vm.uiState.value.isDayClosed)
            click(countText)
            // Tap close with difference button -> opens reason picker
            val closeWithDiffLabel = if (english) "Close with ₹1,57,500 short" else "₹1,57,500 कमी के साथ बंद करें"
            click(closeWithDiffLabel)
            // Pick reason
            click(if (english) "Payment not entered" else "भुगतान बाकी")
            // Tap close with difference again -> opens confirmation dialog
            click(closeWithDiffLabel)
            click(if (english) "Close day" else "दिन बंद करें") // confirmation dialog
            assertTrue(vm.uiState.value.isDayClosed)
            assertEquals(-15_750_000L, vm.uiState.value.discrepancyPaisa)
        } finally {
            scene.close()
            surface.close()
            scope.cancel()
        }
    }

    private fun flatten(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::flatten)
    private fun SemanticsNode.texts() = config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
        config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
}
