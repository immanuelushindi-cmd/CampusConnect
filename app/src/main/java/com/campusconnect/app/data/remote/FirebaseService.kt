package com.campusconnect.app.data.remote

import com.campusconnect.app.domain.model.CourseEntry
import com.campusconnect.app.domain.model.Event
import com.campusconnect.app.domain.model.LecturerEntry
import com.campusconnect.app.domain.model.Notice
import com.campusconnect.app.domain.model.TimetableEntry
import com.campusconnect.app.domain.model.User
import com.campusconnect.app.utils.mappers.toEvent
import com.campusconnect.app.utils.mappers.toNotice
import com.campusconnect.app.utils.mappers.toTimetableEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val messaging: FirebaseMessaging
) {
    val currentUid: String? get() = auth.currentUser?.uid
    val currentUserEmail: String? get() = auth.currentUser?.email

    val noticesCol   get() = firestore.collection("notices")
    val eventsCol    get() = firestore.collection("events")
    val timetableCol get() = firestore.collection("timetable")
    val usersCol     get() = firestore.collection("users")
    val coursesCol   get() = firestore.collection("courses")
    val lecturersCol get() = firestore.collection("lecturers")
    val usernamesCol get() = firestore.collection("usernames")

    /**
     * Looks up the email address of the account registered with username "admin".
     * This is used by the student Help & Support screen so bug reports are always
     * sent to the real admin address — never a hardcoded string.
     * Returns null if the admin account doesn't exist yet.
     */
    suspend fun getAdminEmail(): String? = runCatching {
        usernamesCol.document("admin").get().await().getString("email")
    }.getOrNull()

    // ── Notices ───────────────────────────────────────────────────────────────

    fun streamNotices(): Flow<List<Notice>> = callbackFlow {
        val sub = noticesCol
            .whereEqualTo("isActive", true)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val notices = snap?.documents?.mapNotNull { doc ->
                    doc.data?.toNotice(doc.id)
                } ?: emptyList()
                trySend(notices)
            }
        awaitClose { sub.remove() }
    }

    suspend fun postNotice(notice: Notice): Result<String> = runCatching {
        val ref = if (notice.id.isEmpty()) noticesCol.document() else noticesCol.document(notice.id)
        val data = mapOf(
            "title"             to notice.title,
            "content"           to notice.content,
            "category"          to notice.category.name,
            "priority"          to notice.priority.name,
            "author"            to notice.author,
            "authorEmail"       to (notice.authorEmail.ifBlank { auth.currentUser?.email ?: "" }),
            "authorPhotoUrl"    to notice.authorPhotoUrl,
            "timestamp"         to FieldValue.serverTimestamp(),
            "imageUrl"          to notice.imageUrl,
            "tags"              to notice.tags,
            "isSaved"           to false,
            "viewCount"         to 0,
            "isActive"          to true,
            "targetDepartment"  to notice.targetDepartment,
            "targetCourse"      to notice.targetCourse,
            "targetYearOfStudy" to notice.targetYearOfStudy
        )
        ref.set(data).await()
        ref.id
    }

    suspend fun deleteNotice(id: String): Result<Unit> = runCatching {
        noticesCol.document(id).update("isActive", false).await()
    }

    suspend fun updateNotice(id: String, title: String, content: String, imageUrl: String?): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any?>(
            "title"   to title.trim(),
            "content" to content.trim()
        )
        if (imageUrl != null) updates["imageUrl"] = imageUrl
        noticesCol.document(id).update(updates).await()
    }

    suspend fun toggleSaved(noticeId: String, userId: String, isSaved: Boolean): Result<Unit> =
        runCatching {
            val update = if (isSaved) FieldValue.arrayUnion(noticeId)
            else        FieldValue.arrayRemove(noticeId)
            usersCol.document(userId).update("savedNotices", update).await()
        }

    // ── Timetable ─────────────────────────────────────────────────────────────

    fun streamTimetable(): Flow<List<TimetableEntry>> = callbackFlow {
        val sub = timetableCol
            .orderBy("dayOfWeek")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val entries = snap?.documents?.mapNotNull { doc ->
                    doc.data?.toTimetableEntry(doc.id)
                } ?: emptyList()
                trySend(entries)
            }
        awaitClose { sub.remove() }
    }

    suspend fun postTimetableEntry(entry: TimetableEntry): Result<String> = runCatching {
        val ref = if (entry.id.isEmpty()) timetableCol.document() else timetableCol.document(entry.id)
        val data = mapOf(
            "courseCode"       to entry.courseCode,
            "courseName"       to entry.courseName,
            "lecturer"         to entry.lecturer,
            "room"             to entry.room,
            "building"         to entry.building,
            "dayOfWeek"        to entry.dayOfWeek,
            "startHour"        to entry.startHour,
            "startMinute"      to entry.startMinute,
            "endHour"          to entry.endHour,
            "endMinute"        to entry.endMinute,
            "colorHex"         to entry.colorHex,
            "semester"         to entry.semester,
            "notes"            to entry.notes,
            "targetDepartment" to entry.targetDepartment
        )
        ref.set(data).await()
        ref.id
    }

    suspend fun deleteTimetableEntry(id: String): Result<Unit> = runCatching {
        timetableCol.document(id).delete().await()
    }

    suspend fun updateTimetableEntry(entry: TimetableEntry): Result<Unit> = runCatching {
        val updates = mapOf(
            "courseCode"  to entry.courseCode,
            "courseName"  to entry.courseName,
            "lecturer"    to entry.lecturer,
            "room"        to entry.room,
            "building"    to entry.building,
            "dayOfWeek"   to entry.dayOfWeek,
            "startHour"   to entry.startHour,
            "startMinute" to entry.startMinute,
            "endHour"     to entry.endHour,
            "endMinute"   to entry.endMinute,
            "colorHex"    to entry.colorHex,
            "notes"       to entry.notes
        )
        timetableCol.document(entry.id).update(updates).await()
    }

    // ── Events ────────────────────────────────────────────────────────────────

    fun streamEvents(): Flow<List<Event>> = callbackFlow {
        val sub = eventsCol
            .orderBy("startTimestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val events = snap?.documents?.mapNotNull { doc ->
                    doc.data?.toEvent(doc.id)
                } ?: emptyList()
                trySend(events)
            }
        awaitClose { sub.remove() }
    }

    suspend fun postEvent(event: Event): Result<String> = runCatching {
        val ref = if (event.id.isEmpty()) eventsCol.document() else eventsCol.document(event.id)
        val data = mapOf(
            "title"            to event.title,
            "description"      to event.description,
            "location"         to event.location,
            "startTimestamp"   to event.startTimestampMs,
            "isOnline"         to event.isOnline,
            "meetLink"         to event.meetLink,
            "maxAttendees"     to event.maxAttendees,
            "rsvpCount"        to 0,
            "organizer"        to event.organizer,
            "imageUrl"         to event.imageUrl,
            "latitude"         to event.latitude,
            "longitude"        to event.longitude,
            "isActive"         to true,
            "targetDepartment" to event.targetDepartment,
            "targetCourse"     to event.targetCourse
        )
        ref.set(data).await()
        ref.id
    }

    suspend fun deleteEvent(id: String): Result<Unit> = runCatching {
        eventsCol.document(id).delete().await()
    }

    suspend fun updateEvent(id: String, title: String, description: String, location: String, dateTimeMillis: Long, imageUrl: String?): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any?>(
            "title"            to title.trim(),
            "description"      to description.trim(),
            "location"         to location.trim(),
            "startTimestampMs" to dateTimeMillis
        )
        if (imageUrl != null) updates["imageUrl"] = imageUrl
        eventsCol.document(id).update(updates).await()
    }

    suspend fun rsvpEvent(eventId: String, userId: String, attending: Boolean): Result<Unit> =
        runCatching {

            if (!attending) return@runCatching

            firestore.runTransaction { tx ->
                val userRef  = usersCol.document(userId)
                val eventRef = eventsCol.document(eventId)


                val userSnap = tx.get(userRef)
                @Suppress("UNCHECKED_CAST")
                val alreadyRsvpd = (userSnap["rsvpEvents"] as? List<String>)
                    ?.contains(eventId) == true
                if (alreadyRsvpd) {
                    throw IllegalStateException("You have already RSVP'd to this event.")
                }


                tx.update(eventRef, "rsvpCount", FieldValue.increment(1L))
                tx.update(userRef,  "rsvpEvents", FieldValue.arrayUnion(eventId))
            }.await()
        }

    // ── Courses (admin-managed catalogue) ────────────────────────────────────

    fun streamCourses(): Flow<List<CourseEntry>> = callbackFlow {
        val sub = coursesCol
            .orderBy("code")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val courses = snap?.documents?.mapNotNull { doc ->
                    val code = doc.getString("code") ?: return@mapNotNull null
                    val name = doc.getString("name") ?: return@mapNotNull null
                    CourseEntry(id = doc.id, code = code, name = name)
                } ?: emptyList()
                trySend(courses)
            }
        awaitClose { sub.remove() }
    }

    /** Uses the course code as the document ID so duplicates are impossible. */
    suspend fun addCourse(code: String, name: String): Result<String> = runCatching {
        val key = code.trim().uppercase()
        require(key.isNotBlank()) { "Course code cannot be blank" }
        require(name.isNotBlank()) { "Course name cannot be blank" }
        coursesCol.document(key)
            .set(mapOf("code" to key, "name" to name.trim()))
            .await()
        key
    }

    suspend fun updateCourse(id: String, name: String): Result<Unit> = runCatching {
        require(name.isNotBlank()) { "Course name cannot be blank" }
        coursesCol.document(id).update("name", name.trim()).await()
    }

    suspend fun deleteCourse(id: String): Result<Unit> = runCatching {
        coursesCol.document(id).delete().await()
    }

    // ── Lecturers (admin-managed) ─────────────────────────────────────────────
    // Firestore collection: `lecturers`
    // Each document stores name, lecturerId, and courseIds: List<String> so one
    // lecturer can be linked to multiple courses and filtered per course in the
    // timetable screen.

    fun streamLecturers(): Flow<List<LecturerEntry>> = callbackFlow {
        val sub = lecturersCol
            .orderBy("name")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val lecturers = snap?.documents?.mapNotNull { doc ->
                    LecturerEntry(
                        id         = doc.id,
                        name       = doc.getString("name") ?: return@mapNotNull null,
                        lecturerId = doc.getString("lecturerId") ?: "",
                        courseIds  = (doc.get("courseIds") as? List<*>)
                            ?.filterIsInstance<String>() ?: emptyList()
                    )
                } ?: emptyList()
                trySend(lecturers)
            }
        awaitClose { sub.remove() }
    }

    suspend fun addLecturer(
        name      : String,
        lecturerId: String,
        courseIds : List<String>
    ): Result<String> = runCatching {
        require(name.isNotBlank())       { "Lecturer name cannot be blank" }
        require(lecturerId.isNotBlank()) { "Lecturer ID cannot be blank" }
        val ref = lecturersCol.document()
        ref.set(mapOf(
            "name"       to name.trim(),
            "lecturerId" to lecturerId.trim(),
            "courseIds"  to courseIds
        )).await()
        ref.id
    }

    suspend fun updateLecturer(
        id        : String,
        name      : String,
        lecturerId: String,
        courseIds : List<String>
    ): Result<Unit> = runCatching {
        require(name.isNotBlank())       { "Lecturer name cannot be blank" }
        require(lecturerId.isNotBlank()) { "Lecturer ID cannot be blank" }
        lecturersCol.document(id).update(mapOf(
            "name"       to name.trim(),
            "lecturerId" to lecturerId.trim(),
            "courseIds"  to courseIds
        )).await()
    }

    suspend fun deleteLecturer(id: String): Result<Unit> = runCatching {
        lecturersCol.document(id).delete().await()
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    fun streamCurrentUser(): Flow<User?> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(null); close(); return@callbackFlow }
        val sub = usersCol.document(uid).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            val user = snap?.data?.toUser(uid)
            trySend(user)
        }
        awaitClose { sub.remove() }
    }

    /**
     * Creates (or refreshes) the Firestore user document.
     *
     * [extraFields] lets callers merge additional data at creation time.
     * AuthRepository.signUpWithUsername() uses this to write the "username"
     * field so that isAdmin() can read it back with a single document fetch.
     */
    suspend fun ensureUserDocument(
        extraFields: Map<String, Any?> = emptyMap()
    ): Result<Unit> = runCatching {
        val fbUser = auth.currentUser ?: error("Not logged in")
        val ref = usersCol.document(fbUser.uid)
        val snap = ref.get().await()

        val fcmToken = runCatching { messaging.token.await() }.getOrNull()

        if (!snap.exists()) {
            val data = mutableMapOf<String, Any?>(
                "uid"                  to fbUser.uid,
                "email"                to (fbUser.email ?: ""),
                "displayName"          to (fbUser.displayName ?: ""),
                "photoUrl"             to fbUser.photoUrl?.toString(),
                "notificationsEnabled" to true,
                "savedNotices"         to emptyList<String>(),
                "rsvpEvents"           to emptyList<String>(),
                "fcmToken"             to fcmToken
            )
            data.putAll(extraFields)
            ref.set(data).await()
        } else {
            val updates = mutableMapOf<String, Any?>()
            if (fcmToken != null) updates["fcmToken"] = fcmToken
            updates.putAll(extraFields)
            if (updates.isNotEmpty()) ref.update(updates).await()
        }
    }

    suspend fun updateUserProfile(updates: Map<String, Any?>): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not logged in")
        usersCol.document(uid).update(updates).await()
    }

    private fun Map<String, Any?>.toUser(uid: String) = User(
        uid                  = uid,
        email                = this["email"] as? String ?: "",
        displayName          = this["displayName"] as? String ?: "",
        photoUrl             = this["photoUrl"] as? String,
        bio                  = this["bio"] as? String ?: "",
        department           = this["department"] as? String ?: "",
        yearOfStudy          = (this["yearOfStudy"] as? Number)?.toInt() ?: 1,
        studentId            = this["studentId"] as? String ?: "",
        courses              = (this["courses"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
        isAdmin              = this["isAdmin"] as? Boolean ?: false,
        notificationsEnabled = this["notificationsEnabled"] as? Boolean ?: true,
        savedNotices         = (this["savedNotices"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
        rsvpEvents           = (this["rsvpEvents"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
    )
}