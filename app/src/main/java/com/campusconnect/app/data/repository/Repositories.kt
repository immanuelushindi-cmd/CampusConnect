package com.campusconnect.app.data.repository

import android.net.Uri
import com.campusconnect.app.data.local.database.CampusDatabase
import com.campusconnect.app.data.remote.FirebaseService
import com.campusconnect.app.domain.model.CourseEntry
import com.campusconnect.app.domain.model.Event
import com.campusconnect.app.domain.model.LecturerEntry
import com.campusconnect.app.domain.model.Notice
import com.campusconnect.app.domain.model.TimetableEntry
import com.campusconnect.app.domain.model.User
import com.campusconnect.app.utils.mappers.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// ─── Auth Repository ──────────────────────────────────────────────────────────

@Singleton
class AuthRepository @Inject constructor(
    private val auth:      FirebaseAuth,
    private val firebase:  FirebaseService,
    private val firestore: FirebaseFirestore
) {
    val currentUser get() = auth.currentUser
    val isLoggedIn  get() = auth.currentUser != null

    fun currentUserFlow(): Flow<User?> = firebase.streamCurrentUser()

    private suspend fun emailForUsername(username: String): String? {
        val doc = firestore.collection("usernames")
            .document(username.lowercase().trim())
            .get().await()
        return if (doc.exists()) doc.getString("email") else null
    }

    suspend fun signInWithUsername(username: String, password: String): Result<Unit> =
        runCatching {
            val email = emailForUsername(username)
                ?: error("No account found for \"$username\"")
            auth.signInWithEmailAndPassword(email, password).await()
            firebase.ensureUserDocument()
        }

    suspend fun signUpWithUsername(
        username: String,
        email:    String,
        password: String,
        name:     String
    ): Result<Unit> = runCatching {
        val key = username.lowercase().trim()
        require(key.isNotBlank())              { "Username cannot be blank" }
        require(key.length >= 3)               { "Username must be at least 3 characters" }
        require(key.length <= 30)              { "Username cannot be longer than 30 characters" }
        require(key.none { it == ' ' })        { "Username cannot contain spaces" }
        require(key.all { it.isLetterOrDigit() || it == '_' || it == '.' }) {
            "Username can only contain letters, numbers, underscores and dots"
        }

        val existing = firestore.collection("usernames").document(key).get().await()
        if (existing.exists()) error("Username \"$username\" is already taken")

        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val profileUpdates = userProfileChangeRequest { displayName = name }
        result.user?.updateProfile(profileUpdates)?.await()

        firestore.runTransaction { tx ->
            val snap = tx.get(firestore.collection("usernames").document(key))
            if (snap.exists()) error("Username \"$username\" is already taken")
            tx.set(
                firestore.collection("usernames").document(key),
                mapOf("email" to email)
            )
        }.await()

        firebase.ensureUserDocument(extraFields = mapOf("username" to key))
    }

    suspend fun signInWithGoogle(idToken: String): Result<Unit> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val fbUser = result.user ?: error("Google sign-in returned no user")

        // For new Google users, generate a unique username from their display name
        // and store it in both the users document and the usernames lookup collection.
        val existingDoc = firestore.collection("users").document(fbUser.uid).get().await()
        if (!existingDoc.exists() || existingDoc.getString("username").isNullOrBlank()) {
            val base = (fbUser.displayName ?: fbUser.email?.substringBefore("@") ?: "user")
                .lowercase()
                .replace(Regex("[^a-z0-9_.]"), "")
                .take(20)
                .ifBlank { "user" }

            // Append uid suffix to guarantee uniqueness without a retry loop
            val username = "${base}_${fbUser.uid.take(6)}"

            firebase.ensureUserDocument(extraFields = mapOf("username" to username))

            // Register in the usernames lookup collection so they can also sign in
            // with username + password if they set a password later.
            firestore.collection("usernames").document(username)
                .set(mapOf("email" to (fbUser.email ?: ""))).await()
        } else {
            firebase.ensureUserDocument()
        }
    }

    suspend fun isAdmin(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val doc = firestore.collection("users").document(uid).get().await()
        return doc.getString("username")?.lowercase() == "admin"
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
    }

    fun signOut() = auth.signOut()

    suspend fun updateProfile(
        displayName: String,
        bio:         String       = "",
        department:  String       = "",
        studentId:   String       = "",
        yearOfStudy: Int          = 1,
        courses:     List<String> = emptyList()
    ): Result<Unit> = runCatching {
        val profileUpdates = userProfileChangeRequest { this.displayName = displayName }
        auth.currentUser?.updateProfile(profileUpdates)?.await()
        firebase.updateUserProfile(
            mapOf(
                "displayName" to displayName,
                "bio"         to bio,
                "department"  to department,
                "studentId"   to studentId,
                "yearOfStudy" to yearOfStudy,
                "courses"     to courses
            )
        )
    }

    suspend fun updateProfilePhoto(uri: Uri): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not logged in")
        val storageRef = FirebaseStorage.getInstance().reference
            .child("avatars/$uid/${UUID.randomUUID()}.jpg")
        storageRef.putFile(uri).await()
        val downloadUrl = storageRef.downloadUrl.await().toString()
        val profileUpdates = userProfileChangeRequest { photoUri = Uri.parse(downloadUrl) }
        auth.currentUser?.updateProfile(profileUpdates)?.await()
        firebase.updateUserProfile(mapOf("photoUrl" to downloadUrl))
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean): Result<Unit> = runCatching {
        firebase.updateUserProfile(mapOf("notificationsEnabled" to enabled))
    }
}

