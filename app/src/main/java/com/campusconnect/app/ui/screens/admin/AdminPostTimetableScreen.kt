package com.campusconnect.app.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campusconnect.app.domain.model.CourseEntry
import com.campusconnect.app.domain.model.LecturerEntry
import com.campusconnect.app.utils.UiState
import com.campusconnect.app.viewmodel.AdminViewModel

private val NavyStart  = Color(0xFF060D2E)
private val NavyEnd    = Color(0xFF1A2B6B)
private val AdminBlue  = Color(0xFF3B5BDB)

private val DAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri")

private val COLOR_OPTIONS = listOf(
    "#1E3A8A" to "Navy",
    "#059669" to "Green",
    "#7C3AED" to "Purple",
    "#D97706" to "Amber",
    "#DC2626" to "Red",
    "#0891B2" to "Cyan",
    "#DB2777" to "Pink",
    "#374151" to "Slate"
)

private fun parseHex(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) { Color(0xFF1E3A8A) }

private fun formatTime(hour: Int, minute: Int): String =
    "%02d:%02d".format(hour, minute)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPostTimetableScreen(
    onBack: () -> Unit,
    vm: AdminViewModel = hiltViewModel()
) {
    val postState          by vm.timetableState.collectAsStateWithLifecycle()
    val availableCourses   by vm.availableCourses.collectAsStateWithLifecycle()
    val availableLecturers by vm.availableLecturers.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // ── Course search ────────────────────────────────────────────────────────
    var courseQuery    by remember { mutableStateOf("") }
    var courseExpanded by remember { mutableStateOf(false) }
    var selectedCourse by remember { mutableStateOf<CourseEntry?>(null) }
    var courseCode     by remember { mutableStateOf("") }
    var courseName     by remember { mutableStateOf("") }

    // ── Lecturer search ──────────────────────────────────────────────────────
    var lecturerQuery    by remember { mutableStateOf("") }
    var lecturerExpanded by remember { mutableStateOf(false) }
    var selectedLecturer by remember { mutableStateOf<LecturerEntry?>(null) }

    // Lecturers filtered to the selected course (or all if none selected)
    val lecturersForCourse = remember(availableLecturers, selectedCourse) {
        if (selectedCourse == null) availableLecturers
        else availableLecturers.filter { selectedCourse!!.code in it.courseIds }
    }

    // ── Other form fields ────────────────────────────────────────────────────
    var room     by remember { mutableStateOf("") }
    var building by remember { mutableStateOf("") }
    var notes    by remember { mutableStateOf("") }

    // ── Day selector ─────────────────────────────────────────────────────────
    var selectedDay by remember { mutableIntStateOf(0) }

    // ── Time pickers ─────────────────────────────────────────────────────────
    var startHour   by remember { mutableIntStateOf(8) }
    var startMinute by remember { mutableIntStateOf(0) }
    var endHour     by remember { mutableIntStateOf(9) }
    var endMinute   by remember { mutableIntStateOf(0) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker   by remember { mutableStateOf(false) }

    val startTimeState = rememberTimePickerState(startHour, startMinute, true)
    val endTimeState   = rememberTimePickerState(endHour, endMinute, true)

    // ── Year of study ─────────────────────────────────────────────────────────
    var selectedYearOfStudy by remember { mutableStateOf("All Years") }
    var yearExpanded        by remember { mutableStateOf(false) }

    // ── Color picker ─────────────────────────────────────────────────────────
    var selectedColor by remember { mutableStateOf(COLOR_OPTIONS[0].first) }

    // ── Start time dialog ─────────────────────────────────────────────────────
    if (showStartTimePicker) {
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            title            = { Text("Start Time") },
            text             = { TimePicker(state = startTimeState) },
            confirmButton    = {
                TextButton(onClick = {
                    startHour        = startTimeState.hour
                    startMinute      = startTimeState.minute
                    showStartTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    // ── End time dialog ───────────────────────────────────────────────────────
    if (showEndTimePicker) {
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            title            = { Text("End Time") },
            text             = { TimePicker(state = endTimeState) },
            confirmButton    = {
                TextButton(onClick = {
                    endHour        = endTimeState.hour
                    endMinute      = endTimeState.minute
                    showEndTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    // ── Reset after success ───────────────────────────────────────────────────
    LaunchedEffect(postState) {
        when (postState) {
            is UiState.Success -> {
                snackbarHostState.showSnackbar("✅ Timetable entry posted!")
                courseQuery      = ""; courseCode = ""; courseName = ""
                lecturerQuery    = ""
                selectedCourse   = null; selectedLecturer = null
                room             = ""; building = ""; notes = ""
                selectedDay      = 0
                startHour        = 8; startMinute = 0
                endHour          = 9; endMinute   = 0
                selectedYearOfStudy = "All Years"
                selectedColor    = COLOR_OPTIONS[0].first
                vm.resetTimetableState()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar(
                    "❌ ${(postState as UiState.Error).message}"
                )
                vm.resetTimetableState()
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(NavyStart, NavyEnd)))
                    .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                    Column {
                        Text(
                            "Add Timetable Entry",
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color.White
                        )
                        Text(
                            "Fill in the details below",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(0.75f)
                        )
                    }
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── COURSE SEARCH ────────────────────────────────────────────────
            val filteredCourses = remember(availableCourses, courseQuery, selectedCourse) {
                if (selectedCourse != null) emptyList()
                else availableCourses.filter {
                    courseQuery.isBlank() ||
                            it.code.contains(courseQuery, ignoreCase = true) ||
                            it.name.contains(courseQuery, ignoreCase = true)
                }
            }

            ExposedDropdownMenuBox(
                expanded        = courseExpanded && filteredCourses.isNotEmpty(),
                onExpandedChange = { courseExpanded = it }
            ) {
                OutlinedTextField(
                    value         = courseQuery,
                    onValueChange = {
                        courseQuery    = it
                        courseExpanded = true
                        if (it.isEmpty()) {
                            selectedCourse = null
                            courseCode     = ""
                            courseName     = ""
                        }
                    },
                    label       = { Text("Course *") },
                    leadingIcon = { Icon(Icons.Filled.MenuBook, null) },
                    trailingIcon = {
                        if (selectedCourse != null) {
                            IconButton(onClick = {
                                selectedCourse = null
                                courseQuery    = ""
                                courseCode     = ""
                                courseName     = ""
                                lecturerQuery  = ""
                                selectedLecturer = null
                            }) { Icon(Icons.Default.Clear, null) }
                        }
                    },
                    modifier   = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape      = RoundedCornerShape(16.dp),
                    singleLine = true,
                    isError    = courseCode.isBlank() && postState is UiState.Error
                )

                ExposedDropdownMenu(
                    expanded        = courseExpanded && filteredCourses.isNotEmpty(),
                    onDismissRequest = { courseExpanded = false }
                ) {
                    filteredCourses.forEach { course ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        course.code,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize   = 14.sp
                                    )
                                    Text(
                                        course.name,
                                        fontSize = 12.sp,
                                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                selectedCourse = course
                                courseCode     = course.code
                                courseName     = course.name
                                courseQuery    = "${course.code} – ${course.name}"
                                courseExpanded = false
                                // Reset lecturer when course changes
                                lecturerQuery  = ""
                                selectedLecturer = null
                            }
                        )
                    }
                }
            }

            // ── LECTURER SEARCH ──────────────────────────────────────────────
            val filteredLecturers = remember(lecturersForCourse, lecturerQuery, selectedLecturer) {
                if (selectedLecturer != null) emptyList()
                else lecturersForCourse.filter {
                    lecturerQuery.isBlank() ||
                            it.name.contains(lecturerQuery, ignoreCase = true)
                }
            }

            ExposedDropdownMenuBox(
                expanded        = lecturerExpanded && filteredLecturers.isNotEmpty(),
                onExpandedChange = { lecturerExpanded = it }
            ) {
                OutlinedTextField(
                    value         = lecturerQuery,
                    onValueChange = {
                        lecturerQuery    = it
                        lecturerExpanded = true
                        if (it.isEmpty()) selectedLecturer = null
                    },
                    label       = { Text("Lecturer") },
                    leadingIcon = { Icon(Icons.Filled.Person, null) },
                    trailingIcon = {
                        if (selectedLecturer != null) {
                            IconButton(onClick = {
                                selectedLecturer = null
                                lecturerQuery    = ""
                            }) { Icon(Icons.Default.Clear, null) }
                        }
                    },
                    modifier   = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape      = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                ExposedDropdownMenu(
                    expanded        = lecturerExpanded && filteredLecturers.isNotEmpty(),
                    onDismissRequest = { lecturerExpanded = false }
                ) {
                    filteredLecturers.forEach { lecturer ->
                        DropdownMenuItem(
                            text    = { Text(lecturer.name) },
                            onClick = {
                                selectedLecturer = lecturer
                                lecturerQuery    = lecturer.name
                                lecturerExpanded = false
                            }
                        )
                    }
                }
            }

            // ── DAY SELECTOR ─────────────────────────────────────────────────
            Text(
                "Day of Week",
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DAYS.forEachIndexed { index, day ->
                    val selected = selectedDay == index
                    Surface(
                        onClick      = { selectedDay = index },
                        modifier     = Modifier.weight(1f),
                        shape        = RoundedCornerShape(12.dp),
                        color        = if (selected) AdminBlue else MaterialTheme.colorScheme.surfaceVariant,
                        border       = if (selected) null else BorderStroke(
                            1.dp, MaterialTheme.colorScheme.outline.copy(0.4f)
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                            Text(
                                day,
                                color      = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize   = 13.sp
                            )
                        }
                    }
                }
            }

            // ── TIME PICKERS ─────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Start time
                val startColor = AdminBlue
                OutlinedButton(
                    onClick  = { showStartTimePicker = true },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(16.dp),
                    border   = BorderStroke(1.dp, startColor)
                ) {
                    Icon(
                        Icons.Filled.Schedule, null,
                        modifier = Modifier.size(18.dp),
                        tint     = startColor
                    )
                    Spacer(Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            "Start",
                            fontSize = 10.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatTime(startHour, startMinute),
                            fontWeight = FontWeight.SemiBold,
                            color      = startColor,
                            fontSize   = 14.sp
                        )
                    }
                }

                // End time
                val endColor = AdminBlue
                OutlinedButton(
                    onClick  = { showEndTimePicker = true },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(16.dp),
                    border   = BorderStroke(1.dp, endColor)
                ) {
                    Icon(
                        Icons.Filled.Schedule, null,
                        modifier = Modifier.size(18.dp),
                        tint     = endColor
                    )
                    Spacer(Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            "End",
                            fontSize = 10.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatTime(endHour, endMinute),
                            fontWeight = FontWeight.SemiBold,
                            color      = endColor,
                            fontSize   = 14.sp
                        )
                    }
                }
            }

            // ── ROOM ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value         = room,
                onValueChange = { room = it },
                label         = { Text("Room") },
                leadingIcon   = { Icon(Icons.Filled.MeetingRoom, null) },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(16.dp),
                singleLine    = true
            )

            // ── BUILDING ─────────────────────────────────────────────────────
            OutlinedTextField(
                value         = building,
                onValueChange = { building = it },
                label         = { Text("Building") },
                leadingIcon   = { Icon(Icons.Filled.Domain, null) },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(16.dp),
                singleLine    = true
            )

            // ── YEAR OF STUDY ─────────────────────────────────────────────────
            ExposedDropdownMenuBox(
                expanded        = yearExpanded,
                onExpandedChange = { yearExpanded = it }
            ) {
                OutlinedTextField(
                    value         = selectedYearOfStudy,
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text("Target Year of Study") },
                    leadingIcon   = { Icon(Icons.Filled.School, null) },
                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(yearExpanded) },
                    modifier      = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape         = RoundedCornerShape(16.dp)
                )
                ExposedDropdownMenu(
                    expanded        = yearExpanded,
                    onDismissRequest = { yearExpanded = false }
                ) {
                    (listOf("All Years") + (1..4).map { "Year $it" }).forEach { year ->
                        DropdownMenuItem(
                            text    = { Text(year) },
                            onClick = {
                                selectedYearOfStudy = year
                                yearExpanded        = false
                            }
                        )
                    }
                }
            }

            // ── NOTES ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value         = notes,
                onValueChange = { notes = it },
                label         = { Text("Notes (optional)") },
                leadingIcon   = { Icon(Icons.Filled.Notes, null) },
                modifier      = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                shape    = RoundedCornerShape(16.dp),
                maxLines = 4
            )

            // ── COLOR PICKER ──────────────────────────────────────────────────
            Text(
                "Class Color",
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                COLOR_OPTIONS.forEach { (hex, label) ->
                    val color    = parseHex(hex)
                    val isChosen = selectedColor == hex
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(color, RoundedCornerShape(8.dp))
                            .border(
                                width  = if (isChosen) 3.dp else 0.dp,
                                color  = if (isChosen) Color.White else Color.Transparent,
                                shape  = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedColor = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChosen) Icon(
                            Icons.Default.Check,
                            null,
                            tint     = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ── SUBMIT ────────────────────────────────────────────────────────
            val isPosting = postState is UiState.Loading
            val isReady   = !isPosting && courseCode.isNotBlank() && courseName.isNotBlank()

            Button(
                onClick = {
                    vm.postTimetableEntry(
                        courseCode        = courseCode,
                        courseName        = courseName,
                        lecturer          = lecturerQuery,
                        room              = room,
                        building          = building,
                        dayOfWeek         = selectedDay,
                        startHour         = startHour,
                        startMinute       = startMinute,
                        endHour           = endHour,
                        endMinute         = endMinute,
                        colorHex          = selectedColor,
                        notes             = notes,
                        targetDepartment  = selectedCourse?.code ?: "",
                        targetYearOfStudy = if (selectedYearOfStudy == "All Years") null
                        else selectedYearOfStudy.removePrefix("Year ").toIntOrNull()
                    )
                },
                enabled  = isReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor         = if (isReady) AdminBlue
                    else MaterialTheme.colorScheme.onSurface.copy(0.12f),
                    contentColor           = if (isReady) Color.White
                    else MaterialTheme.colorScheme.onSurface.copy(0.38f),
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(0.12f),
                    disabledContentColor   = MaterialTheme.colorScheme.onSurface.copy(0.38f)
                )
            ) {
                AnimatedVisibility(visible = isPosting) {
                    Row {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
                            color       = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                }
                Text(
                    if (isPosting) "Posting…" else "Post Timetable Entry",
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                    fontSize   = 16.sp
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}