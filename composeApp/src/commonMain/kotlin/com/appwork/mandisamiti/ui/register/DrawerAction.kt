package com.appwork.mandisamiti.ui.register

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.appwork.mandisamiti.ui.theme.*

@Composable
internal fun DrawerViewToggle(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    OutlinedIconToggleButton(
        checked = selected, onCheckedChange = { onClick() }, modifier = Modifier.size(48.dp),
        border = BorderStroke(1.dp, if (selected) MandiTextPrimary else MandiBorder),
        colors = IconButtonDefaults.outlinedIconToggleButtonColors(
            containerColor = MandiSurface, contentColor = MandiTextSecondary,
            checkedContainerColor = MandiSurfaceElevated, checkedContentColor = MandiTextPrimary,
        ),
    ) { Icon(icon, contentDescription = label) }
}
