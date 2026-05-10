package com.campusconnect.app

import com.campusconnect.app.data.repository.AuthRepository
import com.campusconnect.app.data.repository.NoticeRepository
import com.campusconnect.app.domain.model.Notice
import com.campusconnect.app.domain.model.NoticeCategory
import com.campusconnect.app.domain.model.NoticePriority
import com.campusconnect.app.domain.model.User
import com.campusconnect.app.domain.usecase.GetSavedNoticesUseCase
import com.campusconnect.app.domain.usecase.GetNoticesByCategoryUseCase
import com.campusconnect.app.domain.usecase.GetNoticesUseCase
import com.campusconnect.app.domain.usecase.SearchNoticesUseCase
import com.campusconnect.app.utils.SyncState
import com.campusconnect.app.viewmodel.NoticesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyString
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class NoticesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    @Mock
    private lateinit var noticeRepository: NoticeRepository

    @Mock
    private lateinit var authRepository: AuthRepository

    private lateinit var viewModel: NoticesViewModel

    // All categories use actual NoticeCategory enum values (ACADEMIC replaces
    // EXAM which never existed; EMERGENCY replaces URGENT; EVENTS replaces EVENT;
    // CRITICAL priority doesn't exist — HIGH/URGENT are the valid top levels)
    private val sampleNotices = listOf(
        Notice(
            id       = "1",
            title    = "Exam Postponed",
            content  = "Math exam moved to Friday",
            category = NoticeCategory.ACADEMIC,
            priority = NoticePriority.HIGH,
            author   = "Registrar"
        ),
        Notice(
            id       = "2",
            title    = "Fee Deadline",
            content  = "Pay by end of month",
            category = NoticeCategory.FINANCE,
            priority = NoticePriority.URGENT,
            author   = "Finance"
        ),
        Notice(
            id       = "3",
            title    = "Sports Day",
            content  = "Saturday 9am, Main Field",
            category = NoticeCategory.SPORTS,
            priority = NoticePriority.LOW,
            author   = "Dean of Students"
        ),
        Notice(
            id       = "4",
            title    = "Urgent: Fire Drill",
            content  = "Assembly point A",
            category = NoticeCategory.EMERGENCY,
            priority = NoticePriority.URGENT,
            author   = "Safety"
        )
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        `when`(noticeRepository.observeAll()).thenReturn(flowOf(sampleNotices))
        `when`(noticeRepository.observeSaved()).thenReturn(flowOf(emptyList()))
        `when`(noticeRepository.search(anyString())).thenReturn(flowOf(emptyList()))
        `when`(noticeRepository.observeByCategory(anyString())).thenReturn(flowOf(emptyList()))
        `when`(noticeRepository.syncFromFirestore()).thenReturn(flowOf(sampleNotices))
        `when`(authRepository.currentUserFlow()).thenReturn(flowOf(null))
        `when`(authRepository.isAdmin()).thenReturn(false)

        // Constructor matches the actual NoticesViewModel signature: (repo, authRepo)
        viewModel = NoticesViewModel(
            repo     = noticeRepository,
            authRepo = authRepository
        )
    }

    @Test
    fun `initial state loads all notices`() = runTest {
        val notices = viewModel.notices.first()
        assertEquals(4, notices.size)
    }

    @Test
    fun `sync state starts as Loading`() = runTest {
        assertEquals(SyncState.Loading, viewModel.syncState.value)
    }

    @Test
    fun `setSearch updates searchQuery`() = runTest {
        viewModel.setSearch("exam")
        assertEquals("exam", viewModel.searchQuery.value)
    }

    @Test
    fun `setCategory updates selectedCategory`() = runTest {
        viewModel.setCategory(NoticeCategory.ACADEMIC)
        assertEquals(NoticeCategory.ACADEMIC, viewModel.selectedCategory.value)
    }

    @Test
    fun `clearCategory sets selectedCategory to null`() = runTest {
        viewModel.setCategory(NoticeCategory.ACADEMIC)
        viewModel.setCategory(null)
        assertNull(viewModel.selectedCategory.value)
    }

    @Test
    fun `toggleSavedFilter flips showSavedOnly`() = runTest {
        assertFalse(viewModel.showSavedOnly.value)

        viewModel.toggleSavedFilter()
        assertTrue(viewModel.showSavedOnly.value)

        viewModel.toggleSavedFilter()
        assertFalse(viewModel.showSavedOnly.value)
    }
}

class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
