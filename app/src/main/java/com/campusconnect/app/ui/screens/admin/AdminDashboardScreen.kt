package com.campusconnect.app.ui.screens.admin

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.campusconnect.app.domain.model.*
import com.campusconnect.app.ui.screens.admin.shared.EditNoticeSheet
import com.campusconnect.app.ui.screens.admin.shared.NoticeDetailSheet
import com.campusconnect.app.utils.DateFormatters
import com.campusconnect.app.viewmodel.AdminDashboardViewModel

// ── Palette ───────────────────────────────────────────────────────────────────
private val NavyStart    = Color(0xFF060D2E)
private val NavyMid      = Color(0xFF0D1B4B)
private val NavyEnd      = Color(0xFF1A2B6B)
private val AccentBlue   = Color(0xFF3B5BDB)
private val AccentPurple = Color(0xFF7C3AED)
private val AccentGreen  = Color(0xFF059669)
private val AccentOrange = Color(0xFFEA580C)


// ── Dialog / sheet state ──────────────────────────────────────────────────────
private sealed interface AdminDialog {
    data object None                                   : AdminDialog
    data class  NoticeDetail(val n: Notice)            : AdminDialog
    data class  EditNotice(val n: Notice)              : AdminDialog
    data class  DeleteNotice(val n: Notice)            : AdminDialog
    data object DeleteAllNotices                       : AdminDialog
    data class  EventDetail(val e: Event)              : AdminDialog
    data class  DeleteEvent(val e: Event)              : AdminDialog
    data object EventSearch                            : AdminDialog
    data object DeleteAllEvents                        : AdminDialog

