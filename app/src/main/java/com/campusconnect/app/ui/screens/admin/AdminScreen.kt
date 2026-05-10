package com.campusconnect.app.ui.screens.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.campusconnect.app.R
import com.campusconnect.app.domain.model.*
import com.campusconnect.app.utils.UiState
import com.campusconnect.app.viewmodel.AdminViewModel

// ── Palette (mirrors AdminPostEventScreen) ────────────────────────────────────
private val NavyStart  = Color(0xFF060D2E)
private val NavyEnd    = Color(0xFF1A2B6B)
private val AdminBlue  = Color(0xFF3B5BDB)

// ── Priority colours ──────────────────────────────────────────────────────────
private val PriorityUrgent = Color(0xFF6A1B9A)   // deep purple — above HIGH
private val PriorityHigh   = Color(0xFFE53935)
private val PriorityMedium = Color(0xFFFB8C00)
private val PriorityLow    = Color(0xFF43A047)

private fun NoticePriority.color() = when (this) {
    NoticePriority.URGENT -> PriorityUrgent
    NoticePriority.HIGH   -> PriorityHigh
    NoticePriority.MEDIUM -> PriorityMedium
    NoticePriority.LOW    -> PriorityLow
}

private fun NoticePriority.label() = when (this) {
    NoticePriority.URGENT -> "Urgent"
    NoticePriority.HIGH   -> "High"
    NoticePriority.MEDIUM -> "Medium"
    NoticePriority.LOW    -> "Low"
}

// ── Category colors (kept in sync with NoticesScreen.categoryColor) ───────────
private fun NoticeCategory.tint(): Color = when (name) {
    "ACADEMIC"  -> Color(0xFF1E3A8A)
    "EVENTS"    -> Color(0xFF7C3AED)
    "SPORTS"    -> Color(0xFF059669)
    "ADMIN"     -> Color(0xFFB45309)
    "EMERGENCY" -> Color(0xFFDC2626)
    "GENERAL"   -> Color(0xFF6B7280)
    else        -> AdminBlue
}

// ── DM Sans font family (fonts live in res/font/) ─────────────────────────────
private val DmSans = FontFamily(
    Font(R.font.dm_sans_regular,   FontWeight.Normal),
    Font(R.font.dm_sans_medium,    FontWeight.Medium),
    Font(R.font.dm_sans_semibold,  FontWeight.SemiBold),
    Font(R.font.dm_sans_bold,      FontWeight.Bold),
    Font(R.font.dm_sans_extrabold, FontWeight.ExtraBold)
)

private val DmSerif = FontFamily(
    Font(R.font.dm_serif_display_regular, FontWeight.Normal)
)

// ── Limits ────────────────────────────────────────────────────────────────────
private const val TITLE_LIMIT   = 80
private const val CONTENT_LIMIT = 500

