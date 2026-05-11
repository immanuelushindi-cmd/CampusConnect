package com.campusconnect.app.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.app.data.remote.CloudinaryService
import com.campusconnect.app.data.remote.FirebaseService
import com.campusconnect.app.data.repository.AuthRepository
import com.campusconnect.app.data.repository.CourseRepository
import com.campusconnect.app.data.repository.EventRepository
import com.campusconnect.app.data.repository.LecturerRepository
import com.campusconnect.app.data.repository.NoticeRepository
import com.campusconnect.app.data.repository.TimetableRepository
import com.campusconnect.app.domain.model.CourseEntry
import com.campusconnect.app.domain.model.Event
import com.campusconnect.app.domain.model.LecturerEntry
import com.campusconnect.app.domain.model.Notice
import com.campusconnect.app.domain.model.NoticeCategory
import com.campusconnect.app.domain.model.NoticePriority
import com.campusconnect.app.domain.model.TimetableEntry
import com.campusconnect.app.domain.model.User
import com.campusconnect.app.utils.SyncState
import com.campusconnect.app.utils.UiState
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.delay

// ════════════════════════════════════════════════════════════════════════════
//  SHARED DATA CLASSES
// ════════════════════════════════════════════════════════════════════════════

data class AuthUiState(
    val isLoading: Boolean = false,
    val success:   Boolean = false,
    val error:     String? = null
)

data class DashboardStats(
    val noticeCount: Int = 0,
    val eventCount:  Int = 0
)

data class AdminStats(
    val totalNotices: Int = 0,
    val totalEvents:  Int = 0,
    val totalUsers:   Int = 0
)

