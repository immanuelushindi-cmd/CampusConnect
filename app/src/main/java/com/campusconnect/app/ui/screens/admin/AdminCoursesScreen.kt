package com.campusconnect.app.ui.screens.admin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campusconnect.app.domain.model.CourseEntry
import com.campusconnect.app.domain.model.LecturerEntry
import com.campusconnect.app.utils.UiState
import com.campusconnect.app.viewmodel.CourseManagementViewModel

// ── Dialog state machines ─────────────────────────────────────────────────────

private sealed interface CourseDialog {
    data object None                            : CourseDialog
    data object Add                             : CourseDialog
    data class  Edit(val course: CourseEntry)   : CourseDialog
    data class  Delete(val course: CourseEntry) : CourseDialog
}

private sealed interface LecturerDialog {
    data object None                                : LecturerDialog
    data object Add                                 : LecturerDialog
    data class  Edit(val lecturer: LecturerEntry)   : LecturerDialog
    data class  Delete(val lecturer: LecturerEntry) : LecturerDialog
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCoursesScreen(
    onBack: () -> Unit,
    vm: CourseManagementViewModel = hiltViewModel()
) {
    val courses   by vm.courses.collectAsStateWithLifecycle()
    val lecturers by vm.lecturers.collectAsStateWithLifecycle()
    val opState   by vm.opState.collectAsStateWithLifecycle()
    val isLoading = opState is UiState.Loading

    val snackbarHost  = remember { SnackbarHostState() }
    var selectedTab   by remember { mutableIntStateOf(0) }        // 0 = Courses, 1 = Lecturers
    var courseDialog  by remember { mutableStateOf<CourseDialog>(CourseDialog.None) }
    var lecturerDialog by remember { mutableStateOf<LecturerDialog>(LecturerDialog.None) }
    var searchQuery   by remember { mutableStateOf("") }

    LaunchedEffect(opState) {
        when (val s = opState) {
            is UiState.Success -> {
                snackbarHost.showSnackbar(s.data ?: "Saved", duration = SnackbarDuration.Short)
                vm.resetOpState()
            }
            is UiState.Error -> {
                snackbarHost.showSnackbar(s.message, duration = SnackbarDuration.Short)
                vm.resetOpState()
            }
            else -> Unit
        }
    }

    // ── Course dialogs ────────────────────────────────────────────────────────
    when (val d = courseDialog) {
        CourseDialog.Add -> CourseEntryDialog(
            existing  = null,
            allCodes  = courses.map { c -> c.code },
            onDismiss = { courseDialog = CourseDialog.None },
            onSave    = { code, name ->
                vm.addCourse(code, name)
                courseDialog = CourseDialog.None
            }
        )
        is CourseDialog.Edit -> CourseEntryDialog(
            existing  = d.course,
            allCodes  = courses.map { c -> c.code }.filterNot { c -> c == d.course.code },
            onDismiss = { courseDialog = CourseDialog.None },
            onSave    = { _, name ->
                vm.updateCourse(d.course.id, name)
                courseDialog = CourseDialog.None
            }
        )
        is CourseDialog.Delete -> AlertDialog(
            onDismissRequest = { courseDialog = CourseDialog.None },
            icon  = { Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Remove Course?") },
            text  = {
                Text(
                    "\"${d.course.code} — ${d.course.name}\" will be removed. Students " +
                            "enrolled in this unit will no longer receive targeted notices for it.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { vm.deleteCourse(d.course.id); courseDialog = CourseDialog.None },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { courseDialog = CourseDialog.None }) { Text("Cancel") } }
        )
        CourseDialog.None -> Unit
    }

    // ── Lecturer dialogs ──────────────────────────────────────────────────────
    when (val d = lecturerDialog) {
        LecturerDialog.Add -> LecturerEntryDialog(
            existing  = null,
            courses   = courses,
            onDismiss = { lecturerDialog = LecturerDialog.None },
            onSave    = { name, lecturerId, courseIds ->
                vm.addLecturer(name, lecturerId, courseIds)
                lecturerDialog = LecturerDialog.None
            }
        )
        is LecturerDialog.Edit -> LecturerEntryDialog(
            existing  = d.lecturer,
            courses   = courses,
            onDismiss = { lecturerDialog = LecturerDialog.None },
            onSave    = { name, lecturerId, courseIds ->
                vm.updateLecturer(d.lecturer.id, name, lecturerId, courseIds)
                lecturerDialog = LecturerDialog.None
            }
        )
        is LecturerDialog.Delete -> AlertDialog(
            onDismissRequest = { lecturerDialog = LecturerDialog.None },
            icon  = { Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Remove Lecturer?") },
            text  = {
                Text(
                    "\"${d.lecturer.name}\" (${d.lecturer.lecturerId}) will be removed.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { vm.deleteLecturer(d.lecturer.id); lecturerDialog = LecturerDialog.None },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { lecturerDialog = LecturerDialog.None }) { Text("Cancel") } }
        )
        LecturerDialog.None -> Unit
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (selectedTab == 0) courseDialog = CourseDialog.Add
                    else lecturerDialog = LecturerDialog.Add
                },
                icon           = { Icon(Icons.Filled.Add, null) },
                text           = { Text(if (selectedTab == 0) "Add Course" else "Add Lecturer") },
                containerColor = Color(0xFF3B5BDB),
                contentColor   = Color.White
            )
        },
        topBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF060D2E), Color(0xFF1A2B6B))))
                    .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                        Column {
                            Text(
                                "Course Catalogue",
                                style      = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color      = Color.White,
                                letterSpacing = (-0.3).sp
                            )
                            Text(
                                "${courses.size} course${if (courses.size != 1) "s" else ""}" +
                                        " · ${lecturers.size} lecturer${if (lecturers.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(0.65f)
                            )
                        }
                    }
                    // Tab row inside the header
                    TabRow(
                        selectedTabIndex  = selectedTab,
                        containerColor    = Color.Transparent,
                        contentColor      = Color.White,
                        indicator         = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]), // FIX 1: now resolved via import
                                color    = Color(0xFF3B5BDB)
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick  = { selectedTab = 0; searchQuery = "" },
                            text     = { Text("Courses", color = Color.White) },
                            icon     = { Icon(Icons.Outlined.MenuBook, null, tint = Color.White) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick  = { selectedTab = 1; searchQuery = "" },
                            text     = { Text("Lecturers", color = Color.White) },
                            icon     = { Icon(Icons.Outlined.Person, null, tint = Color.White) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // ── Shared search bar ─────────────────────────────────────────────
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder   = {
                    Text(if (selectedTab == 0) "Search courses…" else "Search lecturers…")
                },
                leadingIcon  = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, "Clear")
                        }
                    }
                },
                modifier    = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape       = RoundedCornerShape(16.dp),
                singleLine  = true
            )

            when (selectedTab) {
                0 -> CoursesTab(
                    courses     = courses,
                    searchQuery = searchQuery,
                    isLoading   = isLoading,
                    onEdit      = { courseDialog = CourseDialog.Edit(it) },
                    onDelete    = { courseDialog = CourseDialog.Delete(it) },
                    onAddFirst  = { courseDialog = CourseDialog.Add }
                )
                1 -> LecturersTab(
                    lecturers   = lecturers,
                    courses     = courses,
                    searchQuery = searchQuery,
                    isLoading   = isLoading,
                    onEdit      = { lecturerDialog = LecturerDialog.Edit(it) },
                    onDelete    = { lecturerDialog = LecturerDialog.Delete(it) },
                    onAddFirst  = { lecturerDialog = LecturerDialog.Add }
                )
            }
        }
    }
}

