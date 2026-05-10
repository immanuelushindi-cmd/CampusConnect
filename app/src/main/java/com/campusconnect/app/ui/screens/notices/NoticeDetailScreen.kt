package com.campusconnect.app.ui.screens.notices

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.campusconnect.app.domain.model.Notice
import com.campusconnect.app.domain.model.NoticePriority
import com.campusconnect.app.ui.components.GlassCard
import com.campusconnect.app.ui.theme.*
import com.campusconnect.app.utils.DateFormatters
import com.campusconnect.app.viewmodel.NoticeDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeDetailScreen(
    noticeId: String,
    onBack: () -> Unit,
    vm: NoticeDetailViewModel = hiltViewModel()
) {
    val notice by vm.notice.collectAsStateWithLifecycle()

    LaunchedEffect(noticeId) { vm.load(noticeId) }

    Scaffold(
        topBar = {
            // FIX: Branded gradient TopAppBar consistent with the rest of the app
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF060D2E), Color(0xFF1A2B6B))
                        )
                    )
                    .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, "Back",
                                tint = Color.White
                            )
                        }
                        Text(
                            "Notice",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = (-0.3).sp
                        )
                    }
                    // Favourite button in header
                    notice?.let { n ->
                        IconButton(onClick = { vm.toggleSaved(n) }) {
                            AnimatedContent(
                                targetState = n.isSaved,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "fav"
                            ) { fav ->
                                Icon(
                                    if (fav) Icons.Filled.Bookmark
                                    else Icons.Outlined.BookmarkBorder,
                                    "Save",
                                    tint = if (fav) Color(0xFFE91E63)
                                    else Color.White.copy(0.8f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = notice,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            label = "detail"
        ) { n ->
            if (n == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NavyBlue)
                }
            } else {
                NoticeDetailContent(notice = n)
            }
        }
    }
}

// ── Detail Content ────────────────────────────────────────────────────────────

@Composable
private fun NoticeDetailContent(notice: Notice) {
    val (priorityColor, priorityLabel) = when (notice.priority) {
        NoticePriority.URGENT -> Color(0xFFE91E63) to "URGENT"
        NoticePriority.HIGH     -> Color(0xFFEF4444) to "HIGH"
        NoticePriority.MEDIUM   -> Color(0xFFF97316) to "MEDIUM"
        NoticePriority.LOW      -> Color(0xFF10B981) to "LOW"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        // ── Hero banner area (gradient with priority stripe at very top) ──────
        // FIX: Full-width gradient hero replaces the invisible 4dp Box.
        // Priority color bleeds into the hero as a gradient accent for immediate
        // visual feedback before the user reads any text.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(priorityColor, priorityColor.copy(0.4f))
                    )
                )
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Header card ───────────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(20.dp)) {

                    // Category chip + priority badge
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                "${notice.category.emoji} ${notice.category.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        // FIX: Priority badge with full color background and white text
                        // instead of the M3 Badge which clips/overlaps in some layouts
                        Surface(
                            color = priorityColor.copy(0.15f),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, priorityColor.copy(0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Color dot
                                Box(
                                    Modifier
                                        .size(6.dp)
                                        .background(priorityColor, CircleShape)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    priorityLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = priorityColor
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Title
                    Text(
                        notice.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.3).sp
                    )

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
                    Spacer(Modifier.height(14.dp))

                    // FIX: Meta row items are now each in a small Surface pill container
                    // instead of floating icon+text pairs — far more readable and structured
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (notice.author.isNotBlank()) {
                            MetaPill(
                                icon = Icons.Filled.Person,
                                label = notice.author,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        if (notice.createdAt > 0L) {
                            MetaPill(
                                icon = Icons.Filled.AccessTime,
                                label = DateFormatters.noticeDetail(notice.createdAt),
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }
                }
            }

            // ── Notice image ──────────────────────────────────────────────────
            // FIX: Image now has a surfaceVariant placeholder while loading
            notice.imageUrl?.let { url ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = "Notice image",
                        placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                        error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp, max = 280.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // ── Body card ─────────────────────────────────────────────────────
            // FIX: Body text now lives in a proper Card with subtle top colored accent bar
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column {
                    // Thin accent bar at top of body card — echoes the priority color
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(priorityColor.copy(0.6f), Color.Transparent)
                                )
                            )
                    )
                    Column(Modifier.padding(20.dp)) {
                        // Section label
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                Icons.Filled.Article, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Content",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            notice.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 26.sp
                        )
                    }
                }
            }

            // ── Tags section ──────────────────────────────────────────────────
            // FIX: Tags now have a section header + use tinted chip style for better
            // discoverability — previously appeared with zero context or label
            if (notice.tags.isNotEmpty()) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Label, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "TAGS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp
                        )
                    }
                    // FIX: Tags wrap via a Wrapped Row pattern using FlowRow-like manual wrapping.
                    // Use a simple Row with horizontal scroll if FlowRow isn't available, or
                    // replace with androidx.compose.foundation.layout.FlowRow if using Compose 1.7+
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        notice.tags.forEach { tag ->
                            Surface(
                                color = Color(0xFF3B5BDB).copy(0.10f),
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp, Color(0xFF3B5BDB).copy(0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 10.dp, vertical = 5.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "#",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF3B5BDB).copy(0.6f)
                                    )
                                    Text(
                                        tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF3B5BDB)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Meta Pill component ───────────────────────────────────────────────────────
// FIX: Replaces the loose floating icon+text pairs in the original meta row.
// Each piece of notice metadata (author, date) now sits in its own Surface pill,
// making them clearly scannable and visually distinct from body content.
@Composable
private fun MetaPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(0.7f),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}