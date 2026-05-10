package com.campusconnect.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.campusconnect.app.R

// ─── Font Families ────────────────────────────────────────────────────────────
// DM Sans — geometric humanist sans; clean, modern, authoritative for body/UI
// DM Serif Display — elegant serif for display/headline moments
//
// Add these to res/font/:
//   DMSans-regular.ttf, DMSans-medium.ttf, DMSans-semibold.ttf,
//   DMSans-bold.ttf, dm_serif_display_regular.ttf
//
// Download: https://fonts.google.com/specimen/DM+Sans
//           https://fonts.google.com/specimen/DM+Serif+Display

val DmSans = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_medium, FontWeight.Medium),
    Font(R.font.dm_sans_semibold, FontWeight.SemiBold),
    Font(R.font.dm_sans_bold, FontWeight.Bold),
    Font(R.font.dm_sans_extrabold, FontWeight.ExtraBold),
)

val DmSerifDisplay = FontFamily(
    Font(R.font.dm_serif_display_regular, FontWeight.Normal)
)
// ─── Typography Scale ─────────────────────────────────────────────────────────
// Strategy:
//   • Display styles  → DM Serif Display (editorial identity, used sparingly)
//   • Everything else → DM Sans (readability, modern, campus-professional)
//   • Tightened letter-spacing on large sizes for premium feel
//   • Removed 57sp displayLarge — impractical on mobile; 40sp is the max

val CampusTypography = Typography(

    // ── Display ─────────────────────────────────────────────────────────────
    displayLarge = TextStyle(
        fontFamily   = DmSerifDisplay,
        fontWeight   = FontWeight.Normal,    // Serif display looks best at Normal
        fontSize     = 40.sp,
        lineHeight   = 48.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily   = DmSerifDisplay,
        fontWeight   = FontWeight.Normal,
        fontSize     = 34.sp,
        lineHeight   = 42.sp,
        letterSpacing = (-0.25).sp
    ),
    displaySmall = TextStyle(
        fontFamily   = DmSerifDisplay,
        fontWeight   = FontWeight.Normal,
        fontSize     = 28.sp,
        lineHeight   = 36.sp,
        letterSpacing = 0.sp
    ),

    // ── Headline ─────────────────────────────────────────────────────────────
    headlineLarge = TextStyle(
        fontFamily   = DmSans,
        fontWeight   = FontWeight.Bold,
        fontSize     = 30.sp,
        lineHeight   = 38.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily   = DmSans,
        fontWeight   = FontWeight.Bold,
        fontSize     = 26.sp,
        lineHeight   = 34.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineSmall = TextStyle(
        fontFamily   = DmSans,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 22.sp,
        lineHeight   = 30.sp,
        letterSpacing = (-0.1).sp
    ),

    // ── Title ────────────────────────────────────────────────────────────────
    titleLarge = TextStyle(
        fontFamily   = DmSans,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 20.sp,
        lineHeight   = 28.sp,
        letterSpacing = (-0.1).sp
    ),
    titleMedium = TextStyle(
        fontFamily   = DmSans,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 16.sp,
        lineHeight   = 24.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily   = DmSans,
        fontWeight   = FontWeight.Medium,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // ── Body ─────────────────────────────────────────────────────────────────
    bodyLarge = TextStyle(
        fontFamily   = DmSans,
        fontWeight   = FontWeight.Normal,
        fontSize     = 16.sp,
        lineHeight   = 26.sp,          // slightly loose for readability
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily   = DmSans,
        fontWeight   = FontWeight.Normal,
        fontSize     = 14.sp,
        lineHeight   = 22.sp,
        letterSpacing = 0.15.sp
    ),
    bodySmall = TextStyle(
        fontFamily   = DmSans,
        fontWeight   = FontWeight.Normal,
        fontSize     = 12.sp,
        lineHeight   = 18.sp,
        letterSpacing = 0.2.sp
    ),

    // ── Label ────────────────────────────────────────────────────────────────
    labelLarge = TextStyle(
        fontFamily   = DmSans,
        fontWeight   = FontWeight.Medium,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily   = DmSans,
        fontWeight   = FontWeight.Medium,
        fontSize     = 12.sp,
        lineHeight   = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily   = DmSans,
        fontWeight   = FontWeight.Medium,
        fontSize     = 11.sp,
        lineHeight   = 16.sp,
        letterSpacing = 0.5.sp
    )
)