// ─── Notice Repository (offline-first) ───────────────────────────────────────

@Singleton
class NoticeRepository @Inject constructor(
    private val firebase: FirebaseService,
    private val authRepo: AuthRepository,
    db: CampusDatabase
) {
    private val dao = db.noticeDao()

    fun observeAll(): Flow<List<Notice>> = combine(
        dao.observeAll().map { it.toDomainList() },
        authRepo.currentUserFlow()
    ) { notices, user -> notices.filterForUser(user) }

    fun observeByCategory(category: String): Flow<List<Notice>> = combine(
        dao.observeByCategory(category).map { it.toDomainList() },
        authRepo.currentUserFlow()
    ) { notices, user -> notices.filterForUser(user) }

    fun observeSaved(): Flow<List<Notice>> = combine(
        dao.observeSaved().map { it.toDomainList() },
        authRepo.currentUserFlow()
    ) { notices, user -> notices.filterForUser(user) }

    fun search(query: String): Flow<List<Notice>> = combine(
        dao.search(query).map { it.toDomainList() },
        authRepo.currentUserFlow()
    ) { notices, user -> notices.filterForUser(user) }

    fun observeById(id: String): Flow<Notice?> = dao.observeById(id).map { it?.toDomain() }

    fun syncFromFirestore(): Flow<List<Notice>> =
        firebase.streamNotices()
            .map { notices ->
                dao.upsertAll(notices.toEntityList())
                notices
            }
            .catch { emit(emptyList()) }

    suspend fun postNotice(notice: Notice): Result<String> = firebase.postNotice(notice)

    suspend fun updateNotice(id: String, title: String, content: String, imageUrl: String?): Result<Unit> =
        firebase.updateNotice(id, title, content, imageUrl)

    suspend fun deleteNotice(id: String): Result<Unit> =
        firebase.deleteNotice(id).onSuccess { dao.softDelete(id) }

    suspend fun toggleSaved(noticeId: String, isSaved: Boolean) {
        dao.updateSaved(noticeId, isSaved)
        val uid = firebase.currentUid ?: return
        firebase.toggleSaved(noticeId, uid, isSaved)
            .onFailure { dao.updateSaved(noticeId, !isSaved) }
    }

    suspend fun clearLocalData() = dao.clearAll()
}

// ─── Timetable Repository ─────────────────────────────────────────────────────

@Singleton
class TimetableRepository @Inject constructor(
    private val firebase: FirebaseService,
    private val authRepo: AuthRepository,
    db: CampusDatabase
) {
    private val dao = db.timetableDao()

    fun observeAll(): Flow<List<TimetableEntry>> = combine(
        dao.observeAll().map { it.toTimetableDomainList() },
        authRepo.currentUserFlow()
    ) { entries, user -> entries.filterTimetableForStudent(user) }

    fun observeByDay(day: Int): Flow<List<TimetableEntry>> = combine(
        dao.observeByDay(day).map { it.toTimetableDomainList() },
        authRepo.currentUserFlow()
    ) { entries, user -> entries.filterTimetableForStudent(user) }

    fun syncFromFirestore(): Flow<List<TimetableEntry>> =
        firebase.streamTimetable()
            .map { entries ->
                dao.upsertAll(entries.toTimetableEntityList())
                entries
            }
            .catch { emit(emptyList()) }

    suspend fun postEntry(entry: TimetableEntry): Result<String> =
        firebase.postTimetableEntry(entry)

    suspend fun updateEntry(entry: TimetableEntry): Result<Unit> =
        firebase.updateTimetableEntry(entry)

    suspend fun deleteEntry(id: String): Result<Unit> {
        dao.deleteById(id)
        return firebase.deleteTimetableEntry(id)
    }

    suspend fun clearLocalData() = dao.clearAll()
}

// ─── Event Repository ─────────────────────────────────────────────────────────

