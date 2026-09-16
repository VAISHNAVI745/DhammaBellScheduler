package org.dhamma.bell.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(viewModel: BellViewModel) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "DhammaBell Scheduler",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        if (state.courseDisplayName != null) {
            Text(
                text = "Active Course: ${state.courseDisplayName}",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text = "No course currently scheduled",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        Spacer(Modifier.height(32.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Next Bell",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(8.dp))

                val nextBell = state.nextBell
                if (nextBell != null) {
                    Text(
                        text = nextBell.eventLabel,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatDateTime(nextBell.triggerAtMillis),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = state.countdownText,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "No upcoming bells",
                        fontSize = 20.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        var buttonFocused by remember { mutableStateOf(false) }
        Button(
            onClick = { viewModel.testGongNow() },
            modifier = Modifier
                .height(64.dp)
                .fillMaxWidth(0.7f)
                .onFocusChanged { buttonFocused = it.isFocused }
                .background(
                    if (buttonFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                    RoundedCornerShape(12.dp)
                )
        ) {
            Text("Manual Test Gong", fontSize = 18.sp)
        }
    }
}

private fun formatDateTime(epochMillis: Long): String {
    val instant = Instant.ofEpochMilli(epochMillis)
    val formatter = DateTimeFormatter.ofPattern("EEE, MMM d 'at' h:mm a")
    return formatter.withZone(ZoneId.systemDefault()).format(instant)
}
