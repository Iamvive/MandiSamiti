package com.appwork.mandisamiti.ui.register

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appwork.mandisamiti.ui.theme.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DrawerDatePickerSheet(
    selected: LocalDate,
    earliest: LocalDate,
    latest: LocalDate,
    isEnglish: Boolean,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = selected.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.fromEpochMilliseconds(utcTimeMillis).toLocalDateTime(TimeZone.UTC).date
                return date in earliest..latest
            }
        },
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MandiSurface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 16.dp)) {
            DatePicker(
                state = state,
                title = null,
                headline = null,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(containerColor = MandiSurface),
            )
            Button(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onPick(Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date)
                    }
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MandiPrimaryAction, contentColor = MandiPrimaryActionText),
            ) {
                Text(if (isEnglish) "OK" else "ठीक है", fontWeight = FontWeight.Bold)
            }
        }
    }
}
