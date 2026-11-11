package com.appwork.mandisamiti.ui.stock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.BuyCropLot
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.CommodityParser
import com.appwork.mandisamiti.domain.model.SellCropLot
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.StockLine
import com.appwork.mandisamiti.domain.model.StockVarietyLine
import com.appwork.mandisamiti.domain.model.slipLabel
import com.appwork.mandisamiti.ui.theme.MandiAmberBorder
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import com.appwork.mandisamiti.ui.theme.MandiAmberLight
import com.appwork.mandisamiti.ui.theme.MandiAmberPrimary
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiBlueSell
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenLight
import com.appwork.mandisamiti.ui.theme.MandiGreenReceivable
import com.appwork.mandisamiti.ui.theme.MandiGreenText
import com.appwork.mandisamiti.ui.theme.MandiNavy
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
import com.appwork.mandisamiti.ui.theme.MandiRedBorder
import com.appwork.mandisamiti.ui.theme.MandiRedLight
import com.appwork.mandisamiti.ui.theme.MandiRedPayable
import com.appwork.mandisamiti.ui.theme.MandiRedText
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlin.math.abs

enum class StockViewMode {
    BALANCE,
    MOVEMENT
}

/**
 * 10/10 Godown Stock Register Tab (गोदाम स्टॉक रजिस्टर).
 * Designed according to CRED / Figma craft:
 * - Direct physical bag counting (बोरी / कट्टा) with quintal ledger grounding
 * - Separation of Shop Stock vs Farmer Custody Stock (अमानत / रोकड़)
 * - Single-tap Stock Reconciliation (स्टॉक मिलान / घट-बढ़ / शुरुआती स्टॉक)
 * - Replaced misleading progress bar with 3-way In-Out-Available ledger
 * - Zero filter clutter with clean 2-way mode switch
 */
