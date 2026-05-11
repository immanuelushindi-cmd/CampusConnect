<p align="center">
  <img src="app/src/main/res/drawable/cc_logo.png" width="120" alt="Campus Connect Logo"/>
</p>

# Campus Connect — v2.0.0

> A production-grade Android campus hub built with Jetpack Compose, Firebase, and Clean Architecture.

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF?logo=kotlin)
![Compose BOM](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.12.01-4285F4?logo=jetpackcompose)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%7C%20Firestore%20%7C%20FCM%20%7C%20Storage%20%7C%20Analytics%20%7C%20Crashlytics-FFCA28?logo=firebase)
![Architecture](https://img.shields.io/badge/Architecture-Clean%20MVVM%20%2B%20Hilt-00C853)
![Min SDK](https://img.shields.io/badge/minSdk-26%20(Android%208)-green)
![Target SDK](https://img.shields.io/badge/targetSdk-35-blue)

---

## 📱 What is Campus Connect?

Campus Connect replaces scattered WhatsApp groups, physical notice boards, and clunky student portals with a **single, real-time app** built for campus life. Students get personalised notices, an interactive timetable, upcoming events, and push notifications. Admins get a full management panel.

---

## ✨ Features

| Feature | Details |
|---|---|
| 🔔 Real-time Notices | Firestore live updates → Room cache → UI (offline-first) |
| 🔍 Search & Filter | Full-text search, category chips, saved filter |
| 📋 Notice Detail | Dedicated screen with priority badge, image, tags, save toggle |
| 📅 Timetable | Day-strip selector, filtered by the student's own courses |
| 🎟️ Events | Month-grouped list, RSVP + attendee count, map deep-link, online badge |
| 🔎 Event Search | Real-time search across title, description, and location |
| 🛡️ Admin Panel | Post/edit/delete notices, events, timetable entries; manage courses & lecturers |
| 👤 Profile | Display name, bio, department, year, photo upload, notification toggle |
| 🔑 Forgot Password | Email reset dialog on login screen with success/error feedback |
| 💾 Offline-first | Room cache — core content works without internet |
| 🌙 Dark Mode | Full Material3 dark/light theme, persisted via DataStore |
| 📲 Push Notifications | FCM with 4 channels: Urgent, Timetable, Events, General |
| 🧪 Unit Tests | 6 ViewModel tests with Mockito + StandardTestDispatcher |

---

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                      Jetpack Compose UI                          │
│  Splash → Login/Register → Dashboard → Notices → NoticeDetail   │
│  Timetable → Events → Profile → Admin                           │
├──────────────────────────────────────────────────────────────────┤
│            ViewModels (Hilt @HiltViewModel + StateFlow)          │
│  Auth · Profile · Dashboard · Notices · NoticeDetail            │
│  Timetable · Events · Admin · AdminDashboard · CourseManagement  │
├─────────────────────────┬────────────────────────────────────────┤
│      Domain Layer       │  Use Cases                            │
│  Notice / Event /       │  GetNoticesUseCase                    │
│  TimetableEntry /       │  GetNoticesByCategoryUseCase          │
│  CourseEntry /          │  GetSavedNoticesUseCase               │
│  LecturerEntry / User   │  SearchNoticesUseCase                 │
├─────────────────────────┴────────────────────────────────────────┤
│                     Repository Layer                             │
│  AuthRepo · NoticeRepo · TimetableRepo · EventRepo              │
│  CourseRepo · LecturerRepo                                      │
├──────────────────────┬───────────────────────────────────────────┤
│   Local (Room DB)    │  Remote (Firestore realtime)             │
│   NoticeEntity       │  /notices/{id}                           │
│   TimetableEntity    │  /timetable/{id}                         │
│   EventEntity        │  /events/{id}                            │
│                      │  /users/{uid}                            │
│                      │  /usernames/{username}                   │
│                      │  /courses/{id}                           │
│                      │  /lecturers/{id}                         │
└──────────────────────┴───────────────────────────────────────────┘
```

---

## 📦 Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0.0 |
| UI | Jetpack Compose + Material3 (BOM 2024.12.01) |
| DI | Hilt 2.51.1 |
| Async | Kotlin Coroutines + Flow |
| Local DB | Room 2.6.1 |
| Remote | Firebase Firestore (realtime), Auth, FCM, Storage, Analytics, Crashlytics |
| Google Sign-In | androidx.credentials 1.5.0 + google-identity 1.1.1 (Credential Manager API) |
| Image loading | Coil 2.6.0 + Lottie 6.4.0 |
| Image hosting | Cloudinary — uploaded via Retrofit 2.11.0 + OkHttp 4.12.0 multipart POST |
| Splash screen | androidx.core:core-splashscreen 1.0.1 |
| Theme persistence | DataStore Preferences 1.1.1 |
| Permissions | Accompanist Permissions 0.34.0 |
| Testing | Mockito + kotlinx-coroutines-test (StandardTestDispatcher) |

---

## 🚀 Build & Run

### Prerequisites

| Tool | Version |
|---|---|
| Android Studio | Iguana 2023.2+ |
| JDK | 17+ |
| Android SDK | API 35 |
| Firebase Account | Free Spark plan works |
| Cloudinary Account | Free plan works |

### Step 1 — Clone & Open

```bash
git clone https://github.com/immanuelushindi-cmd/CampusConnect.git
cd CampusConnect
# Open in Android Studio: File → Open → select folder
```

### Step 2 — Firebase Setup (~5 minutes)

1. Go to [Firebase Console](https://console.firebase.google.com) → **Create Project**
2. **Add Android app** — package name: `com.campusconnect.app`
3. Enable the following services:
   - **Authentication** → Email/Password **and** Google
   - **Firestore Database** → Start in test mode
   - **Cloud Messaging** (enabled by default)
   - **Storage** → Start in test mode
   - **Analytics** and **Crashlytics** (optional but recommended)
4. Download **`google-services.json`** → place it inside `app/`
5. Copy your **Web client ID** from:
   `Firebase Console → Authentication → Sign-in method → Google → Web SDK configuration`
6. Create `local.properties` in the project root (if it doesn't exist) and add:

```properties
GOOGLE_WEB_CLIENT_ID=YOUR_WEB_CLIENT_ID_HERE
```

> ⚠️ `local.properties` is git-ignored. Never commit it. The build will error if this key is missing.

### Step 3 — Cloudinary Setup (~3 minutes)

Image uploads (notice images, profile photos) go through Cloudinary using an **unsigned upload preset**.

1. Sign up at [cloudinary.com](https://cloudinary.com) (free plan is sufficient)
2. From your dashboard note your **Cloud name**
3. Go to **Settings → Upload → Upload presets** → click **Add upload preset**
   - Set **Signing mode** to **Unsigned**
   - Give it a name (e.g. `CampusConnect`)
4. Open `app/src/main/java/com/campusconnect/app/data/remote/CloudinaryApi.kt` and replace the cloud name in the POST path:

```text
@POST("v1_1/YOUR_CLOUD_NAME/image/upload")
```

5. Open `CloudinaryService.kt` and replace the upload preset string:

```text
val uploadPreset = "YOUR_PRESET_NAME"
    .toRequestBody("text/plain".toMediaTypeOrNull())
```

> ⚠️ Do not hardcode production credentials in version control. Consider moving these values to `local.properties` and reading them via `BuildConfig` fields (same pattern used for `GOOGLE_WEB_CLIENT_ID`).

### Step 4 — Deploy Firebase Rules & Seed Data (optional)

```bash
npm install -g firebase-tools
firebase login
chmod +x scripts/firebase_deploy.sh
./scripts/firebase_deploy.sh YOUR_PROJECT_ID
```

### Step 5 — Build & Run

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Or press **▶ Run** in Android Studio.

---

## 🗂️ Project Structure

```
app/src/main/java/com/campusconnect/app/
├── CampusConnectApp.kt              # Application class + FCM notification channels
├── MainActivity.kt                  # Entry point + bottom navigation host
│
├── data/
│   ├── local/
│   │   ├── dao/Daos.kt              # Room DAOs: NoticeDao, TimetableDao, EventDao
│   │   ├── database/CampusDatabase.kt
│   │   └── entity/Entities.kt       # NoticeEntity, TimetableEntity, EventEntity
│   ├── remote/
│   │   ├── FirebaseService.kt       # Firestore + FCM + Auth integration
│   │   ├── CloudinaryService.kt     # Image upload via Retrofit multipart POST
│   │   ├── CloudinaryApi.kt         # Retrofit interface — update cloud name here
│   │   └── CloudinaryResponse.kt
│   └── repository/Repositories.kt  # AuthRepo, NoticeRepo, TimetableRepo,
│                                    # EventRepo, CourseRepo, LecturerRepo
│
├── domain/
│   ├── model/
│   │   ├── DomainModels.kt          # Notice, Event, TimetableEntry, User,
│   │   │                            # CourseEntry, LecturerEntry,
│   │   │                            # NoticeCategory, NoticePriority
│   │   └── NoticeExtensions.kt
│   └── usecase/UseCases.kt          # GetNoticesUseCase, SearchNoticesUseCase,
│                                    # GetSavedNoticesUseCase, GetNoticesByCategoryUseCase
│
├── di/Modules.kt                    # Hilt DI bindings
│
├── navigation/
│   ├── AppNavHost.kt                # Nav graph + composable destinations
│   └── Routes.kt                    # Route constants + noticeDetailRoute()
│
├── service/CampusFCMService.kt      # FCM message handler (4 channels)
│
├── ui/
│   ├── components/Components.kt     # GlassCard, NoticeCard, ShimmerBox,
│   │                                # EmptyState, SyncErrorBanner,
│   │                                # CampusBottomBar, PulseDot, CampusLoadingIndicator
│   ├── screens/
│   │   ├── auth/
│   │   │   ├── LoginScreen.kt       # Username + password login; Google (Credential Manager);
│   │   │   │                        # Forgot Password dialog with email reset
│   │   │   └── RegisterScreen.kt    # Username, email (format-validated), password, name;
│   │   │                            # Google Sign-Up (Credential Manager)
│   │   ├── dashboard/DashboardScreen.kt    # Recent notices, upcoming events, next class,
│   │   │                                   # stats strip; "Admin Panel" FAB for admins
│   │   ├── notices/
│   │   │   ├── NoticesScreen.kt     # Search bar, category chips, saved filter, pull-to-refresh
│   │   │   └── NoticeDetailScreen.kt
│   │   ├── timetable/TimetableScreen.kt
│   │   ├── events/EventsScreen.kt   # Real-time search bar, month-grouped list, RSVP
│   │   ├── profile/ProfileScreen.kt # Photo upload (with loading spinner), edit profile,
│   │   │                            # notification toggle, sign-out
│   │   ├── admin/
│   │   │   ├── AdminScreen.kt              # Admin hub / entry point
│   │   │   ├── AdminDashboardScreen.kt     # Live stats: notice count, event count, user count
│   │   │   ├── AdminNoticesScreen.kt       # List + delete with confirmation dialog
│   │   │   ├── AdminNoticeShared.kt        # Shared post/edit notice form
│   │   │   ├── AdminEventsScreen.kt        # List + delete with confirmation dialog
│   │   │   ├── AdminPostEventScreen.kt     # Post / edit event form
│   │   │   ├── AdminTimetableScreen.kt     # List + delete with confirmation dialog
│   │   │   ├── AdminPostTimetableScreen.kt # Post / edit timetable entry form
│   │   │   ├── AdminCoursesScreen.kt       # CRUD for CourseEntry + LecturerEntry
│   │   │   └── AdminProfileScreen.kt
│   │   └── splash/SplashScreen.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       ├── Type.kt
│       ├── ThemePreferences.kt      # DataStore-backed theme preference
│       └── AppThemeState.kt         # App-wide dark mode StateFlow singleton
│
├── utils/
│   ├── UiState.kt                   # UiState<T> (Idle/Loading/Success/Error)
│   │                                # + SyncState (Idle/Loading/Success/Error)
│   ├── DateFormatters.kt            # Centralised java.time formatters (API 26+)
│   └── mappers/Mappers.kt           # Entity ↔ Domain ↔ Firestore map functions
│
└── viewmodel/ViewModels.kt          # All 10 ViewModels in one file
```

---

## 🔐 Authentication Flow

Users **sign in with a username**, not directly with an email address.

- A `usernames` Firestore collection maps each username → the user's email
- On sign-in, `AuthRepository` looks up the email for the given username, then calls Firebase Auth
- On sign-up, the username is validated (3–30 chars, letters/digits/underscores/dots only) and stored in both `usernames/{username}` and `users/{uid}`
- **Google Sign-In** uses the **Credential Manager API** (`androidx.credentials 1.5.0`). New Google users receive an auto-generated username derived from their display name + the first 6 characters of their UID, stored in the `usernames` collection
- **Forgot Password** — available from the login screen. The user enters their email; a Firebase password reset link is sent. The dialog shows a clear success or error message (including "no account found" and "invalid email" cases)

---

## 🛡️ Admin Access

Admin access is granted to any user whose Firestore `users/{uid}` document has `username == "admin"`. This is checked at sign-in via `AuthRepository.isAdmin()`.

To create an admin account:
1. Register normally in the app
2. In the Firebase Console, open `users/{uid}` and set the `username` field to `"admin"`

> The `ADMIN_DOMAIN_1` / `ADMIN_DOMAIN_2` build config fields in `app/build.gradle.kts` are reserved for future email-domain access control and are **not** currently used for admin detection.

---

## 📋 Domain Models

| Model | Key Fields |
|---|---|
| `Notice` | id, title, content, category, priority, author, authorEmail, authorPhotoUrl, imageUrl, tags, viewCount, isActive, createdAt, targetDepartment, targetCourse, targetYearOfStudy, **isSaved**, isDeleted |
| `Event` | id, title, description, location, startTimestampMs, maxAttendees, isOnline, meetLink, organizer, imageUrl, latitude, longitude, isActive, rsvpCount, createdAt, targetDepartment, targetCourse, targetYearOfStudy, isAttending, attendeeCount |
| `TimetableEntry` | id, courseCode, courseName, lecturer, room, building, dayOfWeek (0=Mon…6=Sun), startHour, startMinute, endHour, endMinute, colorHex, semester, notes, targetDepartment, targetYearOfStudy |
| `User` | uid, email, displayName, photoUrl, bio, department, yearOfStudy, studentId, courses, role, isAdmin, notificationsEnabled, savedNotices, rsvpEvents |
| `CourseEntry` | id (= course code), code, name |
| `LecturerEntry` | id, name, lecturerId, courseIds |

**NoticeCategory enum:** `GENERAL`, `ACADEMIC`, `EVENTS`, `SPORTS`, `FINANCE`, `HEALTH`, `CAREERS`, `EMERGENCY`

**NoticePriority enum:** `LOW`, `MEDIUM`, `HIGH`, `URGENT`

---

## 🧪 Running Tests

```bash
./gradlew test
```

Six unit tests in `NoticesViewModelTest.kt` covering:

- Initial state loads all notices
- Sync state starts as `Loading`
- `setSearch` updates `searchQuery`
- `setCategory` updates `selectedCategory`
- Clearing category resets it to `null`
- `toggleSavedFilter` flips `showSavedOnly`

---

## 📦 What Changed in v2.0

| Improvement | Detail |
|---|---|
| ✅ Clean Architecture | Domain models separated from Room entities via a mapper layer |
| ✅ Use Cases | `GetNoticesUseCase`, `SearchNoticesUseCase`, `GetSavedNoticesUseCase`, `GetNoticesByCategoryUseCase` |
| ✅ `UiState<T>` + `SyncState` | Two unified sealed classes replacing ad-hoc state management |
| ✅ Notice Detail screen | Dedicated screen with GlassCard layout, tags, priority badge, save toggle |
| ✅ Skeleton loading | `ShimmerBox` on first load instead of a plain spinner |
| ✅ `GlassCard` | Press-responsive scale + elevation animation |
| ✅ Event search | Real-time search bar across title, description, and location |
| ✅ Saved (not Favourite) | Unified terminology across the whole codebase |
| ✅ Per-user saved state | Stored in `users/{uid}/savedNotices`; never bleeds between accounts |
| ✅ Forgot Password | Email reset dialog with proper success/error messages |
| ✅ Admin Panel FAB | Label changed from "Post Notice" to "Admin Panel"; icon updated |
| ✅ Email format validation | Register button disabled until email contains `@` and `.` |
| ✅ Google Sign-In modernised | Migrated from deprecated `GoogleSignInOptions` to Credential Manager API |
| ✅ Google users get usernames | Auto-generated and stored in the `usernames` Firestore collection |
| ✅ Username validation | 3–30 chars, letters/digits/underscores/dots — prevents Firestore path injection |
| ✅ Photo upload feedback | Spinner overlays avatar during Cloudinary upload; controls disabled to prevent double-tap |
| ✅ Delete confirmations | Confirmation dialog before deleting any notice, event, or timetable entry |
| ✅ `deleteNotice` race fix | Room deletion only runs after Firestore confirms success |
| ✅ `targetYearOfStudy` fix | Field now included in the Firestore map when posting a notice |
| ✅ Build artifacts removed | `app/build/` removed from Git tracking |
| ✅ Unit tests fixed | Constructor signature, category names, and priority values aligned with actual code |

---

## 🧹 Known Housekeeping

| Item | Detail |
|---|---|
| ⚠️ Unused dependency | `com.cloudinary:cloudinary-android:2.3.1` is declared in `build.gradle.kts` but never imported or used — image uploads go through the Retrofit `CloudinaryApi` instead. Safe to remove. |
| ⚠️ Unused dependency | `work-runtime-ktx` and `hilt-work` are declared but no `Worker` subclass exists in the codebase. Safe to remove unless WorkManager support is planned. |
| ⚠️ Cloudinary credentials in source | The cloud name and upload preset are currently hardcoded in `CloudinaryApi.kt` and `CloudinaryService.kt`. Move them to `local.properties` + `BuildConfig` to keep them out of version control. |