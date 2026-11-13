package com.appwork.mandisamiti.ui.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.ui.home.PartyFilter
import com.appwork.mandisamiti.ui.home.PartyTypeFilter
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

internal fun isKhataFilterActive(statusFilter: PartyFilter, typeFilter: PartyTypeFilter): Boolean =
    statusFilter != PartyFilter.ALL || typeFilter != PartyTypeFilter.ALL

/** Explainer shows only for a loaded, empty list with at least one filter active. */
internal fun shouldShowKhataFilterExplainer(
    filteredIsEmpty: Boolean,
    isLoading: Boolean,
    statusFilter: PartyFilter,
    typeFilter: PartyTypeFilter
): Boolean = filteredIsEmpty && !isLoading && isKhataFilterActive(statusFilter, typeFilter)

@Composable
fun KhataFilterExplainerCard(
    isEnglish: Boolean,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .shadow(2.dp, shape)
            .clip(shape)
            .background(MandiSurface)
            .border(1.dp, MandiBorder, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = if (isEnglish) "Filters Explained" else "फ़िल्टर समझें",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MandiTextPrimary
        )
        Text(
            text = if (isEnglish) "No accounts match the chosen filters." else "चुने हुए फ़िल्टर से कोई खाता नहीं मिला।",
            fontSize = 13.sp,
            color = MandiTextSecondary
        )
        Spacer(Modifier.height(4.dp))
        ExplainerSection(
            title = if (isEnglish) "Farmer or Buyer" else "किसान या व्यापारी",
            lines = if (isEnglish) listOf(
                "All: every account",
                "Kisaan: farmers who sell us crop",
                "Vyapari: buyers who buy crop from us"
            ) else listOf(
                "सभी: सभी खाते",
                "किसान: फसल बेचने वाले",
                "व्यापारी: फसल खरीदने वाले"
            )
        )
        ExplainerSection(
            title = if (isEnglish) "Receivable or Payable" else "लेना है या देना है",
            lines = if (isEnglish) listOf(
                "All: every account",
                "Receivable: we have to receive money",
                "Payable: we have to pay money"
            ) else listOf(
                "सभी: सभी खाते",
                "लेना है: हमें पैसे मिलने हैं",
                "देना है: हमें पैसे देने हैं"
            )
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onResetFilters,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MandiPrimaryAction,
                contentColor = MandiPrimaryActionText
            )
        ) {
            Text(
                text = if (isEnglish) "Clear filters" else "फ़िल्टर साफ़ करें",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ExplainerSection(title: String, lines: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MandiSurfaceElevated)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MandiTextPrimary)
        lines.forEach { line ->
            Text(text = "• $line", fontSize = 13.sp, color = MandiTextSecondary)
        }
    }
}
