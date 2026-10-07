package com.appwork.mandisamiti.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavigationTab(
    val titleHi: String,
    val titleEn: String,
    val icon: ImageVector
) {
    DASHBOARD(
        titleHi = "डैशबोर्ड",
        titleEn = "Dashboard",
        icon = Icons.Default.Dashboard
    ),
    KHATA(
        titleHi = "खाता बही",
        titleEn = "Ledger",
        icon = Icons.Default.People
    ),
    GALLA(
        titleHi = "गल्ला रोकड़",
        titleEn = "Cash Drawer",
        icon = Icons.Default.AccountBalanceWallet
    ),
    SETTINGS(
        titleHi = "सेटिंग्स",
        titleEn = "Settings",
        icon = Icons.Default.Settings
    );

    fun getTitle(isEnglish: Boolean): String = if (isEnglish) titleEn else titleHi
}
