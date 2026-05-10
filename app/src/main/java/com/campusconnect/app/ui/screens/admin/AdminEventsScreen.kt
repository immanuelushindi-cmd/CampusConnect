package com.campusconnect.app.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.campusconnect.app.domain.model.Event
import com.campusconnect.app.utils.DateFormatters
import com.campusconnect.app.viewmodel.AdminDashboardViewModel

// ─── Colours ──────────────────────────────────────────────────────────────────
private val EventBlue  = Color(0xFF3B5BDB)
private val EventGreen = Color(0xFF10B981)
private val NavyStart  = Color(0xFF060D2E)
private val NavyEnd    = Color(0xFF1A2B6B)

// ─── Dialog state — sealed to prevent impossible multi-open states ─────────────
// FIX: Replaces three independent nullable/boolean vars that could theoretically
//       all be non-null at the same time.
private sealed interface EventsDialog {
    data object None                        : EventsDialog
    data class  Detail(val event: Event)    : EventsDialog
    data class  Edit(val event: Event)      : EventsDialog
    data class  Delete(val eventId: String) : EventsDialog
}

// ─── Screen ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEventsScreen(
    onPostEvent: () -> Unit,
    vm: AdminDashboardViewModel = hiltViewModel()
) {
    val events    by vm.recentEvents.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()

    var dialog by remember { mutableStateOf<EventsDialog>(EventsDialog.None) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredEvents = remember(events, searchQuery) {
        if (searchQuery.isBlank()) events
        else events.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.organizer.contains(searchQuery, ignoreCase = true) ||
                    it.location.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    // ── Overlays ──────────────────────────────────────────────────────────────
    // FIX: EditEventSheet and AdminEventDetailSheet are called here as top-level
    //       file-level composables — NOT as local functions nested inside this
    //       composable body. Moving them inside was the source of all three errors:
    //       "Unresolved reference" + "Modifier 'private' is not applicable to local function".
    when (val d = dialog) {
        is EventsDialog.Delete -> AlertDialog(
            onDismissRequest = { dialog = EventsDialog.None },
            title = { Text("Delete Event") },
            text  = { Text("This event will be permanently removed. Continue?") },
            confirmButton = {
                Button(
                    onClick = { vm.deleteEvent(d.eventId); dialog = EventsDialog.None },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { dialog = EventsDialog.None }) { Text("Cancel") }
            }
        )
        is EventsDialog.Edit -> EditEventSheet(
            event     = d.event,
            onDismiss = { dialog = EventsDialog.None },
            onSave    = { id, title, desc, loc, millis ->
                vm.updateEvent(id, title, desc, loc, millis)
                dialog = EventsDialog.None
            }
        )
        is EventsDialog.Detail -> AdminEventDetailSheet(
            event     = d.event,
            onDismiss = { dialog = EventsDialog.None },
            onDelete  = { dialog = EventsDialog.Delete(d.event.id) },
            onEdit    = { dialog = EventsDialog.Edit(d.event) }
        )
        EventsDialog.None -> Unit
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
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Events",
                            style         = MaterialTheme.typography.titleLarge,
                            fontWeight    = FontWeight.ExtraBold,
                            color         = Color.White,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            "${events.size} event${if (events.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(0.65f)
                        )
                    }
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
                onClick        = onPostEvent,
                icon           = { Icon(Icons.Filled.AddBox, null) },
                text           = { Text("Post Event") },
                containerColor = EventBlue,
                contentColor   = Color.White
            )
        }
    ) { padding ->
        if (events.isEmpty() && !isLoading) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(EventBlue.copy(0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Event, null,
                            tint     = EventBlue.copy(0.5f),
                            modifier = Modifier.size(36.dp))
                    }
                    Text(
                        "No events posted yet",
                        style      = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Tap the button below to post your first event",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // ── Search bar ────────────────────────────────────────────────
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder   = { Text("Search events by title, organiser or location…") },
                    leadingIcon   = {
                        Icon(Icons.Filled.Search, null, tint = EventBlue)
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
                        focusedBorderColor   = EventBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.4f)
                    ),
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )

                if (filteredEvents.isEmpty()) {
                    Box(
                        modifier         = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Search, null,
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.25f),
                                modifier = Modifier.size(48.dp))
                            Text(
                                "No events match \"${searchQuery.trim()}\"",
                                style      = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Try a different title, organiser or location",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (searchQuery.isNotBlank()) {
                            item {
                                Text(
                                    "${filteredEvents.size} result${if (filteredEvents.size != 1) "s" else ""} for \"${searchQuery.trim()}\"",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        items(filteredEvents, key = { it.id }) { event ->
                            AdminEventListCard(
                                event    = event,
                                onClick  = { dialog = EventsDialog.Detail(event) },
                                onDelete = { dialog = EventsDialog.Delete(event.id) },
                                onEdit   = { dialog = EventsDialog.Edit(event) }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// All composables below are file-level private functions.
// FIX: Previously these were nested inside the screen composable body, making
//       them "local functions" — Kotlin does not allow `private` on local
//       functions, and the compiler cannot resolve them from the call sites
//       in the when() block above.
// ─────────────────────────────────────────────────────────────────────────────

// ── Event list card ───────────────────────────────────────────────────────────
@Composable
private fun AdminEventListCard(
    event:    Event,
    onClick:  () -> Unit,
    onDelete: () -> Unit,
    onEdit:   () -> Unit
) {
    val context = LocalContext.current
    Card(
        onClick   = onClick,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            // ── Banner image (shown only when the event has one) ─────────
            if (!event.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(event.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Event banner",
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
            }
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                // Date column
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .fillMaxHeight()
                        .background(
                            EventBlue.copy(0.1f),
                            RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(vertical = 14.dp)
                    ) {
                        Text(
                            DateFormatters.eventDay(event.startTimestampMs),
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color      = EventBlue
                        )
                        Text(
                            DateFormatters.eventMonth(event.startTimestampMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = EventBlue.copy(0.7f)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        // Time chip
                        Surface(color = EventGreen.copy(0.1f), shape = RoundedCornerShape(20.dp)) {
                            Row(
                                Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.AccessTime, null,
                                    tint = EventGreen, modifier = Modifier.size(11.dp))
                                Text(
                                    DateFormatters.eventTime(event.startTimestampMs),
                                    style      = MaterialTheme.typography.labelSmall,
                                    color      = EventGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Target chip
                        if (!event.targetCourse.isNullOrBlank() || !event.targetDepartment.isNullOrBlank()) {
                            Surface(color = EventBlue.copy(0.1f), shape = RoundedCornerShape(20.dp)) {
                                Text(
                                    "→ ${event.targetCourse ?: event.targetDepartment}",
                                    style    = MaterialTheme.typography.labelSmall,
                                    color    = EventBlue,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Action buttons
                        Row {
                            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Edit, "Edit",
                                    tint = EventBlue, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Delete, "Delete",
                                    tint     = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        event.title,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))

                    if (event.location.isNotBlank()) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Place, null,
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                                modifier = Modifier.size(11.dp))
                            Text(
                                event.location,
                                style    = MaterialTheme.typography.bodySmall,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (event.description.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            event.description,
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } // end Row
        } // end Column wrapper
    }
}

// ── Event detail bottom sheet ─────────────────────────────────────────────────
// FIX: File-level private fun — was incorrectly nested inside the screen body.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminEventDetailSheet(
    event:     Event,
    onDismiss: () -> Unit,
    onDelete:  () -> Unit,
    onEdit:    () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        Brush.horizontalGradient(listOf(EventBlue, EventBlue.copy(0.3f)))
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Event banner image ────────────────────────────────────────
            if (!event.imageUrl.isNullOrBlank()) {
                val context = LocalContext.current
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(event.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Event banner",
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(if (event.imageUrl.isNullOrBlank()) 8.dp else 4.dp))

                // Date + time chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Surface(color = EventBlue.copy(0.1f), shape = RoundedCornerShape(20.dp)) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Filled.CalendarMonth, null,
                                tint = EventBlue, modifier = Modifier.size(14.dp))
                            Text(
                                DateFormatters.eventDate(event.startTimestampMs),
                                style      = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color      = EventBlue
                            )
                        }
                    }
                    Surface(color = EventGreen.copy(0.1f), shape = RoundedCornerShape(20.dp)) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Filled.AccessTime, null,
                                tint = EventGreen, modifier = Modifier.size(14.dp))
                            Text(
                                DateFormatters.eventTime(event.startTimestampMs),
                                style      = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color      = EventGreen
                            )
                        }
                    }
                    if (event.maxAttendees > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                "Max ${event.maxAttendees}",
                                style    = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Text(
                    event.title,
                    style         = MaterialTheme.typography.headlineSmall,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp
                )

                if (event.location.isNotBlank()) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.Place, null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp))
                        Text(
                            event.location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (event.organizer.isNotBlank()) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.Person, null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp))
                        Text(
                            event.organizer,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 2.dp))

                if (event.description.isNotBlank()) {
                    Text(
                        event.description,
                        style      = MaterialTheme.typography.bodyMedium,
                        lineHeight = 24.sp
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick  = onEdit,
                        shape    = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Edit, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Edit Event", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onDelete,
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor   = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape    = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Delete", fontWeight = FontWeight.SemiBold)
                    }
                }
            } // end inner padding Column
        }
    }
}

