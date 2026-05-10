package com.campusconnect.app.ui.screens.timetable

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campusconnect.app.domain.model.TimetableEntry
import com.campusconnect.app.ui.components.CampusLoadingIndicator
import com.campusconnect.app.ui.components.EmptyState
import com.campusconnect.app.ui.theme.NavyBlue
import com.campusconnect.app.utils.SyncState
import com.campusconnect.app.viewmodel.TimetableViewModel
import java.util.Calendar


// FIX: expanded to include Saturday and Sunday (indices 5 and 6)
private val DAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val DAY_COLORS = listOf(
    Color(0xFF1E3A8A), // Mon – Navy
    Color(0xFF059669), // Tue – Emerald
    Color(0xFF7C3AED), // Wed – Violet
    Color(0xFFD97706), // Thu – Amber
    Color(0xFFDC2626), // Fri – Red
    Color(0xFF0891B2), // Sat – Cyan
    Color(0xFF9D174D)  // Sun – Rose
)

/** Darkens a color by [fraction] (0..1) for gradient endpoints. */
private fun Color.darken(fraction: Float = 0.28f): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    hsl[2] = (hsl[2] * (1f - fraction)).coerceIn(0f, 1f)
    return Color(ColorUtils.HSLToColor(hsl))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    onBack: () -> Unit,
    vm: TimetableViewModel = hiltViewModel()
) {
    val allEntries  by vm.allEntries.collectAsStateWithLifecycle()
    val dayEntries  by vm.entriesForSelectedDay.collectAsStateWithLifecycle()
    val selectedDay by vm.selectedDay.collectAsStateWithLifecycle()
    val syncState   by vm.syncState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var selectedEntry by remember { mutableStateOf<TimetableEntry?>(null) }

    if (selectedEntry != null) {
        ClassDetailSheet(entry = selectedEntry!!, onDismiss = { selectedEntry = null })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Timetable",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val dayLabel = DAYS.getOrNull(selectedDay) ?: "Today"
                        val text = buildString {
                            appendLine("📅 Timetable — $dayLabel")
                            appendLine()
                            dayEntries.forEach { e ->
                                val start = "${e.startHour}:${e.startMinute.toString().padStart(2, '0')}"
                                val end   = "${e.endHour}:${e.endMinute.toString().padStart(2, '0')}"
                                appendLine("$start – $end  |  ${e.courseCode}: ${e.courseName}")
                                val room = "${e.room}${if (e.building.isNotBlank()) ", ${e.building}" else ""}"
                                appendLine("  📍 $room  •  👤 ${e.lecturer}")
                                appendLine()
                            }
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Timetable – $dayLabel")
                            putExtra(Intent.EXTRA_TEXT, text.trim())
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Timetable"))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Day Selector Strip ─────────────────────────────────────────
            WeeklyOverviewStrip(
                allEntries  = allEntries,
                selectedDay = selectedDay,
                onDaySelected = { vm.selectDay(it) }
            )

            Spacer(Modifier.height(4.dp))

            if (syncState is SyncState.Loading && allEntries.isEmpty()) {
                CampusLoadingIndicator(Modifier.padding(top = 48.dp))
            } else {
                AnimatedContent(
                    targetState = selectedDay,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { it } + fadeIn() togetherWith
                                    slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() togetherWith
                                    slideOutHorizontally { it } + fadeOut()
                        }
                    },
                    label = "day_transition"
                ) { day ->
                    if (dayEntries.isEmpty()) {
                        // FIX: weekend-aware empty message
                        val emptyMessage = when (day) {
                            5 -> "No classes on Saturday — enjoy your weekend! 🎉"
                            6 -> "No classes on Sunday — rest up! 🎉"
                            else -> "No classes on ${DAYS.getOrNull(day) ?: "this day"} — enjoy your free day! 🎉"
                        }
                        EmptyState(
                            icon     = Icons.Filled.CalendarMonth,
                            title    = "No classes on ${DAYS.getOrNull(day) ?: "this day"}",
                            subtitle = emptyMessage,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(dayEntries, key = { it.id }) { entry ->
                                TimetableEntryCard(
                                    entry   = entry,
                                    onClick = { selectedEntry = entry }
                                )
                            }
                            item { Spacer(Modifier.height(24.dp)) }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WeeklyOverviewStrip  (now Mon–Sun, 7 columns)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WeeklyOverviewStrip(
    allEntries: List<TimetableEntry>,
    selectedDay: Int,
    onDaySelected: (Int) -> Unit
) {
    // FIX: map Calendar day-of-week to our 0-based Mon–Sun index
    val todayIndex = remember {
        when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY    -> 0
            Calendar.TUESDAY   -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY  -> 3
            Calendar.FRIDAY    -> 4
            Calendar.SATURDAY  -> 5
            Calendar.SUNDAY    -> 6
            else               -> 0
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp) // slightly tighter for 7 columns
        ) {
            DAYS.forEachIndexed { index, day ->
                val isSelected = index == selectedDay
                val isToday    = index == todayIndex
                val count      = allEntries.count { it.dayOfWeek == index }
                val dayColor   = DAY_COLORS[index]

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected)
                                Brush.verticalGradient(
                                    listOf(dayColor, dayColor.darken(0.22f))
                                )
                            else
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                        )
                        .clickable { onDaySelected(index) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        day,
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color      = if (isSelected) Color.White
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    )

                    Spacer(Modifier.height(5.dp))

                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) Color.White.copy(alpha = 0.22f)
                                    else dayColor.copy(alpha = 0.12f)
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
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

                    // "Today" dot indicator
                    if (isToday) {
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color.White.copy(0.8f) else dayColor
                                )
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TimetableEntryCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TimetableEntryCard(entry: TimetableEntry, onClick: () -> Unit) {
    val cardColor = runCatching {
        Color(android.graphics.Color.parseColor(entry.colorHex))
    }.getOrDefault(NavyBlue)

    val onCardColor = if (ColorUtils.calculateLuminance(cardColor.toArgb()) > 0.4f)
        Color.Black else Color.White

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape     = RoundedCornerShape(20.dp),
                ambientColor  = cardColor.copy(alpha = 0.35f),
                spotColor     = cardColor.copy(alpha = 0.45f)
            ),
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(cardColor, cardColor.darken(0.30f))
                    )
                )
        ) {
            Row(
                modifier           = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                verticalAlignment  = Alignment.CenterVertically
            ) {
                TimeBlock(
                    startHour   = entry.startHour,
                    startMinute = entry.startMinute,
                    endHour     = entry.endHour,
                    endMinute   = entry.endMinute,
                    onCardColor = onCardColor
                )

                Spacer(Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(76.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    onCardColor.copy(alpha = 0f),
                                    onCardColor.copy(alpha = 0.3f),
                                    onCardColor.copy(alpha = 0f)
                                )
                            )
                        )
                )

                Spacer(Modifier.width(16.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        entry.courseCode,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = onCardColor,
                        fontSize      = 17.sp,
                        letterSpacing = 0.3.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        entry.courseName,
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = onCardColor.copy(alpha = 0.88f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoChip(
                            icon  = Icons.Filled.Place,
                            label = entry.room,
                            tint  = onCardColor
                        )
                        InfoChip(
                            icon     = Icons.Filled.Person,
                            label    = entry.lecturer,
                            tint     = onCardColor,
                            maxChars = 16
                        )
                    }
                }

                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint     = onCardColor.copy(alpha = 0.45f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun TimeBlock(
    startHour: Int, startMinute: Int,
    endHour: Int,   endMinute: Int,
    onCardColor: Color
) {
    Column(
        modifier             = Modifier.width(62.dp),
        horizontalAlignment  = Alignment.CenterHorizontally
    ) {
        Text(
            "${startHour}:${startMinute.toString().padStart(2, '0')}",
            fontWeight = FontWeight.ExtraBold,
            color      = onCardColor,
            fontSize   = 16.sp
        )
        Box(
            modifier = Modifier
                .width(1.5.dp)
                .height(16.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(onCardColor.copy(0.6f), onCardColor.copy(0.1f))
                    )
                )
        )
        Text(
            "${endHour}:${endMinute.toString().padStart(2, '0')}",
            style    = MaterialTheme.typography.labelSmall,
            color    = onCardColor.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun InfoChip(
    icon:     androidx.compose.ui.graphics.vector.ImageVector,
    label:    String,
    tint:     Color,
    maxChars: Int = Int.MAX_VALUE
) {
    val displayLabel = if (label.length > maxChars) label.take(maxChars) + "…" else label

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(tint.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, null, tint = tint.copy(0.75f), modifier = Modifier.size(11.dp))
        Text(displayLabel, style = MaterialTheme.typography.labelSmall, color = tint.copy(0.88f), fontSize = 10.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ClassDetailSheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassDetailSheet(entry: TimetableEntry, onDismiss: () -> Unit) {
    val cardColor = runCatching {
        Color(android.graphics.Color.parseColor(entry.colorHex))
    }.getOrDefault(NavyBlue)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle       = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 36.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(cardColor, cardColor.darken(0.30f))
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 12.dp, top = 16.dp, bottom = 4.dp),
                verticalAlignment     = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.courseName,
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        entry.courseCode,
                        style      = MaterialTheme.typography.titleMedium,
                        color      = cardColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(4.dp))

            DetailRow(
                icon  = Icons.Filled.AccessTime,
                label = "Time",
                value = "${entry.startHour}:${entry.startMinute.toString().padStart(2,'0')} — ${entry.endHour}:${entry.endMinute.toString().padStart(2,'0')}",
                tint  = cardColor
            )
            DetailRow(
                icon  = Icons.Filled.Place,
                label = "Room",
                value = "${entry.room}${if (entry.building.isNotBlank()) ", ${entry.building}" else ""}",
                tint  = cardColor
            )
            DetailRow(Icons.Filled.Person,        "Lecturer", entry.lecturer,                         cardColor)
            DetailRow(Icons.Filled.CalendarToday, "Day",      DAYS.getOrNull(entry.dayOfWeek) ?: "—", cardColor)
            if (entry.semester.isNotBlank()) {
                DetailRow(Icons.Filled.School, "Semester", entry.semester, cardColor)
            }

            if (entry.notes.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Notes",
                            style         = MaterialTheme.typography.labelSmall,
                            color         = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight    = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(entry.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon:  androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint:  Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                label,
                style         = MaterialTheme.typography.labelSmall,
                color         = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(1.dp))
            Text(
                value,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
    HorizontalDivider(
        modifier  = Modifier.padding(start = 70.dp, end = 20.dp),
        thickness = 0.5.dp,
        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}