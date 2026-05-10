package com.campusconnect.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ─── Shape System ─────────────────────────────────────────────────────────────
// Slightly more rounded than M3 defaults — modern, premium, not bubbly.
val CampusShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),    // chips, badges, small tags
    small      = RoundedCornerShape(10.dp),   // text fields, snackbars
    medium     = RoundedCornerShape(14.dp),   // cards, dialogs (default M3 = 12)
    large      = RoundedCornerShape(20.dp),   // bottom sheets, large cards
    extraLarge = RoundedCornerShape(28.dp)    // FAB, modal bottom sheets
)

// ─── Light Color Scheme ───────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary                = LightPrimary,
    onPrimary              = LightOnPrimary,
    primaryContainer       = LightPrimaryContainer,
    onPrimaryContainer     = LightOnPrimaryContainer,
    inversePrimary         = LightInversePrimary,
    secondary              = LightSecondary,
    onSecondary            = LightOnSecondary,
    secondaryContainer     = LightSecondaryContainer,
    onSecondaryContainer   = LightOnSecondaryContainer,
    tertiary               = LightTertiary,
    onTertiary             = LightOnTertiary,
    tertiaryContainer      = LightTertiaryContainer,
    onTertiaryContainer    = LightOnTertiaryContainer,
    background             = LightBackground,           // fixed: was hardcoded white
    onBackground           = LightOnBackground,
    surface                = LightSurface,
    onSurface              = LightOnSurface,
    surfaceVariant         = LightSurfaceVariant,
    onSurfaceVariant       = LightOnSurfaceVariant,
    surfaceTint            = LightSurfaceTint,
    inverseSurface         = LightInverseSurface,
    inverseOnSurface       = LightInverseOnSurface,
    outline                = LightOutline,
    outlineVariant         = LightOutlineVariant,
    scrim                  = LightScrim,
    error                  = LightError,
    onError                = LightOnError,
    errorContainer         = LightErrorContainer,
    onErrorContainer       = LightOnErrorContainer
)

// ─── Dark Color Scheme ────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary                = DarkPrimary,
    onPrimary              = DarkOnPrimary,
    primaryContainer       = DarkPrimaryContainer,
    onPrimaryContainer     = DarkOnPrimaryContainer,
    inversePrimary         = DarkInversePrimary,
    secondary              = DarkSecondary,
    onSecondary            = DarkOnSecondary,
    secondaryContainer     = DarkSecondaryContainer,
    onSecondaryContainer   = DarkOnSecondaryContainer,
    tertiary               = DarkTertiary,
    onTertiary             = DarkOnTertiary,
    tertiaryContainer      = DarkTertiaryContainer,
    onTertiaryContainer    = DarkOnTertiaryContainer,
    background             = DarkBackground,
    onBackground           = DarkOnBackground,
    surface                = DarkSurface,
    onSurface              = DarkOnSurface,
    surfaceVariant         = DarkSurfaceVariant,
    onSurfaceVariant       = DarkOnSurfaceVariant,
    surfaceTint            = DarkSurfaceTint,
    inverseSurface         = DarkInverseSurface,
    inverseOnSurface       = DarkInverseOnSurface,
    outline                = DarkOutline,
    outlineVariant         = DarkOutlineVariant,
    scrim                  = DarkScrim,
    error                  = DarkError,
    onError                = DarkOnError,
    errorContainer         = DarkErrorContainer,
    onErrorContainer       = DarkOnErrorContainer
)

// ─── Theme Composable ─────────────────────────────────────────────────────────
@Composable
fun CampusConnectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled — we want brand consistency over Material You
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Transparent status bar — lets edge-to-edge content breathe under it.
            // The WindowInsets padding in each screen handles safe area offsets.
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = CampusTypography,
        shapes      = CampusShapes,     // ← custom shape tokens applied globally
        content     = content
    )
}