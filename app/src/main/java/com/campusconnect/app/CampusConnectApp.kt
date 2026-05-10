package com.campusconnect.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CampusConnectApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannels(
                listOf(
                    NotificationChannel(CHANNEL_URGENT, "Urgent Notices",
                        NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "High-priority campus announcements"
                    },
                    NotificationChannel(CHANNEL_TIMETABLE, "Timetable Updates",
                        NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "Class schedule changes"
                    },
                    NotificationChannel(CHANNEL_EVENTS, "Events",
                        NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "New events and RSVP reminders"
                    },
                    NotificationChannel(CHANNEL_GENERAL, "General",
                        NotificationManager.IMPORTANCE_LOW).apply {
                        description = "General campus notices"
                    }
                )
            )
        }
    }

    companion object {
        const val CHANNEL_URGENT    = "urgent"
        const val CHANNEL_TIMETABLE = "timetable"
        const val CHANNEL_EVENTS    = "events"
        const val CHANNEL_GENERAL   = "general"
    }
}