// ── Courses tab ───────────────────────────────────────────────────────────────

@Composable
private fun CoursesTab(
    courses     : List<CourseEntry>,
    searchQuery : String,
    isLoading   : Boolean,
    onEdit      : (CourseEntry) -> Unit,
    onDelete    : (CourseEntry) -> Unit,
    onAddFirst  : () -> Unit
) {
    val filtered = remember(courses, searchQuery) {
        if (searchQuery.isBlank()) courses
        else courses.filter {
            it.code.contains(searchQuery.trim(), ignoreCase = true) ||
                    it.name.contains(searchQuery.trim(), ignoreCase = true)
        }
    }
    when {
        isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF3B5BDB))
        }
        filtered.isEmpty() -> EmptyState(
            icon    = Icons.Outlined.MenuBook,
            message = if (searchQuery.isBlank()) "No courses in the catalogue yet"
            else "No courses match \"$searchQuery\"",
            showAdd = searchQuery.isBlank(),
            onAdd   = onAddFirst
        )
        else -> LazyColumn(
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier            = Modifier.fillMaxSize()
        ) {
            items(filtered, key = { c -> c.id }) { course ->
                CourseCard(course = course, onEdit = { onEdit(course) }, onDelete = { onDelete(course) })
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }
}

// ── Lecturers tab ─────────────────────────────────────────────────────────────

