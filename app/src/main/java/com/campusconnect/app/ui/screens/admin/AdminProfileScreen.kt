package com.campusconnect.app.ui.screens.admin

import androidx.compose.foundation.*
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.campusconnect.app.ui.theme.AppThemeState
import com.campusconnect.app.ui.theme.GoldAccent
// FIX: There is no AdminProfileViewModel — the project uses ProfileViewModel for both
//      student and admin profile screens. ProfileViewModel has all needed fields:
//      user, logout(), updateNotificationsEnabled(), adminEmail, etc.
import com.campusconnect.app.viewmodel.ProfileViewModel

@Composable
fun AdminProfileScreen(
    onBack:                     () -> Unit,
    onLogout:                   () -> Unit,
    onNavigateToPostNotice:     () -> Unit,
    onNavigateToAdminDashboard: () -> Unit,
    // FIX: ProfileViewModel — the actual class in ViewModels.kt
    vm: ProfileViewModel = hiltViewModel()
) {
    // FIX: user, photoUrl, displayName, email, notificationsEnabled — all real fields on User domain model
    val user    by vm.user.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showLogoutDialog by remember { mutableStateOf(false) }

    val manualDark by AppThemeState.isDark.collectAsStateWithLifecycle()
    val systemDark  = isSystemInDarkTheme()

    // FIX: Key is the persisted field value, not the whole user object.
    //      Prevents the toggle resetting whenever any other profile field updates.
    var notificationsEnabled by remember(user?.notificationsEnabled) {
        mutableStateOf(user?.notificationsEnabled ?: true)
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon    = { Icon(Icons.Outlined.Logout, null) },
            title   = { Text("Sign Out?") },
            text    = { Text("You'll be returned to the sign-in screen.") },
            confirmButton = {
                Button(
                    // FIX: vm.logout() — exists on ProfileViewModel
                    onClick = { vm.logout(); onLogout() },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Sign Out") }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") } }
        )
    }

    Column(Modifier.fillMaxSize()) {

        // ── Hero header ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(
                    Brush.linearGradient(listOf(Color(0xFF060D2E), Color(0xFF0D1B4B), Color(0xFF1A2B6B)))
                )
        ) {
            IconButton(
                onClick  = onBack,
                modifier = Modifier.align(Alignment.TopStart).padding(top = 44.dp, start = 8.dp)
            ) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
            }

            Column(
                modifier            = Modifier.fillMaxWidth().padding(top = 52.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(36.dp).background(GoldAccent.copy(0.18f), CircleShape)
                        .border(1.dp, GoldAccent.copy(0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Star, null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                }

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .border(2.5.dp, GoldAccent.copy(0.55f), CircleShape)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    // FIX: user?.photoUrl — real field on User data class
                    if (user?.photoUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                // FIX: user!!.photoUrl — real field on User data class
                                .data(user!!.photoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Admin photo",
                            placeholder        = ColorPainter(Color.White.copy(0.2f)),
                            error              = ColorPainter(Color.White.copy(0.2f)),
                            modifier           = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale       = ContentScale.Crop
                        )
                    } else {
                        Text(
                            // FIX: user?.displayName — real field on User data class
                            (user?.displayName?.firstOrNull() ?: "A").toString().uppercase(),
                            fontSize   = 36.sp,
                            color      = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // FIX: user?.displayName — real field on User data class
                Text(
                    user?.displayName ?: "Administrator",
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White
                )
                // FIX: user?.email — real field on User data class
                Text(
                    user?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(0.7f)
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(GoldAccent.copy(0.18f))
                        .border(1.dp, GoldAccent.copy(0.45f), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Icons.Filled.AdminPanelSettings, null,
                        tint = GoldAccent, modifier = Modifier.size(15.dp))
                    Text("Campus Administrator",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldAccent)
                }
            }
        }

        // ── Scrollable content ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AdminSectionLabel("Admin Actions")
            Card(shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column {
                    AdminProfileItem(Icons.Filled.PostAdd, Color(0xFF3B5BDB), "Post New Notice",
                        onClick = onNavigateToPostNotice)
                    HorizontalDivider(Modifier.padding(start = 64.dp, end = 16.dp))
                    AdminProfileItem(Icons.Filled.Dashboard, GoldAccent, "Go to Admin Dashboard",
                        onClick = onNavigateToAdminDashboard)
                }
            }

            AdminSectionLabel("Preferences")
            Card(shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Dark Mode") },
                        leadingContent  = { AdminIconBox(Icons.Outlined.DarkMode, Color(0xFF7C3AED)) },
                        trailingContent = {
                            Switch(
                                checked         = systemDark || manualDark,
                                onCheckedChange = { if (!systemDark) AppThemeState.setDark(it) },
                                enabled         = !systemDark,
                                colors          = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = GoldAccent
                                )
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider(Modifier.padding(start = 64.dp, end = 16.dp))
                    ListItem(
                        headlineContent = { Text("Push Notifications") },
                        leadingContent  = { AdminIconBox(Icons.Outlined.Notifications, Color(0xFFEA580C)) },
                        trailingContent = {
                            Switch(
                                checked         = notificationsEnabled,
                                onCheckedChange = {
                                    notificationsEnabled = it
                                    // FIX: vm.updateNotificationsEnabled — real method on ProfileViewModel
                                    vm.updateNotificationsEnabled(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = GoldAccent
                                )
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            AdminSectionLabel("About")
            Card(shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                AdminProfileItem(Icons.Outlined.Info, Color(0xFF0891B2), "App Version 2.0.0",
                    showChevron = false, onClick = {})
            }

            Spacer(Modifier.height(4.dp))

            OutlinedButton(
                onClick  = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border   = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Filled.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AdminSectionLabel(title: String) {
    Text(title.uppercase(), style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.1.sp,
        modifier = Modifier.padding(start = 6.dp, bottom = 4.dp))
}

@Composable
private fun AdminIconBox(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun AdminProfileItem(
    icon: ImageVector, iconTint: Color, label: String,
    showChevron: Boolean = true, onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        leadingContent  = { AdminIconBox(icon, iconTint) },
        trailingContent = {
            if (showChevron) Icon(Icons.Filled.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
        },
        modifier = if (showChevron) Modifier.clickable(onClick = onClick) else Modifier,
        colors   = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}