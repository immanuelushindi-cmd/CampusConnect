package com.campusconnect.app.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.campusconnect.app.ui.screens.admin.AdminCoursesScreen
import com.campusconnect.app.ui.screens.admin.AdminDashboardScreen
import com.campusconnect.app.ui.screens.admin.AdminEventsScreen
import com.campusconnect.app.ui.screens.admin.AdminNoticesScreen
import com.campusconnect.app.ui.screens.admin.AdminPostEventScreen
import com.campusconnect.app.ui.screens.admin.AdminPostTimetableScreen
import com.campusconnect.app.ui.screens.admin.AdminProfileScreen
import com.campusconnect.app.ui.screens.admin.AdminScreen
import com.campusconnect.app.ui.screens.admin.AdminTimetableScreen
import com.campusconnect.app.ui.screens.auth.LoginScreen
import com.campusconnect.app.ui.screens.auth.RegisterScreen
import com.campusconnect.app.ui.screens.dashboard.DashboardScreen
import com.campusconnect.app.ui.screens.events.EventsScreen
import com.campusconnect.app.ui.screens.notices.NoticeDetailScreen
import com.campusconnect.app.ui.screens.notices.NoticesScreen
import com.campusconnect.app.ui.screens.profile.ProfileScreen
import com.campusconnect.app.ui.screens.splash.SplashScreen
import com.campusconnect.app.ui.screens.timetable.TimetableScreen
import com.campusconnect.app.viewmodel.AuthViewModel


// ─── Transition constants ─────────────────────────────────────────────────────
private const val TRANSITION_DURATION = 300

private val tabEnter  = fadeIn(tween(TRANSITION_DURATION))
private val tabExit   = fadeOut(tween(TRANSITION_DURATION))
private val pushEnter = slideInHorizontally(tween(TRANSITION_DURATION)) { it / 8 } +
        fadeIn(tween(TRANSITION_DURATION))
private val pushExit  = slideOutHorizontally(tween(TRANSITION_DURATION)) { -it / 8 } +
        fadeOut(tween(TRANSITION_DURATION))
private val popEnter  = slideInHorizontally(tween(TRANSITION_DURATION)) { -it / 8 } +
        fadeIn(tween(TRANSITION_DURATION))
private val popExit   = slideOutHorizontally(tween(TRANSITION_DURATION)) { it / 8 } +
        fadeOut(tween(TRANSITION_DURATION))



