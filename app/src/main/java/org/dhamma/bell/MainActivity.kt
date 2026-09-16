package org.dhamma.bell

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.dhamma.bell.scheduler.AlarmScheduler
import org.dhamma.bell.ui.BellViewModel
import org.dhamma.bell.ui.DashboardScreen
import org.dhamma.bell.ui.QueueScreen
import org.dhamma.bell.ui.ScheduleScreen
import org.dhamma.bell.ui.theme.DhammaBellTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BellViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scheduler = AlarmScheduler(this)
        if (!scheduler.canScheduleExactAlarms() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        }

        setContent {
            DhammaBellTheme {
                DhammaBellApp(viewModel)
            }
        }
    }
}

private enum class AppScreen(val label: String) {
    Dashboard("Dashboard"),
    Schedule("New Course"),
    Queue("Alarm Queue")
}

@Composable
fun DhammaBellApp(viewModel: BellViewModel) {
    var currentScreen by remember { mutableStateOf(AppScreen.Dashboard) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.Dashboard,
                    onClick = { currentScreen = AppScreen.Dashboard },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.Schedule,
                    onClick = { currentScreen = AppScreen.Schedule },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "New Course") },
                    label = { Text("New Course") }
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.Queue,
                    onClick = { currentScreen = AppScreen.Queue },
                    icon = { Icon(Icons.Default.List, contentDescription = "Queue") },
                    label = { Text("Queue") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentScreen) {
                AppScreen.Dashboard -> DashboardScreen(viewModel)
                AppScreen.Schedule -> ScheduleScreen(viewModel, onScheduled = { currentScreen = AppScreen.Dashboard })
                AppScreen.Queue -> QueueScreen(viewModel)
            }
        }
    }
}
