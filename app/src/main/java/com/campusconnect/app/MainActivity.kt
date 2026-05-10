package com.campusconnect.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.campusconnect.app.navigation.AppNavHost
import com.campusconnect.app.navigation.ROUT_DASHBOARD
import com.campusconnect.app.navigation.ROUT_NOTICES
import com.campusconnect.app.navigation.ROUT_TIMETABLE
import com.campusconnect.app.navigation.ROUT_EVENTS
import com.campusconnect.app.navigation.ROUT_PROFILE
import com.campusconnect.app.navigation.ROUT_ADMIN_DASHBOARD
import com.campusconnect.app.navigation.ROUT_ADMIN_NOTICES
import com.campusconnect.app.navigation.ROUT_ADMIN_POST_EVENT
import com.campusconnect.app.navigation.ROUT_ADMIN_POST_TIMETABLE
import com.campusconnect.app.navigation.ROUT_ADMIN_EVENTS
import com.campusconnect.app.navigation.ROUT_ADMIN_TIMETABLE
import com.campusconnect.app.navigation.ROUT_ADMIN_PROFILE
import com.campusconnect.app.ui.components.CampusBottomBar
import com.campusconnect.app.ui.theme.AppThemeState
import com.campusconnect.app.ui.theme.CampusConnectTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        // Install and immediately dismiss the system splash screen.
        // It shows only for the few milliseconds Compose needs to render —
        // our designed SplashScreen composable takes over right after.
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        AppThemeState.init(applicationContext, lifecycleScope)

        setContent {
            val manualDarkOverride by AppThemeState.isDark.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDark = systemDark || manualDarkOverride

            CampusConnectTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CampusConnectAppRoot()
                }
            }
        }
    }
}

@Composable
fun CampusConnectAppRoot() {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    val studentRoutes = setOf(
        ROUT_DASHBOARD,
        ROUT_NOTICES,
        ROUT_TIMETABLE,
        ROUT_EVENTS,
        ROUT_PROFILE
    )
    val adminRoutes = setOf(
        ROUT_ADMIN_DASHBOARD,
        ROUT_ADMIN_NOTICES,
        ROUT_ADMIN_POST_EVENT,
        ROUT_ADMIN_POST_TIMETABLE,
        ROUT_ADMIN_EVENTS,
        ROUT_ADMIN_TIMETABLE,
        ROUT_ADMIN_PROFILE
    )

    val isAdminRoute  = currentRoute in adminRoutes
    val showBottomBar = currentRoute in studentRoutes || currentRoute in adminRoutes

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(initialAlpha = 0.3f),
                exit  = slideOutVertically(targetOffsetY = { it }) + fadeOut(targetAlpha = 0.3f)
            ) {
                CampusBottomBar(
                    currentRoute = currentRoute,
                    isAdmin      = isAdminRoute,
                    onNavigate   = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        AppNavHost(
            navController = navController,
            modifier      = Modifier.padding(paddingValues)
        )
    }
}