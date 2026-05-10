package com.campusconnect.app.domain.model

import androidx.compose.ui.graphics.Color

// ─── Priority color / label extension properties ───────────────────────────────
//
// Previously the `when(notice.priority)` color+label resolution was copy-pasted
// verbatim in DashboardScreen, AdminNoticesScreen (×2) and AdminDashboardScreen.
// Centralising them here means a single change propagates everywhere.

val NoticePriority.color: Color
    get() = when (this) {
        NoticePriority.URGENT -> Color(0xFFE91E63)
        NoticePriority.HIGH   -> Color(0xFFEF4444)
        NoticePriority.MEDIUM -> Color(0xFFF97316)
        NoticePriority.LOW    -> Color(0xFF10B981)
    }

val NoticePriority.label: String
    get() = when (this) {
        NoticePriority.URGENT -> "URGENT"
        NoticePriority.HIGH   -> "HIGH"
        NoticePriority.MEDIUM -> "MEDIUM"
        NoticePriority.LOW    -> "LOW"
    }
