package com.campusconnect.app.ui.screens.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.campusconnect.app.domain.model.TARGET_AUDIENCES
import com.campusconnect.app.utils.UiState
import com.campusconnect.app.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*

private val NavyStart  = Color(0xFF060D2E)
private val NavyEnd    = Color(0xFF1A2B6B)
private val AdminBlue  = Color(0xFF3B5BDB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPostEventScreen(
    onBack: () -> Unit,
    vm: AdminViewModel = hiltViewModel()
) {
    val postState        by vm.eventState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context           = LocalContext.current

    // Form fields
    var title            by remember { mutableStateOf("") }
    var description      by remember { mutableStateOf("") }
    var location         by remember { mutableStateOf("") }
    var maxAttendees     by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Audience targeting
    var selectedAudience by remember { mutableStateOf(TARGET_AUDIENCES.first()) }
    var audienceExpanded by remember { mutableStateOf(false) }
    var selectedYearOfStudy by remember { mutableStateOf("All Years") }
    var yearExpanded by remember { mutableStateOf(false) }

    // Date/time picker state
    var pickedDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker   by remember { mutableStateOf(false) }
    var showTimePicker   by remember { mutableStateOf(false) }
    var pickedHour       by remember { mutableIntStateOf(9) }
    var pickedMinute     by remember { mutableIntStateOf(0) }

    val dateLabel = remember(pickedDateMillis, pickedHour, pickedMinute) {
        pickedDateMillis?.let { ms ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = ms
                set(Calendar.HOUR_OF_DAY, pickedHour)
                set(Calendar.MINUTE, pickedMinute)
            }
            SimpleDateFormat("EEE, dd MMM yyyy  HH:mm", Locale.getDefault()).format(cal.time)
        } ?: "Pick date & time"
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

    // Date picker dialog
    val datePickerState = rememberDatePickerState()
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker   = false
                    showTimePicker   = true
                }) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    // Time picker dialog
    val timePickerState = rememberTimePickerState(
        initialHour   = pickedHour,
        initialMinute = pickedMinute
    )
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pick time") },
            text  = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    pickedHour     = timePickerState.hour
                    pickedMinute   = timePickerState.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    // Handle post result
    LaunchedEffect(postState) {
        when (postState) {
            is UiState.Success -> {
                snackbarHostState.showSnackbar("✅ Event posted successfully!")
                title            = ""
                description      = ""
                location         = ""
                maxAttendees     = ""
                selectedImageUri = null
                pickedDateMillis = null
                selectedAudience = TARGET_AUDIENCES.first()
                selectedYearOfStudy = "All Years"
                vm.resetEventState()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar("❌ ${(postState as UiState.Error).message}")
                vm.resetEventState()
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
                        Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Column {
                        Text(
                            "Post Event",
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

            // ── Title ──────────────────────────────────────────────────────
            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                label         = { Text("Event Title *") },
                leadingIcon   = { Icon(Icons.Filled.Event, null) },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(16.dp),
                singleLine    = true,
                isError       = title.isBlank() && postState is UiState.Error
            )

            // ── Description ────────────────────────────────────────────────
            OutlinedTextField(
                value         = description,
                onValueChange = { description = it },
                label         = { Text("Description") },
                leadingIcon   = { Icon(Icons.Filled.Notes, null) },
                modifier      = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                shape    = RoundedCornerShape(16.dp),
                maxLines = 6
            )

            // ── Date & Time ────────────────────────────────────────────────
            val dateButtonColor = if (pickedDateMillis != null) AdminBlue
            else MaterialTheme.colorScheme.onSurfaceVariant
            OutlinedButton(
                onClick  = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(16.dp),
                border   = BorderStroke(1.dp, dateButtonColor)
            ) {
                Icon(
                    Icons.Filled.CalendarMonth, null,
                    modifier = Modifier.size(18.dp),
                    tint     = dateButtonColor
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    dateLabel,
                    fontWeight = if (pickedDateMillis != null) FontWeight.SemiBold
                    else FontWeight.Normal,
                    color      = dateButtonColor
                )
            }

            // ── Location / Venue ───────────────────────────────────────────
            OutlinedTextField(
                value         = location,
                onValueChange = { location = it },
                label         = { Text("Location / Venue") },
                leadingIcon   = { Icon(Icons.Filled.Place, null) },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(16.dp),
                singleLine    = true
            )

            // ── Max Attendees ──────────────────────────────────────────────
            OutlinedTextField(
                value         = maxAttendees,
                onValueChange = { if (it.all(Char::isDigit)) maxAttendees = it },
                label         = { Text("Max Attendees (0 = unlimited)") },
                leadingIcon   = { Icon(Icons.Filled.Group, null) },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(16.dp),
                singleLine    = true
            )

            // ── Target Audience ────────────────────────────────────────────
            ExposedDropdownMenuBox(
                expanded        = audienceExpanded,
                onExpandedChange = { audienceExpanded = it }
            ) {
                OutlinedTextField(
                    value         = selectedAudience,
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text("Target Audience") },
                    leadingIcon   = {
                        Icon(
                            imageVector = if (selectedAudience == TARGET_AUDIENCES.first())
                                Icons.Filled.Public else Icons.Filled.Groups,
                            contentDescription = null
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(audienceExpanded) },
                    modifier     = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(16.dp)
                )
                ExposedDropdownMenu(
                    expanded        = audienceExpanded,
                    onDismissRequest = { audienceExpanded = false }
                ) {
                    TARGET_AUDIENCES.forEach { audience ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (audience == TARGET_AUDIENCES.first())
                                            Icons.Filled.Public else Icons.Filled.School,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint     = if (audience == TARGET_AUDIENCES.first())
                                            AdminBlue
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(audience)
                                }
                            },
                            onClick = {
                                selectedAudience = audience
                                audienceExpanded = false
                            }
                        )
                    }
                }
            }

            // ── Target Year of Study ───────────────────────────────────────
            ExposedDropdownMenuBox(
                expanded = yearExpanded,
                onExpandedChange = { yearExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedYearOfStudy,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Target Year of Study") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = null
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                ExposedDropdownMenu(
                    expanded = yearExpanded,
                    onDismissRequest = { yearExpanded = false }
                ) {
                    (listOf("All Years") + (1..4).map { it.toString() }).forEach { year ->
                        DropdownMenuItem(
                            text = { Text(year) },
                            onClick = {
                                selectedYearOfStudy = year
                                yearExpanded = false
                            }
                        )
                    }
                }
            }

            // ── Image Picker ───────────────────────────────────────────────
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
                        contentDescription = "Event banner",
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
                                Color.Black.copy(0.5f), RoundedCornerShape(50)
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
                    Text("Attach Banner Image (optional)")
                }
            }

            // ── Submit ─────────────────────────────────────────────────────────────────
            val isReady = postState !is UiState.Loading && title.isNotBlank()
            Button(
                onClick = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = pickedDateMillis ?: System.currentTimeMillis()
                        set(Calendar.HOUR_OF_DAY, pickedHour)
                        set(Calendar.MINUTE, pickedMinute)
                        set(Calendar.SECOND, 0)
                    }
                    vm.postEvent(
                        title            = title,
                        description      = description,
                        location         = location,
                        dateTimeMillis   = cal.timeInMillis,
                        maxAttendees     = maxAttendees.toIntOrNull() ?: 0,
                        isOnline         = false,
                        meetLink         = "",
                        imageUri         = selectedImageUri,
                        targetDepartment = if (selectedAudience == TARGET_AUDIENCES.first())
                            null else selectedAudience,
                        targetYearOfStudy = selectedYearOfStudy.toIntOrNull()
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape   = RoundedCornerShape(16.dp),
                colors  = ButtonDefaults.buttonColors(
                    containerColor         = if (isReady) AdminBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    contentColor           = if (isReady) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledContentColor   = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                ),
                enabled = isReady
            ) {
                AnimatedVisibility(visible = postState is UiState.Loading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (postState is UiState.Loading) "Posting…" else "Post Event",
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                    fontSize   = 16.sp
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}