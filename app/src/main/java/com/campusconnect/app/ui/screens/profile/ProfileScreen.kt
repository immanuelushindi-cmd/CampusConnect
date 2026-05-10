package com.campusconnect.app.ui.screens.profile

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.campusconnect.app.ui.theme.AppThemeState
import com.campusconnect.app.ui.theme.NavyBlue
import com.campusconnect.app.ui.theme.NavyBlueDark
import com.campusconnect.app.viewmodel.ProfileViewModel


// ─── Accent colours ───────────────────────────────────────────────────────────
private val AdminChipBg     = Color(0xFFFFF8E1)
private val AdminChipBorder = Color(0xFFFFC107)
private val AdminChipIcon   = Color(0xFFFF8F00)
private val AdminChipText   = Color(0xFFE65100)

private val ProgrammeChipBg     = Color(0xFFEDE9FE)
private val ProgrammeChipBorder = Color(0xFF7C3AED)
private val ProgrammeChipText   = Color(0xFF4C1D95)

private val CourseChipBg     = Color(0xFFE0F2FE)
private val CourseChipBorder = Color(0xFF0284C7)
private val CourseChipText   = Color(0xFF0C4A6E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val user by vm.user.collectAsStateWithLifecycle()
    val adminEmail by vm.adminEmail.collectAsStateWithLifecycle()
    val isUploadingPhoto by vm.isUploadingPhoto.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }
    var notificationsEnabled by remember(user) {
        mutableStateOf(user?.notificationsEnabled ?: true)
    }

    val manualDark by AppThemeState.isDark.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { vm.updateProfilePhoto(it) } }

    // ── Logout Dialog ─────────────────────────────────────────────────────────
    if (showPrivacySheet) {
        PrivacyPolicySheet(onDismiss = { showPrivacySheet = false })
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Outlined.Logout, null) },
            title = { Text("Sign Out?") },
            text = { Text("You'll need to sign in again to access Campus Connect.") },
            confirmButton = {
                Button(
                    onClick = { vm.logout(); onLogout() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Sign Out") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Edit Profile Bottom Sheet ─────────────────────────────────────────────
    // Department (programme) is read-only — it drives content targeting and
    // cannot be self-edited. Courses are fully editable.
    if (showEditSheet) {
        EditProfileSheet(
            currentName      = user?.displayName ?: "",
            currentBio       = user?.bio ?: "",
            currentStudentId = user?.studentId ?: "",
            currentYear      = user?.yearOfStudy ?: 1,
            currentCourses   = user?.courses ?: emptyList(),
            onDismiss = { showEditSheet = false },
            onSave = { name, bio, studentId, year, courses ->
                vm.updateProfile(
                    name        = name,
                    bio         = bio,
                    studentId   = studentId,
                    yearOfStudy = year,
                    department  = user?.department ?: "",   // preserved, not user-editable
                    courses     = courses
                )
                showEditSheet = false
            }
        )
    }

    Column(Modifier.fillMaxSize()) {

        // ── Hero Header ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Brush.verticalGradient(listOf(NavyBlueDark, NavyBlue)))
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 44.dp, start = 8.dp)
            ) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 52.dp, bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar with camera badge
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .border(
                            width = 2.5.dp,
                            color = Color.White.copy(alpha = 0.55f),
                            shape = CircleShape
                        )
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.2f))
                        .clickable(enabled = !isUploadingPhoto) { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (user?.photoUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(user!!.photoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile",
                            placeholder = ColorPainter(Color.White.copy(0.2f)),
                            error = ColorPainter(Color.White.copy(0.2f)),
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            (user?.displayName?.firstOrNull() ?: "S").toString().uppercase(),
                            fontSize = 36.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Upload spinner overlay
                    if (isUploadingPhoto) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.Black.copy(0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                }

                // Camera badge
                Box(
                    modifier = Modifier
                        .offset(x = 32.dp, y = (-18).dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isUploadingPhoto) Color.Gray else NavyBlue)
                        .border(2.dp, Color.White, CircleShape)
                        .clickable(enabled = !isUploadingPhoto) { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.CameraAlt, null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    user?.displayName ?: "Student",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    user?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(0.75f)
                )

                // Programme pill — read-only
                if (!user?.department.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ProgrammeChipBg)
                            .border(1.dp, ProgrammeChipBorder, RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Filled.School, null,
                            modifier = Modifier.size(13.dp),
                            tint = ProgrammeChipBorder
                        )
                        Text(
                            user!!.department,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ProgrammeChipText
                        )
                    }
                }

                // Year of Study pill — read-only
                if (user?.yearOfStudy != null && user!!.yearOfStudy > 0) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ProgrammeChipBg)
                            .border(1.dp, ProgrammeChipBorder, RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Filled.CalendarMonth, null,
                            modifier = Modifier.size(13.dp),
                            tint = ProgrammeChipBorder
                        )
                        Text(
                            "Year ${user!!.yearOfStudy}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ProgrammeChipText
                        )
                    }
                }

                // Bio
                if (!user?.bio.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        user!!.bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(0.65f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }

                // Admin chip
                if (user?.isAdmin == true) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(AdminChipBg)
                            .border(1.dp, AdminChipBorder, RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.AdminPanelSettings, null,
                            modifier = Modifier.size(14.dp),
                            tint = AdminChipIcon
                        )
                        Text(
                            "Admin",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AdminChipText
                        )
                    }
                }
            }
        }

        // ── Scrollable Content ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Stats row
            user?.let { u ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        icon = Icons.Outlined.CalendarMonth,
                        label = "Year",
                        value = if (u.yearOfStudy > 0) "Year ${u.yearOfStudy}" else "—",
                        accentColor = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Outlined.MenuBook,
                        label = "Courses",
                        value = if (u.courses.isNotEmpty()) u.courses.size.toString() else "—",
                        accentColor = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f)
                    )
                    // CHANGE 1: Bookmark icon instead of Favorite, green accent
                    StatCard(
                        icon = Icons.Outlined.Bookmark,
                        label = "Saved",
                        value = u.savedNotices.size.toString(),
                        accentColor = Color(0xFF059669),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Enrolled Courses card ─────────────────────────────────────────
            // Shows the courses the student has declared; each appears as a
            // dismissible chip. Tapping "Edit Profile" opens the sheet where
            // they can add or remove courses.
            val enrolledCourses = user?.courses ?: emptyList()
            SectionCard("My Courses") {
                if (enrolledCourses.isEmpty()) {
                    ListItem(
                        headlineContent = {
                            Text(
                                "No courses added yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                "Tap Edit Profile to add your enrolled course units.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0284C7).copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.MenuBook, null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        modifier = Modifier.clickable { showEditSheet = true },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Wrap chips in a flow-like row using multiple rows
                        val chunked = enrolledCourses.chunked(3)
                        chunked.forEach { rowCourses ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowCourses.forEach { course ->
                                    CourseChip(course)
                                }
                            }
                        }
                        TextButton(
                            onClick = { showEditSheet = true },
                            modifier = Modifier
                                .align(Alignment.End)
                                .height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                Icons.Filled.Edit, null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Edit courses",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // Account section
            SectionCard("Account") {
                ProfileMenuItem(
                    icon = Icons.Outlined.Person,
                    iconTint = NavyBlue,
                    label = "Edit Profile"
                ) { showEditSheet = true }

                HorizontalDivider(Modifier.padding(start = 64.dp, end = 16.dp))

                ProfileMenuItem(
                    icon = Icons.Outlined.Badge,
                    iconTint = Color(0xFF7C3AED),
                    label = "Student ID: ${user?.studentId?.ifBlank { "Not set" } ?: "—"}"
                ) { showEditSheet = true }
            }

            // Preferences section
            SectionCard("Preferences") {
                DarkModeToggle(
                    systemDark = systemDark,
                    manualDark = manualDark,
                    onToggle = { AppThemeState.setDark(it) }
                )
                HorizontalDivider(Modifier.padding(start = 64.dp, end = 16.dp))
                PreferenceToggle(
                    icon = Icons.Outlined.Notifications,
                    iconTint = Color(0xFFEA580C),
                    label = "Push Notifications",
                    checked = notificationsEnabled,
                    onToggle = {
                        notificationsEnabled = it
                        vm.updateNotificationsEnabled(it)
                    }
                )
            }

            // About section
            SectionCard("About") {
                ProfileMenuItem(
                    icon = Icons.Outlined.Info,
                    iconTint = Color(0xFF0891B2),
                    label = "App Version 2.0.0",
                    showChevron = false
                ) { }

                HorizontalDivider(Modifier.padding(start = 64.dp, end = 16.dp))

                ProfileMenuItem(
                    icon = Icons.Outlined.PrivacyTip,
                    iconTint = Color(0xFF059669),
                    label = "Privacy Policy"
                ) { showPrivacySheet = true }

                HorizontalDivider(Modifier.padding(start = 64.dp, end = 16.dp))

                ProfileMenuItem(
                    icon = Icons.Outlined.Help,
                    iconTint = Color(0xFF7C3AED),
                    label = "Help & Support"
                ) {
                    val email = adminEmail ?: return@ProfileMenuItem
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$email")
                        putExtra(Intent.EXTRA_SUBJECT, "Campus Connect — Help & Support")
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Student: ${user?.displayName ?: ""}\n" +
                                    "ID: ${user?.studentId ?: ""}\n" +
                                    "Programme: ${user?.department ?: ""}\n\n" +
                                    "Issue:\n"
                        )
                    }
                    context.startActivity(intent)
                }
            }

            Spacer(Modifier.height(4.dp))

            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Filled.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Course chip ───────────────────────────────────────────────────────────────
@Composable
private fun CourseChip(code: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CourseChipBg)
            .border(1.dp, CourseChipBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            Icons.Filled.Book, null,
            modifier = Modifier.size(12.dp),
            tint = CourseChipBorder
        )
        Text(
            code,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = CourseChipText
        )
    }
}

// ── Edit Profile Bottom Sheet ─────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(
    currentName:      String,
    currentBio:       String,
    currentStudentId: String,
    currentYear:      Int,
    currentCourses:   List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, bio: String, studentId: String, year: Int, courses: List<String>) -> Unit
) {
    var name      by remember { mutableStateOf(currentName) }
    var bio       by remember { mutableStateOf(currentBio) }
    var studentId by remember { mutableStateOf(currentStudentId) }
    var yearText  by remember { mutableStateOf(if (currentYear > 0) currentYear.toString() else "") }

    // Courses managed as a mutable list so the UI reacts immediately
    val courseList = remember { mutableStateListOf(*currentCourses.toTypedArray()) }
    var newCourseInput by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Edit Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── Basic info ────────────────────────────────────────────────────
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display Name") },
                leadingIcon = { Icon(Icons.Outlined.Person, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                leadingIcon = { Icon(Icons.Outlined.Info, null) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = studentId,
                onValueChange = { studentId = it },
                label = { Text("Student ID") },
                leadingIcon = { Icon(Icons.Outlined.Badge, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = yearText,
                onValueChange = { if (it.length <= 1 && it.all(Char::isDigit)) yearText = it },
                label = { Text("Year of Study") },
                leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text("e.g. 2") }
            )

            // ── Programme note — read-only info ───────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF7C3AED).copy(alpha = 0.07f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Outlined.Info, null,
                    tint = Color(0xFF7C3AED),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    "Your programme is set at registration and cannot be changed here. " +
                            "Contact the registrar's office for corrections.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4C1D95)
                )
            }

            HorizontalDivider()

            // ── Enrolled Courses ──────────────────────────────────────────────
            // Students add the course unit codes they are currently enrolled in.
            // These codes are used by admins when targeting notices and events.
            Text(
                "Enrolled Course Units",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                "Add every course code you are currently enrolled in (e.g. CS101, MATH201). " +
                        "Admins use these to send notices directly to students in a specific course.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Input row for adding a new course
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newCourseInput,
                    onValueChange = { newCourseInput = it.uppercase() },
                    label = { Text("Course Code") },
                    leadingIcon = { Icon(Icons.Outlined.Add, null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("e.g. CS101") }
                )
                FilledTonalButton(
                    onClick = {
                        val code = newCourseInput.trim().uppercase()
                        if (code.isNotBlank() && !courseList.contains(code)) {
                            courseList.add(code)
                        }
                        newCourseInput = ""
                    },
                    enabled = newCourseInput.isNotBlank(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Add")
                }
            }

            // Current course chips with remove button
            if (courseList.isNotEmpty()) {
                // Display each course as a chip with a remove (×) button
                courseList.toList().forEach { course ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CourseChipBg)
                            .border(1.dp, CourseChipBorder, RoundedCornerShape(12.dp))
                            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Book, null,
                                tint = CourseChipBorder,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                course,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = CourseChipText
                            )
                        }
                        IconButton(
                            onClick = { courseList.remove(course) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close, "Remove $course",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else {
                // Empty state hint
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Outlined.MenuBook, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "No courses added. Use the field above to add your enrolled units.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Action buttons ────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Cancel") }

                Button(
                    onClick = {
                        onSave(
                            name.trim(),
                            bio.trim(),
                            studentId.trim(),
                            yearText.toIntOrNull() ?: 1,
                            courseList.toList()
                        )
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B5BDB),
                        contentColor   = Color.White
                    ),
                    enabled = name.isNotBlank()
                ) { Text("Save", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ── Dark Mode Toggle ──────────────────────────────────────────────────────────
@Composable
private fun DarkModeToggle(
    systemDark: Boolean,
    manualDark: Boolean,
    onToggle: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = {
            Text("Dark Mode", style = MaterialTheme.typography.bodyMedium)
        },
        supportingContent = {
            if (systemDark) {
                Text(
                    "Controlled by system settings",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF7C3AED).copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.DarkMode, null,
                    tint = Color(0xFF7C3AED), modifier = Modifier.size(20.dp))
            }
        },
        trailingContent = {
            Switch(
                checked = systemDark || manualDark,
                onCheckedChange = { if (!systemDark) onToggle(it) },
                enabled = !systemDark,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = NavyBlue
                )
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

// ── Supporting Composables ────────────────────────────────────────────────────

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = accentColor, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.1.sp,
            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
        )
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    label: String,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
        },
        trailingContent = {
            if (showChevron) {
                Icon(Icons.Filled.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        },
        modifier = if (showChevron) Modifier.clickable(onClick = onClick) else Modifier,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun PreferenceToggle(
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = NavyBlue
                )
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

// ── Privacy Policy Bottom Sheet ───────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrivacyPolicySheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Box(
                    Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Privacy Policy",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Last updated: May 2026",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PrivacySection(
                title = "1. Information We Collect",
                body = "We collect information you provide directly when you register an account, " +
                        "including your name, email address, student ID, programme, year of study, " +
                        "and the course units you enrol in. We also collect profile photos you " +
                        "choose to upload."
            )
            PrivacySection(
                title = "2. How We Use Your Information",
                body = "Your information is used solely to operate Campus Connect. Specifically:\n\n" +
                        "• Your course enrolments determine which notices and timetable entries are " +
                        "shown to you — only content relevant to your courses is displayed.\n\n" +
                        "• Your email address is used for account authentication and to receive " +
                        "password reset links.\n\n" +
                        "• Your name and student ID are displayed on your profile and included in " +
                        "support emails you choose to send."
            )
            PrivacySection(
                title = "3. Data Storage",
                body = "Your data is stored securely on Google Firebase (Firestore and Firebase " +
                        "Storage), hosted in Google's cloud infrastructure. Profile photos are " +
                        "stored on Cloudinary. Both services apply industry-standard encryption " +
                        "at rest and in transit."
            )
            PrivacySection(
                title = "4. Data Sharing",
                body = "We do not sell, trade, or share your personal information with third " +
                        "parties. Your data is only accessible to:\n\n" +
                        "• You — through your own profile.\n\n" +
                        "• The campus administrator — who can see aggregate statistics (total " +
                        "user count) but cannot view individual student profiles or course lists."
            )
            PrivacySection(
                title = "5. Push Notifications",
                body = "If you enable push notifications, your device's FCM token is stored to " +
                        "deliver relevant campus announcements. You can disable notifications at " +
                        "any time from your profile settings."
            )
            PrivacySection(
                title = "6. Crash Reporting",
                body = "We use Firebase Crashlytics to automatically collect crash reports when " +
                        "the app encounters an unexpected error. These reports contain technical " +
                        "device information and the state of the app at the time of the crash — " +
                        "they do not include your name, email, or any personal identifiers."
            )
            PrivacySection(
                title = "7. Your Rights",
                body = "You may request deletion of your account and all associated data at any " +
                        "time by contacting the campus administrator through the Help & Support " +
                        "option in your profile. Account deletion is permanent and cannot be undone."
            )
            PrivacySection(
                title = "8. Changes to This Policy",
                body = "We may update this privacy policy from time to time. Any significant " +
                        "changes will be communicated through the app. Continued use of Campus " +
                        "Connect after changes are posted constitutes acceptance of the updated policy."
            )
            PrivacySection(
                title = "9. Contact",
                body = "If you have questions about this privacy policy or how your data is " +
                        "handled, please contact the campus administrator using the " +
                        "Help & Support option in your profile settings."
            )
        }
    }
}

@Composable
private fun PrivacySection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )
    }
}