@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = ROUT_SPLASH
) {
    NavHost(
        navController       = navController,
        startDestination    = startDestination,
        modifier            = modifier,
        enterTransition     = { pushEnter },
        exitTransition      = { pushExit },
        popEnterTransition  = { popEnter },
        popExitTransition   = { popExit }
    ) {

        // ── Splash ────────────────────────────────────────────────────────────
        composable(
            route           = ROUT_SPLASH,
            enterTransition = { fadeIn(tween(500)) },
            exitTransition  = { fadeOut(tween(400)) }
        ) {
            val authVm: AuthViewModel = hiltViewModel()
            val user by authVm.currentUserFlow.collectAsStateWithLifecycle(null)

            SplashScreen(
                onNavigateToAuth = {
                    navController.navigate(ROUT_AUTH) {
                        popUpTo(ROUT_SPLASH) { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    val destination = if (user?.isAdmin == true) ROUT_ADMIN_DASHBOARD
                    else ROUT_DASHBOARD
                    navController.navigate(destination) {
                        popUpTo(ROUT_SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // ── Login (ROUT_AUTH kept as "auth" for back-compat with logout calls) ─
        composable(ROUT_AUTH) {
            LoginScreen(
                onLoginSuccess = { isAdmin ->
                    val destination = if (isAdmin) ROUT_ADMIN_DASHBOARD else ROUT_DASHBOARD
                    navController.navigate(destination) {
                        popUpTo(ROUT_AUTH) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(ROUT_REGISTER)
                }
            )
        }

        // ── Register ──────────────────────────────────────────────────────────
        composable(
            route              = ROUT_REGISTER,
            enterTransition    = { pushEnter },
            exitTransition     = { pushExit },
            popEnterTransition = { popEnter },
            popExitTransition  = { popExit }
        ) {
            RegisterScreen(
                onRegisterSuccess = { isAdmin ->
                    val destination = if (isAdmin) ROUT_ADMIN_DASHBOARD else ROUT_DASHBOARD
                    navController.navigate(destination) {
                        popUpTo(ROUT_AUTH) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // ════════════════════════════════════════════════════════════════════
        // STUDENT routes (bottom-bar tabs)
        // ════════════════════════════════════════════════════════════════════

        composable(
            route           = ROUT_DASHBOARD,
            enterTransition = { tabEnter },
            exitTransition  = { tabExit }
        ) {
            DashboardScreen(
                onNavigateToNotices   = { navController.navigate(ROUT_NOTICES) },
                onNavigateToTimetable = { navController.navigate(ROUT_TIMETABLE) },
                onNavigateToEvents    = { navController.navigate(ROUT_EVENTS) },
                onNavigateToProfile   = { navController.navigate(ROUT_PROFILE) },
                onNavigateToAdmin     = { navController.navigate(ROUT_ADMIN) }
            )
        }

        composable(
            route           = ROUT_NOTICES,
            enterTransition = { tabEnter },
            exitTransition  = { tabExit }
        ) {
            NoticesScreen(
                onBack = { navController.popBackStack() },
                onNoticeClick = { noticeId ->
                    navController.navigate(noticeDetailRoute(noticeId))
                }
            )
        }

        composable(
            route     = ROUT_NOTICE_DETAIL,
            arguments = listOf(navArgument("noticeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noticeId = backStackEntry.arguments?.getString("noticeId")
                ?: return@composable
            NoticeDetailScreen(
                noticeId = noticeId,
                onBack   = { navController.popBackStack() }
            )
        }

        composable(
            route           = ROUT_TIMETABLE,
            enterTransition = { tabEnter },
            exitTransition  = { tabExit }
        ) {
            TimetableScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route           = ROUT_EVENTS,
            enterTransition = { tabEnter },
            exitTransition  = { tabExit }
        ) {
            EventsScreen(
                onBack      = { navController.popBackStack() },
                onPostEvent = { navController.navigate(ROUT_ADMIN_POST_EVENT) }
            )
        }

        composable(
            route           = ROUT_PROFILE,
            enterTransition = { tabEnter },
            exitTransition  = { tabExit }
        ) {
            ProfileScreen(
                onBack   = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(ROUT_AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ════════════════════════════════════════════════════════════════════
        // ADMIN routes (admin bottom-bar tabs + push screens)
        // ════════════════════════════════════════════════════════════════════

        composable(
            route           = ROUT_ADMIN_DASHBOARD,
            enterTransition = { tabEnter },
            exitTransition  = { tabExit }
        ) {
            AdminDashboardScreen(
                onNavigateToPostNotice    = { navController.navigate(ROUT_ADMIN_POST_NOTICE) },
                onNavigateToPostEvent     = { navController.navigate(ROUT_ADMIN_POST_EVENT) },
                onNavigateToPostTimetable = { navController.navigate(ROUT_ADMIN_POST_TIMETABLE) },
                onNavigateToEvents        = { navController.navigate(ROUT_ADMIN_EVENTS) },
                onNavigateToProfile       = { navController.navigate(ROUT_ADMIN_PROFILE) },
                onNavigateToAdminNotices  = { navController.navigate(ROUT_ADMIN_NOTICES) },
                onManageCourses           = { navController.navigate(ROUT_ADMIN_COURSES) }
            )
        }

        // Admin Notices tab — shows list of all posted notices with FAB to post new
        composable(
            route           = ROUT_ADMIN_NOTICES,
            enterTransition = { tabEnter },
            exitTransition  = { tabExit }
        ) {
            AdminNoticesScreen(
                onPostNotice = { navController.navigate(ROUT_ADMIN_POST_NOTICE) }
            )
        }

        // Admin Post Notice form (push screen)
        composable(
            route              = ROUT_ADMIN_POST_NOTICE,
            enterTransition    = { pushEnter },
            exitTransition     = { pushExit },
            popEnterTransition = { popEnter },
            popExitTransition  = { popExit }
        ) {
            AdminScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route           = ROUT_ADMIN_EVENTS,
            enterTransition = { tabEnter },
            exitTransition  = { tabExit }
        ) {
            AdminEventsScreen(
                onPostEvent = { navController.navigate(ROUT_ADMIN_POST_EVENT) }
            )
        }

        composable(
            route              = ROUT_ADMIN_POST_EVENT,
            enterTransition    = { pushEnter },
            exitTransition     = { pushExit },
            popEnterTransition = { popEnter },
            popExitTransition  = { popExit }
        ) {
            AdminPostEventScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route           = ROUT_ADMIN_TIMETABLE,
            enterTransition = { tabEnter },
            exitTransition  = { tabExit }
        ) {
            AdminTimetableScreen(
                onPostEntry = { navController.navigate(ROUT_ADMIN_POST_TIMETABLE) }
            )
        }

        composable(
            route              = ROUT_ADMIN_POST_TIMETABLE,
            enterTransition    = { pushEnter },
            exitTransition     = { pushExit },
            popEnterTransition = { popEnter },
            popExitTransition  = { popExit }
        ) {
            AdminPostTimetableScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route              = ROUT_ADMIN_PROFILE,
            enterTransition    = { pushEnter },
            exitTransition     = { pushExit },
            popEnterTransition = { popEnter },
            popExitTransition  = { popExit }
        ) {
            AdminProfileScreen(
                onBack   = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(ROUT_AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToPostNotice     = { navController.navigate(ROUT_ADMIN_POST_NOTICE) },
                onNavigateToAdminDashboard = {
                    navController.navigate(ROUT_ADMIN_DASHBOARD) {
                        popUpTo(ROUT_ADMIN_DASHBOARD) { inclusive = false }
                    }
                }
            )
        }

        // Legacy deep-link — kept so any existing navigate(ROUT_ADMIN) calls still work
        composable(ROUT_ADMIN) {
            AdminScreen(onBack = { navController.popBackStack() })
        }

        // Admin course catalogue management (push screen)
        composable(
            route              = ROUT_ADMIN_COURSES,
            enterTransition    = { pushEnter },
            exitTransition     = { pushExit },
            popEnterTransition = { popEnter },
            popExitTransition  = { popExit }
        ) {
            AdminCoursesScreen(onBack = { navController.popBackStack() })
        }
    }
}
