package com.campusconnect.app.data.local.dao

import androidx.room.*
import com.campusconnect.app.data.local.entity.EventEntity
import com.campusconnect.app.data.local.entity.NoticeEntity
import com.campusconnect.app.data.local.entity.TimetableEntity
import kotlinx.coroutines.flow.Flow

// ─── Notice DAO ───────────────────────────────────────────────────────────────

@Dao
interface NoticeDao {
    @Query("SELECT * FROM notices WHERE isActive = 1 ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<NoticeEntity>>

    @Query("SELECT * FROM notices WHERE category = :category AND isActive = 1 ORDER BY createdAt DESC")
    fun observeByCategory(category: String): Flow<List<NoticeEntity>>

    @Query("SELECT * FROM notices WHERE isFavorite = 1 AND isActive = 1 ORDER BY createdAt DESC")
    fun observeSaved(): Flow<List<NoticeEntity>>

    @Query("""
        SELECT * FROM notices
        WHERE isActive = 1
          AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
    """)
    fun search(query: String): Flow<List<NoticeEntity>>

    @Query("SELECT * FROM notices WHERE id = :id")
    fun observeById(id: String): Flow<NoticeEntity?>

    @Upsert
    suspend fun upsertAll(notices: List<NoticeEntity>)

    @Upsert
    suspend fun upsert(notice: NoticeEntity)

    @Query("UPDATE notices SET isFavorite = :saved WHERE id = :id")
    suspend fun updateSaved(id: String, saved: Boolean)

    @Query("UPDATE notices SET isActive = 0 WHERE id = :id")
    suspend fun softDelete(id: String)

    @Query("DELETE FROM notices")
    suspend fun clearAll()
}

// ─── Timetable DAO ────────────────────────────────────────────────────────────

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable ORDER BY dayOfWeek, startHour, startMinute")
    fun observeAll(): Flow<List<TimetableEntity>>

    @Query("SELECT * FROM timetable WHERE dayOfWeek = :day ORDER BY startHour, startMinute")
    fun observeByDay(day: Int): Flow<List<TimetableEntity>>

    @Upsert
    suspend fun upsertAll(entries: List<TimetableEntity>)

    @Query("DELETE FROM timetable WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM timetable")
    suspend fun clearAll()
}

// ─── Event DAO ────────────────────────────────────────────────────────────────

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY startTimestampMs ASC")
    fun observeAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE isAttending = 1 ORDER BY startTimestampMs ASC")
    fun observeRsvpd(): Flow<List<EventEntity>>

    @Upsert
    suspend fun upsertAll(events: List<EventEntity>)

    @Query("UPDATE events SET isAttending = :rsvp, rsvpCount = rsvpCount + :delta WHERE id = :id")
    suspend fun updateRsvp(id: String, rsvp: Boolean, delta: Int)

    // Admin-only — hard deletes the event row from the local Room cache.
    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM events")
    suspend fun clearAll()
}