// ════════════════════════════════════════════════════════════════════════════
//  AUTH VIEW MODEL
// ════════════════════════════════════════════════════════════════════════════

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val isLoggedIn: Boolean get() = authRepo.isLoggedIn

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    val currentUserFlow = authRepo.currentUserFlow()

    fun signIn(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState(error = "Username and password are required")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            authRepo.signInWithUsername(username.trim(), password)
                .onSuccess {
                    _isAdmin.value = authRepo.isAdmin()
                    _uiState.value = AuthUiState(success = true)
                }
                .onFailure { err ->
                    _uiState.value = AuthUiState(error = err.message ?: "Sign in failed")
                }
        }
    }

    fun signUp(username: String, email: String, password: String, name: String) {
        if (username.isBlank() || email.isBlank() || password.isBlank() || name.isBlank()) {
            _uiState.value = AuthUiState(error = "All fields are required")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            authRepo.signUpWithUsername(username.trim(), email.trim(), password, name.trim())
                .onSuccess {
                    _isAdmin.value = authRepo.isAdmin()
                    _uiState.value = AuthUiState(success = true)
                }
                .onFailure { err ->
                    _uiState.value = AuthUiState(error = err.message ?: "Sign up failed")
                }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            authRepo.signInWithGoogle(idToken)
                .onSuccess {
                    _isAdmin.value = authRepo.isAdmin()
                    _uiState.value = AuthUiState(success = true)
                }
                .onFailure { err ->
                    _uiState.value = AuthUiState(error = err.message ?: "Google sign-in failed")
                }
        }
    }

    // FIX: added here so LoginScreen (which uses AuthViewModel) can call it
    // from the Forgot Password dialog before the user is logged in.
    suspend fun sendPasswordReset(email: String): String {
        val target = email.trim().takeIf { it.isNotBlank() }
            ?: return "Please enter your email address."
        return authRepo.sendPasswordReset(target)
            .fold(
                onSuccess = { "Reset link sent to $target. Check your inbox." },
                onFailure = { err ->
                    when {
                        err.message?.contains("no user record", ignoreCase = true) == true ->
                            "No account found for that email address."
                        err.message?.contains("badly formatted", ignoreCase = true) == true ->
                            "Please enter a valid email address."
                        else -> err.message ?: "Failed to send reset email."
                    }
                }
            )
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  PROFILE VIEW MODEL
// ════════════════════════════════════════════════════════════════════════════

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val firebase: FirebaseService
) : ViewModel() {

    val user: StateFlow<User?> = authRepo.currentUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _adminEmail = MutableStateFlow<String?>(null)
    val adminEmail: StateFlow<String?> = _adminEmail.asStateFlow()

    init {
        viewModelScope.launch {
            _adminEmail.value = firebase.getAdminEmail()
        }
    }

    fun updateProfile(
        name:        String,
        bio:         String       = "",
        department:  String       = "",
        studentId:   String       = "",
        yearOfStudy: Int          = 1,
        courses:     List<String> = emptyList()
    ) {
        viewModelScope.launch {
            authRepo.updateProfile(
                displayName = name,
                bio         = bio,
                department  = department,
                studentId   = studentId,
                yearOfStudy = yearOfStudy,
                courses     = courses
            )
        }
    }

    private val _isUploadingPhoto = MutableStateFlow(false)
    val isUploadingPhoto: StateFlow<Boolean> = _isUploadingPhoto.asStateFlow()

    fun updateProfilePhoto(uri: Uri) {
        viewModelScope.launch {
            _isUploadingPhoto.value = true
            authRepo.updateProfilePhoto(uri)
            _isUploadingPhoto.value = false
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { authRepo.updateNotificationsEnabled(enabled) }
    }

    // FIX: single sendPasswordReset — duplicate overload removed.
    // Works for both logged-in (no email arg needed) and logged-out flows.
    suspend fun sendPasswordReset(email: String? = null): String {
        val target = email?.takeIf { it.isNotBlank() }
            ?: authRepo.currentUser?.email
            ?: return "Could not determine email address."
        return authRepo.sendPasswordReset(target)
            .fold(
                onSuccess = { "Reset link sent to $target. Check your inbox." },
                onFailure = { err ->
                    when {
                        err.message?.contains("no user record", ignoreCase = true) == true ->
                            "No account found for that email address."
                        err.message?.contains("badly formatted", ignoreCase = true) == true ->
                            "Please enter a valid email address."
                        else -> err.message ?: "Failed to send reset email."
                    }
                }
            )
    }

    fun logout() {
        authRepo.signOut()
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  DASHBOARD VIEW MODEL  (student home screen)
// ════════════════════════════════════════════════════════════════════════════

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepo:      AuthRepository,
    private val noticeRepo:    NoticeRepository,
    private val timetableRepo: TimetableRepository,
    private val eventRepo:     EventRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val user: StateFlow<User?> = authRepo.currentUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val recentNotices: StateFlow<List<Notice>> = noticeRepo.observeAll()
        .map { it.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val upcomingEvents: StateFlow<List<Event>> = eventRepo.observeAll()
        .map { events ->
            val nowMs = System.currentTimeMillis()
            events
                .filter { it.startTimestampMs >= nowMs }
                .sortedBy { it.startTimestampMs }
                .take(3)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val nextClass: StateFlow<TimetableEntry?> = timetableRepo.observeAll()
        .map { entries ->
            val cal        = Calendar.getInstance()
            val todayIndex = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY    -> 0
                Calendar.TUESDAY   -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY  -> 3
                Calendar.FRIDAY    -> 4
                Calendar.SATURDAY  -> 5
                Calendar.SUNDAY    -> 6
                else               -> 0
            }
            val nowMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            entries
                .filter { it.dayOfWeek == todayIndex }
                .filter { it.startHour * 60 + it.startMinute >= nowMins }
                .minByOrNull { it.startHour * 60 + it.startMinute }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val stats: StateFlow<DashboardStats> = noticeRepo.observeAll()
        .map { notices -> DashboardStats(noticeCount = notices.size) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardStats())

    init { syncAll() }

    private fun syncAll() {
        viewModelScope.launch {
            _isLoading.value = true
            launch { noticeRepo.syncFromFirestore().catch { }.collect { } }
            launch { timetableRepo.syncFromFirestore().catch { }.collect { } }
            launch { eventRepo.syncFromFirestore().catch { }.collect { } }
            delay(800)
            _isLoading.value = false
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  NOTICES VIEW MODEL
// ════════════════════════════════════════════════════════════════════════════

@HiltViewModel
class NoticesViewModel @Inject constructor(
    private val repo:     NoticeRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _searchQuery      = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<NoticeCategory?>(null)
    private val _showSavedOnly    = MutableStateFlow(false)
    private val _syncState        = MutableStateFlow<SyncState>(SyncState.Idle)
    private val _isAdmin          = MutableStateFlow(false)

    val searchQuery:      StateFlow<String>          = _searchQuery.asStateFlow()
    val selectedCategory: StateFlow<NoticeCategory?> = _selectedCategory.asStateFlow()
    val showSavedOnly:    StateFlow<Boolean>          = _showSavedOnly.asStateFlow()
    val syncState:        StateFlow<SyncState>        = _syncState.asStateFlow()

    private val userCoursesFlow: Flow<List<String>> = authRepo.currentUserFlow()
        .map { user -> user?.courses ?: emptyList() }

    @OptIn(ExperimentalCoroutinesApi::class)
    val notices: StateFlow<List<Notice>> = combine(
        _isAdmin,
        userCoursesFlow,
        _showSavedOnly,
        _selectedCategory,
        _searchQuery
    ) { args ->
        val isAdmin     = args[0] as Boolean
        val userCourses = args[1] as List<*>
        val favOnly     = args[2] as Boolean
        val cat         = args[3] as NoticeCategory?
        val query       = args[4] as String
        listOf(isAdmin, userCourses, favOnly, cat, query)
    }.flatMapLatest { args ->
        val isAdmin     = args[0] as Boolean
        @Suppress("UNCHECKED_CAST")
        val userCourses = args[1] as List<String>
        val favOnly     = args[2] as Boolean
        val cat         = args[3] as NoticeCategory?
        val query       = args[4] as String
        val base: Flow<List<Notice>> = when {
            favOnly            -> repo.observeSaved()
            query.isNotBlank() -> repo.search(query)
            cat != null        -> repo.observeByCategory(cat.name)
            else               -> repo.observeAll()
        }
        if (isAdmin) {
            base
        } else {
            base.map { list ->
                list.filter { notice ->
                    notice.targetCourse.isNullOrBlank() ||
                            notice.targetCourse in userCourses
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { _isAdmin.value = authRepo.isAdmin() }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading
            repo.syncFromFirestore()
                .catch { err -> _syncState.value = SyncState.Error(err.message ?: "Sync failed") }
                .collect { _syncState.value = SyncState.Success }
        }
    }

    fun setSearch(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) _selectedCategory.value = null
    }

    fun setCategory(cat: NoticeCategory?) {
        _selectedCategory.value = cat
        _searchQuery.value = ""
        _showSavedOnly.value = false
    }

    fun toggleSavedFilter() {
        _showSavedOnly.value = !_showSavedOnly.value
    }

    fun toggleSaved(notice: Notice) {
        viewModelScope.launch { repo.toggleSaved(notice.id, !notice.isSaved) }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  NOTICE DETAIL VIEW MODEL
// ════════════════════════════════════════════════════════════════════════════

@HiltViewModel
class NoticeDetailViewModel @Inject constructor(
    private val repo: NoticeRepository
) : ViewModel() {

    private val _notice = MutableStateFlow<Notice?>(null)
    val notice: StateFlow<Notice?> = _notice.asStateFlow()

    fun load(noticeId: String) {
        viewModelScope.launch {
            repo.observeById(noticeId).collect { _notice.value = it }
        }
    }

    fun toggleSaved(notice: Notice) {
        viewModelScope.launch { repo.toggleSaved(notice.id, !notice.isSaved) }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  EVENTS VIEW MODEL
// ════════════════════════════════════════════════════════════════════════════

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val repo: EventRepository
) : ViewModel() {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _allEvents = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val events: StateFlow<List<Event>> = combine(_allEvents, _query) { list, q ->
        if (q.isBlank()) list
        else list.filter { e ->
            e.title.contains(q, ignoreCase = true) ||
                    e.description.contains(q, ignoreCase = true) ||
                    e.location.contains(q, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onSearchQueryChange(q: String) { _query.value = q }
    fun clearSearch() { _query.value = "" }

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading
            repo.syncFromFirestore()
                .catch { err -> _syncState.value = SyncState.Error(err.message ?: "Sync failed") }
                .collect { _syncState.value = SyncState.Success }
        }
    }

    fun toggleRsvp(eventId: String, attending: Boolean) {
        // Un-RSVP is not permitted; silently ignore any attempt to un-attend.
        if (!attending) return
        viewModelScope.launch { repo.rsvp(eventId, true) }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  TIMETABLE VIEW MODEL  (student timetable screen)
// ════════════════════════════════════════════════════════════════════════════

@HiltViewModel
class TimetableViewModel @Inject constructor(
    private val repo: TimetableRepository
) : ViewModel() {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val todayIndex = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY    -> 0
        Calendar.TUESDAY   -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY  -> 3
        Calendar.FRIDAY    -> 4
        Calendar.SATURDAY  -> 5
        Calendar.SUNDAY    -> 6
        else               -> 0
    }

    private val _selectedDay = MutableStateFlow(todayIndex)
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    val allEntries: StateFlow<List<TimetableEntry>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val entriesForSelectedDay: StateFlow<List<TimetableEntry>> = _selectedDay
        .flatMapLatest { day -> repo.observeByDay(day) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { refresh() }

    fun selectDay(day: Int) { _selectedDay.value = day.coerceIn(0, 6) }

    fun refresh() {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading
            repo.syncFromFirestore()
                .catch { err -> _syncState.value = SyncState.Error(err.message ?: "Sync failed") }
                .collect { _syncState.value = SyncState.Success }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  ADMIN VIEW MODEL  (post notices / events / timetable entries)
// ════════════════════════════════════════════════════════════════════════════

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val noticeRepo:    NoticeRepository,
    private val eventRepo:     EventRepository,
    private val timetableRepo: TimetableRepository,
    private val courseRepo:    CourseRepository,
    private val lecturerRepo:  LecturerRepository,
    private val firebase:      FirebaseService,
    private val cloudinary:    CloudinaryService
) : ViewModel() {

    private val _postState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val postState: StateFlow<UiState<String>> = _postState.asStateFlow()

    private val _eventState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val eventState: StateFlow<UiState<String>> = _eventState.asStateFlow()

    private val _timetableState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val timetableState: StateFlow<UiState<String>> = _timetableState.asStateFlow()

    val availableCourses: StateFlow<List<CourseEntry>> = courseRepo.streamCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availableLecturers: StateFlow<List<LecturerEntry>> = lecturerRepo.streamLecturers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _coursesLoaded = MutableStateFlow(false)
    val coursesLoaded: StateFlow<Boolean> = _coursesLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            availableCourses.collect { _coursesLoaded.value = true }
        }
    }

    fun postNotice(
        title:             String,
        content:           String,
        category:          NoticeCategory,
        priority:          NoticePriority,
        imageUri:          Uri?,
        targetDepartment:  String?,
        targetCourse:      String?,
        targetYearOfStudy: Int?
    ) {
        if (title.isBlank() || content.isBlank()) {
            _postState.value = UiState.Error("Title and content are required")
            return
        }
        viewModelScope.launch {
            _postState.value = UiState.Loading
            val imageUrl: String? = imageUri?.let { cloudinary.uploadImage(it).getOrNull() }
            val notice = Notice(
                title             = title.trim(),
                content           = content.trim(),
                category          = category,
                priority          = priority,
                author            = firebase.currentUserEmail ?: "Admin",
                authorEmail       = firebase.currentUserEmail ?: "",
                imageUrl          = imageUrl,
                targetDepartment  = targetDepartment,
                targetCourse      = targetCourse,
                targetYearOfStudy = targetYearOfStudy
            )
            noticeRepo.postNotice(notice)
                .onSuccess { id -> _postState.value = UiState.Success(id) }
                .onFailure { err -> _postState.value = UiState.Error(err.message ?: "Post failed") }
        }
    }

    fun resetPostState() { _postState.value = UiState.Idle }

    fun postEvent(
        title:             String,
        description:       String,
        location:          String,
        dateTimeMillis:    Long,
        maxAttendees:      Int,
        isOnline:          Boolean,
        meetLink:          String,
        imageUri:          Uri?,
        targetDepartment:  String?,
        targetYearOfStudy: Int?
    ) {
        if (title.isBlank()) {
            _eventState.value = UiState.Error("Event title is required")
            return
        }
        viewModelScope.launch {
            _eventState.value = UiState.Loading
            val imageUrl: String? = imageUri?.let { cloudinary.uploadImage(it).getOrNull() }
            val event = Event(
                title             = title.trim(),
                description       = description.trim(),
                location          = location.trim(),
                startTimestampMs  = dateTimeMillis,
                maxAttendees      = maxAttendees,
                isOnline          = isOnline,
                meetLink          = meetLink.takeIf { it.isNotBlank() },
                organizer         = firebase.currentUserEmail ?: "Admin",
                imageUrl          = imageUrl,
                targetDepartment  = targetDepartment,
                targetCourse      = null,
                targetYearOfStudy = targetYearOfStudy
            )
            eventRepo.postEvent(event)
                .onSuccess { id -> _eventState.value = UiState.Success(id) }
                .onFailure { err -> _eventState.value = UiState.Error(err.message ?: "Post failed") }
        }
    }

    fun resetEventState() { _eventState.value = UiState.Idle }

    fun postTimetableEntry(
        courseCode:        String,
        courseName:        String,
        lecturer:          String,
        room:              String,
        building:          String,
        dayOfWeek:         Int,
        startHour:         Int,
        startMinute:       Int,
        endHour:           Int,
        endMinute:         Int,
        colorHex:          String,
        notes:             String,
        targetDepartment:  String,
        targetYearOfStudy: Int?
    ) {
        if (courseCode.isBlank() || courseName.isBlank()) {
            _timetableState.value = UiState.Error("Course code and name are required")
            return
        }
        viewModelScope.launch {
            _timetableState.value = UiState.Loading
            val entry = TimetableEntry(
                courseCode        = courseCode.trim().uppercase(),
                courseName        = courseName.trim(),
                lecturer          = lecturer.trim(),
                room              = room.trim(),
                building          = building.trim(),
                dayOfWeek         = dayOfWeek,
                startHour         = startHour,
                startMinute       = startMinute,
                endHour           = endHour,
                endMinute         = endMinute,
                colorHex          = colorHex,
                notes             = notes.trim(),
                targetDepartment  = targetDepartment,
                targetYearOfStudy = targetYearOfStudy
            )
            timetableRepo.postEntry(entry)
                .onSuccess { id -> _timetableState.value = UiState.Success(id) }
                .onFailure { err -> _timetableState.value = UiState.Error(err.message ?: "Post failed") }
        }
    }

    fun resetTimetableState() { _timetableState.value = UiState.Idle }

    fun updateNotice(id: String, title: String, content: String, imageUri: Uri?, existingImageUrl: String?) {
        if (title.isBlank() || content.isBlank()) {
            _postState.value = UiState.Error("Title and content are required")
            return
        }
        viewModelScope.launch {
            _postState.value = UiState.Loading
            val imageUrl: String? = if (imageUri != null) cloudinary.uploadImage(imageUri).getOrNull() else existingImageUrl
            noticeRepo.updateNotice(id, title, content, imageUrl)
                .onSuccess { _postState.value = UiState.Success(id) }
                .onFailure { err -> _postState.value = UiState.Error(err.message ?: "Update failed") }
        }
    }

    fun updateEvent(id: String, title: String, description: String, location: String, dateTimeMillis: Long, imageUri: Uri?, existingImageUrl: String?) {
        if (title.isBlank()) {
            _eventState.value = UiState.Error("Event title is required")
            return
        }
        viewModelScope.launch {
            _eventState.value = UiState.Loading
            val imageUrl: String? = if (imageUri != null) cloudinary.uploadImage(imageUri).getOrNull() else existingImageUrl
            eventRepo.updateEvent(id, title, description, location, dateTimeMillis, imageUrl)
                .onSuccess { _eventState.value = UiState.Success(id) }
                .onFailure { err -> _eventState.value = UiState.Error(err.message ?: "Update failed") }
        }
    }

    fun updateTimetableEntry(entry: TimetableEntry) {
        viewModelScope.launch {
            _timetableState.value = UiState.Loading
            timetableRepo.updateEntry(entry)
                .onSuccess { _timetableState.value = UiState.Success(entry.id) }
                .onFailure { err -> _timetableState.value = UiState.Error(err.message ?: "Update failed") }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  COURSE MANAGEMENT VIEW MODEL
// ════════════════════════════════════════════════════════════════════════════

@HiltViewModel
class CourseManagementViewModel @Inject constructor(
    private val courseRepo:   CourseRepository,
    private val lecturerRepo: LecturerRepository
) : ViewModel() {

    val courses: StateFlow<List<CourseEntry>> = courseRepo.streamCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val lecturers: StateFlow<List<LecturerEntry>> = lecturerRepo.streamLecturers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _opState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val opState: StateFlow<UiState<String>> = _opState.asStateFlow()

    fun addCourse(code: String, name: String) {
        viewModelScope.launch {
            _opState.value = UiState.Loading
            courseRepo.addCourse(code, name)
                .onSuccess { _opState.value = UiState.Success("Course added") }
                .onFailure { _opState.value = UiState.Error(it.message ?: "Failed to add course") }
        }
    }

    fun updateCourse(id: String, name: String) {
        viewModelScope.launch {
            _opState.value = UiState.Loading
            courseRepo.updateCourse(id, name)
                .onSuccess { _opState.value = UiState.Success("Course updated") }
                .onFailure { _opState.value = UiState.Error(it.message ?: "Failed to update course") }
        }
    }

    fun deleteCourse(id: String) {
        viewModelScope.launch { courseRepo.deleteCourse(id) }
    }

    fun addLecturer(name: String, lecturerId: String, courseIds: List<String>) {
        viewModelScope.launch {
            _opState.value = UiState.Loading
            lecturerRepo.addLecturer(name, lecturerId, courseIds)
                .onSuccess { _opState.value = UiState.Success("Lecturer added") }
                .onFailure { _opState.value = UiState.Error(it.message ?: "Failed to add lecturer") }
        }
    }

    fun updateLecturer(id: String, name: String, lecturerId: String, courseIds: List<String>) {
        viewModelScope.launch {
            _opState.value = UiState.Loading
            lecturerRepo.updateLecturer(id, name, lecturerId, courseIds)
                .onSuccess { _opState.value = UiState.Success("Lecturer updated") }
                .onFailure { _opState.value = UiState.Error(it.message ?: "Failed to update lecturer") }
        }
    }

    fun deleteLecturer(id: String) {
        viewModelScope.launch { lecturerRepo.deleteLecturer(id) }
    }

    fun resetOpState() { _opState.value = UiState.Idle }
}

// ════════════════════════════════════════════════════════════════════════════
//  ADMIN DASHBOARD VIEW MODEL
// ════════════════════════════════════════════════════════════════════════════

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val noticeRepo:    NoticeRepository,
    private val eventRepo:     EventRepository,
    private val timetableRepo: TimetableRepository,
    private val courseRepo:    CourseRepository,
    private val authRepo:      AuthRepository,
    private val firebase:      FirebaseService,
    private val firestore:     FirebaseFirestore
) : ViewModel() {

    val user: StateFlow<User?> = authRepo.currentUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val recentNotices: StateFlow<List<Notice>> = firebase.streamNotices()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentEvents: StateFlow<List<Event>> = eventRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentTimetable: StateFlow<List<TimetableEntry>> = firebase.streamTimetable()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availableCourses: StateFlow<List<CourseEntry>> = courseRepo.streamCourses()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _stats = MutableStateFlow(AdminStats())
    val stats: StateFlow<AdminStats> = _stats.asStateFlow()

    init {
        syncContent()
        fetchStats()
    }

    private fun syncContent() {
        viewModelScope.launch {
            _isLoading.value = true
            withTimeoutOrNull(5_000L) {
                launch { noticeRepo.syncFromFirestore().catch { }.collect { } }
                launch { eventRepo.syncFromFirestore().catch { }.collect { } }
                launch { timetableRepo.syncFromFirestore().catch { }.collect { } }
            }
            _isLoading.value = false
        }
    }

    private fun fetchStats() {
        viewModelScope.launch {
            runCatching {
                val noticesCount = firestore.collection("notices")
                    .whereEqualTo("isActive", true).get().await().size()
                val eventsCount = firestore.collection("events").get().await().size()
                val usersCount  = firestore.collection("users").get().await().size()
                _stats.value = AdminStats(
                    totalNotices = noticesCount,
                    totalEvents  = eventsCount,
                    totalUsers   = usersCount
                )
            }
        }
    }

    fun deleteNotice(id: String) {
        viewModelScope.launch { noticeRepo.deleteNotice(id); fetchStats() }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch { eventRepo.deleteEvent(id); fetchStats() }
    }

    fun deleteTimetableEntry(id: String) {
        viewModelScope.launch { timetableRepo.deleteEntry(id) }
    }

    fun updateTimetableEntry(entry: TimetableEntry) {
        viewModelScope.launch { timetableRepo.updateEntry(entry) }
    }

    fun updateNotice(id: String, title: String, content: String) {
        viewModelScope.launch { noticeRepo.updateNotice(id, title, content, imageUrl = null) }
    }

    fun updateEvent(id: String, title: String, description: String, location: String, dateTimeMillis: Long) {
        viewModelScope.launch {
            eventRepo.updateEvent(id, title, description, location, dateTimeMillis, imageUrl = null)
        }
    }

    fun deleteAllNotices(notices: List<Notice>) {
        viewModelScope.launch { notices.forEach { noticeRepo.deleteNotice(it.id) }; fetchStats() }
    }

    fun deleteAllTimetableEntries(entries: List<TimetableEntry>) {
        viewModelScope.launch { entries.forEach { timetableRepo.deleteEntry(it.id) } }
    }
}