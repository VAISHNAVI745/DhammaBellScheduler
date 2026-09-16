package org.dhamma.bell.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.bell.data.CourseTemplate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(viewModel: BellViewModel, onScheduled: () -> Unit) {
    val templates = viewModel.templates
    var selectedTemplate by remember { mutableStateOf<CourseTemplate?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    val resultMessage by viewModel.lastScheduleResult.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Schedule New Course", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        Text("Course Template", fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedTemplate?.displayName ?: "Select a course template",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) }
            )
            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                templates.forEach { template ->
                    DropdownMenuItem(
                        text = { Text(template.displayName) },
                        onClick = {
                            selectedTemplate = template
                            dropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("Start Date", fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedDate?.toString() ?: "Pick a start date")
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = System.currentTimeMillis()
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Spacer(Modifier.height(16.dp))

        if (selectedTemplate != null) {
            Text(
                text = "This template runs Day 0 through Day ${selectedTemplate!!.totalDaySpan}, " +
                    "${selectedTemplate!!.events.size} bell/audio events total.",
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                val template = selectedTemplate
                val date = selectedDate
                if (template != null && date != null) {
                    viewModel.scheduleCourse(template, date)
                }
            },
            enabled = selectedTemplate != null && selectedDate != null,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Schedule & Arm Bells", fontSize = 18.sp)
        }

        resultMessage?.let { msg ->
            Spacer(Modifier.height(16.dp))
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text(msg)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = {
                            viewModel.clearResultMessage()
                            onScheduled()
                        }) { Text("OK") }
                    }
                }
            }
        }
    }
}
