package org.dhamma.bell.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.dhamma.bell.audio.AudioResolver
import org.dhamma.bell.data.AppDatabase
import org.dhamma.bell.data.CourseTemplate
import org.dhamma.bell.data.EventType
import org.dhamma.bell.data.ScheduledBellEntity
import org.dhamma.bell.data.VipassanaTemplates
import org.dhamma.bell.scheduler.AlarmScheduler
import java.time.LocalDate
import java.io.File

data class ActiveCourseUiState(
    val courseDisplayName: String? = null,
    val nextBell: ScheduledBellEntity? = null,
    val countdownText: String = "--:--:--",
    val upcoming: List<ScheduledBellEntity> = emptyList()
)

class BellViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val scheduler = AlarmScheduler(application)

    val templates: List<CourseTemplate> = VipassanaTemplates.all

    private val _uiState = MutableStateFlow(ActiveCourseUiState())
    val uiState: StateFlow<ActiveCourseUiState> = _uiState

    private val _lastScheduleResult = MutableStateFlow<String?>(null)
    val lastScheduleResult: StateFlow<String?> = _lastScheduleResult

    val upcomingBells: StateFlow<List<ScheduledBellEntity>> =
        db.bellDao().observeUpcomingBells(System.currentTimeMillis())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            upcomingBells.collect { list ->
                val name = list.firstOrNull()?.courseDisplayName
                _uiState.value = _uiState.value.copy(
                    courseDisplayName = name,
                    nextBell = list.firstOrNull(),
                    upcoming = list
                )
            }
        }
        startCountdownTicker()
    }

    private fun startCountdownTicker() {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                val next = _uiState.value.nextBell
                val text = if (next == null) {
                    "No bells scheduled"
                } else {
                    formatCountdown(next.triggerAtMillis - System.currentTimeMillis())
                }
                _uiState.value = _uiState.value.copy(countdownText = text)
                delay(1000)
            }
        }
    }

    private fun formatCountdown(deltaMillis: Long): String {
        if (deltaMillis <= 0) return "Now"
        val totalSeconds = deltaMillis / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    fun scheduleCourse(template: CourseTemplate, startDate: LocalDate) {
        viewModelScope.launch {
            if (!scheduler.canScheduleExactAlarms()) {
                _lastScheduleResult.value = "Exact alarm permission not granted. Enable 'Alarms & reminders' for this app in system settings, then try again."
                return@launch
            }
            scheduler.scheduleCourse(template, startDate)
            _lastScheduleResult.value = "Armed '${template.displayName}' starting ${startDate}."
        }
    }

    fun cancelActiveCourse() {
        viewModelScope.launch {
            scheduler.cancelActiveCourse()
            _lastScheduleResult.value = "Active course cancelled."
        }
    }

    fun clearResultMessage() {
        _lastScheduleResult.value = null
    }

    /** Manual Test Gong button: plays the default gong immediately, bypassing scheduling entirely. */
    fun testGongNow() {
        val app = getApplication<Application>()
        val dir = AudioResolver.audioDirectory(app)
        val file = File(dir, AudioResolver.DEFAULT_GONG_FILE)
        val resolved = if (file.exists()) file else null
        viewModelScope.launch(Dispatchers.IO) {
            org.dhamma.bell.audio.AudioPlaybackManager.playWithHdmiWakeBurst(
                context = app,
                audioFile = resolved,
                isGongEvent = true,
                onComplete = { }
            )
        }
    }
}
