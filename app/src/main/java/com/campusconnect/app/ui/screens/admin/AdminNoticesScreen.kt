package com.campusconnect.app.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.campusconnect.app.domain.model.CourseEntry
import com.campusconnect.app.domain.model.Notice
import com.campusconnect.app.domain.model.NoticePriority
import com.campusconnect.app.utils.DateFormatters
import com.campusconnect.app.viewmodel.AdminDashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNoticesScreen(
    onPostNotice: () -> Unit,
    vm: AdminDashboardViewModel = hiltViewModel()
) {
    val notices          by vm.recentNotices.collectAsStateWithLifecycle()
    val isLoading        by vm.isLoading.collectAsStateWithLifecycle()
    val availableCourses by vm.availableCourses.collectAsStateWithLifecycle()

    // Course grouping
    val noticesByCourse = remember(notices) {
        notices.groupBy { it.targetCourse?.uppercase()?.trim() ?: "" }
    }
    val courseTabs = remember(noticesByCourse) { noticesByCourse.keys.sorted() }
    var selectedCourseSheet by remember { mutableStateOf<String?>(null) }

    // Search state — filters the displayed course tabs
    var searchQuery by remember { mutableStateOf("") }
    val filteredCourseTabs = remember(courseTabs, searchQuery, availableCourses) {
        if (searchQuery.isBlank()) courseTabs
        else {
            val q = searchQuery.trim()
            courseTabs.filter { courseCode ->
                if (courseCode.isBlank()) {
                    // "All Students" / "General" group — match common keywords
                    listOf("all", "general", "gen", "student").any {
                        q.contains(it, ignoreCase = true) || it.contains(q, ignoreCase = true)
                    }
                } else {
                    val name = availableCourses.firstOrNull { it.code == courseCode }?.name ?: ""
                    courseCode.contains(q, ignoreCase = true) || name.contains(q, ignoreCase = true)
                }
            }
        }
    }

    // Per-item state
    var selectedNotice   by remember { mutableStateOf<Notice?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var noticeToDelete   by remember { mutableStateOf<String?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var editingNotice    by remember { mutableStateOf<Notice?>(null) }

    // Delete single notice dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Notice") },
            text  = { Text("This notice will be permanently removed. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    noticeToDelete?.let { vm.deleteNotice(it) }
                    showDeleteDialog = false
                    selectedNotice   = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Delete all notices in tab dialog
    if (showDeleteAllDialog) {
        val tab   = selectedCourseSheet ?: ""
        val count = noticesByCourse[tab]?.size ?: 0
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete All Notices") },
            text  = { Text("All $count notice(s) for \"${tab.ifBlank { "General" }}\" will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    noticesByCourse[tab]?.let { vm.deleteAllNotices(it) }
                    showDeleteAllDialog   = false
                    selectedCourseSheet   = null
                }) { Text("Delete All", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Course notices bottom sheet
    selectedCourseSheet?.let { courseCode ->
        val noticesInTab = noticesByCourse[courseCode] ?: emptyList()
        val courseName   = availableCourses.firstOrNull { it.code == courseCode }?.name ?: ""
        AdminCourseNoticesSheet(
            courseCode    = courseCode,
            courseName    = courseName,
            notices       = noticesInTab,
            onDismiss     = { selectedCourseSheet = null },
            onNoticeClick = { selectedNotice = it },
            onDelete      = { noticeToDelete = it; showDeleteDialog = true },
            onEdit        = { editingNotice = it },
            onDeleteAll   = { showDeleteAllDialog = true }
        )
    }

    // Edit notice sheet
    editingNotice?.let { notice ->
        EditNoticeSheet(
            notice    = notice,
            onDismiss = { editingNotice = null },
            onSave    = { id, title, content ->
                vm.updateNotice(id, title, content)
                editingNotice = null
            }
        )
    }

    // Notice detail sheet
    selectedNotice?.let { notice ->
        NoticeDetailSheet(
            notice    = notice,
            onDismiss = { selectedNotice = null },
            onDelete  = { noticeToDelete = notice.id; showDeleteDialog = true }
        )
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF060D2E), Color(0xFF1A2B6B)))
                    )
                    .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Notices",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White,
                        letterSpacing = (-0.3).sp
                    )
                    AnimatedVisibility(visible = isLoading, enter = fadeIn(), exit = fadeOut()) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color       = Color.White.copy(0.7f)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick        = onPostNotice,
                icon           = { Icon(Icons.Filled.PostAdd, null) },
                text           = { Text("Post Notice") },
                containerColor = Color(0xFF3B5BDB),
                contentColor   = Color.White
            )
        }
    ) { paddingValues ->
        if (notices.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape)
                            .background(Color(0xFF3B5BDB).copy(0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Campaign, null,
                            tint = Color(0xFF3B5BDB).copy(0.5f), modifier = Modifier.size(36.dp))
                    }
                    Text("No notices posted yet",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Tap the button below to post your first notice",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── Search bar ────────────────────────────────────────────────
                item {
                    OutlinedTextField(
                        value         = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder   = { Text("Search by course code, name or 'all students'…") },
                        leadingIcon   = {
                            Icon(Icons.Filled.Search, null, tint = AdminBlue)
                        },
                        trailingIcon  = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, "Clear search")
                                }
                            }
                        },
                        singleLine    = true,
                        shape         = RoundedCornerShape(14.dp),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = AdminBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.4f)
                        ),
                        modifier      = Modifier.fillMaxWidth()
                    )
                }

                item {
                    val displayCount = filteredCourseTabs.size
                    val totalCount   = notices.size
                    Text(
                        if (searchQuery.isBlank())
                            "$totalCount notice${if (totalCount != 1) "s" else ""} · ${courseTabs.size} group${if (courseTabs.size != 1) "s" else ""}"
                        else
                            "$displayCount group${if (displayCount != 1) "s" else ""} found",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (filteredCourseTabs.isEmpty() && searchQuery.isNotBlank()) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Search, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.25f),
                                    modifier = Modifier.size(48.dp))
                                Text(
                                    "No groups match \"${searchQuery.trim()}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Try a course code, name, or type 'all students'",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                                )
                            }
                        }
                    }
                } else {
                    items(filteredCourseTabs) { courseCode ->
                        val count      = noticesByCourse[courseCode]?.size ?: 0
                        val courseName = if (courseCode.isBlank()) "All Students"
                        else availableCourses.firstOrNull { it.code == courseCode }?.name ?: ""
                        AdminCourseTabCard(
                            courseCode  = courseCode.ifBlank { "GEN" },
                            courseName  = courseName.ifBlank { "General Notices" },
                            itemCount   = count,
                            accentColor = Color(0xFF3B5BDB),
                            onClick     = { selectedCourseSheet = courseCode }
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ── Shared accent blue ────────────────────────────────────────────────────────
private val AdminBlue = Color(0xFF3B5BDB)

// ── Course tab card ───────────────────────────────────────────────────────────
@Composable
private fun AdminCourseTabCard(
    courseCode:  String,
    courseName:  String,
    itemCount:   Int,
    accentColor: Color,
    onClick:     () -> Unit
) {
    Card(
        onClick   = onClick,
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.width(6.dp).height(72.dp)
                    .background(
                        Brush.verticalGradient(listOf(accentColor, accentColor.copy(0.4f))),
                        RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                    )
            )
            Box(
                modifier = Modifier.padding(start = 14.dp, end = 10.dp)
                    .size(48.dp).clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    courseCode.take(4),
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color      = accentColor,
                    fontSize   = if (courseCode.length <= 4) 13.sp else 10.sp
                )
            }
            Column(
                modifier = Modifier.weight(1f).padding(vertical = 14.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(courseCode, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold, color = accentColor)
                if (courseName.isNotBlank()) {
                    Text(courseName, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(4.dp))
                Surface(color = accentColor.copy(0.1f), shape = RoundedCornerShape(20.dp)) {
                    Text(
                        "$itemCount notice${if (itemCount != 1) "s" else ""}",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Icon(Icons.Filled.ChevronRight, null,
                tint = accentColor.copy(0.6f),
                modifier = Modifier.padding(end = 14.dp).size(20.dp))
        }
    }
}

// ── Course notices bottom sheet ───────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminCourseNoticesSheet(
    courseCode:    String,
    courseName:    String,
    notices:       List<Notice>,
    onDismiss:     () -> Unit,
    onNoticeClick: (Notice) -> Unit,
    onDelete:      (String) -> Unit,
    onEdit:        (Notice) -> Unit,
    onDeleteAll:   () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        dragHandle = {
            Column(
                Modifier.fillMaxWidth()
                    .background(AdminBlue.copy(0.08f))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Box(
                    Modifier.width(40.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AdminBlue.copy(0.3f))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(courseCode.ifBlank { "GEN" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold, color = AdminBlue)
                        if (courseName.isNotBlank())
                            Text(courseName, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (notices.isNotEmpty()) {
                        TextButton(onClick = onDeleteAll,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                            Icon(Icons.Filled.DeleteSweep, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Delete All (${notices.size})", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    ) {
        if (notices.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No notices in this group.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(notices, key = { it.id }) { notice ->
                    AdminNoticeListCard(
                        notice   = notice,
                        onClick  = { onNoticeClick(notice) },
                        onDelete = { onDelete(notice.id) },
                        onEdit   = { onEdit(notice) }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

// ── Notice list card ──────────────────────────────────────────────────────────
@Composable
private fun AdminNoticeListCard(
    notice:   Notice,
    onClick:  () -> Unit,
    onDelete: () -> Unit,
    onEdit:   () -> Unit
) {
    val priorityColor = when (notice.priority) {
        NoticePriority.URGENT -> Color(0xFFE91E63)
        NoticePriority.HIGH   -> Color(0xFFEF4444)
        NoticePriority.MEDIUM -> Color(0xFFF97316)
        NoticePriority.LOW    -> Color(0xFF10B981)
    }
    val priorityLabel = when (notice.priority) {
        NoticePriority.URGENT -> "URGENT"
        NoticePriority.HIGH   -> "HIGH"
        NoticePriority.MEDIUM -> "MEDIUM"
        NoticePriority.LOW    -> "LOW"
    }

    Card(
        onClick   = onClick,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            // Priority strip
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        priorityColor,
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // Top row: chips + delete
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        // Category chip
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                "${notice.category.emoji} ${notice.category.displayName}",
                                style    = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                color    = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        // Priority chip
                        Surface(
                            color = priorityColor.copy(0.12f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier          = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(5.dp)
                                        .background(priorityColor, CircleShape)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    priorityLabel,
                                    style      = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color      = priorityColor
                                )
                            }
                        }
                        // Course target chip
                        if (!notice.targetCourse.isNullOrBlank()) {
                            Surface(
                                color = Color(0xFF3B5BDB).copy(0.12f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    "→ ${notice.targetCourse}",
                                    style    = MaterialTheme.typography.labelSmall,
                                    color    = Color(0xFF3B5BDB),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    // Edit + Delete buttons
                    Row {
                        IconButton(
                            onClick  = onEdit,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.Edit, "Edit",
                                tint     = Color(0xFF3B5BDB),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick  = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.Delete, "Delete",
                                tint     = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Title
                Text(
                    notice.title,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    color      = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(3.dp))

                // Content preview
                Text(
                    notice.content,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Timestamp
                if (notice.createdAt > 0L) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.AccessTime, null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            DateFormatters.noticeDetail(notice.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                        )
                    }
                }
            }
        }
    }
}

// ── Notice detail bottom sheet (reused from AdminDashboardScreen) ─────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoticeDetailSheet(
    notice:    Notice,
    onDismiss: () -> Unit,
    onDelete:  () -> Unit
) {
    val sheetState    = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val priorityColor = when (notice.priority) {
        NoticePriority.URGENT -> Color(0xFFE91E63)
        NoticePriority.HIGH   -> Color(0xFFEF4444)
        NoticePriority.MEDIUM -> Color(0xFFF97316)
        NoticePriority.LOW    -> Color(0xFF10B981)
    }
    val priorityLabel = when (notice.priority) {
        NoticePriority.URGENT -> "URGENT"
        NoticePriority.HIGH   -> "HIGH"
        NoticePriority.MEDIUM -> "MEDIUM"
        NoticePriority.LOW    -> "LOW"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        dragHandle       = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        Brush.horizontalGradient(listOf(priorityColor, priorityColor.copy(0.3f)))
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Chips row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "${notice.category.emoji} ${notice.category.displayName}",
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Surface(
                    color = priorityColor.copy(0.12f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier          = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(6.dp).background(priorityColor, CircleShape))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            priorityLabel,
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color      = priorityColor
                        )
                    }
                }
                if (!notice.targetCourse.isNullOrBlank()) {
                    Surface(
                        color = Color(0xFF3B5BDB).copy(0.12f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            "→ ${notice.targetCourse}",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = Color(0xFF3B5BDB),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Title
            Text(
                notice.title,
                style         = MaterialTheme.typography.headlineSmall,
                fontWeight    = FontWeight.ExtraBold,
                color         = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.3).sp
            )

            // Author + date
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Person, null,
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    notice.author.ifBlank { notice.authorEmail },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (notice.createdAt > 0L) {
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        DateFormatters.noticeDetail(notice.createdAt),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 2.dp))

            // Body
            Text(
                notice.content,
                style      = MaterialTheme.typography.bodyMedium,
                color      = MaterialTheme.colorScheme.onSurface,
                lineHeight = 24.sp
            )

            // Image
            notice.imageUrl?.let { url ->
                Card(
                    shape     = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    AsyncImage(
                        model              = url,
                        contentDescription = "Notice image",
                        placeholder        = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                        error              = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                        modifier           = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        contentScale       = ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Delete
            Button(
                onClick  = onDelete,
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor   = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Delete Notice", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Edit Notice Sheet ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditNoticeSheet(
    notice:    Notice,
    onDismiss: () -> Unit,
    onSave:    (id: String, title: String, content: String) -> Unit
) {
    var title   by remember { mutableStateOf(notice.title) }
    var content by remember { mutableStateOf(notice.content) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Column(
                Modifier.fillMaxWidth()
                    .background(Color(0xFF3B5BDB).copy(0.06f))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Box(
                    Modifier.width(40.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF3B5BDB).copy(0.3f))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "Edit Notice",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color(0xFF3B5BDB)
                    )
                    TextButton(
                        onClick  = { onSave(notice.id, title, content) },
                        enabled  = title.isNotBlank() && content.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                label         = { Text("Notice Title *") },
                leadingIcon   = { Icon(Icons.Filled.Title, null) },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(14.dp),
                singleLine    = true
            )
            OutlinedTextField(
                value         = content,
                onValueChange = { content = it },
                label         = { Text("Notice Content *") },
                leadingIcon   = { Icon(Icons.Filled.Notes, null) },
                modifier      = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                shape         = RoundedCornerShape(14.dp),
                maxLines      = 12
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}