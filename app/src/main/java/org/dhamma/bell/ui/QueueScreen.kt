package org.dhamma.bell.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.bell.data.ScheduledBellEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun QueueScreen(viewModel: BellViewModel) {
    val upcoming by viewModel.upcomingBells.collectAsState()
    var showCancelConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Active Alarm Queue", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            if (upcoming.isNotEmpty()) {
                OutlinedButton(onClick = { showCancelConfirm = true }) {
                    Text("Cancel Course")
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        if (upcoming.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No upcoming bells. Schedule a course to see the alarm queue.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(upcoming, key = { it.id }) { bell ->
                    BellQueueRow(bell)
                }
            }
        }
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text("Cancel active course?") },
            text = { Text("This disarms every remaining bell for the currently scheduled course. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelActiveCourse()
                    showCancelConfirm = false
                }) { Text("Cancel Course") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) { Text("Keep It") }
            }
        )
    }
}

@Composable
private fun BellQueueRow(bell: ScheduledBellEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(bell.eventLabel, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Day ${bell.dayOffset} · ${bell.eventType.replace('_', ' ').lowercase()}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Text(formatDateTime(bell.triggerAtMillis), fontSize = 13.sp)
        }
    }
}

private fun formatDateTime(epochMillis: Long): String {
    val instant = Instant.ofEpochMilli(epochMillis)
    val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
    return formatter.withZone(ZoneId.systemDefault()).format(instant)
}
