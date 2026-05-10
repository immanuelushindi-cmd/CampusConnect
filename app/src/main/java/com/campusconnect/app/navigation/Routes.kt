package com.campusconnect.app.navigation

// ─── Utility / Auth ───────────────────────────────────────────────────────────
const val ROUT_SPLASH    = "splash"
const val ROUT_AUTH      = "auth"      // login screen — kept as "auth" for back-compat
const val ROUT_REGISTER  = "register"  // new register screen

// ─── Student routes (bottom-bar tabs) ────────────────────────────────────────
const val ROUT_DASHBOARD = "dashboard"
const val ROUT_NOTICES   = "notices"
const val ROUT_TIMETABLE = "timetable"
const val ROUT_EVENTS    = "events"
const val ROUT_PROFILE   = "profile"

const val ROUT_NOTICE_DETAIL = "notices/{noticeId}"
fun noticeDetailRoute(noticeId: String) = "notices/$noticeId"

// ─── Admin routes (admin bottom-bar tabs + push screens) ─────────────────────
const val ROUT_ADMIN                = "admin"                // legacy deep-link, kept for back-compat
const val ROUT_ADMIN_DASHBOARD      = "admin_dashboard"
const val ROUT_ADMIN_NOTICES        = "admin_notices"        // view & manage posted notices
const val ROUT_ADMIN_POST_NOTICE    = "admin_post_notice"    // post a new notice (form)
const val ROUT_ADMIN_COURSES        = "admin_courses"        // manage course catalogue
const val ROUT_ADMIN_EVENTS         = "admin_events"         // view & manage events
const val ROUT_ADMIN_TIMETABLE      = "admin_timetable"      // view & manage timetable entries
const val ROUT_ADMIN_PROFILE        = "admin_profile"        // admin-specific profile screen
const val ROUT_ADMIN_POST_EVENT     = "admin_post_event"     // post / edit an event
const val ROUT_ADMIN_POST_TIMETABLE = "admin_post_timetable" // post / edit a timetable entry
