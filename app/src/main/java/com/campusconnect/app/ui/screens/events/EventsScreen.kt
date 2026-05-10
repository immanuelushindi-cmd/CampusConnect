package com.campusconnect.app.ui.screens.events

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.campusconnect.app.domain.model.Event
import com.campusconnect.app.ui.components.CampusLoadingIndicator
import com.campusconnect.app.ui.components.EmptyState
import com.campusconnect.app.utils.DateFormatters
import com.campusconnect.app.utils.SyncState
import com.campusconnect.app.viewmodel.AuthViewModel
import com.campusconnect.app.viewmodel.EventsViewModel
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    onBack:       () -> Unit,
    onPostEvent:  (() -> Unit)? = null,
    vm:           EventsViewModel = hiltViewModel(),
    authVm:       AuthViewModel   = hiltViewModel()
) {
    val events    by vm.events.collectAsStateWithLifecycle()
    val syncState by vm.syncState.collectAsStateWithLifecycle()
    val isAdmin   by authVm.isAdmin.collectAsStateWithLifecycle()
    val query     by vm.query.collectAsStateWithLifecycle()
    val haptic    = LocalHapticFeedback.current
    val context   = LocalContext.current

    val isRefreshing = syncState is SyncState.Loading && events.isNotEmpty()
    val pullState    = rememberPullToRefreshState()

    Scaffold(
        floatingActionButton = {
            if (isAdmin && onPostEvent != null) {
                ExtendedFloatingActionButton(
                    onClick        = onPostEvent,
                    icon           = { Icon(Icons.Filled.Add, null) },
                    text           = { Text("Post Event") },
                    containerColor = Color(0xFF3B5BDB),
                    contentColor   = Color.White
                )
            }
        },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF060D2E), Color(0xFF1A2B6B)))
                    )
                    .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Text(
                        "Events",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White,
                        letterSpacing = (-0.3).sp,
                        modifier   = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value         = query,
                    onValueChange = { vm.onSearchQueryChange(it) },
                    placeholder   = { Text("Search events…", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon   = { Icon(Icons.Filled.Search, null, tint = Color.White.copy(0.7f)) },
                    trailingIcon  = if (query.isNotEmpty()) {
                        { IconButton(onClick = { vm.clearSearch() }) {
                            Icon(Icons.Filled.Close, "Clear", tint = Color.White.copy(0.7f)) } }
                    } else null,
                    singleLine    = true,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor        = Color.White.copy(0.5f),
                        unfocusedBorderColor      = Color.White.copy(0.25f),
                        focusedTextColor          = Color.White,
                        unfocusedTextColor        = Color.White,
                        cursorColor               = Color.White,
                        focusedPlaceholderColor   = Color.White.copy(0.5f),
                        unfocusedPlaceholderColor = Color.White.copy(0.4f)
                    ),
                    shape    = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(top = 4.dp)
                )
            }
        }
    ) { padding ->

        if (syncState is SyncState.Loading && events.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CampusLoadingIndicator() }
            return@Scaffold
        }

        val grouped = remember(events) {
            events.groupBy { e ->
                if (e.startTimestampMs > 0L) DateFormatters.eventGroup(e.startTimestampMs)
                else "Upcoming"
            }
        }

        PullToRefreshBox(
            state        = pullState,
            isRefreshing = isRefreshing,
            onRefresh    = { vm.refresh() },
            modifier     = Modifier.fillMaxSize().padding(padding)
        ) {
            if (events.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon     = Icons.Outlined.Event,
                        title    = if (query.isNotBlank()) "No results for \"$query\"" else "No upcoming events",
                        subtitle = if (query.isNotBlank()) "Try a different search term" else "Events will appear here when posted"
                    )
                }
            } else {
                LazyColumn(
                    modifier        = Modifier.fillMaxSize(),
                    contentPadding  = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    grouped.forEach { (month, monthEvents) ->
                        item(key = "header_$month") {
                            Row(
                                Modifier.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .width(3.dp)
                                        .height(16.dp)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF3B5BDB), Color(0xFF7C3AED))
                                            ),
                                            CircleShape
                                        )
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    month.uppercase(),
                                    style         = MaterialTheme.typography.labelMedium,
                                    fontWeight    = FontWeight.ExtraBold,
                                    color         = Color(0xFF3B5BDB),
                                    letterSpacing = 1.5.sp
                                )
                            }
                        }
                        items(monthEvents, key = { it.id }) { event ->
                            EventCard(
                                event      = event,
                                onRsvp     = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    vm.toggleRsvp(event.id, !event.isAttending)
                                },
                                onMapClick = {
                                    if (event.latitude != null && event.longitude != null) {
                                        val uri = Uri.parse(
                                            "geo:${event.latitude},${event.longitude}" +
                                                    "?q=${Uri.encode(event.location)}"
                                        )
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    }
                                }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(40.dp)) }
                }
            }
        }
    }
}