@Composable
private fun LecturersTab(
    lecturers   : List<LecturerEntry>,
    courses     : List<CourseEntry>,
    searchQuery : String,
    isLoading   : Boolean,
    onEdit      : (LecturerEntry) -> Unit,
    onDelete    : (LecturerEntry) -> Unit,
    onAddFirst  : () -> Unit
) {

    val courseMap = remember(courses) { courses.associateBy { it.code } }

    val filtered = remember(lecturers, searchQuery) {
        if (searchQuery.isBlank()) lecturers
        else lecturers.filter {
            it.name.contains(searchQuery.trim(), ignoreCase = true) ||
                    it.lecturerId.contains(searchQuery.trim(), ignoreCase = true) ||
                    it.courseIds.any { code -> code.contains(searchQuery.trim(), ignoreCase = true) }
        }
    }
    when {
        isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF3B5BDB))
        }
        filtered.isEmpty() -> EmptyState(
            icon    = Icons.Outlined.Person,
            message = if (searchQuery.isBlank()) "No lecturers added yet"
            else "No lecturers match \"$searchQuery\"",
            showAdd = searchQuery.isBlank(),
            onAdd   = onAddFirst
        )
        else -> LazyColumn(
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier            = Modifier.fillMaxSize()
        ) {
            items(filtered, key = { l -> l.id }) { lecturer ->
                LecturerCard(
                    lecturer  = lecturer,
                    courseMap = courseMap,
                    onEdit    = { onEdit(lecturer) },
                    onDelete  = { onDelete(lecturer) }
                )
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }
}

// ── Cards ─────────────────────────────────────────────────────────────────────

@Composable
private fun CourseCard(course: CourseEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1E3A8A), Color(0xFF3B5BDB))))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(course.code, fontWeight = FontWeight.ExtraBold, color = Color.White,
                    fontSize = 13.sp, letterSpacing = 0.5.sp)
            }
            Spacer(Modifier.width(14.dp))
            Text(course.name, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 2)
            Spacer(Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalIconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Filled.Edit, "Edit", modifier = Modifier.size(16.dp))
                }
                FilledTonalIconButton(
                    onClick  = onDelete,
                    modifier = Modifier.size(34.dp),
                    colors   = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor   = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) { Icon(Icons.Filled.Delete, "Delete", modifier = Modifier.size(16.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LecturerCard(
    lecturer  : LecturerEntry,
    courseMap : Map<String, CourseEntry>,
    onEdit    : () -> Unit,
    onDelete  : () -> Unit
) {
    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1E3A8A), Color(0xFF3B5BDB)))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    lecturer.name.firstOrNull()?.uppercase() ?: "?",
                    fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 18.sp
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(lecturer.name, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    lecturer.lecturerId,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF3B5BDB),
                    fontWeight = FontWeight.Bold
                )
                if (lecturer.courseIds.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    // Show chips for each linked course
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement   = Arrangement.spacedBy(4.dp)
                    ) {
                        lecturer.courseIds.forEach { code ->
                            val name = courseMap[code]?.name
                            AssistChip(
                                onClick = {},
                                label   = {
                                    Text(
                                        if (name != null) "$code · $name" else code,
                                        style    = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalIconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Filled.Edit, "Edit", modifier = Modifier.size(16.dp))
                }
                FilledTonalIconButton(
                    onClick  = onDelete,
                    modifier = Modifier.size(34.dp),
                    colors   = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor   = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) { Icon(Icons.Filled.Delete, "Delete", modifier = Modifier.size(16.dp)) }
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(
    icon    : androidx.compose.ui.graphics.vector.ImageVector,
    message : String,
    showAdd : Boolean,
    onAdd   : () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f))
            Spacer(Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (showAdd) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onAdd) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add First")
                }
            }
        }
    }
}

