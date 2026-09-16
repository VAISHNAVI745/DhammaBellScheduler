package org.dhamma.bell.data

import java.time.LocalTime

/**
 * Hardcoded blueprint repository for the four supported course types.
 * All times are LocalTime-of-day; dayOffset is added to the chosen Start Date
 * by AlarmScheduler to compute the real trigger instant.
 *
 * Audio file names are the exact filenames the app expects inside:
 *   /Android/data/org.dhamma.bell/files/DhammaAudio/
 * If a file is missing at trigger time, AudioPlaybackManager falls back to
 * gong_single.mp3 (or a synthesized tone) and logs the miss instead of crashing.
 */
object VipassanaTemplates {

    private fun t(h: Int, m: Int) = LocalTime.of(h, m)

    // ---------------------------------------------------------------
    // Shared helper: builds one "standard sitting day" skeleton used by
    // both the 10-Day and 3-Day Old Student courses, parameterized by
    // which day's chanting/discourse audio to attach and whether the
    // 2-sit self-practice slots keep the group-sitting gong or not.
    // ---------------------------------------------------------------
    private fun standardDay(
        dayOffset: Int,
        chantFile: String?,
        dohaFile: String?,
        discourseFile: String?,
        metta: Boolean = false
    ): List<TemplateEvent> {
        val events = mutableListOf<TemplateEvent>()

        events += TemplateEvent(dayOffset, t(4, 0), "Morning Wake-up Gong", EventType.GONG_LONG)
        events += TemplateEvent(dayOffset, t(4, 30), "Morning Meditation Begins", EventType.GONG_SHORT)
        if (chantFile != null) {
            events += TemplateEvent(dayOffset, t(4, 30), "Morning Chanting", EventType.AUDIO_TRACK, chantFile)
        }
        events += TemplateEvent(dayOffset, t(6, 30), "Breakfast Gong", EventType.GONG_SHORT)
        events += TemplateEvent(dayOffset, t(8, 0), "Group Sitting Gong (Hall)", EventType.GONG_LONG)
        events += TemplateEvent(dayOffset, t(9, 0), "Self-Practice Gong", EventType.GONG_SHORT)
        events += TemplateEvent(dayOffset, t(11, 0), "Lunch Gong", EventType.GONG_SHORT)
        events += TemplateEvent(dayOffset, t(13, 0), "Afternoon Meditation Resumes", EventType.GONG_SHORT)
        events += TemplateEvent(dayOffset, t(14, 30), "Group Sitting Gong (Hall)", EventType.GONG_LONG)
        events += TemplateEvent(dayOffset, t(15, 30), "Self-Practice Gong", EventType.GONG_SHORT)
        events += TemplateEvent(dayOffset, t(17, 0), "Tea Break Gong", EventType.GONG_SHORT)
        events += TemplateEvent(dayOffset, t(18, 0), "Group Sitting Gong (Hall)", EventType.GONG_LONG)
        events += TemplateEvent(dayOffset, t(19, 0), "Discourse Begins", EventType.GONG_SHORT)
        if (discourseFile != null) {
            events += TemplateEvent(dayOffset, t(19, 0), "Evening Discourse", EventType.AUDIO_TRACK, discourseFile)
        }
        events += TemplateEvent(dayOffset, t(20, 15), "Group Sitting Gong (Hall)", EventType.GONG_LONG)
        if (dohaFile != null) {
            events += TemplateEvent(dayOffset, t(20, 15), "Evening Doha", EventType.AUDIO_TRACK, dohaFile)
        }
        events += TemplateEvent(dayOffset, t(21, 0), "Question Time Gong", EventType.GONG_SHORT)
        events += TemplateEvent(dayOffset, t(21, 30), "Lights Out / Noble Silence Gong", EventType.GONG_SHORT)

        if (metta) {
            events += TemplateEvent(dayOffset, t(10, 0), "Metta Bhavana", EventType.AUDIO_TRACK, "metta_bhavana.mp3")
        }
        return events
    }

