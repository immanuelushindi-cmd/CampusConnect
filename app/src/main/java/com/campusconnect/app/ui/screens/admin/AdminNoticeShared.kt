package com.campusconnect.app.ui.screens.admin.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.campusconnect.app.domain.model.Notice
import com.campusconnect.app.domain.model.color
import com.campusconnect.app.domain.model.label
import com.campusconnect.app.utils.DateFormatters


@Composable
fun NoticeChipsRow(notice: Notice) {
    val priorityColor = notice.priority.color   // from NoticeExtensions.kt
    val priorityLabel = notice.priority.label

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // Category chip
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                "${notice.category.emoji} ${notice.category.displayName}",
                style      = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier   = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }

        // Priority chip
        Surface(
            color = priorityColor.copy(0.12f),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier          = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(6.dp).background(priorityColor, CircleShape))
                Spacer(Modifier.width(5.dp))
                Text(
                    priorityLabel,
                    style      = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color      = priorityColor
                )
            }
        }

        // Course target chip
        if (!notice.targetCourse.isNullOrBlank()) {
            Surface(
                color = Color(0xFF3B5BDB).copy(0.12f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    "→ ${notice.targetCourse}",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = Color(0xFF3B5BDB),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

// ── Priority strip — full-width gradient bar for the top of a detail sheet ────
@Composable
fun PriorityGradientStrip(notice: Notice, modifier: Modifier = Modifier) {
    val priorityColor = notice.priority.color
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(priorityColor, priorityColor.copy(0.3f))
                )
            )
    )
}

// ── Notice detail bottom sheet ────────────────────────────────────────────────
// Canonical single implementation — used by both AdminDashboardScreen and
// AdminNoticesScreen.  Previously each file had its own copy.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeDetailSheet(
    notice:    Notice,
    onDismiss: () -> Unit,
    onDelete:  () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        dragHandle       = { PriorityGradientStrip(notice) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            NoticeChipsRow(notice)

            Text(
                notice.title,
                style         = MaterialTheme.typography.headlineSmall,
                fontWeight    = FontWeight.ExtraBold,
                color         = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.3).sp
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Person, null,
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    notice.author.ifBlank { notice.authorEmail },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (notice.createdAt > 0L) {
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        DateFormatters.noticeDetail(notice.createdAt),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 2.dp))

            Text(
                notice.content,
                style      = MaterialTheme.typography.bodyMedium,
                color      = MaterialTheme.colorScheme.onSurface,
                lineHeight = 24.sp
            )

            notice.imageUrl?.let { url ->
                Card(
                    shape     = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    AsyncImage(
                        model              = url,
                        contentDescription = "Notice image",
                        placeholder        = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                        error              = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                        modifier           = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        contentScale       = ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick  = onDelete,
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor   = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Delete Notice", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Edit Notice Sheet ─────────────────────────────────────────────────────────
// FIX: `notice.id` is used as the `remember` key so fields are always
//       populated from the correct notice even if the admin taps Edit on a
//       different notice while the sheet is open.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNoticeSheet(
    notice:    Notice,
    onDismiss: () -> Unit,
    onSave:    (id: String, title: String, content: String) -> Unit
) {
    // FIX: key = notice.id — stale fields from a previously-edited notice are cleared
    var title   by remember(notice.id) { mutableStateOf(notice.title) }
    var content by remember(notice.id) { mutableStateOf(notice.content) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF3B5BDB).copy(0.06f))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Box(
                    Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF3B5BDB).copy(0.3f))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "Edit Notice",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color(0xFF3B5BDB)
                    )
                    TextButton(
                        onClick = { onSave(notice.id, title, content) },
                        enabled = title.isNotBlank() && content.isNotBlank()
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
                label         = { Text("Notice Title *") },
                leadingIcon   = { Icon(Icons.Filled.Title, null) },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(14.dp),
                singleLine    = true
            )
            OutlinedTextField(
                value         = content,
                onValueChange = { content = it },
                label         = { Text("Notice Content *") },
                leadingIcon   = { Icon(Icons.Filled.Notes, null) },
                modifier      = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                shape         = RoundedCornerShape(14.dp),
                maxLines      = 12
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
