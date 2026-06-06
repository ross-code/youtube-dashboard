package com.duoplan.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.duoplan.app.data.model.PlanCategory
import com.duoplan.app.ui.asWhenLabel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Shared form for proposing a new plan or replacing a turned-down one. The
 * caller computes start/end and persists; this screen only collects input.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeRequestScreen(
    screenTitle: String,
    submitLabel: String,
    initialTitle: String = "",
    initialCategory: PlanCategory = PlanCategory.MEAL,
    busy: Boolean,
    onBack: () -> Unit,
    onSubmit: (title: String, category: PlanCategory, start: Instant, end: Instant) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    var title by remember { mutableStateOf(initialTitle) }
    var category by remember { mutableStateOf(initialCategory) }
    var date by remember { mutableStateOf(LocalDate.now(zone)) }
    var time by remember { mutableStateOf(LocalTime.of(19, 0)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val start = date.atTime(time).atZone(zone).toInstant()
    val end = start.plusSeconds(90 * 60)
    val today = LocalDate.now(zone)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                PlanCategory.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = category == option,
                        onClick = { category = option },
                        shape = SegmentedButtonDefaults.itemShape(index, PlanCategory.entries.size),
                    ) {
                        Text(if (option == PlanCategory.MEAL) "🍽️ Meal" else "🎉 Activity")
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(if (category == PlanCategory.MEAL) "What should we eat?" else "What should we do?") },
                placeholder = { Text(if (category == PlanCategory.MEAL) "Tacos from El Rey" else "Movie night at home") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
            )

            Text("When", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = date == today,
                    onClick = { date = today },
                    label = { Text("Tonight") },
                )
                FilterChip(
                    selected = date == today.plusDays(1),
                    onClick = { date = today.plusDays(1) },
                    label = { Text("Tomorrow") },
                )
                FilterChip(
                    selected = date != today && date != today.plusDays(1),
                    onClick = { showDatePicker = true },
                    label = { Text("Pick date") },
                )
            }
            OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text(start.asWhenLabel(zone))
            }

            Button(
                onClick = { onSubmit(title, category, start, end) },
                enabled = title.isNotBlank() && !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(submitLabel)
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = false,
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text("Pick a time", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                    TimePicker(state = timeState)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            time = LocalTime.of(timeState.hour, timeState.minute)
                            showTimePicker = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }
}