    // =================================================================
    // 1. 10-DAY VIPASSANA COURSE (Day 0 -> Day 11)
    // =================================================================
    val tenDayVipassana: CourseTemplate by lazy {
        val events = mutableListOf<TemplateEvent>()

        // --- Day 0: arrival evening only ---
        events += TemplateEvent(0, t(14, 0), "Registration Gong", EventType.GONG_SHORT)
        events += TemplateEvent(0, t(18, 0), "Dinner Gong", EventType.GONG_SHORT)
        events += TemplateEvent(0, t(19, 0), "Orientation Begins Gong", EventType.GONG_SHORT)
        events += TemplateEvent(0, t(19, 0), "Day 0 Orientation Talk", EventType.AUDIO_TRACK, "day0_orientation.mp3")
        events += TemplateEvent(0, t(20, 0), "Five Precepts / Sila Ceremony Gong", EventType.GONG_LONG)
        events += TemplateEvent(0, t(20, 0), "Sila Ceremony Audio", EventType.AUDIO_TRACK, "day0_sila_ceremony.mp3")
        events += TemplateEvent(0, t(21, 0), "Noble Silence Begins Gong", EventType.GONG_LONG)

        // --- Days 1-3: Anapana practice ---
        for (day in 1..3) {
            events += standardDay(
                dayOffset = day,
                chantFile = "morning_chant_day$day.mp3",
                dohaFile = "evening_doha_day$day.mp3",
                discourseFile = "discourse_day$day.mp3"
            )
        }

        // --- Day 4: Vipassana initiation - 2hr uninterrupted block at 14:30-16:30 ---
        events += standardDay(
            dayOffset = 4,
            chantFile = "morning_chant_day4.mp3",
            dohaFile = "evening_doha_day4.mp3",
            discourseFile = "discourse_day4.mp3"
        ).filterNot {
            // remove the normal 14:30/15:30 slots - replaced below by the 2hr initiation block
            it.time == t(14, 30) || it.time == t(15, 30)
        }
        events += TemplateEvent(4, t(14, 30), "Vipassana Initiation Begins (2hr Uninterrupted)", EventType.SILENT_BLOCK_START, "vipassana_initiation_instructions.mp3")
        events += TemplateEvent(4, t(16, 30), "Vipassana Initiation Ends Gong", EventType.SILENT_BLOCK_END)

        // --- Days 5-9: Vipassana practice, progressive discourses ---
        for (day in 5..9) {
            events += standardDay(
                dayOffset = day,
                chantFile = "morning_chant_day$day.mp3",
                dohaFile = "evening_doha_day$day.mp3",
                discourseFile = "discourse_day$day.mp3"
            )
        }

        // --- Day 10: Metta Bhavana introduced at 10:00, Noble Silence lifted, lighter afternoon ---
        events += standardDay(
            dayOffset = 10,
            chantFile = "morning_chant_day10.mp3",
            dohaFile = "evening_doha_day10.mp3",
            discourseFile = "discourse_day10.mp3",
            metta = true
        )
        events += TemplateEvent(10, t(8, 45), "Noble Silence Lifted Gong", EventType.GONG_SHORT)

        // --- Day 11: course ends after morning meditation ---
        events += TemplateEvent(11, t(4, 0), "Final Morning Wake-up Gong", EventType.GONG_LONG)
        events += TemplateEvent(11, t(4, 30), "Final Morning Meditation Begins", EventType.GONG_SHORT)
        events += TemplateEvent(11, t(4, 30), "Day 11 Closing Chant", EventType.AUDIO_TRACK, "morning_chant_day11.mp3")
        events += TemplateEvent(11, t(6, 0), "Group Metta Gong", EventType.GONG_LONG)
        events += TemplateEvent(11, t(6, 30), "Course Ends Gong", EventType.GONG_LONG)

        CourseTemplate(
            id = "vipassana_10day",
            displayName = "10-Day Vipassana Course",
            totalDaySpan = 11,
            events = events.sortedWith(compareBy({ it.dayOffset }, { it.time }))
        )
    }

    // =================================================================
    // 2. 3-DAY OLD STUDENT COURSE (Day 0 -> Day 4)
    // =================================================================
    val threeDayOldStudent: CourseTemplate by lazy {
        val events = mutableListOf<TemplateEvent>()

        events += TemplateEvent(0, t(15, 0), "Registration Gong", EventType.GONG_SHORT)
        events += TemplateEvent(0, t(18, 0), "Dinner Gong", EventType.GONG_SHORT)
        events += TemplateEvent(0, t(19, 0), "Orientation Talk", EventType.AUDIO_TRACK, "old_student_orientation.mp3")
        events += TemplateEvent(0, t(20, 0), "Sila Ceremony Gong", EventType.GONG_LONG)
        events += TemplateEvent(0, t(20, 0), "Sila Ceremony Audio", EventType.AUDIO_TRACK, "day0_sila_ceremony.mp3")
        events += TemplateEvent(0, t(21, 0), "Noble Silence Begins Gong", EventType.GONG_LONG)

        for (day in 1..3) {
            events += standardDay(
                dayOffset = day,
                chantFile = "os_morning_chant_day$day.mp3",
                dohaFile = "os_evening_doha_day$day.mp3",
                discourseFile = "os_discourse_day$day.mp3"
            )
        }

        events += TemplateEvent(4, t(4, 0), "Final Morning Wake-up Gong", EventType.GONG_LONG)
        events += TemplateEvent(4, t(4, 30), "Final Morning Meditation Begins", EventType.GONG_SHORT)
        events += TemplateEvent(4, t(6, 0), "Group Metta Gong", EventType.GONG_LONG)
        events += TemplateEvent(4, t(6, 30), "Course Ends Gong", EventType.GONG_LONG)

        CourseTemplate(
            id = "old_student_3day",
            displayName = "3-Day Old Student Course",
            totalDaySpan = 4,
            events = events.sortedWith(compareBy({ it.dayOffset }, { it.time }))
        )
    }

