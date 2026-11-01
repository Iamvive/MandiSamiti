package com.appwork.mandisamiti.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated

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
            shape = RoundedCornerShape(12.dp),
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
            shape = RoundedCornerShape(12.dp),
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

