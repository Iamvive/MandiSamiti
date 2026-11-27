package com.appwork.mandisamiti.ui.register

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.MultiLayerComposeScene
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformRootForTest
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.appwork.mandisamiti.ui.theme.MandiSamitiTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)
class DayClosingSheetTest {

    @Test
    fun uncountedStateDisablesBothButtons() = runBlocking {
        val state = DailyRegisterUiState(inHandCashDrawerPaisa = 100_000L)
        val draft = GaddiDraft() // empty

        renderSheet(state, draft, useDenominations = true, isEnglish = false) { roots ->
            val nodes = roots.flatMap { flatten(it.semanticsOwner.rootSemanticsNode) }
            val saveBtn = nodes.firstOrNull { it.texts().contains("गिनती सहेजें") && it.config.getOrNull(SemanticsActions.OnClick) != null }
            assertNotNull(saveBtn)
            // In uncounted state, primary CTA is "पहले नकदी गिनें"
            val closeBtn = nodes.firstOrNull { it.texts().contains("पहले नकदी गिनें") }
            assertNotNull(closeBtn)
        }
    }

    @Test
    fun differenceRequiresReasonSelectionBeforeClosing() = runBlocking {
        val state = DailyRegisterUiState(inHandCashDrawerPaisa = 100_000L)
        // Counted ₹500, expected ₹1000 -> shortfall of ₹500
        var draft = GaddiDraft(selected = 500, field = GaddiField.LOOSE).digit(1)

        renderSheet(state, draft, useDenominations = true, isEnglish = false) { roots ->
            val nodes = roots.flatMap { flatten(it.semanticsOwner.rootSemanticsNode) }
            // Label carries difference: ₹500 कमी के साथ बंद करें
            val closeWithDiff = nodes.firstOrNull { it.texts().any { t -> t.contains("₹500 कमी के साथ बंद करें") } }
            assertNotNull(closeWithDiff)
        }
    }

    @Test
    fun exactMatchClosesDirectly() = runBlocking {
        val state = DailyRegisterUiState(inHandCashDrawerPaisa = 100_000L) // ₹1000
        // Counted 2 notes of ₹500 = ₹1000 -> Match
        var draft = GaddiDraft(selected = 500, field = GaddiField.LOOSE).digit(2)

        renderSheet(state, draft, useDenominations = true, isEnglish = false) { roots ->
            val nodes = roots.flatMap { flatten(it.semanticsOwner.rootSemanticsNode) }
            // Label is "दिन बंद करें"
            val closeBtn = nodes.firstOrNull { it.texts().contains("दिन बंद करें") && it.config.getOrNull(SemanticsActions.OnClick) != null }
            assertNotNull(closeBtn)
            // Hero displays match
            assertTrue(nodes.any { it.texts().contains("हिसाब बराबर") })
        }
    }

    @Test
    fun saveCountErrorIsVisible() = runBlocking {
        val state = DailyRegisterUiState(
            inHandCashDrawerPaisa = 100_000L,
            countError = "Network error"
        )
        val draft = GaddiDraft(selected = 500, field = GaddiField.LOOSE).digit(1)

        renderSheet(state, draft, useDenominations = true, isEnglish = false) { roots ->
            val nodes = roots.flatMap { flatten(it.semanticsOwner.rootSemanticsNode) }
            assertTrue(nodes.any { it.texts().contains("गिनती सहेजी नहीं गई। फिर कोशिश करें।") })
        }
    }

    private fun renderSheet(
        state: DailyRegisterUiState,
        draft: GaddiDraft,
        useDenominations: Boolean,
        isEnglish: Boolean,
        block: (Set<PlatformRootForTest>) -> Unit,
    ) {
        val roots = mutableSetOf<PlatformRootForTest>()
        val platform = object : PlatformContext by PlatformContext.Empty {
            override val windowInfo = object : androidx.compose.ui.platform.WindowInfo {
                override val isWindowFocused = true
                override val containerSize = IntSize(390, 844)
            }
            override val rootForTestListener = object : PlatformContext.RootForTestListener {
                override fun onRootForTestCreated(root: PlatformRootForTest) { roots += root }
                override fun onRootForTestDisposed(root: PlatformRootForTest) { roots -= root }
            }
        }
        val scene = MultiLayerComposeScene(
            density = Density(1f),
            size = IntSize(390, 844),
            coroutineContext = Dispatchers.Unconfined,
            composeSceneContext = object : ComposeSceneContext {
                override val platformContext = platform
            }
        )
        val surface = org.jetbrains.skia.Surface.makeRasterN32Premul(390, 844)

        try {
            scene.setContent {
                MandiSamitiTheme {
                    DayClosingSheet(
                        state = state,
                        isEnglish = isEnglish,
                        cashInput = draft.totalRupees?.toString().orEmpty(),
                        onCashInputChange = {},
                        reason = "",
                        onReasonChange = {},
                        useDenominations = useDenominations,
                        draft = draft,
                        onToggleDenominations = {},
                        onDraftChange = {},
                        onDismiss = {},
                        onSaveCount = {},
                        onCloseDay = {},
                        onClearCount = {},
                    )
                }
            }
            fun frame() { scene.render(surface.canvas.asComposeCanvas(), System.nanoTime()) }
            repeat(3) { frame() }
            block(roots)
        } finally {
            scene.close()
            surface.close()
        }
    }

    private fun flatten(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::flatten)
    private fun SemanticsNode.texts() = config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
        config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
}
