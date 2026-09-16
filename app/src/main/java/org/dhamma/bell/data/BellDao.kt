package org.dhamma.bell.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BellDao {

    @Insert
    suspend fun insertAll(bells: List<ScheduledBellEntity>)

    @Update
    suspend fun update(bell: ScheduledBellEntity)

    /** All bells belonging to the course that is currently active (not cancelled, has future events). */
    @Query("""
        SELECT * FROM scheduled_bells
        WHERE isCancelled = 0
        ORDER BY triggerAtMillis ASC
    """)
    fun observeAllActiveBells(): Flow<List<ScheduledBellEntity>>

    @Query("""
        SELECT * FROM scheduled_bells
        WHERE isCancelled = 0 AND isTriggered = 0
        ORDER BY triggerAtMillis ASC
    """)
    suspend fun getUntriggeredBells(): List<ScheduledBellEntity>

    @Query("""
        SELECT * FROM scheduled_bells
        WHERE isCancelled = 0 AND isTriggered = 0 AND triggerAtMillis > :nowMillis
        ORDER BY triggerAtMillis ASC
        LIMIT 1
    """)
    suspend fun getNextUpcomingBell(nowMillis: Long): ScheduledBellEntity?

    @Query("""
        SELECT * FROM scheduled_bells
        WHERE isCancelled = 0 AND isTriggered = 0 AND triggerAtMillis > :nowMillis
        ORDER BY triggerAtMillis ASC
    """)
    fun observeUpcomingBells(nowMillis: Long): Flow<List<ScheduledBellEntity>>

    @Query("SELECT * FROM scheduled_bells WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ScheduledBellEntity?

    @Query("UPDATE scheduled_bells SET isTriggered = 1 WHERE id = :id")
    suspend fun markTriggered(id: Long)

    @Query("""
        UPDATE scheduled_bells SET isCancelled = 1
        WHERE courseInstanceId = :courseInstanceId
    """)
    suspend fun cancelCourse(courseInstanceId: String)

    @Query("""
        SELECT DISTINCT courseInstanceId FROM scheduled_bells
        WHERE isCancelled = 0 AND isTriggered = 0
        ORDER BY triggerAtMillis ASC
        LIMIT 1
    """)
    suspend fun getActiveCourseInstanceId(): String?

    @Query("""
        SELECT courseDisplayName FROM scheduled_bells
        WHERE courseInstanceId = :courseInstanceId LIMIT 1
    """)
    suspend fun getCourseDisplayName(courseInstanceId: String): String?

    @Query("DELETE FROM scheduled_bells WHERE triggerAtMillis < :beforeMillis AND isTriggered = 1")
    suspend fun purgeOldTriggered(beforeMillis: Long)
}