@Singleton
class EventRepository @Inject constructor(
    private val firebase: FirebaseService,
    private val authRepo: AuthRepository,
    db: CampusDatabase
) {
    private val dao = db.eventDao()

    fun observeAll(): Flow<List<Event>> = combine(
        dao.observeAll().map { it.toEventDomainList() },
        authRepo.currentUserFlow()
    ) { events, user -> events.filterEventsForUser(user) }

    fun observeRsvpd(): Flow<List<Event>> = combine(
        dao.observeRsvpd().map { it.toEventDomainList() },
        authRepo.currentUserFlow()
    ) { events, user -> events.filterEventsForUser(user) }

    fun syncFromFirestore(): Flow<List<Event>> =
        firebase.streamEvents()
            .map { events ->
                dao.upsertAll(events.toEventEntityList())
                events
            }
            .catch { emit(emptyList()) }

    suspend fun postEvent(event: Event): Result<String> = firebase.postEvent(event)

    suspend fun updateEvent(id: String, title: String, description: String, location: String, dateTimeMillis: Long, imageUrl: String?): Result<Unit> =
        firebase.updateEvent(id, title, description, location, dateTimeMillis, imageUrl)

    suspend fun deleteEvent(id: String): Result<Unit> {
        dao.deleteById(id)
        return firebase.deleteEvent(id)
    }

    suspend fun rsvp(eventId: String, attending: Boolean) {
        // One-way RSVP only: users cannot withdraw once they have committed.
        if (!attending) return

        // Optimistic local update.
        dao.updateRsvp(eventId, true, 1)
        val uid = firebase.currentUid ?: return
        firebase.rsvpEvent(eventId, uid, true)
            .onFailure {
                // Roll back the optimistic update if the server rejects the RSVP
                // (e.g. the user had already RSVP'd in another session).
                dao.updateRsvp(eventId, false, -1)
            }
    }

    suspend fun clearLocalData() = dao.clearAll()
}

// ─── Course Repository ────────────────────────────────────────────────────────
// The admin maintains a `courses` Firestore collection (doc ID = course code).
// This is the single source of truth for course codes and names.

@Singleton
class CourseRepository @Inject constructor(
    private val firebase: FirebaseService
) {
    fun streamCourses(): Flow<List<CourseEntry>> = firebase.streamCourses()
        .catch { emit(emptyList()) }

    suspend fun addCourse(code: String, name: String): Result<String> =
        firebase.addCourse(code, name)

    suspend fun updateCourse(id: String, name: String): Result<Unit> =
        firebase.updateCourse(id, name)

    suspend fun deleteCourse(id: String): Result<Unit> =
        firebase.deleteCourse(id)
}

// ─── Lecturer Repository ─────────────────────────────────────────────────────
// One lecturer document can reference multiple course codes via `courseIds`,
// which is what allows the timetable screen to filter lecturers per course.

@Singleton
class LecturerRepository @Inject constructor(
    private val firebase: FirebaseService
) {
    fun streamLecturers(): Flow<List<LecturerEntry>> = firebase.streamLecturers()
        .catch { emit(emptyList()) }

    suspend fun addLecturer(
        name      : String,
        lecturerId: String,
        courseIds : List<String>
    ): Result<String> = firebase.addLecturer(name, lecturerId, courseIds)

    suspend fun updateLecturer(
        id        : String,
        name      : String,
        lecturerId: String,
        courseIds : List<String>
    ): Result<Unit> = firebase.updateLecturer(id, name, lecturerId, courseIds)

    suspend fun deleteLecturer(id: String): Result<Unit> = firebase.deleteLecturer(id)
}

// ─── Filter helpers ───────────────────────────────────────────────────────────

private fun List<Notice>.filterForUser(user: User?): List<Notice> =
    filter { notice ->
        val deptOk = notice.targetDepartment.isNullOrBlank() ||
                notice.targetDepartment == user?.department
        val courseOk = notice.targetCourse.isNullOrBlank() ||
                user?.courses?.map { it.uppercase() }
                    ?.contains(notice.targetCourse!!.uppercase()) == true
        deptOk && courseOk
    }

/**
 * A timetable entry is visible to a student when ANY of these is true:
 *   1. targetDepartment is blank — the entry is broadcast to everyone.
 *   2. targetDepartment matches the student's declared department.
 *   3. The student has the entry's courseCode in their enrolled courses list.
 */
private fun List<TimetableEntry>.filterTimetableForStudent(user: User?): List<TimetableEntry> {
    val userCourses = user?.courses?.map { it.trim().uppercase() }?.toSet() ?: emptySet()
    return filter { entry ->
        entry.targetDepartment.isBlank() ||
                entry.targetDepartment == user?.department ||
                userCourses.contains(entry.courseCode.trim().uppercase())
    }
}

private fun List<Event>.filterEventsForUser(user: User?): List<Event> =
    filter { event ->
        val deptOk = event.targetDepartment.isNullOrBlank() ||
                event.targetDepartment == user?.department
        val courseOk = event.targetCourse.isNullOrBlank() ||
                user?.courses?.map { it.uppercase() }
                    ?.contains(event.targetCourse!!.uppercase()) == true
        deptOk && courseOk
    }