// ── Edit Event Sheet ──────────────────────────────────────────────────────────
// FIX: File-level private fun — was incorrectly nested inside the screen body.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditEventSheet(
    event:     Event,
    onDismiss: () -> Unit,
    onSave:    (id: String, title: String, desc: String, loc: String, millis: Long) -> Unit
) {
    var title       by remember(event.id) { mutableStateOf(event.title) }
    var description by remember(event.id) { mutableStateOf(event.description) }
    var location    by remember(event.id) { mutableStateOf(event.location) }
    var dateMillis  by remember(event.id) { mutableLongStateOf(event.startTimestampMs) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val cal          = remember(dateMillis) {
        java.util.Calendar.getInstance().apply { timeInMillis = dateMillis }
    }
    var pickedHour   by remember(event.id) { mutableIntStateOf(cal.get(java.util.Calendar.HOUR_OF_DAY)) }
    var pickedMinute by remember(event.id) { mutableIntStateOf(cal.get(java.util.Calendar.MINUTE)) }

    val dateLabel = remember(dateMillis, pickedHour, pickedMinute) {
        val c = java.util.Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(java.util.Calendar.HOUR_OF_DAY, pickedHour)
            set(java.util.Calendar.MINUTE, pickedMinute)
        }
        java.text.SimpleDateFormat("EEE, dd MMM yyyy  HH:mm", java.util.Locale.getDefault())
            .format(c.time)
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateMillis     = datePickerState.selectedDateMillis ?: dateMillis
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    val timePickerState = rememberTimePickerState(initialHour = pickedHour, initialMinute = pickedMinute)
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pick time") },
            text  = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    pickedHour   = timePickerState.hour
                    pickedMinute = timePickerState.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(EventBlue.copy(0.06f))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Box(
                    Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(EventBlue.copy(0.3f))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("Edit Event",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color      = EventBlue)
                    TextButton(
                        onClick = {
                            val finalCal = java.util.Calendar.getInstance().apply {
                                timeInMillis = dateMillis
                                set(java.util.Calendar.HOUR_OF_DAY, pickedHour)
                                set(java.util.Calendar.MINUTE, pickedMinute)
                                set(java.util.Calendar.SECOND, 0)
                            }
                            onSave(event.id, title, description, location, finalCal.timeInMillis)
                        },
                        enabled = title.isNotBlank()
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
                label         = { Text("Event Title *") },
                leadingIcon   = { Icon(Icons.Filled.Event, null) },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(14.dp),
                singleLine    = true
            )
            OutlinedTextField(
                value         = description,
                onValueChange = { description = it },
                label         = { Text("Description") },
                modifier      = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                shape         = RoundedCornerShape(14.dp),
                maxLines      = 6
            )
            OutlinedButton(
                onClick  = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.CalendarMonth, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(dateLabel)
            }
            OutlinedTextField(
                value         = location,
                onValueChange = { location = it },
                label         = { Text("Location / Venue") },
                leadingIcon   = { Icon(Icons.Filled.Place, null) },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(14.dp),
                singleLine    = true
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}