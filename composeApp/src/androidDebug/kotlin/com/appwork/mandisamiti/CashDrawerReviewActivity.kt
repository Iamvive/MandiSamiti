package com.appwork.mandisamiti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appwork.mandisamiti.data.repository.*
import com.appwork.mandisamiti.database.DriverFactory
import com.appwork.mandisamiti.database.createDatabase
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import com.appwork.mandisamiti.review.DrawerReviewScenario
import com.appwork.mandisamiti.ui.register.DailyCashRegisterScreen
import com.appwork.mandisamiti.ui.register.DailyRegisterViewModel
import com.appwork.mandisamiti.ui.theme.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone

/** Only registered in the opt-in, network-disabled .drawerreview app. */
class CashDrawerReviewActivity : ComponentActivity() {
    private val reviewScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var sound: SoundboxTtsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(packageName.endsWith(".drawerreview"))
        val database = createDatabase(DriverFactory(applicationContext))
        sound = SoundboxTtsManager(applicationContext)
        setContent {
            var ready by remember { mutableStateOf(false) }
            var dark by remember { mutableStateOf(false) }
            var english by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) { DrawerReviewScenario.seed(database) }
                ready = true
            }
            MandiSamitiTheme(darkTheme = dark) {
                Column(Modifier.fillMaxSize().systemBarsPadding()) {
                    Surface(color = MandiSurface) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TEST · 30 / 31 Oct · offline", color = MandiTextPrimary, modifier = Modifier.weight(1f).padding(vertical = 14.dp))
                            IconButton(onClick = { dark = !dark }) {
                                Icon(Icons.Default.DarkMode, contentDescription = "Toggle review theme", tint = MandiTextPrimary)
                            }
                            IconButton(onClick = { english = !english }) {
                                Icon(Icons.Default.Translate, contentDescription = "Toggle review language", tint = MandiTextPrimary)
                            }
                        }
                    }
                    if (ready) {
                        val vm = remember {
                            DailyRegisterViewModel(
                                shopId = DrawerReviewScenario.shopId,
                                cashRepository = OfflineFirstCashTransactionRepository(database),
                                partyRepository = OfflineFirstPartyRepository(database),
                                shopProfileRepository = OfflineFirstShopProfileRepository(database),
                                expenseRepository = OfflineFirstExpenseRepository(database),
                                drawerCountRepository = OfflineFirstDrawerCountRepository(database),
                                ttsManager = sound, viewModelScope = reviewScope,
                                clock = object : Clock { override fun now() = DrawerReviewScenario.now },
                                timeZone = TimeZone.of("Asia/Kolkata"),
                            )
                        }
                        DailyCashRegisterScreen(vm, isEnglish = english, showBackButton = false)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        reviewScope.cancel()
        if (::sound.isInitialized) sound.shutdown()
        super.onDestroy()
    }
}
