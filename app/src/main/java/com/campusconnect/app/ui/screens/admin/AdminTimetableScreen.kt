package com.campusconnect.app.ui.screens.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campusconnect.app.domain.model.TimetableEntry
import com.campusconnect.app.viewmodel.AdminDashboardViewModel

private val AdminBlue       = Color(0xFF3B5BDB)
private val TimetablePurple = Color(0xFF7C3AED)
private val NavyStart       = Color(0xFF060D2E)
private val NavyEnd         = Color(0xFF1A2B6B)

private val DAYS_SHORT  = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val DAYS_FULL   = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
private val DAY_COLORS  = listOf(
    Color(0xFF1E3A8A), Color(0xFF059669), Color(0xFF7C3AED),
    Color(0xFFD97706), Color(0xFFDC2626),
    Color(0xFF0891B2), Color(0xFFDB2777)   // Sat = Cyan, Sun = Pink
)

// Only grid view is used

private fun Color.darken(f: Float = 0.28f): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    hsl[2] = (hsl[2] * (1f - f)).coerceIn(0f, 1f)
    return Color(ColorUtils.HSLToColor(hsl))
}

@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTimetableScreen(
    onPostEntry: () -> Unit,
    vm: AdminDashboardViewModel = hiltViewModel()
) {
    val timetable by vm.recentTimetable.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()

    var selectedDay by remember { mutableIntStateOf(0) }

    // Entries grouped by course (used by search + course sheet)
    val timetableByCourse = remember(timetable) {
        timetable.groupBy { it.courseCode.uppercase().trim() }
    }

    // Sheet / dialog state
    var selectedCourseSheet by remember { mutableStateOf<String?>(null) }
    var entryToDelete       by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog    by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var editingEntry        by remember { mutableStateOf<TimetableEntry?>(null) }

    // Delete single
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Entry") },
            text  = { Text("This timetable entry will be permanently removed. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    entryToDelete?.let { vm.deleteTimetableEntry(it) }
                    showDeleteDialog = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Delete all in tab
    if (showDeleteAllDialog) {
        val tab   = selectedCourseSheet ?: ""
        val count = timetableByCourse[tab]?.size ?: 0
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete All Entries") },
            text  = { Text("All $count entry/entries for \"$tab\" will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    timetableByCourse[tab]?.let { vm.deleteAllTimetableEntries(it) }
                    showDeleteAllDialog  = false
                    selectedCourseSheet = null
                }) { Text("Delete All", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Edit sheet
    editingEntry?.let { entry ->
        EditTimetableSheet(
            entry    = entry,
            onDismiss = { editingEntry = null },
            onSave    = { updated ->
                vm.updateTimetableEntry(updated)
                editingEntry = null
            }
        )
    }

    // Course timetable bottom sheet (cards view)
    selectedCourseSheet?.let { courseCode ->
        val entriesInTab = timetableByCourse[courseCode] ?: emptyList()
        val courseName   = entriesInTab.firstOrNull()?.courseName ?: ""
        AdminCourseTimetableSheet(
            courseCode  = courseCode,
            courseName  = courseName,
            entries     = entriesInTab,
            onDismiss   = { selectedCourseSheet = null },
            onDelete    = { entryToDelete = it; showDeleteDialog = true },
            onEdit      = { editingEntry = it },
            onDeleteAll = { showDeleteAllDialog = true }
        )
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(NavyStart, NavyEnd)))
                    .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Timetable",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White,
                        letterSpacing = (-0.3).sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AnimatedVisibility(visible = isLoading, enter = fadeIn(), exit = fadeOut()) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White.copy(0.7f))
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick        = onPostEntry,
                icon           = { Icon(Icons.Filled.TableChart, null) },
                text           = { Text("Add Entry") },
                containerColor = AdminBlue,
                contentColor   = Color.White
            )
        }
    ) { paddingValues ->
        if (timetable.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape).background(TimetablePurple.copy(0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.CalendarMonth, null, tint = TimetablePurple.copy(0.5f), modifier = Modifier.size(36.dp))
                    }
                    Text("No timetable entries yet", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Tap the button below to add your first entry", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f))
                }
            }
        } else {
            TimetableGridView(
                timetable          = timetable,
                timetableByCourse  = timetableByCourse,
                selectedDay        = selectedDay,
                onDaySelected      = { selectedDay = it },
                onEntryEdit        = { editingEntry = it },
                onEntryDelete      = { entryToDelete = it; showDeleteDialog = true },
                onCourseClick      = { selectedCourseSheet = it },
                modifier           = Modifier.padding(paddingValues)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GRID VIEW — spreadsheet-style Mon–Fri columns
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TimetableGridView(
    timetable:         List<TimetableEntry>,
    timetableByCourse: Map<String, List<TimetableEntry>>,
    selectedDay:       Int,
    onDaySelected:     (Int) -> Unit,
    onEntryEdit:       (TimetableEntry) -> Unit,
    onEntryDelete:     (String) -> Unit,
    onCourseClick:     (String) -> Unit,
    modifier:          Modifier = Modifier
) {
    // Always compute day entries (cheap with remember)
    val dayEntries = remember(timetable, selectedDay) {
        timetable.filter { it.dayOfWeek == selectedDay }
            .sortedWith(compareBy({ it.startHour }, { it.startMinute }))
    }

    // Search state
    var courseQuery by remember { mutableStateOf("") }
    val filteredCourses = remember(courseQuery, timetableByCourse) {
        val q = courseQuery.trim()
        if (q.isBlank()) emptyList()
        else timetableByCourse.entries
            .filter { (code, entries) ->
                code.contains(q, ignoreCase = true) ||
                        entries.firstOrNull()?.courseName?.contains(q, ignoreCase = true) == true
            }
            .map { it.key }
            .sorted()
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Day strip
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            shape    = RoundedCornerShape(20.dp),
            shadowElevation = 4.dp,
            color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DAYS_SHORT.forEachIndexed { index, day ->
                    val isSelected = index == selectedDay
                    val count      = timetable.count { it.dayOfWeek == index }
                    val dayColor   = DAY_COLORS[index]
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected)
                                    Brush.verticalGradient(listOf(dayColor, dayColor.darken(0.22f)))
                                else
                                    Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface))
                            )
                            .clickable { onDaySelected(index) }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            day,
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color      = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(0.75f),
                            fontSize   = 12.sp
                        )
                        Spacer(Modifier.height(5.dp))
                        if (count > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Color.White.copy(0.22f) else dayColor.copy(0.12f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    "${count}c",
                                    style      = MaterialTheme.typography.labelSmall,
                                    fontSize   = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = if (isSelected) Color.White else dayColor
                                )
                            }
                        } else {
                            Spacer(Modifier.height(14.dp))
                        }
                    }
                }
            }
        }

        // ── Course search bar (below day strip) ───────────────────────────
        OutlinedTextField(
            value         = courseQuery,
            onValueChange = { courseQuery = it },
            placeholder   = { Text("Search course by code or name…") },
            leadingIcon   = { Icon(Icons.Filled.Search, null, tint = AdminBlue) },
            trailingIcon  = {
                if (courseQuery.isNotBlank()) {
                    IconButton(onClick = { courseQuery = "" }) {
                        Icon(Icons.Filled.Close, "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            modifier   = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 4.dp),
            shape      = RoundedCornerShape(14.dp),
            singleLine = true,
            colors     = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = AdminBlue,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.4f)
            )
        )

        // ── Content: search results OR day entries ────────────────────────
        if (courseQuery.isNotBlank()) {
            if (filteredCourses.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Filled.Search,
                            null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.25f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "No courses match \"${courseQuery.trim()}\"",
                            style      = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Try a different course code or name",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier            = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            "${filteredCourses.size} course${if (filteredCourses.size != 1) "s" else ""} found · tap to view timetable",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(filteredCourses) { courseCode ->
                        val count      = timetableByCourse[courseCode]?.size ?: 0
                        val courseName = timetableByCourse[courseCode]?.firstOrNull()?.courseName ?: ""
                        TimetableCourseTabCard(
                            courseCode = courseCode,
                            courseName = courseName,
                            itemCount  = count,
                            onClick    = { onCourseClick(courseCode) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        } else {
            // Original day-filtered content
            AnimatedContent(
                targetState  = selectedDay,
                transitionSpec = {
                    if (targetState > initialState)
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    else
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                },
                label = "day_grid"
            ) { currentDay ->
                if (dayEntries.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.CalendarMonth, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f), modifier = Modifier.size(48.dp))
                            Text(
                                "No classes on ${DAYS_FULL.getOrNull(currentDay) ?: "this day"}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Text(
                                "${dayEntries.size} class${if (dayEntries.size != 1) "es" else ""} · ${DAYS_FULL.getOrNull(currentDay)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(dayEntries, key = { it.id }) { entry ->
                            AdminGridEntryCard(
                                entry    = entry,
                                onEdit   = { onEntryEdit(entry) },
                                onDelete = { onEntryDelete(entry.id) }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminGridEntryCard(
    entry:    TimetableEntry,
    onEdit:   () -> Unit,
    onDelete: () -> Unit
) {
    val cardColor = runCatching {
        Color(android.graphics.Color.parseColor(entry.colorHex))
    }.getOrDefault(TimetablePurple)

    val onCardColor = if (ColorUtils.calculateLuminance(cardColor.toArgb()) > 0.4f) Color.Black else Color.White

    Card(
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier  = Modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp), ambientColor = cardColor.copy(0.3f), spotColor = cardColor.copy(0.4f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(cardColor, cardColor.darken(0.28f))))
        ) {
            Row(
                modifier          = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time block
                Column(
                    modifier            = Modifier.width(52.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "${entry.startHour}:${entry.startMinute.toString().padStart(2, '0')}",
                        fontWeight = FontWeight.ExtraBold,
                        color      = onCardColor,
                        fontSize   = 14.sp
                    )
                    Box(
                        modifier = Modifier.width(1.5.dp).height(12.dp)
                            .background(Brush.verticalGradient(listOf(onCardColor.copy(0.5f), onCardColor.copy(0.1f))))
                    )
                    Text(
                        "${entry.endHour}:${entry.endMinute.toString().padStart(2, '0')}",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = onCardColor.copy(0.7f),
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier.width(1.dp).height(52.dp)
                        .background(Brush.verticalGradient(listOf(onCardColor.copy(0f), onCardColor.copy(0.25f), onCardColor.copy(0f))))
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.courseCode,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = onCardColor,
                        fontSize      = 15.sp,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        entry.courseName,
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = onCardColor.copy(0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (entry.room.isNotBlank()) {
                            AdminInfoChip(Icons.Filled.Place, entry.room, onCardColor)
                        }
                        if (entry.lecturer.isNotBlank()) {
                            AdminInfoChip(Icons.Filled.Person, entry.lecturer, onCardColor, maxChars = 14)
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Edit, "Edit", tint = onCardColor.copy(0.8f), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, "Delete", tint = onCardColor.copy(0.6f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminInfoChip(
    icon:     androidx.compose.ui.graphics.vector.ImageVector,
    label:    String,
    tint:     Color,
    maxChars: Int = Int.MAX_VALUE
) {
    val display = if (label.length > maxChars) label.take(maxChars) + "…" else label
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(tint.copy(0.15f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, null, tint = tint.copy(0.75f), modifier = Modifier.size(10.dp))
        Text(display, style = MaterialTheme.typography.labelSmall, color = tint.copy(0.88f), fontSize = 10.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CARDS VIEW — course tab cards
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TimetableCardsView(
    courseTabs:        List<String>,
    timetableByCourse: Map<String, List<TimetableEntry>>,
    onCourseClick:     (String) -> Unit
) {
    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            val total = timetableByCourse.values.sumOf { it.size }
            Text(
                "$total entries · ${courseTabs.size} course${if (courseTabs.size != 1) "s" else ""}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(courseTabs) { courseCode ->
            val count      = timetableByCourse[courseCode]?.size ?: 0
            val courseName = timetableByCourse[courseCode]?.firstOrNull()?.courseName ?: ""
            TimetableCourseTabCard(
                courseCode = courseCode,
                courseName = courseName,
                itemCount  = count,
                onClick    = { onCourseClick(courseCode) }
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun TimetableCourseTabCard(
    courseCode: String,
    courseName: String,
    itemCount:  Int,
    onClick:    () -> Unit
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
                    .background(Brush.verticalGradient(listOf(TimetablePurple, TimetablePurple.copy(0.4f))),
                        RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
            )
            Box(
                modifier = Modifier.padding(start = 14.dp, end = 10.dp).size(48.dp)
                    .clip(RoundedCornerShape(12.dp)).background(TimetablePurple.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    courseCode.take(4),
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color      = TimetablePurple,
                    fontSize   = if (courseCode.length <= 4) 13.sp else 10.sp
                )
            }
            Column(
                modifier = Modifier.weight(1f).padding(vertical = 14.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(courseCode, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = TimetablePurple)
                if (courseName.isNotBlank()) {
                    Text(courseName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(4.dp))
                Surface(color = TimetablePurple.copy(0.1f), shape = RoundedCornerShape(20.dp)) {
                    Text(
                        "$itemCount slot${if (itemCount != 1) "s" else ""}",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = TimetablePurple,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Icon(Icons.Filled.ChevronRight, null, tint = TimetablePurple.copy(0.6f), modifier = Modifier.padding(end = 14.dp).size(20.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COURSE SHEET (cards view detail)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminCourseTimetableSheet(
    courseCode:  String,
    courseName:  String,
    entries:     List<TimetableEntry>,
    onDismiss:   () -> Unit,
    onDelete:    (String) -> Unit,
    onEdit:      (TimetableEntry) -> Unit,
    onDeleteAll: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        dragHandle = {
            Column(
                Modifier.fillMaxWidth().background(TimetablePurple.copy(0.08f)).padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(TimetablePurple.copy(0.3f)).align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(courseCode, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = TimetablePurple)
                        if (courseName.isNotBlank())
                            Text(courseName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (entries.isNotEmpty()) {
                        TextButton(onClick = onDeleteAll, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                            Icon(Icons.Filled.DeleteSweep, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Delete All (${entries.size})", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    ) {
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No entries for this course.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier            = Modifier.fillMaxWidth()
            ) {
                items(entries, key = { it.id }) { entry ->
                    SheetTimetableEntryCard(
                        entry    = entry,
                        onDelete = { onDelete(entry.id) },
                        onEdit   = { onEdit(entry) }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun SheetTimetableEntryCard(
    entry:    TimetableEntry,
    onDelete: () -> Unit,
    onEdit:   () -> Unit
) {
    val cardColor = runCatching {
        Color(android.graphics.Color.parseColor(entry.colorHex))
    }.getOrDefault(TimetablePurple)

    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.width(5.dp).height(76.dp).background(cardColor, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)))
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (entry.dayOfWeek in 0..6) {
                            Surface(color = cardColor.copy(0.12f), shape = RoundedCornerShape(20.dp)) {
                                Text(DAYS_SHORT[entry.dayOfWeek], style = MaterialTheme.typography.labelSmall, color = cardColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(20.dp)) {
                            Text(
                                "${entry.startHour}:${entry.startMinute.toString().padStart(2,'0')} – ${entry.endHour}:${entry.endMinute.toString().padStart(2,'0')}",
                                style    = MaterialTheme.typography.labelSmall,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(entry.courseName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (entry.lecturer.isNotBlank() || entry.room.isNotBlank()) {
                        Text(
                            buildString {
                                if (entry.lecturer.isNotBlank()) append(entry.lecturer)
                                if (entry.lecturer.isNotBlank() && entry.room.isNotBlank()) append("  ·  ")
                                if (entry.room.isNotBlank()) append(entry.room)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Edit, "Edit", tint = AdminBlue, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EDIT TIMETABLE SHEET
// ─────────────────────────────────────────────────────────────────────────────

private val DAYS_EDIT = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
private val COLOR_OPTIONS_EDIT = listOf(
    "#1E3A8A" to "Navy", "#059669" to "Green", "#7C3AED" to "Purple",
    "#D97706" to "Amber", "#DC2626" to "Red",  "#0891B2" to "Cyan",
    "#DB2777" to "Pink",  "#374151" to "Slate"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTimetableSheet(
    entry:    TimetableEntry,
    onDismiss: () -> Unit,
    onSave:    (TimetableEntry) -> Unit
) {
    var lecturer     by remember { mutableStateOf(entry.lecturer) }
    var room         by remember { mutableStateOf(entry.room) }
    var building     by remember { mutableStateOf(entry.building) }
    var notes        by remember { mutableStateOf(entry.notes) }
    var selectedDay  by remember { mutableIntStateOf(entry.dayOfWeek) }
    var selectedColor by remember { mutableStateOf(entry.colorHex) }
    var startHour    by remember { mutableIntStateOf(entry.startHour) }
    var startMinute  by remember { mutableIntStateOf(entry.startMinute) }
    var endHour      by remember { mutableIntStateOf(entry.endHour) }
    var endMinute    by remember { mutableIntStateOf(entry.endMinute) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker   by remember { mutableStateOf(false) }

    val startState = rememberTimePickerState(initialHour = startHour, initialMinute = startMinute, is24Hour = true)
    val endState   = rememberTimePickerState(initialHour = endHour,   initialMinute = endMinute,   is24Hour = true)

    if (showStartPicker) {
        AlertDialog(
            onDismissRequest = { showStartPicker = false },
            title = { Text("Start Time") },
            text  = { TimePicker(state = startState) },
            confirmButton = { TextButton(onClick = { startHour = startState.hour; startMinute = startState.minute; showStartPicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("Cancel") } }
        )
    }
    if (showEndPicker) {
        AlertDialog(
            onDismissRequest = { showEndPicker = false },
            title = { Text("End Time") },
            text  = { TimePicker(state = endState) },
            confirmButton = { TextButton(onClick = { endHour = endState.hour; endMinute = endState.minute; showEndPicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("Cancel") } }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Column(Modifier.fillMaxWidth().background(AdminBlue.copy(0.06f)).padding(horizontal = 20.dp, vertical = 14.dp)) {
                Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(AdminBlue.copy(0.3f)).align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Edit Entry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = AdminBlue)
                        Text("${entry.courseCode} — ${entry.courseName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = {
                        onSave(entry.copy(
                            lecturer  = lecturer.trim(), room = room.trim(), building = building.trim(),
                            notes     = notes.trim(), dayOfWeek = selectedDay, colorHex = selectedColor,
                            startHour = startHour, startMinute = startMinute, endHour = endHour, endMinute = endMinute
                        ))
                    }) {
                        Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Day selector
            Text("DAY", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DAYS_EDIT.forEachIndexed { i, day ->
                    val sel = i == selectedDay
                    Surface(
                        onClick = { selectedDay = i },
                        shape  = RoundedCornerShape(12.dp),
                        color  = if (sel) AdminBlue else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            day.take(3), style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (sel) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Time pickers
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.Schedule, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${startHour}:${startMinute.toString().padStart(2,'0')}")
                }
                OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.Schedule, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${endHour}:${endMinute.toString().padStart(2,'0')}")
                }
            }

            OutlinedTextField(value = lecturer, onValueChange = { lecturer = it }, label = { Text("Lecturer") }, leadingIcon = { Icon(Icons.Filled.Person, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true)
            OutlinedTextField(value = room,     onValueChange = { room = it },     label = { Text("Room") },     leadingIcon = { Icon(Icons.Filled.Place, null) },  modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true)
            OutlinedTextField(value = building, onValueChange = { building = it }, label = { Text("Building") }, leadingIcon = { Icon(Icons.Filled.Business, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true)
            OutlinedTextField(value = notes,    onValueChange = { notes = it },    label = { Text("Notes") },    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp), shape = RoundedCornerShape(14.dp), maxLines = 4)

            // Color picker
            Text("COLOUR", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                COLOR_OPTIONS_EDIT.forEach { (hex, name) ->
                    val selected = hex == selectedColor
                    val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(AdminBlue)
                    Box(
                        modifier = Modifier
                            .size(36.dp).clip(CircleShape)
                            .background(color)
                            .then(if (selected) Modifier.border(3.dp, Color.White, CircleShape) else Modifier)
                            .clickable { selectedColor = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) Icon(Icons.Filled.Check, name, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// Wire updateTimetableEntry into AdminDashboardViewModel via extension-like call
// (the actual function is in AdminViewModel — AdminDashboardViewModel exposes it through delegation)