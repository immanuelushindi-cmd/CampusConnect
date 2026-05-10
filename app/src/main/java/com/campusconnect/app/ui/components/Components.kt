package com.campusconnect.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campusconnect.app.domain.model.Notice
import com.campusconnect.app.domain.model.NoticePriority
import com.campusconnect.app.navigation.*
import com.campusconnect.app.utils.DateFormatters

// ─────────────────────────────────────────────────────────────────────────────
// Components.kt — shared UI building blocks for Campus Connect
//
// Exported composables:
//   • CampusBottomBar       — unified student + admin bottom nav (isAdmin flag)
//   • GlassCard             — frosted-glass surface for detail screens
//   • SyncErrorBanner       — inline error strip for LazyColumn
//   • NoticeCard            — notice list item with priority stripe + favourite
//   • PulseDot              — animated live indicator dot
//   • ShimmerBox            — shimmer loading placeholder
//   • EmptyState            — empty list placeholder
//   • CampusLoadingIndicator— full-screen centered spinner
// ─────────────────────────────────────────────────────────────────────────────

// ─── Nav item model ───────────────────────────────────────────────────────────

data class NavItem(
    val label:         String,
    val route:         String,
    val selectedIcon:   ImageVector,
    val unselectedIcon: ImageVector
)

private val studentNavItems = listOf(
    NavItem("Home",      ROUT_DASHBOARD, Icons.Filled.Home,          Icons.Outlined.Home),
    NavItem("Notices",   ROUT_NOTICES,   Icons.Filled.Campaign,      Icons.Outlined.Campaign),
    NavItem("Timetable", ROUT_TIMETABLE, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    NavItem("Events",    ROUT_EVENTS,    Icons.Filled.Event,         Icons.Outlined.Event),
    NavItem("Profile",   ROUT_PROFILE,   Icons.Filled.Person,        Icons.Outlined.Person)
)

private val adminNavItems = listOf(
    NavItem("Overview",  ROUT_ADMIN_DASHBOARD, Icons.Filled.Dashboard,      Icons.Outlined.Dashboard),
    NavItem("Notices",   ROUT_ADMIN_NOTICES,   Icons.Filled.Campaign,       Icons.Outlined.Campaign),
    NavItem("Events",    ROUT_ADMIN_EVENTS,    Icons.Filled.Event,           Icons.Outlined.Event),
    NavItem("Timetable", ROUT_ADMIN_TIMETABLE, Icons.Filled.CalendarMonth,  Icons.Outlined.CalendarMonth),
    NavItem("Profile",   ROUT_ADMIN_PROFILE,   Icons.Filled.ManageAccounts, Icons.Outlined.ManageAccounts)
)

// ─── Unified Bottom Bar ───────────────────────────────────────────────────────

@Composable
fun CampusBottomBar(
    currentRoute: String?,
    isAdmin:      Boolean = false,
    onNavigate:   (String) -> Unit
) {
    val activeColor   = if (isAdmin) Color(0xFFFFB300) else MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
    val pillBg        = activeColor.copy(alpha = 0.13f)
    val items         = if (isAdmin) adminNavItems else studentNavItems

    Surface(
        modifier        = Modifier.fillMaxWidth(),
        color           = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
        tonalElevation  = 0.dp
    ) {
        Box {
            HorizontalDivider(
                modifier  = Modifier.align(Alignment.TopStart),
                thickness = 0.5.dp,
                color     = MaterialTheme.colorScheme.outlineVariant.copy(0.6f)
            )

            if (isAdmin) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.TopStart)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFFFB300).copy(alpha = 0f),
                                    Color(0xFFFFB300).copy(alpha = 0.7f),
                                    Color(0xFFFFB300).copy(alpha = 0.7f),
                                    Color(0xFFFFB300).copy(alpha = 0f)
                                )
                            )
                        )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(64.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = currentRoute == item.route
                    BottomNavTab(
                        item          = item,
                        selected      = selected,
                        activeColor   = activeColor,
                        inactiveColor = inactiveColor,
                        pillBg        = pillBg,
                        onClick       = { if (!selected) onNavigate(item.route) },
                        modifier      = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavTab(
    item:          NavItem,
    selected:      Boolean,
    activeColor:   Color,
    inactiveColor: Color,
    pillBg:        Color,
    onClick:       () -> Unit,
    modifier:      Modifier = Modifier
) {
    val iconScale by animateFloatAsState(
        targetValue   = if (selected) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "iconScale"
    )
    val pillAlpha by animateFloatAsState(
        targetValue   = if (selected) 1f else 0f,
        animationSpec = tween(200),
        label         = "pillAlpha"
    )
    val labelAlpha by animateFloatAsState(
        targetValue   = if (selected) 1f else 0.6f,
        animationSpec = tween(180),
        label         = "labelAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(pillBg.copy(alpha = pillBg.alpha * pillAlpha))
                .padding(horizontal = 18.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = if (selected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                tint               = if (selected) activeColor else inactiveColor,
                modifier           = Modifier
                    .size(22.dp)
                    .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
            )
        }

        Spacer(Modifier.height(3.dp))

        Text(
            text       = item.label,
            fontSize   = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color      = if (selected) activeColor else inactiveColor,
            maxLines   = 1,
            softWrap   = false,
            overflow   = TextOverflow.Clip,
            modifier   = Modifier.graphicsLayer { alpha = labelAlpha }
        )
    }
}

// ─── Glass Card ───────────────────────────────────────────────────────────────

@Composable
fun GlassCard(
    modifier:        Modifier = Modifier,
    backgroundColor: Color    = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
    borderColor:     Color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    shape:           Shape    = RoundedCornerShape(20.dp),
    content:         @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier        = modifier,
        shape           = shape,
        color           = backgroundColor,
        tonalElevation  = 0.dp,
        shadowElevation = 6.dp,
        border          = BorderStroke(0.8.dp, borderColor)
    ) {
        Column(content = content)
    }
}

// ─── Sync Error Banner ────────────────────────────────────────────────────────

@Composable
fun SyncErrorBanner(
    message:  String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape  = RoundedCornerShape(12.dp),
        color  = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Filled.WifiOff, null,
                tint     = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text       = message,
                style      = MaterialTheme.typography.bodySmall,
                color      = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Medium,
                modifier   = Modifier.weight(1f)
            )
        }
    }
}