private enum class TargetMode { ALL, COURSE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    vm: AdminViewModel = hiltViewModel()
) {
    val postState        by vm.postState.collectAsStateWithLifecycle()
    val availableCourses by vm.availableCourses.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context           = LocalContext.current

    // ── Form fields ───────────────────────────────────────────────────────────
    var title            by remember { mutableStateOf("") }
    var content          by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(NoticeCategory.GENERAL) }
    var selectedPriority by remember { mutableStateOf(NoticePriority.MEDIUM) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // ── Targeting ─────────────────────────────────────────────────────────────
    var targetMode     by remember { mutableStateOf(TargetMode.ALL) }
    var selectedCourse by remember { mutableStateOf<CourseEntry?>(null) }
    var targetYear     by remember { mutableStateOf<Int?>(null) }

    // ── Course search ─────────────────────────────────────────────────────────
    var courseSearch by remember { mutableStateOf("") }
    // Keep display text in sync when a course is picked or cleared externally
    LaunchedEffect(selectedCourse) {
        courseSearch = selectedCourse?.let { "${it.code} – ${it.name}" } ?: ""
    }
    val filteredCourses = remember(courseSearch, availableCourses) {
        val q = courseSearch.trim()
        if (q.isBlank() || selectedCourse != null) availableCourses
        else availableCourses.filter { c ->
            c.code.contains(q, ignoreCase = true) ||
                    c.name.contains(q, ignoreCase = true)
        }
    }

    // ── Dropdown expanded flags ───────────────────────────────────────────────
    var categoryExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }
    var courseExpanded   by remember { mutableStateOf(false) }
    var yearExpanded     by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

    // ── Reset after result ────────────────────────────────────────────────────
    LaunchedEffect(postState) {
        when (postState) {
            is UiState.Success -> {
                snackbarHostState.showSnackbar("✅ Notice posted successfully!")
                title            = ""
                content          = ""
                selectedImageUri = null
                selectedCourse   = null
                courseSearch     = ""
                targetYear       = null
                targetMode       = TargetMode.ALL
                vm.resetPostState()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar("❌ ${(postState as UiState.Error).message}")
                vm.resetPostState()
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // ── Gradient top bar (identical to AdminPostEventScreen) ──────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(NavyStart, NavyEnd)))
                    .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Column {
                        Text(
                            text       = "Post Notice",
                            fontFamily = DmSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 20.sp,
                            color      = Color.White
                        )
                        Text(
                            text       = "Fill in the details below",
                            fontFamily = DmSans,
                            fontWeight = FontWeight.Normal,
                            fontSize   = 13.sp,
                            color      = Color.White.copy(alpha = 0.75f)
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

            // ── 1. Title ──────────────────────────────────────────────────────
            OutlinedTextField(
                value         = title,
                onValueChange = { if (it.length <= TITLE_LIMIT) title = it },
                label         = {
                    Text(
                        "Notice Title *",
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium
                    )
                },
                leadingIcon   = { Icon(Icons.Filled.Campaign, null) },
                supportingText = {
                    Text(
                        "${title.length} / $TITLE_LIMIT",
                        fontFamily = DmSans,
                        fontSize   = 11.sp,
                        color      = if (title.length >= TITLE_LIMIT)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier   = Modifier.fillMaxWidth(),
                shape      = RoundedCornerShape(16.dp),
                singleLine = true,
                isError    = title.isBlank() && postState is UiState.Error,
                textStyle  = LocalTextStyle.current.copy(fontFamily = DmSans)
            )

            // ── 2. Content ────────────────────────────────────────────────────
            OutlinedTextField(
                value         = content,
                onValueChange = { if (it.length <= CONTENT_LIMIT) content = it },
                label         = {
                    Text(
                        "Content",
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium
                    )
                },
                leadingIcon    = { Icon(Icons.Filled.Notes, null) },
                supportingText = {
                    Text(
                        "${content.length} / $CONTENT_LIMIT",
                        fontFamily = DmSans,
                        fontSize   = 11.sp,
                        color      = if (content.length >= CONTENT_LIMIT)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier  = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                shape     = RoundedCornerShape(16.dp),
                maxLines  = 8,
                textStyle = LocalTextStyle.current.copy(fontFamily = DmSans)
            )

            // ── 3. Target Audience ────────────────────────────────────────────
            Text(
                text       = "Target Audience",
                fontFamily = DmSerif,
                fontWeight = FontWeight.Normal,
                fontSize   = 15.sp,
                color      = MaterialTheme.colorScheme.onSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TargetChip(
                    text     = "All Students",
                    icon     = Icons.Filled.Public,
                    selected = targetMode == TargetMode.ALL,
                    onClick  = {
                        targetMode     = TargetMode.ALL
                        selectedCourse = null
                        targetYear     = null
                    }
                )
                TargetChip(
                    text     = "By Course",
                    icon     = Icons.Filled.School,
                    selected = targetMode == TargetMode.COURSE,
                    onClick  = { targetMode = TargetMode.COURSE }
                )
            }

            AnimatedVisibility(visible = targetMode == TargetMode.COURSE) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                    // ── Course searchable dropdown ────────────────────────────
                    ExposedDropdownMenuBox(
                        expanded         = courseExpanded,
                        onExpandedChange = { courseExpanded = it }
                    ) {
                        OutlinedTextField(
                            value         = courseSearch,
                            onValueChange = { query ->
                                courseSearch   = query
                                selectedCourse = null
                                courseExpanded = true
                            },
                            label         = {
                                Text(
                                    "Course",
                                    fontFamily = DmSans,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon  = { Icon(Icons.Filled.School, null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(courseExpanded) },
                            modifier  = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape      = RoundedCornerShape(16.dp),
                            singleLine = true,
                            textStyle  = LocalTextStyle.current.copy(fontFamily = DmSans)
                        )
                        ExposedDropdownMenu(
                            expanded         = courseExpanded,
                            onDismissRequest = { courseExpanded = false }
                        ) {
                            if (filteredCourses.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "No courses found",
                                            fontFamily = DmSans,
                                            color      = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    onClick = {}
                                )
                            } else {
                                filteredCourses.forEach { course ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text       = course.code,
                                                    fontFamily = DmSans,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize   = 14.sp
                                                )
                                                Text(
                                                    text       = course.name,
                                                    fontFamily = DmSans,
                                                    fontWeight = FontWeight.Normal,
                                                    fontSize   = 12.sp,
                                                    color      = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedCourse = course
                                            courseExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // ── Year of study dropdown ────────────────────────────────
                    ExposedDropdownMenuBox(
                        expanded         = yearExpanded,
                        onExpandedChange = { yearExpanded = it }
                    ) {
                        OutlinedTextField(
                            value         = targetYear?.let { "Year $it" } ?: "All Years",
                            onValueChange = {},
                            readOnly      = true,
                            label         = {
                                Text(
                                    "Year of Study",
                                    fontFamily = DmSans,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon  = { Icon(Icons.Filled.Groups, null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(yearExpanded) },
                            modifier     = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape     = RoundedCornerShape(16.dp),
                            textStyle = LocalTextStyle.current.copy(fontFamily = DmSans)
                        )
                        ExposedDropdownMenu(
                            expanded         = yearExpanded,
                            onDismissRequest = { yearExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text    = {
                                    Text(
                                        "All Years",
                                        fontFamily = DmSans
                                    )
                                },
                                onClick = { targetYear = null; yearExpanded = false }
                            )
                            (1..5).forEach { yr ->
                                DropdownMenuItem(
                                    text    = {
                                        Text(
                                            "Year $yr",
                                            fontFamily = DmSans
                                        )
                                    },
                                    onClick = { targetYear = yr; yearExpanded = false }
                                )
                            }
                        }
                    }
                }
            }

            // ── 4. Category ───────────────────────────────────────────────────
            ExposedDropdownMenuBox(
                expanded         = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value         = selectedCategory.displayName,
                    onValueChange = {},
                    readOnly      = true,
                    label         = {
                        Text(
                            "Category",
                            fontFamily = DmSans,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    leadingIcon  = {
                        Text(
                            text     = selectedCategory.emoji,
                            fontSize = 18.sp
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                    modifier     = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape     = RoundedCornerShape(16.dp),
                    textStyle = LocalTextStyle.current.copy(fontFamily = DmSans)
                )
                ExposedDropdownMenu(
                    expanded         = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    NoticeCategory.entries.forEach { cat ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text     = cat.emoji,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text       = cat.displayName,
                                        fontFamily = DmSans,
                                        fontWeight = if (cat == selectedCategory)
                                            FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            },
                            onClick = { selectedCategory = cat; categoryExpanded = false }
                        )
                    }
                }
            }

            // ── 5. Priority ───────────────────────────────────────────────────
            ExposedDropdownMenuBox(
                expanded         = priorityExpanded,
                onExpandedChange = { priorityExpanded = it }
            ) {
                OutlinedTextField(
                    value         = selectedPriority.label(),
                    onValueChange = {},
                    readOnly      = true,
                    label         = {
                        Text(
                            "Priority",
                            fontFamily = DmSans,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        // Colored dot indicating current priority
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(selectedPriority.color())
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(priorityExpanded) },
                    modifier     = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape     = RoundedCornerShape(16.dp),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = DmSans,
                        color      = selectedPriority.color(),
                        fontWeight = FontWeight.SemiBold
                    )
                )
                ExposedDropdownMenu(
                    expanded         = priorityExpanded,
                    onDismissRequest = { priorityExpanded = false }
                ) {
                    NoticePriority.entries.forEach { pri ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(pri.color())
                                    )
                                    Text(
                                        text       = pri.label(),
                                        fontFamily = DmSans,
                                        fontWeight = if (pri == selectedPriority)
                                            FontWeight.SemiBold else FontWeight.Normal,
                                        color      = pri.color()
                                    )
                                }
                            },
                            onClick = { selectedPriority = pri; priorityExpanded = false }
                        )
                    }
                }
            }

            // ── 6. Image Picker ───────────────────────────────────────────────
            if (selectedImageUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(selectedImageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Notice image",
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop
                    )
                    IconButton(
                        onClick  = { selectedImageUri = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Cancel, "Remove image",
                            tint     = Color.White,
                            modifier = Modifier.background(
                                Color.Black.copy(alpha = 0.5f), CircleShape
                            )
                        )
                    }
                }
            } else {
                OutlinedButton(
                    onClick  = { imagePicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Image, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Attach Image (optional)",
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // ── 7. Submit ─────────────────────────────────────────────────────
            val isPosting = postState is UiState.Loading
            val isReady   = !isPosting && title.isNotBlank() && content.isNotBlank()

            Button(
                onClick = {
                    vm.postNotice(
                        title             = title,
                        content           = content,
                        category          = selectedCategory,
                        priority          = selectedPriority,
                        imageUri          = selectedImageUri,
                        targetDepartment  = null,
                        targetCourse      = selectedCourse?.code,
                        targetYearOfStudy = targetYear
                    )
                },
                enabled  = isReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor         = AdminBlue,
                    contentColor           = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledContentColor   = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
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
                    text       = if (isPosting) "Posting…" else "Post Notice",
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    color      = Color.White
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Target chip ───────────────────────────────────────────────────────────────
@Composable
private fun TargetChip(
    text:     String,
    icon:     ImageVector,
    selected: Boolean,
    onClick:  () -> Unit
) {
    val containerColor = if (selected) AdminBlue else MaterialTheme.colorScheme.surfaceVariant
    val contentColor   = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick  = onClick,
        shape    = RoundedCornerShape(12.dp),
        color    = containerColor,
        border   = if (!selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        modifier = Modifier.height(38.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier              = Modifier.padding(horizontal = 14.dp)
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                modifier           = Modifier.size(15.dp),
                tint               = contentColor
            )
            Text(
                text       = text,
                fontFamily = DmSans,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize   = 14.sp,
                color      = contentColor
            )
        }
    }
}