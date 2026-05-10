package com.campusconnect.app.domain.model

// ─── Notice ───────────────────────────────────────────────────────────────────
//
// targetDepartment — null/blank = all programmes; non-null = one programme.
// targetCourse     — null/blank = all courses;    non-null = one course unit.
//
// Filter logic in NoticeRepository.filterForUser():
//   deptOk   = targetDepartment is null  OR  matches user.department
//   courseOk = targetCourse     is null  OR  user.courses contains targetCourse
//   visible  = deptOk && courseOk

data class Notice(
    val id:              String         = "",
    val title:           String         = "",
    val content:         String         = "",
    val category:        NoticeCategory = NoticeCategory.GENERAL,
    val priority:        NoticePriority = NoticePriority.MEDIUM,
    val author:          String         = "",
    val authorEmail:     String         = "",
    val authorPhotoUrl:  String?        = null,   // ← used in Repositories.kt
    val imageUrl:        String?        = null,
    val tags:            List<String>   = emptyList(), // ← used in Repositories.kt
    val viewCount:       Int            = 0,           // ← used in Repositories.kt
    val isActive:        Boolean        = true,        // ← used in Repositories.kt
    val createdAt:       Long           = System.currentTimeMillis(),

    // ── Targeting ────────────────────────────────────────────────────────────
    val targetDepartment: String?       = null,
    val targetCourse:     String?       = null,
    val targetYearOfStudy: Int?         = null,

    // ── Student state ─────────────────────────────────────────────────────────
    val isSaved:         Boolean        = false,
    val isDeleted:       Boolean        = false
)

// ─── Event ────────────────────────────────────────────────────────────────────

data class Event(
    val id:               String  = "",
    val title:            String  = "",
    val description:      String  = "",
    val location:         String  = "",
    val startTimestampMs: Long    = 0L,
    val maxAttendees:     Int     = 0,
    val isOnline:         Boolean = false,
    val meetLink:         String? = null,
    val organizer:        String  = "",
    val imageUrl:         String? = null,
    val latitude:         Double? = null,   // ← used in Repositories.kt
    val longitude:        Double? = null,   // ← used in Repositories.kt
    val isActive:         Boolean = true,   // ← used in Repositories.kt
    val rsvpCount:        Int     = 0,      // ← used in Repositories.kt (rsvpCount)
    val createdAt:        Long    = System.currentTimeMillis(),

    // ── Targeting ─────────────────────────────────────────────────────────────
    /** Programme / school filter. null = broadcast to all programmes. */
    val targetDepartment: String? = null,
    /** Course-unit filter. null = no course restriction. */
    val targetCourse:     String? = null,
    /** Year of study filter. null = no year restriction. */
    val targetYearOfStudy: Int?    = null,

    // ── Student state ─────────────────────────────────────────────────────────
    val isAttending:      Boolean = false,
    val attendeeCount:    Int     = 0
)

// ─── NoticeCategory ───────────────────────────────────────────────────────────

enum class NoticeCategory(val displayName: String, val emoji: String) {
    GENERAL("General",          "📢"),
    ACADEMIC("Academic",        "📚"),
    EVENTS("Events",            "🎉"),
    SPORTS("Sports",            "⚽"),
    FINANCE("Finance",          "💰"),
    HEALTH("Health & Wellness", "🏥"),
    CAREERS("Careers",          "💼"),
    EMERGENCY("Emergency",      "🚨")
}

// ─── NoticePriority ───────────────────────────────────────────────────────────

enum class NoticePriority { LOW, MEDIUM, HIGH, URGENT }

// ─── TimetableEntry ───────────────────────────────────────────────────────────

data class TimetableEntry(
    val id:               String = "",
    val courseCode:       String = "",
    val courseName:       String = "",
    val lecturer:         String = "",
    val room:             String = "",
    val building:         String = "",
    val dayOfWeek:        Int    = 0,      // 0=Mon … 4=Fri
    val startHour:        Int    = 8,
    val startMinute:      Int    = 0,
    val endHour:          Int    = 9,
    val endMinute:        Int    = 0,
    val colorHex:         String = "#1E3A5F",
    val semester:         String = "",     // ← used in Repositories.kt
    val notes:            String = "",
    val targetDepartment: String = "",      // programme-specific; blank = all
    val targetYearOfStudy: Int?   = null,
)

// ─── User ─────────────────────────────────────────────────────────────────────
//
// Consolidating User here keeps all domain models in one place.
// Repositories.kt's toUser() extension maps every field listed below.

data class User(
    val uid:                   String       = "",
    val email:                 String       = "",
    val displayName:           String       = "",
    val photoUrl:              String?      = null,
    val bio:                   String       = "",
    val department:            String       = "",
    val yearOfStudy:           Int          = 1,
    val studentId:             String       = "",
    val courses:               List<String> = emptyList(),
    val role:                  String       = "student",
    val isAdmin:               Boolean      = false,
    val notificationsEnabled:  Boolean      = true,
    val savedNotices:            List<String> = emptyList(),
    val rsvpEvents:            List<String> = emptyList()
)



data class CourseEntry(
    val id:   String = "",   // = document ID = course code
    val code: String = "",
    val name: String = ""
)

//LecturerEntry
data class LecturerEntry(
    val id        : String       = "",   // Firestore document ID
    val name      : String       = "",   // e.g. "Dr. Jane Otieno"
    val lecturerId: String       = "",   // e.g. "L-2024-007"
    val courseIds : List<String> = emptyList() // e.g. ["CS101", "CS202"]
)

val TARGET_AUDIENCES: List<String> = listOf("All Students")