package org.dhamma.bell.data

import java.time.LocalTime

/**
 * A single blueprint event, anchored only to a day-offset + time-of-day.
 * The scheduling engine turns this into a concrete epoch millis once a
 * real calendar Start Date is chosen.
 */
data class TemplateEvent(
    val dayOffset: Int,
    val time: LocalTime,
    val label: String,
    val type: String, // see EventType
    val audioFileName: String? = null
)

data class CourseTemplate(
    val id: String,
    val displayName: String,
    val totalDaySpan: Int, // inclusive day range 0..totalDaySpan
    val events: List<TemplateEvent>
)