// ── Course entry dialog (unchanged logic) ─────────────────────────────────────

@Composable
private fun CourseEntryDialog(
    existing:  CourseEntry?,
    allCodes:  List<String>,
    onDismiss: () -> Unit,
    onSave:    (code: String, name: String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val isEditing    = existing != null

    var code by remember(existing?.id) { mutableStateOf(existing?.code ?: "") }
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }

    val codeBlank     = code.isBlank()
    val codeDuplicate = !codeBlank && allCodes.any { it.equals(code.trim(), ignoreCase = true) }
    val nameBlank     = name.isBlank()
    val canSave       = !codeBlank && !codeDuplicate && !nameBlank

    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Icon(if (isEditing) Icons.Filled.Edit else Icons.Filled.Add, null, tint = Color(0xFF3B5BDB)) },
        title = { Text(if (isEditing) "Edit Course" else "Add Course", fontWeight = FontWeight.Bold) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = code,
                    onValueChange = { if (!isEditing) code = it.uppercase().replace(" ", "") },
                    label         = { Text(if (isEditing) "Course Code (fixed)" else "Course Code *") },
                    placeholder   = { Text("e.g. CS101") },
                    singleLine    = true,
                    readOnly      = isEditing,
                    shape         = RoundedCornerShape(12.dp),
                    isError       = !isEditing && (codeBlank || codeDuplicate),
                    supportingText = when {
                        isEditing     -> { { Text("Code is set at creation and cannot be changed", style = MaterialTheme.typography.labelSmall) } }
                        codeDuplicate -> { { Text("This code already exists") } }
                        codeBlank     -> { { Text("Required") } }
                        else          -> null
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction      = ImeAction.Next,
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Course Name *") },
                    placeholder   = { Text("e.g. Introduction to Computing") },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    isError       = nameBlank,
                    supportingText = if (nameBlank) { { Text("Required") } } else null,
                    keyboardOptions = KeyboardOptions(
                        imeAction      = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Words
                    ),
                    keyboardActions = KeyboardActions(onDone = { if (canSave) onSave(code.trim(), name.trim()) }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = { onSave(code.trim(), name.trim()) },
                enabled  = canSave,
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B5BDB))
            ) { Text(if (isEditing) "Save" else "Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Lecturer entry dialog ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class) // FIX 2: added ExperimentalLayoutApi
@Composable
private fun LecturerEntryDialog(
    existing  : LecturerEntry?,
    courses   : List<CourseEntry>,
    onDismiss : () -> Unit,
    onSave    : (name: String, lecturerId: String, courseIds: List<String>) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val isEditing    = existing != null

    var name          by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var lecturerId    by remember(existing?.id) { mutableStateOf(existing?.lecturerId ?: "") }
    // Multi-select course codes
    var selectedCodes by remember(existing?.id) {
        mutableStateOf(existing?.courseIds?.toSet() ?: emptySet())
    }

    // Course search for the dropdown
    var courseQuery    by remember { mutableStateOf("") }
    var courseExpanded by remember { mutableStateOf(false) }

    val filteredCourses = remember(courses, courseQuery) {
        if (courseQuery.isBlank()) courses
        else courses.filter {
            it.code.contains(courseQuery.trim(), ignoreCase = true) ||
                    it.name.contains(courseQuery.trim(), ignoreCase = true)
        }
    }

    val canSave = name.isNotBlank() && lecturerId.isNotBlank() && selectedCodes.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Icon(if (isEditing) Icons.Filled.Edit else Icons.Filled.PersonAdd, null, tint = Color(0xFF3B5BDB)) },
        title = { Text(if (isEditing) "Edit Lecturer" else "Add Lecturer", fontWeight = FontWeight.Bold) },
        text  = {
            Column(
                modifier              = Modifier.verticalScroll(rememberScrollState()), // FIX 3 & 4: now resolved via imports
                verticalArrangement   = Arrangement.spacedBy(12.dp)
            ) {
                // ── Name ──────────────────────────────────────────────────────
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Full Name *") },
                    placeholder   = { Text("e.g. Dr. Jane Otieno") },
                    leadingIcon   = { Icon(Icons.Filled.Person, null) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    isError       = name.isBlank(),
                    supportingText = if (name.isBlank()) { { Text("Required") } } else null,
                    keyboardOptions = KeyboardOptions(
                        imeAction      = ImeAction.Next,
                        capitalization = KeyboardCapitalization.Words
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Lecturer ID ───────────────────────────────────────────────
                OutlinedTextField(
                    value         = lecturerId,
                    onValueChange = { lecturerId = it },
                    label         = { Text("Lecturer ID *") },
                    placeholder   = { Text("e.g. L-2024-007") },
                    leadingIcon   = { Icon(Icons.Filled.Badge, null) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    isError       = lecturerId.isBlank(),
                    supportingText = if (lecturerId.isBlank()) { { Text("Required") } } else null,
                    keyboardOptions = KeyboardOptions(
                        imeAction      = ImeAction.Next,
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Courses (searchable multi-select) ─────────────────────────
                Text(
                    "Courses Taught *",
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExposedDropdownMenuBox(
                    expanded         = courseExpanded,
                    onExpandedChange = { courseExpanded = it }
                ) {
                    OutlinedTextField(
                        value         = courseQuery,
                        onValueChange = { courseQuery = it; courseExpanded = true },
                        placeholder   = { Text("Search and select courses…") },
                        leadingIcon   = { Icon(Icons.Filled.Search, null) },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(courseExpanded) },
                        modifier      = Modifier.fillMaxWidth().menuAnchor(),
                        shape         = RoundedCornerShape(12.dp),
                        singleLine    = true,
                        isError       = selectedCodes.isEmpty()
                    )
                    ExposedDropdownMenu(
                        expanded         = courseExpanded && filteredCourses.isNotEmpty(),
                        onDismissRequest = { courseExpanded = false }
                    ) {
                        filteredCourses.forEach { course ->
                            val isChecked = course.code in selectedCodes
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Checkbox(
                                            checked       = isChecked,
                                            onCheckedChange = null,   // handled by item click
                                            modifier      = Modifier.size(20.dp)
                                        )
                                        Text(
                                            course.code,
                                            fontWeight = FontWeight.Bold,
                                            color      = Color(0xFF3B5BDB),
                                            style      = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(course.name, style = MaterialTheme.typography.bodyMedium)
                                    }
                                },
                                onClick = {
                                    selectedCodes = if (isChecked)
                                        selectedCodes - course.code
                                    else
                                        selectedCodes + course.code
                                    // Keep dropdown open for multi-select
                                }
                            )
                        }
                    }
                }

                // Show selected courses as chips
                if (selectedCodes.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement   = Arrangement.spacedBy(6.dp)
                    ) {
                        selectedCodes.forEach { code ->
                            InputChip(
                                selected = true,
                                onClick  = { selectedCodes = selectedCodes - code },
                                label    = { Text(code) },
                                trailingIcon = {
                                    Icon(Icons.Filled.Close, "Remove",
                                        modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        "At least one course is required",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = { onSave(name.trim(), lecturerId.trim(), selectedCodes.toList()) },
                enabled  = canSave,
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B5BDB))
            ) { Text(if (isEditing) "Save" else "Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}