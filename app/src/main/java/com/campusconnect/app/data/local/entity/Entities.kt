package com.campusconnect.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// ─── Notice Entity ────────────────────────────────────────────────────────────

@Entity(tableName = "notices")
data class NoticeEntity(
    @PrimaryKey val id: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = "GENERAL",
    val priority: String = "MEDIUM",
    val author: String = "",
    val authorEmail: String = "",
    val authorPhotoUrl: String? = null,
    @ColumnInfo(name = "createdAt") val createdAt: Long = 0L,
    val imageUrl: String? = null,
    val tags: String = "",           // comma-separated
    @ColumnInfo(name = "isFavorite") val isSaved: Boolean = false,
    val viewCount: Int = 0,
    val isActive: Boolean = true,
    // FIX: Added targeting columns — these were in the domain model and Firestore
    // mapper but missing from the entity, silently breaking all offline targeting.
    val targetDepartment: String? = null,
    val targetCourse: String? = null
)

// ─── Timetable Entity ─────────────────────────────────────────────────────────

@Entity(tableName = "timetable")
data class TimetableEntity(
    @PrimaryKey val id: String = "",
    val courseCode: String = "",
    val courseName: String = "",
    val lecturer: String = "",
    val room: String = "",
    val building: String = "",
    val dayOfWeek: Int = 0,
    val startHour: Int = 8,
    val startMinute: Int = 0,
    val endHour: Int = 9,
    val endMinute: Int = 0,
    val colorHex: String = "#1E3A8A",
    val notes: String = "",
    val semester: String = "",
    // targetDepartment already existed here — kept as-is
    val targetDepartment: String = ""
)

// ─── Event Entity ─────────────────────────────────────────────────────────────

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val startTimestampMs: Long = 0L,
    val organizer: String = "",
    val imageUrl: String? = null,
    val maxAttendees: Int = 0,
    val rsvpCount: Int = 0,
    @ColumnInfo(name = "isAttending") val isAttending: Boolean = false,
    val isOnline: Boolean = false,
    val meetLink: String? = null,
    // FIX: Added targeting columns — same issue as NoticeEntity above.
    val targetDepartment: String? = null,
    val targetCourse: String? = null
)