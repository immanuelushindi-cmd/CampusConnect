package com.campusconnect.app.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.campusconnect.app.domain.model.Notice
import com.campusconnect.app.domain.model.color
import com.campusconnect.app.domain.model.label
import com.campusconnect.app.ui.components.PulseDot
import com.campusconnect.app.ui.theme.NavyBlue
import com.campusconnect.app.utils.DateFormatters
import com.campusconnect.app.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    onNavigateToNotices:   () -> Unit,
    onNavigateToTimetable: () -> Unit,
    onNavigateToEvents:    () -> Unit,
    onNavigateToProfile:   () -> Unit,
    onNavigateToAdmin:     () -> Unit,
    vm: DashboardViewModel = hiltViewModel()
) {
    val user          by vm.user.collectAsStateWithLifecycle()
    val recentNotices by vm.recentNotices.collectAsStateWithLifecycle()
    val recentEvents  by vm.upcomingEvents.collectAsStateWithLifecycle()
    val isLoading     by vm.isLoading.collectAsStateWithLifecycle()
    val context       = LocalContext.current

    val currentHour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = remember(currentHour) {
        when (currentHour) {
            in 5..11  -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else      -> "Good Evening"
        }
    }

    Scaffold(
        floatingActionButton = {
            if (user?.isAdmin == true) {
                ExtendedFloatingActionButton(
                    onClick        = onNavigateToAdmin,
                    icon           = { Icon(Icons.Filled.Settings, null) },
                    text           = { Text("Admin Panel") },
                    containerColor = NavyBlue,
                    contentColor   = Color.White
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Top
        ) {

            // ── Hero Header ───────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF060D2E),
                                    Color(0xFF0D1B4B),
                                    Color(0xFF1A2B6B)
                                )
                            )
                        )
                        .shadow(
                            elevation  = 12.dp,
                            shape      = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp),
                            spotColor  = Color(0xFF1A2B6B).copy(0.5f)
                        )
                ) {
                    Canvas(Modifier.fillMaxWidth().height(220.dp)) {
                        drawCircle(
                            Color.White.copy(0.04f), 280f,
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.88f, 40f)
                        )
                        drawCircle(
                            Color(0xFF3B5BDB).copy(0.15f), 180f,
                            center = androidx.compose.ui.geometry.Offset(
                                size.width * 0.08f, size.height * 0.8f
                            )
                        )
                    }

                    Column(
                        Modifier.padding(
                            start = 24.dp, end = 24.dp, top = 52.dp, bottom = 32.dp
                        )
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "$greeting,",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(0.72f)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    user?.displayName?.split(" ")?.firstOrNull() ?: "Student",
                                    style         = MaterialTheme.typography.headlineMedium,
                                    fontWeight    = FontWeight.ExtraBold,
                                    color         = Color.White,
                                    letterSpacing = (-0.5).sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .border(2.5.dp, Color.White.copy(0.55f), CircleShape)
                                    .padding(3.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(0.2f))
                                    .clickable { onNavigateToProfile() },
                                contentAlignment = Alignment.Center
                            ) {
                                if (user?.photoUrl != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(user!!.photoUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Avatar",
                                        placeholder        = ColorPainter(Color.White.copy(0.2f)),
                                        error              = ColorPainter(Color.White.copy(0.2f)),
                                        modifier           = Modifier.fillMaxSize(),
                                        contentScale       = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        (user?.displayName?.firstOrNull() ?: "S")
                                            .toString().uppercase(),
                                        color      = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize   = 20.sp
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = !isLoading && recentNotices.isNotEmpty(),
                            enter   = fadeIn() + expandVertically(),
                            exit    = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                Spacer(Modifier.height(16.dp))
                                Surface(
                                    color = Color.White.copy(0.12f),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PulseDot()
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "${recentNotices.size} live notice${if (recentNotices.size != 1) "s" else ""}",
                                            style      = MaterialTheme.typography.labelSmall,
                                            color      = Color.White.copy(0.9f),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Quick Actions ─────────────────────────────────────────────────
            item {
                Column(Modifier.padding(24.dp, 28.dp, 24.dp, 8.dp)) {
                    Text(
                        "Quick Access",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionCard(
                            icon          = Icons.Filled.Campaign,
                            label         = "Notices",
                            gradientStart = Color(0xFF1E3A8A),
                            gradientEnd   = Color(0xFF3B5BDB),
                            modifier      = Modifier.weight(1f),
                            onClick       = onNavigateToNotices
                        )
                        QuickActionCard(
                            icon          = Icons.Filled.CalendarMonth,
                            label         = "Timetable",
                            gradientStart = Color(0xFF065F46),
                            gradientEnd   = Color(0xFF059669),
                            modifier      = Modifier.weight(1f),
                            onClick       = onNavigateToTimetable
                        )
                        QuickActionCard(
                            icon          = Icons.Filled.Event,
                            label         = "Events",
                            gradientStart = Color(0xFF4C1D95),
                            gradientEnd   = Color(0xFF7C3AED),
                            modifier      = Modifier.weight(1f),
                            onClick       = onNavigateToEvents
                        )
                    }
                }
            }

            // ═════════════════════════════════════════════════════════════════
            // LIVE NOTICES SECTION
            // ═════════════════════════════════════════════════════════════════

            item {
                SectionHeader(
                    leadingContent = {
                        PulseDot()
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Live Notices",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    onSeeAll = onNavigateToNotices,
                    seeAllColor = NavyBlue
                )
            }

            if (isLoading && recentNotices.isEmpty()) {
                item { SkeletonPlaceholderRows() }
            } else if (recentNotices.isEmpty()) {
                item {
                    SectionEmptyPlaceholder(
                        icon    = Icons.Outlined.Notifications,
                        message = "No live notices"
                    )
                }
            } else {
                items(recentNotices, key = { it.id }) { notice ->
                    DashboardNoticeItem(
                        notice   = notice,
                        onClick  = onNavigateToNotices,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                    )
                }
            }

            // ═════════════════════════════════════════════════════════════════
            // UPCOMING EVENTS SECTION
            // ═════════════════════════════════════════════════════════════════

            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader(
                    leadingContent = {
                        Icon(
                            Icons.Filled.Event,
                            contentDescription = null,
                            tint     = Color(0xFF7C3AED),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Upcoming Events",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    onSeeAll    = onNavigateToEvents,
                    seeAllColor = Color(0xFF7C3AED)
                )
            }

            if (isLoading && recentEvents.isEmpty()) {
                item { SkeletonPlaceholderRows() }
            } else if (recentEvents.isEmpty()) {
                item {
                    SectionEmptyPlaceholder(
                        icon    = Icons.Outlined.EventBusy,
                        message = "No live events"
                    )
                }
            } else {
                items(recentEvents, key = { it.id }) { event ->
                    DashboardEventItem(
                        event    = event,
                        onClick  = onNavigateToEvents,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                    )
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

// ── Reusable section header ───────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    leadingContent: @Composable RowScope.() -> Unit,
    onSeeAll:       () -> Unit,
    seeAllColor:    Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            content = leadingContent
        )
        TextButton(onClick = onSeeAll) {
            Text(
                "See all →",
                color      = seeAllColor,
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Section empty placeholder ─────────────────────────────────────────────────

@Composable
private fun SectionEmptyPlaceholder(
    icon:    ImageVector,
    message: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon, null,
                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                message,
                style      = MaterialTheme.typography.bodyMedium,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Skeleton placeholder rows (loading state) ─────────────────────────────────

@Composable
private fun SkeletonPlaceholderRows() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.45f))
            )
        }
    }
}

// ── Quick Action Card ─────────────────────────────────────────────────────────

@Composable
private fun QuickActionCard(
    icon:          ImageVector,
    label:         String,
    gradientStart: Color,
    gradientEnd:   Color,
    modifier:      Modifier = Modifier,
    onClick:       () -> Unit
) {
    Card(
        onClick   = onClick,
        modifier  = modifier.height(96.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(gradientStart.copy(0.14f), gradientEnd.copy(0.08f))
                    )
                )
                .border(1.dp, gradientEnd.copy(0.25f), RoundedCornerShape(20.dp))
                .padding(12.dp)
        ) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .background(
                            Brush.linearGradient(listOf(gradientStart, gradientEnd)),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, label, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text(
                    label,
                    style      = MaterialTheme.typography.labelSmall,
                    color      = gradientEnd,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Notice List Item ──────────────────────────────────────────────────────────

@Composable
private fun DashboardNoticeItem(
    notice:   Notice,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    val priorityColor = notice.priority.color
    val priorityLabel = notice.priority.label

    Card(
        onClick   = onClick,
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        priorityColor,
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )
            Row(
                Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            notice.title,
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                            modifier   = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            color = priorityColor.copy(0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                priorityLabel,
                                style      = MaterialTheme.typography.labelSmall,
                                color      = priorityColor,
                                fontWeight = FontWeight.Bold,
                                modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        notice.content,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (notice.createdAt > 0L) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        DateFormatters.dashboardItem(notice.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Event List Item ───────────────────────────────────────────────────────────

@Composable
private fun DashboardEventItem(
    event:    Event,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(0xFF7C3AED)

    Card(
        onClick   = onClick,
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        accentColor,
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )
            Row(
                Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(0.10f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        DateFormatters.eventDay(event.startTimestampMs),
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color      = accentColor
                    )
                    Text(
                        DateFormatters.eventMonth(event.startTimestampMs),
                        style      = MaterialTheme.typography.labelSmall,
                        color      = accentColor.copy(0.8f),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Title + location
                Column(Modifier.weight(1f)) {
                    Text(
                        event.title,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (event.location.isNotBlank()) {
                            Icon(
                                Icons.Outlined.LocationOn, null,
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                event.location,
                                style    = MaterialTheme.typography.bodySmall,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                DateFormatters.dashboardItem(event.startTimestampMs),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Event type chip — Event model has no category field; show a
                // generic calendar emoji so the card layout stays consistent.
                Surface(
                    color = accentColor.copy(0.10f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        if (event.isOnline) "💻" else "📅",
                        style    = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}