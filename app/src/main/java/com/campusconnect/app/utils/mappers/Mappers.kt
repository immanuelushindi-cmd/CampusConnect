package com.campusconnect.app.utils.mappers

import com.campusconnect.app.data.local.entity.EventEntity
import com.campusconnect.app.data.local.entity.NoticeEntity
import com.campusconnect.app.data.local.entity.TimetableEntity
import com.campusconnect.app.domain.model.Event
import com.campusconnect.app.domain.model.Notice
import com.campusconnect.app.domain.model.NoticeCategory
import com.campusconnect.app.domain.model.NoticePriority
import com.campusconnect.app.domain.model.TimetableEntry

// ─── Notice ───────────────────────────────────────────────────────────────────

fun NoticeEntity.toDomain() = Notice(
    id               = id,
    title            = title,
    content          = content,
    category         = runCatching { NoticeCategory.valueOf(category) }.getOrDefault(NoticeCategory.GENERAL),
    priority         = runCatching { NoticePriority.valueOf(priority) }.getOrDefault(NoticePriority.MEDIUM),
    author           = author,
    authorEmail      = authorEmail,
    authorPhotoUrl   = authorPhotoUrl,
    createdAt        = createdAt,
    imageUrl         = imageUrl,
    tags             = if (tags.isBlank()) emptyList() else tags.split(","),
    isSaved          = isSaved,
    viewCount        = viewCount,
    isActive         = isActive,
    targetDepartment = targetDepartment,
    targetCourse     = targetCourse
)

fun Notice.toEntity() = NoticeEntity(
    id               = id,
    title            = title,
    content          = content,
    category         = category.name,
    priority         = priority.name,
    author           = author,
    authorEmail      = authorEmail,
    authorPhotoUrl   = authorPhotoUrl,
    createdAt        = createdAt,
    imageUrl         = imageUrl,
    tags             = tags.joinToString(","),
    isSaved          = isSaved,
    viewCount        = viewCount,
    isActive         = isActive,
    targetDepartment = targetDepartment,
    targetCourse     = targetCourse
)

fun List<NoticeEntity>.toDomainList() = map { it.toDomain() }
fun List<Notice>.toEntityList()       = map { it.toEntity() }

// ─── Timetable ────────────────────────────────────────────────────────────────

fun TimetableEntity.toDomain() = TimetableEntry(
    id               = id,
    courseCode       = courseCode,
    courseName       = courseName,
    lecturer         = lecturer,
    room             = room,
    building         = building,
    dayOfWeek        = dayOfWeek,
    startHour        = startHour,
    startMinute      = startMinute,
    endHour          = endHour,
    endMinute        = endMinute,
    colorHex         = colorHex,
    notes            = notes,
    semester         = semester,
    targetDepartment = targetDepartment
)

fun TimetableEntry.toEntity() = TimetableEntity(
    id               = id,
    courseCode       = courseCode,
    courseName       = courseName,
    lecturer         = lecturer,
    room             = room,
    building         = building,
    dayOfWeek        = dayOfWeek,
    startHour        = startHour,
    startMinute      = startMinute,
    endHour          = endHour,
    endMinute        = endMinute,
    colorHex         = colorHex,
    notes            = notes,
    semester         = semester,
    targetDepartment = targetDepartment
)

fun List<TimetableEntity>.toTimetableDomainList() = map { it.toDomain() }
fun List<TimetableEntry>.toTimetableEntityList()  = map { it.toEntity() }

// ─── Event ────────────────────────────────────────────────────────────────────

fun EventEntity.toDomain() = Event(
    id               = id,
    title            = title,
    description      = description,
    location         = location,
    latitude         = latitude,
    longitude        = longitude,
    startTimestampMs = startTimestampMs,
    organizer        = organizer,
    imageUrl         = imageUrl,
    maxAttendees     = maxAttendees,
    rsvpCount        = rsvpCount,
    isAttending      = isAttending,
    isOnline         = isOnline,
    meetLink         = meetLink,
    targetDepartment = targetDepartment,
    targetCourse     = targetCourse
)

fun Event.toEntity() = EventEntity(
    id               = id,
    title            = title,
    description      = description,
    location         = location,
    latitude         = latitude,
    longitude        = longitude,
    startTimestampMs = startTimestampMs,
    organizer        = organizer,
    imageUrl         = imageUrl,
    maxAttendees     = maxAttendees,
    rsvpCount        = rsvpCount,
    isAttending      = isAttending,
    isOnline         = isOnline,
    meetLink         = meetLink,
    targetDepartment = targetDepartment,
    targetCourse     = targetCourse
)