@Composable
private fun EventCard(
    event: Event,
    onRsvp: () -> Unit,
    onMapClick: () -> Unit
) {
    val context   = LocalContext.current
    val startDate = event.startTimestampMs.takeIf { it > 0L }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            // ── Banner image (no online chip — feature removed) ───────────────
            if (event.imageUrl != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(164.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(event.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = event.title,
                        placeholder        = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                        error              = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop
                    )
                    // Bottom gradient scrim
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(0.45f))
                                )
                            )
                    )
                }
            }

            Column(Modifier.padding(16.dp)) {

                // ── Date badge + title ────────────────────────────────────────
                Row(verticalAlignment = Alignment.Top) {
                    startDate?.let { ms ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF1E3A8A), Color(0xFF3B5BDB))
                                    )
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    DateFormatters.eventDay(ms),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize   = 22.sp,
                                    color      = Color.White
                                )
                                Text(
                                    DateFormatters.eventMonth(ms).uppercase(),
                                    style         = MaterialTheme.typography.labelSmall,
                                    color         = Color.White.copy(0.8f),
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            event.title,
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines   = 2,
                            overflow   = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        startDate?.let { ms ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.AccessTime, null,
                                    modifier = Modifier.size(13.dp),
                                    tint     = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "${DateFormatters.eventDate(ms)} · ${DateFormatters.eventTime(ms)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // ── Description ───────────────────────────────────────────────
                if (event.description.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        event.description,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(10.dp))

                // ── Location row ──────────────────────────────────────────────
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Place, null,
                        modifier = Modifier.size(14.dp),
                        tint     = Color(0xFF3B5BDB)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        event.location,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = Color(0xFF3B5BDB),
                        modifier = Modifier.weight(1f)
                    )
                    if (event.latitude != null) {
                        TextButton(onClick = onMapClick, contentPadding = PaddingValues(4.dp)) {
                            Text(
                                "Map →",
                                style      = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color      = Color(0xFF3B5BDB)
                            )
                        }
                    }
                }

                // Meet link and isOnline chip removed — feature was removed from posting flow.
                // Showing a "Join Meeting" button with an empty/blank link would crash the app.

                HorizontalDivider(Modifier.padding(vertical = 10.dp))

                // ── RSVP footer ───────────────────────────────────────────────
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.People, null,
                                    modifier = Modifier.size(14.dp),
                                    tint     = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                buildString {
                                    append("${event.rsvpCount} attending")
                                    if (event.maxAttendees > 0) append(" / ${event.maxAttendees}")
                                },
                                style      = MaterialTheme.typography.bodySmall,
                                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        // Attendance fill bar
                        if (event.maxAttendees > 0) {
                            Spacer(Modifier.height(6.dp))
                            val fill = (event.rsvpCount.toFloat() / event.maxAttendees)
                                .coerceIn(0f, 1f)
                            val fillColor = when {
                                fill >= 0.9f -> Color(0xFFEF4444)
                                fill >= 0.6f -> Color(0xFFF97316)
                                else         -> Color(0xFF10B981)
                            }
                            Box(
                                Modifier
                                    .fillMaxWidth(0.55f)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(fill)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(fillColor)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    // RSVP button
                    Button(
                        onClick        = onRsvp,
                        colors         = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        shape          = RoundedCornerShape(14.dp)
                    ) {
                        AnimatedContent(
                            targetState  = event.isAttending,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label        = "rsvp"
                        ) { rsvpd ->
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (rsvpd)
                                            Brush.linearGradient(
                                                listOf(Color(0xFF065F46), Color(0xFF059669))
                                            )
                                        else
                                            Brush.linearGradient(
                                                listOf(Color(0xFF1E3A8A), Color(0xFF3B5BDB))
                                            ),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        if (rsvpd) Icons.Filled.CheckCircle
                                        else Icons.Filled.AddCircle,
                                        null,
                                        modifier = Modifier.size(16.dp),
                                        tint     = Color.White
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (rsvpd) "Going ✓" else "RSVP",
                                        fontWeight = FontWeight.Bold,
                                        color      = Color.White,
                                        fontSize   = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}