// ─── Notice Card ──────────────────────────────────────────────────────────────

@Composable
fun NoticeCard(
    notice:     Notice,
    onClick:    () -> Unit,
    onSave: () -> Unit,
    modifier:   Modifier = Modifier
) {
    val priorityColor: Color = when (notice.priority) {
        NoticePriority.URGENT -> Color(0xFFDC2626)
        NoticePriority.HIGH   -> Color(0xFFEF4444)
        NoticePriority.MEDIUM -> Color(0xFF2563EB)
        NoticePriority.LOW    -> Color(0xFF10B981)
    }

    Card(
        onClick   = onClick,
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {

            // Priority colour stripe
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        priorityColor,
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )

            Column(
                Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                // Category chip
                Surface(
                    color = priorityColor.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text       = "${notice.category.emoji} ${notice.category.displayName}",
                        style      = MaterialTheme.typography.labelSmall,
                        color      = priorityColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text       = notice.title,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text     = notice.content,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text  = notice.author,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                    )

                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (notice.createdAt > 0L) {
                            Text(
                                text  = DateFormatters.dashboardItem(notice.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable(
                                    indication        = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick           = onSave
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = if (notice.isSaved)
                                    Icons.Filled.Bookmark
                                else
                                    Icons.Outlined.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint               = if (notice.isSaved) Color(0xFF3B5BDB)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                                modifier           = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Pulse Dot ────────────────────────────────────────────────────────────────

@Composable
fun PulseDot(
    color:    Color = Color(0xFF10B981),
    size:     Dp    = 10.dp,
    maxScale: Float = 1.4f,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "pulseDot")
    val scale by transition.animateFloat(
        initialValue  = 1f,
        targetValue   = maxScale,
        animationSpec = infiniteRepeatable(
            animation  = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotScale"
    )
    val alpha by transition.animateFloat(
        initialValue  = 1f,
        targetValue   = 0.35f,
        animationSpec = infiniteRepeatable(
            animation  = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

// ─── Shimmer Box ──────────────────────────────────────────────────────────────

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape:    Shape    = RoundedCornerShape(12.dp)
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1000f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = shimmerColors,
                    start  = Offset(translateAnim - 200f, 0f),
                    end    = Offset(translateAnim, 0f)
                )
            )
    )
}

// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
fun EmptyState(
    icon:     ImageVector,
    title:    String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                modifier           = Modifier.size(36.dp),
                tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text      = subtitle,
            style     = MaterialTheme.typography.bodySmall,
            color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

// ─── Loading Indicator ────────────────────────────────────────────────────────

@Composable
fun CampusLoadingIndicator(
    modifier: Modifier = Modifier.fillMaxSize()
) {
    Box(
        modifier         = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier    = Modifier.size(48.dp),
            color       = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )
    }
}