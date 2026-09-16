package org.dhamma.bell.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One concrete, dated, timed event computed from a CourseTemplate.
 * Each row is what AlarmManager actually schedules.
 */
@Entity(tableName = "scheduled_bells")
data class ScheduledBellEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    // Grouping key so a whole course can be cancelled together.
    val courseInstanceId: String,
    val courseTemplateId: String,
    val courseDisplayName: String,

    // Which day of the course this belongs to (0 = arrival evening, etc.)
    val dayOffset: Int,

    // Human label, e.g. "Morning Wake-up Gong", "Day 3 Chanting"
    val eventLabel: String,

    // Type of event, used by the playback engine to decide behavior.
    val eventType: String, // GONG_SHORT, GONG_LONG, AUDIO_TRACK, SILENT_BLOCK_START, SILENT_BLOCK_END

    // Relative audio file name inside DhammaAudio folder. Null for a bare gong.
    val audioFileName: String?,

    // Epoch millis of exact trigger time.
    val triggerAtMillis: Long,

    // Whether this alarm has already fired (used to skip re-arming stale/past alarms on boot).
    val isTriggered: Boolean = false,

    // Whether the whole course was cancelled by the user.
    val isCancelled: Boolean = false
)

object EventType {
    const val GONG_SHORT = "GONG_SHORT"
    const val GONG_LONG = "GONG_LONG"
    const val AUDIO_TRACK = "AUDIO_TRACK"
    const val SILENT_BLOCK_START = "SILENT_BLOCK_START"
    const val SILENT_BLOCK_END = "SILENT_BLOCK_END"
}
