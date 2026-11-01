package com.appwork.mandisamiti.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

/**
 * Standardized MandiCard component conforming to NGDL §5.3.
 * Supports standard surface and elevated nested mode.
 */
@Composable
fun MandiCard(
    modifier: Modifier = Modifier,
    isElevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    spacing: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val containerColor = if (isElevated) MandiSurfaceElevated else MandiSurface
    val borderStroke = BorderStroke(1.dp, MandiBorder)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = borderStroke
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(spacing),
                content = content
            )
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = borderStroke
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(spacing),
                content = content
            )
        }
    }
}

/**
 * B final Card with internal row lines (.card.lines).
 * Used for "हिसाब पर्ची", "पर्ची के बाद का हिसाब" totals, and receipt bills.
 */
@Composable
fun MandiLinesCard(
    modifier: Modifier = Modifier,
    content: @Composable MandiLinesScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MandiSurface),
        border = BorderStroke(1.dp, MandiBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val scope = MandiLinesScopeImpl()
            scope.content()
        }
    }
}

interface MandiLinesScope {
    @Composable
    fun Line(
        label: String,
        amountText: String,
        isMuted: Boolean = true,
        modifier: Modifier = Modifier
    )

    @Composable
    fun TotalLine(
        label: String,
        amountText: String,
        largeAmount: Boolean = false,
        modifier: Modifier = Modifier
    )

    @Composable
    fun CustomRow(content: @Composable () -> Unit)
}

private class MandiLinesScopeImpl : MandiLinesScope {
    private var isFirst = true

    @Composable
    override fun Line(
        label: String,
        amountText: String,
        isMuted: Boolean,
        modifier: Modifier
    ) {
        if (!isFirst) {
            HorizontalDivider(color = MandiBorder, thickness = 1.dp)
        }
        isFirst = false
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = if (isMuted) MandiTextSecondary else MandiTextPrimary
            )
            Text(
                text = amountText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = MandiTextPrimary
            )
        }
    }

    @Composable
    override fun TotalLine(
        label: String,
        amountText: String,
        largeAmount: Boolean,
        modifier: Modifier
    ) {
        HorizontalDivider(color = MandiBorder, thickness = 1.dp)
        isFirst = false
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MandiTextPrimary
            )
            Text(
                text = amountText,
                fontSize = if (largeAmount) 20.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                color = MandiTextPrimary
            )
        }
    }

    @Composable
    override fun CustomRow(content: @Composable () -> Unit) {
        if (!isFirst) {
            HorizontalDivider(color = MandiBorder, thickness = 1.dp)
        }
        isFirst = false
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            content()
        }
    }
}