fun List<EventEntity>.toEventDomainList() = map { it.toDomain() }
fun List<Event>.toEventEntityList()       = map { it.toEntity() }

// ─── Firestore Map → Domain ───────────────────────────────────────────────────

fun Map<String, Any?>.toNotice(docId: String): Notice {
    val ts = this["timestamp"]
    val tsMs: Long = when (ts) {
        is com.google.firebase.Timestamp -> ts.seconds * 1000L + ts.nanoseconds / 1_000_000L
        is Long   -> ts
        is Number -> ts.toLong()
        else      -> 0L
    }
    return Notice(
        id                = docId,
        title             = this["title"]           as? String ?: "",
        content           = this["content"]         as? String ?: "",
        category          = runCatching {
            NoticeCategory.valueOf((this["category"] as? String ?: "").uppercase())
        }.getOrDefault(NoticeCategory.GENERAL),
        priority          = runCatching {
            NoticePriority.valueOf((this["priority"] as? String ?: "").uppercase())
        }.getOrDefault(NoticePriority.MEDIUM),
        author            = this["author"]          as? String ?: "",
        authorEmail       = this["authorEmail"]     as? String ?: "",
        authorPhotoUrl    = this["authorPhotoUrl"]  as? String,
        createdAt         = tsMs,
        imageUrl          = this["imageUrl"]        as? String,
        tags              = (this["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
        isSaved           = false, // saved state is local-only; never read from Firestore
        viewCount         = (this["viewCount"]      as? Number)?.toInt() ?: 0,
        isActive          = this["isActive"]        as? Boolean ?: true,
        targetDepartment  = this["targetDepartment"]  as? String,
        targetCourse      = this["targetCourse"]      as? String,
        targetYearOfStudy = (this["targetYearOfStudy"] as? Number)?.toInt()
    )
}

fun Map<String, Any?>.toEvent(docId: String): Event {
    fun tsMs(key: String): Long {
        val v = this[key]
        return when (v) {
            is com.google.firebase.Timestamp -> v.seconds * 1000L + v.nanoseconds / 1_000_000L
            is Long   -> v
            is Number -> v.toLong()
            else      -> 0L
        }
    }
    return Event(
        id               = docId,
        title            = this["title"]       as? String ?: "",
        description      = this["description"] as? String ?: "",
        location         = this["location"]    as? String ?: "",
        latitude         = (this["latitude"]   as? Number)?.toDouble(),
        longitude        = (this["longitude"]  as? Number)?.toDouble(),
        startTimestampMs = tsMs("startTimestamp"),
        organizer        = this["organizer"]   as? String ?: "",
        imageUrl         = this["imageUrl"]    as? String,
        maxAttendees     = (this["maxAttendees"] as? Number)?.toInt() ?: 0,
        rsvpCount        = (this["rsvpCount"]    as? Number)?.toInt() ?: 0,
        isAttending      = this["isAttending"]   as? Boolean ?: false,
        isOnline         = this["isOnline"]      as? Boolean ?: false,
        meetLink         = this["meetLink"]      as? String,
        targetDepartment = this["targetDepartment"] as? String,
        targetCourse     = this["targetCourse"]     as? String
    )
}

fun Map<String, Any?>.toTimetableEntry(docId: String) = TimetableEntry(
    id               = docId,
    courseCode       = this["courseCode"]   as? String ?: "",
    courseName       = this["courseName"]   as? String ?: "",
    lecturer         = this["lecturer"]     as? String ?: "",
    room             = this["room"]         as? String ?: "",
    building         = this["building"]     as? String ?: "",
    dayOfWeek        = (this["dayOfWeek"]   as? Number)?.toInt() ?: 0,
    startHour        = (this["startHour"]   as? Number)?.toInt() ?: 8,
    startMinute      = (this["startMinute"] as? Number)?.toInt() ?: 0,
    endHour          = (this["endHour"]     as? Number)?.toInt() ?: 9,
    endMinute        = (this["endMinute"]   as? Number)?.toInt() ?: 0,
    colorHex         = this["colorHex"]     as? String ?: "#1E3A8A",
    notes            = this["notes"]        as? String ?: "",
    semester         = this["semester"]     as? String ?: "",
    targetDepartment = this["targetDepartment"] as? String ?: ""
)
