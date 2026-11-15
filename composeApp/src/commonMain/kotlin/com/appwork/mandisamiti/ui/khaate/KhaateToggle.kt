package com.appwork.mandisamiti.ui.khaate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.ui.components.MandiChip
import com.appwork.mandisamiti.ui.theme.MandiAmberBorder
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import com.appwork.mandisamiti.ui.theme.MandiAmberLight
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary

/** Mirrors strings.xml `khaate_toggle`. */
fun khaateToggleLabel(isEnglish: Boolean) = if (isEnglish) "Khaate (spare crop)" else "खाते (बचा माल)"

/** Mirrors strings.xml `khaate_short_warning`. */
fun khaateShortWarningText(isEnglish: Boolean) =
    if (isEnglish) "Khaate stock is lower than this sale. It will still be saved."
    else "खाते में इतना माल नहीं है। फिर भी सेव होगा।"

@Composable
fun KhaateToggleRow(isKhaate: Boolean, isEnglish: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MandiChip(label = khaateToggleLabel(isEnglish), selected = isKhaate, onClick = { onChange(!isKhaate) })
    }
}

@Composable
fun KhaateShortWarningBanner(isEnglish: Boolean, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MandiAmberLight,
        border = BorderStroke(1.dp, MandiAmberBorder),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MandiAmberDark, modifier = Modifier.size(18.dp))
            Text(
                text = khaateShortWarningText(isEnglish),
                fontSize = 13.sp,
                color = MandiTextPrimary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
