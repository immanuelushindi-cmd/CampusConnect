package com.campusconnect.app.ui.screens.notices

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campusconnect.app.domain.model.NoticeCategory
import com.campusconnect.app.ui.components.*
import com.campusconnect.app.ui.theme.NavyBlue
import com.campusconnect.app.utils.SyncState
import com.campusconnect.app.viewmodel.NoticesViewModel
import kotlinx.coroutines.launch
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticesScreen(
    onBack: () -> Unit,
    onNoticeClick: (String) -> Unit,
    vm: NoticesViewModel = hiltViewModel()
) {
    val notices by vm.notices.collectAsStateWithLifecycle()
    val syncState by vm.syncState.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by vm.selectedCategory.collectAsStateWithLifecycle()
    val showSavedOnly by vm.showSavedOnly.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val showScrollTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }

    var everHadData by remember { mutableStateOf(notices.isNotEmpty()) }
    LaunchedEffect(notices) { if (notices.isNotEmpty()) everHadData = true }
    val showSkeleton = notices.isEmpty() && syncState is SyncState.Loading && !everHadData

    val isRefreshing = syncState is SyncState.Loading && everHadData
    val pullState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, "Back",
                            tint = Color.White
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Notices",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = (-0.3).sp
                        )
                        AnimatedVisibility(
                            visible = syncState is SyncState.Loading,
                            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                            exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                        ) {
                            Text(
                                "Syncing…",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(0.62f)
                            )
                        }
                    }
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.toggleSavedFilter()
                    }) {
                        AnimatedContent(
                            targetState = showSavedOnly,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "bookmarks"
                        ) { active ->
                            Icon(
                                if (active) Icons.Filled.Bookmarks else Icons.Outlined.Bookmarks,
                                "Saved",
                                tint = if (active) Color(0xFFFFC107) else Color.White.copy(0.8f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showScrollTop,
                enter = scaleIn(tween(220)) + fadeIn(tween(220)),
                exit = scaleOut(tween(180)) + fadeOut(tween(180))
            ) {
                SmallFloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    containerColor = Color.Transparent,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1E3A8A), Color(0xFF3B5BDB))
                            ),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        "Scroll to top",
                        tint = Color.White
                    )
                }
            }
        }
    ) { padding ->
        PullToRefreshBox(
            state = pullState,
            isRefreshing = isRefreshing,
            onRefresh = { vm.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // ── Search bar ────────────────────────────────────────────────
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(0.7f),
                            tonalElevation = 2.dp,
                            shadowElevation = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { vm.setSearch(it) },
                                placeholder = {
                                    Text(
                                        "Search notices…",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Search, null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    AnimatedVisibility(
                                        visible = searchQuery.isNotEmpty(),
                                        enter = fadeIn() + scaleIn(),
                                        exit = fadeOut() + scaleOut()
                                    ) {
                                        IconButton(onClick = { vm.setSearch("") }) {
                                            Icon(
                                                Icons.Filled.Clear, "Clear",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF3B5BDB).copy(0.6f),
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                        }
                    }
                }

                // ── Category chips ────────────────────────────────────────────
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            val allSelected = selectedCategory == null && !showSavedOnly
                            FilterChip(
                                selected = allSelected,
                                onClick = { vm.setCategory(null) },
                                label = {
                                    Text(
                                        "All",
                                        fontWeight = if (allSelected) FontWeight.SemiBold
                                        else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (allSelected) {
                                    { Icon(Icons.Filled.Done, null, Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1E3A8A).copy(0.15f),
                                    selectedLabelColor = Color(0xFF1E3A8A)
                                )
                            )
                        }
                        items(NoticeCategory.entries) { cat ->
                            val isSelected = selectedCategory == cat
                            val catColor = categoryColor(cat)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    vm.setCategory(if (isSelected) null else cat)
                                },
                                label = {
                                    Text(
                                        "${cat.emoji} ${cat.displayName}",
                                        fontWeight = if (isSelected) FontWeight.SemiBold
                                        else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Filled.Done, null, Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = catColor.copy(0.15f),
                                    selectedLabelColor = catColor
                                )
                            )
                        }
                    }
                }

                // Sync error banner
                if (syncState is SyncState.Error) {
                    item { SyncErrorBanner((syncState as SyncState.Error).msg) }
                }

                // ── Content ───────────────────────────────────────────────────
                when {
                    showSkeleton -> item { NoticesSkeletonScreen() }

                    notices.isEmpty() -> item {
                        EmptyState(
                            icon = Icons.Outlined.Campaign,
                            title = if (showSavedOnly) "No saved notices yet"
                            else "No notices found",
                            subtitle = if (showSavedOnly) "Tap the bookmark on any notice to save it"
                            else "Check back later",
                            modifier = Modifier.padding(top = 32.dp)
                        )
                    }

                    // Students can only read and favourite notices — no delete
                    else -> items(notices, key = { it.id }) { notice ->
                        NoticeCard(
                            notice = notice,
                            onClick = { onNoticeClick(notice.id) },
                            onSave = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.toggleSaved(notice)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Category color mapping ────────────────────────────────────────────────────
private fun categoryColor(cat: NoticeCategory): Color = when (cat.name) {
    "ACADEMIC"  -> Color(0xFF1E3A8A)
    "EVENTS"    -> Color(0xFF7C3AED)
    "SPORTS"    -> Color(0xFF059669)
    "ADMIN"     -> Color(0xFFB45309)
    "EMERGENCY" -> Color(0xFFDC2626)
    "GENERAL"   -> Color(0xFF6B7280)
    else        -> Color(0xFF3B5BDB)
}

// ── Notices Skeleton Screen ───────────────────────────────────────────────────
@Composable
private fun NoticesSkeletonScreen() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(6) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}