    data object NoticeCourseSearch                     : AdminDialog
    data class  NoticesByCourse(val course: CourseEntry) : AdminDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToAdminNotices:  () -> Unit,
    onNavigateToPostNotice:    () -> Unit,
    onNavigateToPostEvent:     () -> Unit,
    onNavigateToEvents:        () -> Unit,
    onNavigateToProfile:       () -> Unit,
    onNavigateToPostTimetable: () -> Unit,
    onManageCourses:           () -> Unit,
    vm: AdminDashboardViewModel = hiltViewModel()
) {
    val user      by vm.user.collectAsStateWithLifecycle()
    val notices: List<Notice>           by vm.recentNotices.collectAsStateWithLifecycle()
    val events: List<Event>             by vm.recentEvents.collectAsStateWithLifecycle()
    val courses: List<CourseEntry>      by vm.availableCourses.collectAsStateWithLifecycle()
    val stats     by vm.stats.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()

    var dialog          by remember { mutableStateOf<AdminDialog>(AdminDialog.None) }

    // ── Overlays ──────────────────────────────────────────────────────────────
    when (val d = dialog) {
        is AdminDialog.NoticeDetail -> NoticeDetailSheet(
            notice    = d.n,
            onDismiss = { dialog = AdminDialog.None },
            onDelete  = { dialog = AdminDialog.DeleteNotice(d.n) }
        )
        is AdminDialog.EditNotice -> EditNoticeSheet(
            notice    = d.n,
            onDismiss = { dialog = AdminDialog.None },
            onSave    = { id, title, content ->
                vm.updateNotice(id, title, content)
                dialog = AdminDialog.None
            }
        )
        is AdminDialog.DeleteNotice -> ConfirmDeleteDialog(
            title     = "Delete Notice?",
            body      = "\"${d.n.title}\" will be permanently removed.",
            onConfirm = { vm.deleteNotice(d.n.id); dialog = AdminDialog.None },
            onDismiss = { dialog = AdminDialog.None }
        )
        AdminDialog.DeleteAllNotices -> ConfirmDeleteDialog(
            title     = "Delete All Notices?",
            body      = "All ${notices.size} notices will be permanently deleted.",
            onConfirm = { vm.deleteAllNotices(notices); dialog = AdminDialog.None },
            onDismiss = { dialog = AdminDialog.None }
        )
        is AdminDialog.EventDetail -> DashboardEventDetailSheet(
            event     = d.e,
            onDismiss = { dialog = AdminDialog.None },
            onDelete  = { dialog = AdminDialog.DeleteEvent(d.e) },
            onEdit    = onNavigateToEvents
        )
        is AdminDialog.DeleteEvent -> ConfirmDeleteDialog(
            title     = "Delete Event?",
            body      = "\"${d.e.title}\" will be permanently removed.",
            onConfirm = { vm.deleteEvent(d.e.id); dialog = AdminDialog.None },
            onDismiss = { dialog = AdminDialog.None }
        )

        // ── Course-search sheet for Notices ───────────────────────────────────
        AdminDialog.NoticeCourseSearch -> CourseSearchSheet(
            title       = "Browse Notices by Course",
            subtitle    = "Search a course to see its targeted notices",
            courses     = courses,
            countFor    = { course -> notices.count { it.targetCourse == course.code } },
            accentColor = AccentBlue,
            onDismiss   = { dialog = AdminDialog.None },
            onSelect    = { course -> dialog = AdminDialog.NoticesByCourse(course) }
        )
        is AdminDialog.NoticesByCourse -> CourseNoticesSheet(
            course    = d.course,
            notices   = notices.filter { it.targetCourse == d.course.code },
            onBack    = { dialog = AdminDialog.NoticeCourseSearch },
            onDismiss = { dialog = AdminDialog.None },
            onNoticeClick = { n -> dialog = AdminDialog.NoticeDetail(n) }
        )


        AdminDialog.DeleteAllEvents -> ConfirmDeleteDialog(
            title     = "Delete All Events?",
            body      = "All ${events.size} events will be permanently deleted.",
            onConfirm = { events.forEach { vm.deleteEvent(it.id) }; dialog = AdminDialog.None },
            onDismiss = { dialog = AdminDialog.None }
        )
        AdminDialog.EventSearch -> EventSearchSheet(
            events    = events,
            onDismiss = { dialog = AdminDialog.None },
            onSelect  = { event -> dialog = AdminDialog.EventDetail(event) }
        )
        AdminDialog.None -> Unit
    }

    // ── Screen ────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(NavyStart, NavyMid, NavyEnd)))
                    .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Admin Dashboard",
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color.White,
                            fontSize   = 26.sp,
                            letterSpacing = (-0.3).sp)
                        Text(
                            if (isLoading) "Syncing…"
                            else "Welcome back, ${user?.displayName?.split(" ")?.firstOrNull() ?: "Admin"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(0.65f)
                        )
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Filled.ManageAccounts, "Profile", tint = Color.White)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── Stats ─────────────────────────────────────────────────────────
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CompactStatCard(Icons.Outlined.Campaign, "Notices",
                        notices.size.toString(), AccentBlue, Modifier.weight(1f)) {}
                    CompactStatCard(Icons.Outlined.Event, "Events",
                        events.size.toString(), AccentPurple, Modifier.weight(1f), onNavigateToEvents)
                    CompactStatCard(Icons.Outlined.People, "Users",
                        stats.totalUsers.toString(), AccentGreen, Modifier.weight(1f)) {}
                }
            }

            // ── Quick Actions ─────────────────────────────────────────────────
            item {
                DashboardSectionHeader("Quick Actions")
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VerticalQuickTile(Icons.Filled.PostAdd,        "Post Notice",      AccentBlue,   Modifier.weight(1f), onNavigateToPostNotice)
                        VerticalQuickTile(Icons.Filled.EventAvailable, "Post Event",       AccentPurple, Modifier.weight(1f), onNavigateToPostEvent)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VerticalQuickTile(Icons.Filled.Schedule, "Add to Timetable",
                            AccentGreen, Modifier.weight(1f), onNavigateToPostTimetable)
                        VerticalQuickTile(Icons.Filled.MenuBook, "Manage Courses",
                            AccentOrange, Modifier.weight(1f), onManageCourses)
                    }
                }
            }

            // ── Notices section ───────────────────────────────────────────────
            item {
                SectionHeader(
                    title       = "Recent Notices",
                    count       = notices.size,
                    countColor  = AccentBlue,
                    accentColor = AccentBlue,

                    seeAllLabel = "See all Notices",
                    searchLabel = "Browse by course",
                    onSearch    = { if (courses.isNotEmpty()) dialog = AdminDialog.NoticeCourseSearch },
                    onSeeAll    = onNavigateToAdminNotices,
                    onDeleteAll = if (notices.isNotEmpty()) {
                        { dialog = AdminDialog.DeleteAllNotices }
                    } else null
                )
            }

            if (notices.isEmpty()) {
                item { AdminEmptyHint("No notices yet") }
            } else {
                items(notices.take(3), key = { it.id }) { notice ->
                    CompactNoticeRow(
                        notice   = notice,
                        onClick  = { dialog = AdminDialog.NoticeDetail(notice) },
                        onEdit   = { dialog = AdminDialog.EditNotice(notice) },
                        onDelete = { dialog = AdminDialog.DeleteNotice(notice) }
                    )
                }
                if (notices.size > 3) {
                    item {
                        TextButton(modifier = Modifier.fillMaxWidth(), onClick = {}) {
                            Text("+ ${notices.size - 3} more notices",
                                style = MaterialTheme.typography.bodySmall, color = AccentBlue)
                        }
                    }
                }
            }

            // ── Events section ────────────────────────────────────────────────
            item {
                SectionHeader(
                    title       = "Recent Events",
                    count       = events.size,
                    countColor  = AccentPurple,
                    accentColor = AccentPurple,
                    seeAllLabel = "See all Events",
                    searchLabel = "Search events",
                    onSearch    = { if (events.isNotEmpty()) dialog = AdminDialog.EventSearch },
                    onSeeAll    = onNavigateToEvents,
                    onDeleteAll = if (events.isNotEmpty()) {
                        { dialog = AdminDialog.DeleteAllEvents }
                    } else null
                )
            }

            if (events.isEmpty()) {
                item { AdminEmptyHint("No events posted") }
            } else {
                items(events.take(2), key = { it.id }) { event ->
                    DashboardEventCard(
                        event    = event,
                        onClick  = { dialog = AdminDialog.EventDetail(event) },
                        onDelete = { dialog = AdminDialog.DeleteEvent(event) }
                    )
                }
                if (events.size > 2) {
                    item {
                        TextButton(onClick = onNavigateToEvents, modifier = Modifier.fillMaxWidth()) {
                            Text("+ ${events.size - 2} more events",
                                style = MaterialTheme.typography.bodySmall, color = AccentPurple)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SectionHeader — unified header used across dashboard sections.
// Shows: Title · count badge · "Browse by course" search pill · New · Delete-all.
// The count badge replaces the old chip row — one number, clean.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SectionHeader(
    title:       String,
    count:       Int,
    countColor:  Color,
    accentColor: Color,
    searchLabel: String,
    onSearch:    () -> Unit,
    seeAllLabel: String,
    onSeeAll:    () -> Unit,
    onDeleteAll: (() -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Title + count pill
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(title,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold)
                // Count badge
                if (count > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(countColor.copy(0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            count.toString(),
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color      = countColor
                        )
                    }
                }
            }
            // Action buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSeeAll) {
                    Text(seeAllLabel, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(2.dp))
                    Icon(Icons.Filled.ArrowForward, null, modifier = Modifier.size(14.dp))
                }
                if (onDeleteAll != null) {
                    IconButton(onClick = onDeleteAll, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.DeleteSweep, "Delete all",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // "Browse by course" search trigger pill — consistent for both sections
        Surface(
            onClick     = onSearch,
            color       = accentColor.copy(0.06f),
            shape       = RoundedCornerShape(12.dp),
            border      = BorderStroke(1.dp, accentColor.copy(0.18f)),
            modifier    = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Filled.Search, null,
                    tint     = accentColor.copy(0.6f),
                    modifier = Modifier.size(18.dp))
                Text(searchLabel,
                    style  = MaterialTheme.typography.bodySmall,
                    color  = accentColor.copy(0.55f),
                    modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, null,
                    tint     = accentColor.copy(0.4f),
                    modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CourseSearchSheet — reusable across dashboard sections.
// Displays a live-search list of courses. Tapping one drills into the content.
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseSearchSheet(
    title:       String,
    subtitle:    String,
    courses:     List<CourseEntry>,
    countFor:    (CourseEntry) -> Int,
    accentColor: Color,
    onDismiss:   () -> Unit,
    onSelect:    (CourseEntry) -> Unit
) {
    var query by remember { mutableStateOf("") }

    val filtered: List<CourseEntry> = remember(courses, query) {
        if (query.isBlank()) courses
        else courses.filter {
            it.code.contains(query, ignoreCase = true) ||
                    it.name.contains(query, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            // Coloured header strip — matches the calling section's accent
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(accentColor.copy(0.06f))
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Box(
                    Modifier
                        .width(36.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accentColor.copy(0.3f))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(12.dp))
                Text(title,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color      = accentColor)
                Text(subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                // Live search field
                OutlinedTextField(
                    value         = query,
                    onValueChange = { query = it },
                    placeholder   = { Text("Search course code or name…") },
                    leadingIcon   = {
                        Icon(Icons.Filled.Search, null,
                            tint = accentColor, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon  = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, "Clear",
                                    modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine    = true,
                    shape         = RoundedCornerShape(14.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = accentColor,
                        unfocusedBorderColor = accentColor.copy(0.3f)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        }
    ) {
        if (filtered.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.SearchOff, null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.35f))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (query.isBlank()) "No courses in the catalogue"
                        else "No courses match \"$query\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filtered.forEach { course ->
                    val count = countFor(course)
                    CourseResultRow(
                        course      = course,
                        count       = count,
                        accentColor = accentColor,
                        onClick     = { onSelect(course) }
                    )
                }
            }
        }
    }
}

// ── Single course result row inside the search sheet ─────────────────────────
@Composable
private fun CourseResultRow(
    course:      CourseEntry,
    count:       Int,
    accentColor: Color,
    onClick:     () -> Unit
) {
    Card(
        onClick   = onClick,
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Code badge
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(
                        listOf(accentColor.copy(0.85f), accentColor)
                    ))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(course.code,
                    fontWeight    = FontWeight.ExtraBold,
                    fontSize      = 12.sp,
                    color         = Color.White,
                    letterSpacing = 0.5.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(course.name,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis)
                Text(
                    if (count == 0) "No entries"
                    else "$count ${if (count == 1) "entry" else "entries"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (count > 0) accentColor.copy(0.8f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                )
            }
            Icon(Icons.Filled.ChevronRight, null,
                tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f),
                modifier = Modifier.size(18.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CourseNoticesSheet — drills into notices for one specific course
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseNoticesSheet(
    course:         CourseEntry,
    notices:        List<Notice>,
    onBack:         () -> Unit,
    onDismiss:      () -> Unit,
    onNoticeClick:  (Notice) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(AccentBlue.copy(0.06f))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Box(
                    Modifier.width(36.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AccentBlue.copy(0.3f))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.ArrowBack, "Back",
                            tint = AccentBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text("${course.code} — ${course.name}",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color      = AccentBlue)
                        Text(
                            "${notices.size} notice${if (notices.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (notices.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Campaign, null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f))
                        Spacer(Modifier.height(10.dp))
                        Text("No notices targeted at ${course.code}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                notices.forEach { notice ->
                    val priorityColor: Color = when (notice.priority) {
                        NoticePriority.URGENT -> Color(0xFFE91E63)
                        NoticePriority.HIGH   -> Color(0xFFEF4444)
                        NoticePriority.MEDIUM -> Color(0xFFF97316)
                        NoticePriority.LOW    -> Color(0xFF10B981)
                    }
                    Card(
                        onClick   = { onNoticeClick(notice) },
                        shape     = RoundedCornerShape(14.dp),
                        colors    = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp),
                        modifier  = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                            Box(Modifier.width(4.dp).fillMaxHeight()
                                .background(priorityColor, RoundedCornerShape(
                                    topStart = 14.dp, bottomStart = 14.dp)))
                            Column(Modifier.weight(1f).padding(12.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Surface(color = priorityColor.copy(0.12f),
                                        shape = RoundedCornerShape(6.dp)) {
                                        Text(notice.priority.name,
                                            style      = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color      = priorityColor,
                                            modifier   = Modifier.padding(
                                                horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    if (notice.createdAt > 0L) {
                                        Text(DateFormatters.dashboardItem(notice.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Spacer(Modifier.height(5.dp))
                                Text(notice.title,
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines   = 1,
                                    overflow   = TextOverflow.Ellipsis)
                                Text(notice.content,
                                    style    = MaterialTheme.typography.bodySmall,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis)
                            }
                            Icon(Icons.Filled.ChevronRight, null,
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f),
                                modifier = Modifier.align(Alignment.CenterVertically)
                                    .padding(end = 10.dp).size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Event detail sheet — full event info mirroring AdminEventsScreen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardEventDetailSheet(
    event:     Event,
    onDismiss: () -> Unit,
    onDelete:  () -> Unit,
    onEdit:    () -> Unit
) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Box(Modifier.fillMaxWidth().height(5.dp)
                .background(Brush.horizontalGradient(
                    listOf(AccentPurple, AccentPurple.copy(0.3f)))))
        }
    ) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            if (event.imageUrl != null) {
                Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(event.imageUrl).crossfade(true).build(),
                        contentDescription = event.title,
                        placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                        error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (event.startTimestampMs > 0L) {
                    InfoChip(Icons.Filled.CalendarMonth, DateFormatters.eventDate(event.startTimestampMs), AccentBlue)
                    InfoChip(Icons.Filled.AccessTime, DateFormatters.eventTime(event.startTimestampMs), AccentGreen)
                }
                if (event.maxAttendees > 0) {
                    InfoChip(Icons.Filled.People, "Max ${event.maxAttendees}", AccentOrange)
                }
            }
            Text(event.title, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp)
            if (event.organizer.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Person, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                    Text(event.organizer, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (event.location.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Place, null,
                        tint = AccentPurple, modifier = Modifier.size(15.dp))
                    Text(event.location, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (event.maxAttendees > 0) {
                Surface(color = AccentPurple.copy(0.07f), shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.HowToReg, null,
                                tint = AccentPurple, modifier = Modifier.size(18.dp))
                            Text("${event.rsvpCount} / ${event.maxAttendees} attending",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold, color = AccentPurple)
                        }
                        val fill = (event.rsvpCount.toFloat() / event.maxAttendees).coerceIn(0f, 1f)
                        Box(Modifier.width(80.dp).height(6.dp).clip(CircleShape)
                            .background(AccentPurple.copy(0.2f))) {
                            Box(Modifier.fillMaxWidth(fill).fillMaxHeight().clip(CircleShape)
                                .background(AccentPurple))
                        }
                    }
                }
            }
            if (event.description.isNotBlank()) {
                HorizontalDivider(Modifier.padding(vertical = 2.dp))
                Text(event.description, style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 24.sp)
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit in Events", fontWeight = FontWeight.SemiBold)
                }
                Button(onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor   = MaterialTheme.colorScheme.onErrorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Filled.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, label: String, color: Color) {
    Surface(color = color.copy(0.10f), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
            Text(label, style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

// ── Event dashboard card ──────────────────────────────────────────────────────
@Composable
private fun DashboardEventCard(event: Event, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight()
                .background(AccentPurple, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)))
            Row(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).background(AccentPurple.copy(0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Event, null, tint = AccentPurple, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(event.title, style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (event.startTimestampMs > 0L) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.CalendarMonth, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(11.dp))
                            Text(DateFormatters.eventDate(event.startTimestampMs),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (event.location.isNotBlank()) {
                                Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall)
                                Icon(Icons.Filled.Place, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(11.dp))
                                Text(event.location, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false))
                            }
                        }
                    }
                }
                Icon(Icons.Filled.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f),
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ── Compact notice row ────────────────────────────────────────────────────────
@Composable
private fun CompactNoticeRow(
    notice: Notice, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit
) {
    val priorityColor: Color = when (notice.priority) {
        NoticePriority.URGENT -> Color(0xFFE91E63)
        NoticePriority.HIGH   -> Color(0xFFEF4444)
        NoticePriority.MEDIUM -> Color(0xFFF97316)
        NoticePriority.LOW    -> Color(0xFF10B981)
    }
    Card(onClick = onClick, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight()
                .background(priorityColor, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)))
            Row(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(notice.title, style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(notice.content, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Filled.Edit, "Edit", modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Filled.Delete, "Delete", modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ── Vertical Quick Tile ───────────────────────────────────────────────────────
@Composable
private fun VerticalQuickTile(
    icon: ImageVector, label: String, color: Color,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = modifier.height(80.dp), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.09f)),
        border = BorderStroke(1.dp, color.copy(0.22f)),
        elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(30.dp).background(color.copy(0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                color = color, textAlign = TextAlign.Center,
                maxLines = 2, lineHeight = 17.sp, overflow = TextOverflow.Clip)
        }
    }
}

// ── Compact Stat Card ─────────────────────────────────────────────────────────
@Composable
private fun CompactStatCard(
    icon: ImageVector, label: String, value: String,
    accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = modifier.height(72.dp), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.09f)),
        elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            Column {
                Text(value, fontWeight = FontWeight.ExtraBold, color = accent, fontSize = 22.sp)
                Text(label, style = MaterialTheme.typography.labelMedium, color = accent.copy(0.7f))
            }
        }
    }
}

// ── Section header (simple title) ─────────────────────────────────────────────
@Composable
private fun DashboardSectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
}

// ── Empty hint ────────────────────────────────────────────────────────────────
@Composable
private fun AdminEmptyHint(text: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))
        .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// EventSearchSheet — search events by title, organiser or location.
// Mirrors CourseSearchSheet structure for full consistency.
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventSearchSheet(
    events:    List<Event>,
    onDismiss: () -> Unit,
    onSelect:  (Event) -> Unit
) {
    var query by remember { mutableStateOf("") }

    val filtered: List<Event> = remember(events, query) {
        if (query.isBlank()) events
        else events.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.organizer.contains(query, ignoreCase = true) ||
                    it.location.contains(query, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(AccentPurple.copy(0.06f))
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Box(
                    Modifier
                        .width(36.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AccentPurple.copy(0.3f))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(12.dp))
                Text("Search Events",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color      = AccentPurple)
                Text("Search by title, organiser or location",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value         = query,
                    onValueChange = { query = it },
                    placeholder   = { Text("e.g. Orientation, Sports Day…") },
                    leadingIcon   = {
                        Icon(Icons.Filled.Search, null,
                            tint = AccentPurple, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine    = true,
                    shape         = RoundedCornerShape(14.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = AccentPurple,
                        unfocusedBorderColor = AccentPurple.copy(0.3f)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        }
    ) {
        if (filtered.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.SearchOff, null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.35f))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (query.isBlank()) "No events posted yet"
                        else "No events match \"$query\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filtered.forEach { event ->
                    Card(
                        onClick   = { onSelect(event) },
                        shape     = RoundedCornerShape(14.dp),
                        colors    = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp),
                        modifier  = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Date badge — mirrors CourseResultRow code badge style
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(listOf(
                                            AccentPurple.copy(0.85f), AccentPurple
                                        ))
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (event.startTimestampMs > 0L) {
                                        Text(
                                            DateFormatters.eventDay(event.startTimestampMs),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize   = 16.sp,
                                            color      = Color.White
                                        )
                                        Text(
                                            DateFormatters.eventMonth(event.startTimestampMs).uppercase(),
                                            style         = MaterialTheme.typography.labelSmall,
                                            color         = Color.White.copy(0.8f),
                                            letterSpacing = 0.5.sp
                                        )
                                    } else {
                                        Icon(Icons.Filled.Event, null,
                                            tint     = Color.White,
                                            modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(event.title,
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines   = 1,
                                    overflow   = TextOverflow.Ellipsis)
                                if (event.organizer.isNotBlank()) {
                                    Text(event.organizer,
                                        style    = MaterialTheme.typography.bodySmall,
                                        color    = AccentPurple.copy(0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis)
                                }
                                if (event.location.isNotBlank()) {
                                    Row(
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(Icons.Filled.Place, null,
                                            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                                            modifier = Modifier.size(11.dp))
                                        Text(event.location,
                                            style    = MaterialTheme.typography.labelSmall,
                                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                            Icon(Icons.Filled.ChevronRight, null,
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f),
                                modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── Confirm delete ────────────────────────────────────────────────────────────
@Composable
private fun ConfirmDeleteDialog(
    title: String, body: String, onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss,
        icon    = { Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
        title   = { Text(title, fontWeight = FontWeight.Bold) },
        text    = { Text(body, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
}