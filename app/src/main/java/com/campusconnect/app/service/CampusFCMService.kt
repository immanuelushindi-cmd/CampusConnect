package com.campusconnect.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.campusconnect.app.CampusConnectApp
import com.campusconnect.app.MainActivity
import com.campusconnect.app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random



class CampusFCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update("fcmToken", token)

            .addOnFailureListener { /* no-op: user may not be logged in yet */ }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title  = message.notification?.title ?: message.data["title"] ?: "Campus Connect"
        val body   = message.notification?.body  ?: message.data["body"]  ?: "You have a new update"
        val topic  = message.data["topic"] ?: "general"

        val channelId = when (topic) {
            "urgent"    -> CampusConnectApp.CHANNEL_URGENT
            "timetable" -> CampusConnectApp.CHANNEL_TIMETABLE
            "events"    -> CampusConnectApp.CHANNEL_EVENTS
            else        -> CampusConnectApp.CHANNEL_GENERAL
        }

        showNotification(title, body, channelId)
    }

    private fun showNotification(title: String, body: String, channelId: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setPriority(
                if (channelId == CampusConnectApp.CHANNEL_URGENT)
                    NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setContentIntent(pendingIntent)
            .build()

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(Random.nextInt(), notification)
    }
}