@Composable
fun StockRegisterScreen(
    stockLines: List<StockLine>,
    isEnglish: Boolean,
    buys: List<BuyEntry> = emptyList(),
    sells: List<SellEntry> = emptyList(),
    partyNames: Map<String, String> = emptyMap(),
    onOpenBuy: (String) -> Unit = {},
    onOpenSell: (String) -> Unit = {},
    onSaveStockAudit: ((commodityId: String, physicalBags: Long, bagKg: Double, reason: String, farmerCustodyBags: Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bills = remember(buys, sells, partyNames) { buildBills(buys, sells, partyNames) }
    var viewMode by remember { mutableStateOf(StockViewMode.BALANCE) }
    var selectedGrain by remember { mutableStateOf<String?>(null) }
    var showAuditSheet by remember { mutableStateOf(false) }
    var auditTargetCrop by remember { mutableStateOf("") }

    val visibleLines = stockLines.filter { selectedGrain == null || it.commodityId == selectedGrain }
    val totalBags = stockLines.sumOf { it.onHandBags }
    val totalGrams = stockLines.sumOf { it.onHandGrams }
    val totalOwnBags = stockLines.sumOf { it.ownStockBags }
    val totalFarmerCustodyBags = stockLines.sumOf { it.farmerCustodyBags }

    val recentMovementBills = remember(bills, selectedGrain) {
        val all = if (selectedGrain == null) bills.values.flatten() else bills[selectedGrain].orEmpty()
        all.sortedByDescending { it.at }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MandiBackground),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 64.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Bar / Screen Header
        item(key = "godown_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Godown Inventory" else "गोदाम बहीखाता",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MandiGreenLight,
                            border = BorderStroke(1.dp, MandiGreenBorder)
                        ) {
                            Text(
                                text = if (isEnglish) "● LIVE" else "● लाइव",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiGreenText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = if (isEnglish) "Live stock & physical shed ledger" else "लाइव स्टॉक एवं गोदाम आवक-निकासी",
                        style = MaterialTheme.typography.bodySmall,
                        color = MandiTextSecondary
                    )
                }

                if (onSaveStockAudit != null) {
                    Button(
                        onClick = {
                            auditTargetCrop = stockLines.firstOrNull()?.commodityId ?: "गेहूं"
                            showAuditSheet = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MandiNavy,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isEnglish) "Audit Stock" else "स्टॉक मिलाएँ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. View Mode Segmented Bar (Zero Clutter Navigation)
        item(key = "godown_view_mode") {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MandiSurfaceElevated,
                border = BorderStroke(1.dp, MandiBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isBal = viewMode == StockViewMode.BALANCE
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isBal) MandiSurface else Color.Transparent,
                        border = if (isBal) BorderStroke(1.dp, MandiBorder) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewMode = StockViewMode.BALANCE }
                    ) {
                        Text(
                            text = if (isEnglish) "Stock Balance" else "उपलब्ध स्टॉक (Balance)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isBal) FontWeight.Bold else FontWeight.Medium,
                            color = if (isBal) MandiTextPrimary else MandiTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    val isMov = viewMode == StockViewMode.MOVEMENT
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isMov) MandiSurface else Color.Transparent,
                        border = if (isMov) BorderStroke(1.dp, MandiBorder) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewMode = StockViewMode.MOVEMENT }
                    ) {
                        Text(
                            text = if (isEnglish) "Daily Movement" else "आज की आवक-जावक (${recentMovementBills.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isMov) FontWeight.Bold else FontWeight.Medium,
                            color = if (isMov) MandiTextPrimary else MandiTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 3. Hero Card: Physical Bags & Custody Split
        item(key = "godown_hero") {
            GodownHeroCard(
                totalBags = totalBags,
                totalGrams = totalGrams,
                totalOwnBags = totalOwnBags,
                totalFarmerCustodyBags = totalFarmerCustodyBags,
                lastAuditedAt = stockLines.mapNotNull { it.lastAuditedAt }.maxOrNull(),
                isEnglish = isEnglish
            )
        }

        // 4. Quick Crop Filter Pills
        if (stockLines.isNotEmpty()) {
            item(key = "godown_crop_pills") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedGrain == null,
                            onClick = { selectedGrain = null },
                            label = {
                                Text(
                                    if (isEnglish) "All (${stockLines.size})" else "सभी (${stockLines.size})",
                                    fontWeight = if (selectedGrain == null) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MandiNavy,
                                selectedLabelColor = Color.White,
                                containerColor = MandiSurfaceElevated,
                                labelColor = MandiTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedGrain == null,
                                borderColor = MandiBorder,
                                selectedBorderColor = MandiNavy
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    items(stockLines, key = { it.commodityId }) { line ->
                        val isSelected = selectedGrain == line.commodityId
                        val unitLabel = if (line.commodityId.contains("सरसों") || line.commodityId.contains("mustard", ignoreCase = true)) {
                            if (isEnglish) "bags" else "कट्टे"
                        } else {
                            if (isEnglish) "bags" else "बोरी"
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedGrain = if (isSelected) null else line.commodityId },
                            label = {
                                Text(
                                    "${line.commodityId} (${line.onHandBags} $unitLabel)",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MandiNavy,
                                selectedLabelColor = Color.White,
                                containerColor = MandiSurfaceElevated,
                                labelColor = MandiTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MandiBorder,
                                selectedBorderColor = MandiNavy
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        // 5. Main Body depending on ViewMode
        if (viewMode == StockViewMode.BALANCE) {
            if (visibleLines.isEmpty()) {
                item(key = "godown_empty") {
                    StockEmptyState(
                        hasStockInStore = stockLines.isNotEmpty(),
                        query = selectedGrain.orEmpty(),
                        isEnglish = isEnglish,
                        onResetFilter = { selectedGrain = null }
                    )
                }
            } else {
                items(visibleLines, key = { it.commodityId }) { line ->
                    GodownCommodityLedgerCard(
                        line = line,
                        isEnglish = isEnglish,
                        bills = bills[line.commodityId].orEmpty(),
                        onOpenBuy = onOpenBuy,
                        onOpenSell = onOpenSell,
                        onReconcile = {
                            auditTargetCrop = line.commodityId
                            showAuditSheet = true
                        }
                    )
                }
            }
        } else {
            // MOVEMENT View
            if (recentMovementBills.isEmpty()) {
                item(key = "godown_movement_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEnglish) "No grain transactions found" else "कोई आवक या निकासी पर्चा नहीं मिला",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MandiTextMuted
                        )
                    }
                }
            } else {
                items(recentMovementBills, key = { "${it.id}_${it.grams}_${it.isBuy}" }) { bill ->
                    StockBillItemRow(
                        bill = bill,
                        isEnglish = isEnglish,
                        onClick = { if (bill.isBuy) onOpenBuy(bill.id) else onOpenSell(bill.id) }
                    )
                }
            }
        }
    }

    // Interactive Stock Reconciliation Modal Bottom Sheet
    if (showAuditSheet && onSaveStockAudit != null) {
        StockReconciliationSheet(
            stockLines = stockLines,
            initialCrop = auditTargetCrop.ifBlank { stockLines.firstOrNull()?.commodityId ?: "गेहूं" },
            isEnglish = isEnglish,
            onDismiss = { showAuditSheet = false },
            onSave = { crop, physicalBags, bagKg, reason, custodyBags ->
                onSaveStockAudit(crop, physicalBags, bagKg, reason, custodyBags)
                showAuditSheet = false
            }
        )
    }
}

/**
 * Hero Card: Real Physical Stock in Shed with Custody Split
 */
@Composable
private fun GodownHeroCard(
    totalBags: Long,
    totalGrams: Long,
    totalOwnBags: Long,
    totalFarmerCustodyBags: Long,
    lastAuditedAt: Long?,
    isEnglish: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MandiSurface),
        border = BorderStroke(1.dp, MandiBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEnglish) "TOTAL PHYSICAL SHED STOCK" else "गोदाम में कुल उपलब्ध माल (शेष)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MandiTextSecondary,
                    letterSpacing = 0.5.sp
                )
                if (lastAuditedAt != null && lastAuditedAt > 0L) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MandiGreenLight,
                        border = BorderStroke(1.dp, MandiGreenBorder)
                    ) {
                        Text(
                            text = if (isEnglish) "✓ Audited" else "✓ सत्यापित",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiGreenText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Big Numbers: Bags + Quintals
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${MandiMathEngine.formatIndianGrouping(totalBags)}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MandiTextPrimary
                )
                Text(
                    text = if (isEnglish) "Bags" else "बोरी / कट्टा",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MandiTextSecondary,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MandiSurfaceElevated,
                    border = BorderStroke(1.dp, MandiBorder)
                ) {
                    Text(
                        text = "${formatQuintals(totalGrams)} Qtl",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MandiNavy,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Custody Split: Own Stock vs Farmer Custody
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Shop Owned Stock
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MandiSurfaceElevated,
                    border = BorderStroke(1.dp, MandiBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Shop's Own Stock" else "दुकान का खुद का माल",
                            fontSize = 10.sp,
                            color = MandiTextSecondary
                        )
                        Text(
                            text = "${MandiMathEngine.formatIndianGrouping(totalOwnBags)} ${if (isEnglish) "Bags" else "बोरी"}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                    }
                }

                // Farmer Custody
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (totalFarmerCustodyBags > 0) MandiAmberLight else MandiSurfaceElevated,
                    border = BorderStroke(1.dp, if (totalFarmerCustodyBags > 0) MandiAmberBorder else MandiBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Farmer Custody" else "किसान की अमानत (रोकड़)",
                            fontSize = 10.sp,
                            color = if (totalFarmerCustodyBags > 0) MandiAmberDark else MandiTextSecondary
                        )
                        Text(
                            text = "${MandiMathEngine.formatIndianGrouping(totalFarmerCustodyBags)} ${if (isEnglish) "Bags" else "बोरी"}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (totalFarmerCustodyBags > 0) MandiAmberDark else MandiTextPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual Commodity Card: Replaces misleading progress bar with 3-Way In/Out/Balance Ledger
 */
@Composable
private fun GodownCommodityLedgerCard(
    line: StockLine,
    isEnglish: Boolean,
    bills: List<StockBill>,
    onOpenBuy: (String) -> Unit,
    onOpenSell: (String) -> Unit,
    onReconcile: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isNegative = line.onHandGrams < 0
    val unitLabel = if (line.commodityId.contains("सरसों") || line.commodityId.contains("mustard", ignoreCase = true)) {
        if (isEnglish) "Katta" else "कट्टे"
    } else {
        if (isEnglish) "Bags" else "बोरी"
    }

    val bagGrams = (line.standardBagKg * 1000.0).toLong()
    val boughtBags = if (bagGrams > 0) line.boughtGrams / bagGrams else 0L
    val soldBags = if (bagGrams > 0) line.soldGrams / bagGrams else 0L

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MandiSurface),
        border = BorderStroke(1.dp, if (isNegative) MandiRedBorder else MandiBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Avatar, Name, Unit, and Net Available Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isNegative) MandiRedLight else MandiSurfaceElevated)
                        .border(1.dp, if (isNegative) MandiRedBorder else MandiBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = line.commodityId.take(1),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isNegative) MandiRedText else MandiTextPrimary
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = line.commodityId,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                        if (line.varieties.size > 1) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MandiSurfaceElevated
                            ) {
                                Text(
                                    text = if (isEnglish) "${line.varieties.size} varieties" else "${line.varieties.size} किस्में",
                                    fontSize = 10.sp,
                                    color = MandiTextSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${if (isEnglish) "Std Bag: " else "मानक बोरी: "}${line.standardBagKg.toInt()} kg",
                        fontSize = 11.sp,
                        color = MandiTextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${line.onHandBags} $unitLabel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isNegative) MandiRedText else MandiGreenText
                    )
                    Text(
                        text = "${formatQuintals(line.onHandGrams)} Qtl",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MandiTextSecondary
                    )
                }

                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MandiTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 3-Way In/Out/Available Ledger (High Contrast, replaces broken progress bar)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MandiSurfaceElevated,
                border = BorderStroke(1.dp, MandiBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isEnglish) "Total Inward" else "कुल आवक",
                            fontSize = 10.sp,
                            color = MandiTextSecondary
                        )
                        Text(
                            text = "+$boughtBags $unitLabel",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MandiBorder))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isEnglish) "Total Outward" else "कुल निकासी",
                            fontSize = 10.sp,
                            color = MandiTextSecondary
                        )
                        Text(
                            text = "-$soldBags $unitLabel",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MandiBorder))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isEnglish) "In Shed (Balance)" else "उपलब्ध माल",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isNegative) MandiRedText else MandiGreenText
                        )
                        Text(
                            text = "${line.onHandBags} $unitLabel",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isNegative) MandiRedText else MandiGreenText
                        )
                    }
                }
            }

            // Farmer Custody notice if present
            if (line.farmerCustodyBags > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MandiAmberPrimary)
                    )
                    Text(
                        text = if (isEnglish) {
                            "${line.farmerCustodyBags} $unitLabel held as farmer custody (cannot sell)"
                        } else {
                            "${line.farmerCustodyBags} $unitLabel किसान अमानत/रोकड़ में शामिल (बिक्री योग्य नहीं)"
                        },
                        fontSize = 11.sp,
                        color = MandiAmberDark,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Quick Audit Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (line.lastAuditReason != null) {
                    Text(
                        text = "अंतिम समायोजन: ${line.lastAuditReason}",
                        fontSize = 10.sp,
                        color = MandiTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }

                Text(
                    text = if (isEnglish) "Adjust Stock →" else "घट-बढ़ दर्ज करें →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MandiNavy,
                    modifier = Modifier
                        .clickable(onClick = onReconcile)
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                )
            }

            // Expandable Section: Variety breakdown & recent bills
            if (expanded) {
                HorizontalDivider(color = MandiBorder, thickness = 1.dp)

                if (line.varieties.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isEnglish) "VARIETY BREAKDOWN" else "किस्मवार स्टॉक विवरण",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextSecondary
                        )
                        line.varieties.forEach { v ->
                            VarietyItemRow(v = v, isEnglish = isEnglish)
                        }
                    }
                }

                if (bills.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isEnglish) "Recent Slips (${bills.size})" else "हाल के सौदे (${bills.size} पर्चे)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextSecondary
                        )
                        bills.take(4).forEach { b ->
                            StockBillItemRow(
                                bill = b,
                                isEnglish = isEnglish,
                                onClick = { if (b.isBuy) onOpenBuy(b.id) else onOpenSell(b.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Modal Bottom Sheet for Physical Stock Audit / Opening Stock / Reconciliation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockReconciliationSheet(
    stockLines: List<StockLine>,
    initialCrop: String,
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onSave: (commodityId: String, physicalBags: Long, bagKg: Double, reason: String, farmerCustodyBags: Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedCrop by remember { mutableStateOf(initialCrop) }
    val currentLine = stockLines.firstOrNull { it.commodityId == selectedCrop }
    val defaultBagKg = currentLine?.standardBagKg ?: if (selectedCrop.contains("सरसों")) 70.0 else 50.0

    var physicalBagsInput by remember { mutableStateOf(currentLine?.onHandBags?.toString().orEmpty()) }
    var bagWeightInput by remember { mutableStateOf(defaultBagKg.toInt().toString()) }
    var farmerCustodyInput by remember { mutableStateOf(currentLine?.farmerCustodyBags?.toString().orEmpty()) }

    val reasons = listOf(
        if (isEnglish) "Moisture Shrinkage" else "नमी / सूख वजन कमी",
        if (isEnglish) "Damaged / Torn Bag" else "बोरी फटने से नुकसान",
        if (isEnglish) "Opening Stock Entry" else "प्रारंभिक स्टॉक दर्ज किया",
        if (isEnglish) "Audit Count Correction" else "गिनती संशोधन (ऑडिट सुधार)"
    )
    var selectedReason by remember { mutableStateOf(reasons.first()) }

    val physicalBags = physicalBagsInput.toLongOrNull() ?: 0L
    val bookBags = currentLine?.bookOnHandGrams?.let { if (defaultBagKg > 0) (it / (defaultBagKg * 1000.0)).toLong() else 0L } ?: 0L
    val diffBags = physicalBags - bookBags

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MandiSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isEnglish) "Stock Audit & Adjustment" else "स्टॉक मिलान व समायोजन",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary
                    )
                    Text(
                        text = if (isEnglish) "Reconcile floor count with ledger" else "गोदाम की वास्तविक गिनती दर्ज करें",
                        style = MaterialTheme.typography.bodySmall,
                        color = MandiTextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MandiTextSecondary)
                }
            }

            // 1. Commodity Selector Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isEnglish) "Select Commodity:" else "जिंस चुनें:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MandiTextSecondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val availableCrops = if (stockLines.isNotEmpty()) stockLines.map { it.commodityId } else listOf("गेहूं", "सरसों", "चना")
                    items(availableCrops) { crop ->
                        val isSel = crop == selectedCrop
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                selectedCrop = crop
                                val newLine = stockLines.firstOrNull { it.commodityId == crop }
                                physicalBagsInput = newLine?.onHandBags?.toString().orEmpty()
                                farmerCustodyInput = newLine?.farmerCustodyBags?.toString().orEmpty()
                                bagWeightInput = (newLine?.standardBagKg ?: if (crop.contains("सरसों")) 70.0 else 50.0).toInt().toString()
                            },
                            label = { Text(crop, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MandiNavy,
                                selectedLabelColor = Color.White,
                                containerColor = MandiSurfaceElevated,
                                labelColor = MandiTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSel,
                                borderColor = MandiBorder,
                                selectedBorderColor = MandiNavy
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // 2. Physical Count & Live Difference
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = physicalBagsInput,
                    onValueChange = { physicalBagsInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text(if (isEnglish) "Physical Bags" else "गोदाम में असली बोरी") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MandiSurface,
                        unfocusedContainerColor = MandiSurface,
                        focusedBorderColor = MandiNavy,
                        unfocusedBorderColor = MandiBorder
                    ),
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        diffBags > 0 -> MandiGreenLight
                        diffBags < 0 -> MandiAmberLight
                        else -> MandiSurfaceElevated
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            diffBags > 0 -> MandiGreenBorder
                            diffBags < 0 -> MandiAmberBorder
                            else -> MandiBorder
                        }
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Difference" else "घट-बढ़ (अंतर)",
                            fontSize = 10.sp,
                            color = MandiTextSecondary
                        )
                        Text(
                            text = when {
                                diffBags > 0 -> "+$diffBags ${if (isEnglish) "Bags (Surplus)" else "बोरी (अधिक)"}"
                                diffBags < 0 -> "$diffBags ${if (isEnglish) "Bags (Deficit)" else "बोरी (कमी)"}"
                                else -> if (isEnglish) "0 (Exact match)" else "0 (बराबर)"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                diffBags > 0 -> MandiGreenText
                                diffBags < 0 -> MandiAmberDark
                                else -> MandiTextPrimary
                            }
                        )
                    }
                }
            }

            // 3. Reason for Adjustment Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isEnglish) "Reason for Adjustment:" else "समायोजन का कारण:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MandiTextSecondary
                )
                reasons.forEach { r ->
                    val isSel = r == selectedReason
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) MandiSurfaceElevated else MandiSurface,
                        border = BorderStroke(1.dp, if (isSel) MandiNavy else MandiBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = r }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isSel) MandiNavy else MandiBorder)
                            )
                            Text(
                                text = r,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = MandiTextPrimary
                            )
                        }
                    }
                }
            }

            // 4. Packaging Weight & Farmer Custody Inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = bagWeightInput,
                    onValueChange = { bagWeightInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text(if (isEnglish) "Bag Weight (Kg)" else "बोरी वजन (कि.ग्रा.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MandiSurface,
                        unfocusedContainerColor = MandiSurface,
                        focusedBorderColor = MandiNavy,
                        unfocusedBorderColor = MandiBorder
                    ),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = farmerCustodyInput,
                    onValueChange = { farmerCustodyInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text(if (isEnglish) "Farmer Custody (Bags)" else "किसान अमानत (बोरी)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MandiSurface,
                        unfocusedContainerColor = MandiSurface,
                        focusedBorderColor = MandiNavy,
                        unfocusedBorderColor = MandiBorder
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(4.dp))

            // Save Audit Button
            Button(
                onClick = {
                    val bagKg = bagWeightInput.toDoubleOrNull() ?: defaultBagKg
                    val custodyBags = farmerCustodyInput.toLongOrNull() ?: 0L
                    onSave(selectedCrop, physicalBags, bagKg, selectedReason, custodyBags)
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MandiNavy,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = if (isEnglish) "Save Godown Audit" else "स्टॉक मिलान सहेजें",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

/**
 * Individual variety row with in/out summary and remaining balance pill.
 */
@Composable
private fun VarietyItemRow(v: StockVarietyLine, isEnglish: Boolean) {
    val isNeg = v.onHandGrams < 0
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MandiSurfaceElevated,
        border = BorderStroke(1.dp, MandiBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = v.varietyName.ifBlank { if (isEnglish) "General / Mixed" else "सामान्य / मिक्स" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MandiTextPrimary
                )
                Text(
                    text = (if (isEnglish) "In: " else "आवक: ") + "${formatQuintals(v.boughtGrams)} Q  |  " +
                        (if (isEnglish) "Out: " else "बिक्री: ") + "${formatQuintals(v.soldGrams)} Q",
                    style = MaterialTheme.typography.labelSmall,
                    color = MandiTextSecondary
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isNeg) MandiRedLight else MandiSurface,
                border = BorderStroke(1.dp, if (isNeg) MandiRedBorder else MandiBorder)
            ) {
                Text(
                    text = "${if (v.onHandGrams > 0) "+" else ""}${formatQuintals(v.onHandGrams)} Q",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isNeg) MandiRedText else MandiNavy,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Individual bill row with direction badge, party name, slip #, date, and tap action.
 */
@Composable
private fun StockBillItemRow(
    bill: StockBill,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MandiSurface,
        border = BorderStroke(1.dp, MandiBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (bill.isBuy) MandiGreenLight else MandiRedLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (bill.isBuy) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (bill.isBuy) MandiGreenReceivable else MandiRedPayable,
                    modifier = Modifier.size(15.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = bill.partyName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MandiTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MandiSurfaceElevated
                    ) {
                        Text(
                            text = "#${bill.slip}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MandiTextSecondary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = "${if (bill.isBuy) (if (isEnglish) "Purchase" else "खरीद") else (if (isEnglish) "Sale" else "बिक्री")} • ${bill.date}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MandiTextMuted
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${formatQuintals(bill.grams)} Q",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MandiTextPrimary
                )
                Text(
                    text = formatBags(bill.grams, isEnglish),
                    style = MaterialTheme.typography.labelSmall,
                    color = MandiTextSecondary
                )
            }

            Spacer(Modifier.width(6.dp))

            Icon(
                Icons.Default.Edit,
                contentDescription = if (isEnglish) "Open slip to edit" else "पर्चा खोलें",
                tint = MandiTextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Empty state when no inventory is recorded or search produces 0 results.
 */
@Composable
private fun StockEmptyState(
    hasStockInStore: Boolean,
    query: String,
    isEnglish: Boolean,
    onResetFilter: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MandiSurface),
        border = BorderStroke(1.dp, MandiBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MandiSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = MandiTextSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = if (!hasStockInStore) {
                    if (isEnglish) "No Stock Recorded Yet" else "गोदाम में अभी कोई स्टॉक नहीं है"
                } else {
                    if (isEnglish) "No commodity matches \"$query\"" else "\"$query\" से कोई जिंस नहीं मिली"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MandiTextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (!hasStockInStore) {
                    if (isEnglish) {
                        "Enter purchases or tap 'Audit Stock' above to record opening stock."
                    } else {
                        "खरीद दर्ज करें या ऊपर 'स्टॉक मिलाएँ' बटन दबाकर गोदाम का शुरुआती स्टॉक दर्ज करें।"
                    }
                } else {
                    if (isEnglish) {
                        "Try searching with a different commodity name or clear your filters."
                    } else {
                        "अलग नाम से खोजें या फ़िल्टर हटाकर पूरा स्टॉक देखें।"
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MandiTextSecondary,
                textAlign = TextAlign.Center
            )

            if (hasStockInStore) {
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onResetFilter,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MandiNavy)
                ) {
                    Text(
                        if (isEnglish) "Clear Filters" else "फ़िल्टर हटाएं",
                        color = MandiNavy,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Exact integer-based formatting for quintals (2 decimal places) without float precision drift.
 */
private fun formatQuintals(grams: Long): String {
    val isNeg = grams < 0
    val absGrams = abs(grams)
    val whole = absGrams / MandiMathEngine.GRAMS_PER_QUINTAL
    val fracTwo = ((absGrams % MandiMathEngine.GRAMS_PER_QUINTAL) / 1_000L)
        .toString()
        .padStart(2, '0')
        .take(2)
    val prefix = if (isNeg) "-" else ""
    return "$prefix$whole.$fracTwo"
}

/**
 * Standard mandi bag estimation (~50 kg / 0.50 Qtl per bag).
 */
private fun formatBags(grams: Long, isEnglish: Boolean): String {
    if (grams == 0L) return if (isEnglish) "~0 bags" else "~0 बोरी"
    val isNeg = grams < 0
    val absGrams = abs(grams)
    val bags = (absGrams + 25_000L) / 50_000L
    return if (isNeg) {
        if (isEnglish) "-$bags bags (Deficit)" else "-$bags बोरी (कमी)"
    } else {
        if (isEnglish) "~$bags bags" else "~$bags बोरी"
    }
}

private data class StockBill(
    val id: String,
    val isBuy: Boolean,
    val partyName: String,
    val slip: String,
    val date: String,
    val grams: Long,
    val at: Long
)

private fun buildBills(
    buys: List<BuyEntry>,
    sells: List<SellEntry>,
    names: Map<String, String>
): Map<String, List<StockBill>> {
    val json = Json { ignoreUnknownKeys = true }
    fun day(ms: Long) = Instant.fromEpochMilliseconds(ms)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()

    val out = mutableMapOf<String, MutableList<StockBill>>()

    buys.filter { !it.isVoid }.forEach { e ->
        val raw = e.remarks?.takeIf { it.contains("DATA: ") }?.substringAfter("DATA: ")?.trim()
        val lots = raw?.let { runCatching { json.decodeFromString<List<BuyCropLot>>(it) }.getOrNull() }
        if (!lots.isNullOrEmpty()) {
            lots.groupBy { CommodityParser.parseBaseCommodity(it.cropName) }.forEach { (base, ls) ->
                out.getOrPut(base) { mutableListOf() }.add(
                    StockBill(
                        id = e.id,
                        isBuy = true,
                        partyName = names[e.farmerId] ?: e.farmerId,
                        slip = slipLabel(e.slipNo, e.id),
                        date = day(e.entryDate),
                        grams = ls.sumOf { it.totalWeightGrams },
                        at = e.entryDate
                    )
                )
            }
        } else if (e.commodityId.isNotBlank() && e.totalWeightGrams > 0) {
            val base = CommodityParser.parseBaseCommodity(e.commodityId)
            out.getOrPut(base) { mutableListOf() }.add(
                StockBill(
                    id = e.id,
                    isBuy = true,
                    partyName = names[e.farmerId] ?: e.farmerId,
                    slip = slipLabel(e.slipNo, e.id),
                    date = day(e.entryDate),
                    grams = e.totalWeightGrams,
                    at = e.entryDate
                )
            )
        }
    }

    sells.filter { !it.isVoid }.forEach { e ->
        val raw = e.remarks?.trim()
        val lots = raw?.takeIf { it.startsWith("[") }?.let { runCatching { json.decodeFromString<List<SellCropLot>>(it) }.getOrNull() }
        if (!lots.isNullOrEmpty()) {
            lots.groupBy { CommodityParser.parseBaseCommodity(it.cropName) }.forEach { (base, ls) ->
                out.getOrPut(base) { mutableListOf() }.add(
                    StockBill(
                        id = e.id,
                        isBuy = false,
                        partyName = names[e.buyerId] ?: e.buyerId,
                        slip = slipLabel(e.slipNo, e.id),
                        date = day(e.entryDate),
                        grams = ls.sumOf { it.totalWeightGrams },
                        at = e.entryDate
                    )
                )
            }
        } else if (e.commodityId.isNotBlank() && e.totalWeightGrams > 0) {
            val base = CommodityParser.parseBaseCommodity(e.commodityId)
            out.getOrPut(base) { mutableListOf() }.add(
                StockBill(
                    id = e.id,
                    isBuy = false,
                    partyName = names[e.buyerId] ?: e.buyerId,
                    slip = slipLabel(e.slipNo, e.id),
                    date = day(e.entryDate),
                    grams = e.totalWeightGrams,
                    at = e.entryDate
                )
            )
        }
    }

    return out.mapValues { (_, v) -> v.sortedByDescending { it.at } }
}