    // =================================================================
    // 3. 1-DAY CHILDREN'S ANAPANA COURSE (single day, 08:30 - 15:30)
    // =================================================================
    val oneDayChildrenAnapana: CourseTemplate by lazy {
        val events = listOf(
            TemplateEvent(0, t(8, 30), "Welcome Gong", EventType.GONG_SHORT),
            TemplateEvent(0, t(8, 45), "Orientation for Children", EventType.AUDIO_TRACK, "children_orientation.mp3"),
            TemplateEvent(0, t(9, 15), "Sitting Gong", EventType.GONG_SHORT),
            TemplateEvent(0, t(9, 15), "Anapana Instructions Part 1", EventType.AUDIO_TRACK, "children_anapana_part1.mp3"),
            TemplateEvent(0, t(10, 0), "Break Gong", EventType.GONG_SHORT),
            TemplateEvent(0, t(10, 30), "Sitting Gong", EventType.GONG_SHORT),
            TemplateEvent(0, t(10, 30), "Anapana Instructions Part 2", EventType.AUDIO_TRACK, "children_anapana_part2.mp3"),
            TemplateEvent(0, t(11, 30), "Story Time Gong", EventType.GONG_SHORT),
            TemplateEvent(0, t(11, 30), "Dhamma Story for Children", EventType.AUDIO_TRACK, "children_story.mp3"),
            TemplateEvent(0, t(12, 30), "Lunch Gong", EventType.GONG_SHORT),
            TemplateEvent(0, t(13, 30), "Sitting Gong", EventType.GONG_SHORT),
            TemplateEvent(0, t(13, 30), "Anapana Instructions Part 3", EventType.AUDIO_TRACK, "children_anapana_part3.mp3"),
            TemplateEvent(0, t(14, 30), "Group Metta Gong", EventType.GONG_LONG),
            TemplateEvent(0, t(14, 30), "Children Metta Session", EventType.AUDIO_TRACK, "children_metta.mp3"),
            TemplateEvent(0, t(15, 15), "Certificate Distribution Gong", EventType.GONG_SHORT),
            TemplateEvent(0, t(15, 30), "Course Ends Gong", EventType.GONG_LONG)
        )
        CourseTemplate(
            id = "children_anapana_1day",
            displayName = "1-Day Children Anapana Course",
            totalDaySpan = 0,
            events = events
        )
    }

    // =================================================================
    // 4. 3-DAY CHILDREN'S ANAPANA COURSE (Day 0 -> Day 2, shorter daily hours)
    // =================================================================
    val threeDayChildrenAnapana: CourseTemplate by lazy {
        val events = mutableListOf<TemplateEvent>()

        for (day in 0..2) {
            val partNum = day + 1
            events += TemplateEvent(day, t(8, 30), "Welcome Gong", EventType.GONG_SHORT)
            events += TemplateEvent(day, t(9, 0), "Sitting Gong", EventType.GONG_SHORT)
            events += TemplateEvent(day, t(9, 0), "Anapana Instructions Day $partNum", EventType.AUDIO_TRACK, "children_anapana_day${partNum}_part1.mp3")
            events += TemplateEvent(day, t(10, 15), "Break Gong", EventType.GONG_SHORT)
            events += TemplateEvent(day, t(10, 45), "Sitting Gong", EventType.GONG_SHORT)
            events += TemplateEvent(day, t(10, 45), "Anapana Instructions Day $partNum Part 2", EventType.AUDIO_TRACK, "children_anapana_day${partNum}_part2.mp3")
            events += TemplateEvent(day, t(11, 45), "Story Time Gong", EventType.GONG_SHORT)
            events += TemplateEvent(day, t(11, 45), "Dhamma Story Day $partNum", EventType.AUDIO_TRACK, "children_story_day$partNum.mp3")
            events += TemplateEvent(day, t(12, 30), "Lunch / Closing Gong", EventType.GONG_SHORT)
            if (day == 2) {
                events += TemplateEvent(day, t(13, 0), "Group Metta Gong", EventType.GONG_LONG)
                events += TemplateEvent(day, t(13, 0), "Closing Metta Session", EventType.AUDIO_TRACK, "children_metta.mp3")
                events += TemplateEvent(day, t(13, 30), "Certificate Distribution & Course Ends Gong", EventType.GONG_LONG)
            }
        }

        CourseTemplate(
            id = "children_anapana_3day",
            displayName = "3-Day Children Anapana Course",
            totalDaySpan = 2,
            events = events.sortedWith(compareBy({ it.dayOffset }, { it.time }))
        )
    }

    val all: List<CourseTemplate> by lazy {
        listOf(tenDayVipassana, threeDayOldStudent, oneDayChildrenAnapana, threeDayChildrenAnapana)
    }

    fun byId(id: String): CourseTemplate? = all.firstOrNull { it.id == id }
}
