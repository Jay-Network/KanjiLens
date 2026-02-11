package com.jworks.kanjilens.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jworks.kanjilens.MainActivity
import com.jworks.kanjilens.R
import com.jworks.kanjilens.domain.repository.FeedbackRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FeedbackFCMService : FirebaseMessagingService() {

    @Inject
    lateinit var feedbackRepository: FeedbackRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val PREFS_NAME = "kanjilens_prefs"
        private const val EMAIL_KEY = "user_email"
        private const val CHANNEL_ID = "feedback_updates"
        private const val CHANNEL_NAME = "Feedback Updates"
        private const val NOTIFICATION_ID = 1001
        private const val APP_ID = "kanjilens"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        serviceScope.launch {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val userEmail = prefs.getString(EMAIL_KEY, null) ?: return@launch

            feedbackRepository.registerFcmToken(
                email = userEmail,
                appId = APP_ID,
                fcmToken = token,
                deviceInfo = mapOf(
                    "os" to "Android",
                    "osVersion" to Build.VERSION.RELEASE,
                    "device" to Build.MODEL
                )
            )
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        createNotificationChannel()

        val newStatus = message.data["new_status"] ?: "updated"
        val completionNote = message.data["completion_note"]

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Feedback Update")
            .setContentText("Your feedback is now: $newStatus")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (!completionNote.isNullOrBlank()) {
            notificationBuilder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your feedback is now: $newStatus\n\nNote: $completionNote")
            )
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_feedback", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        notificationBuilder.setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for feedback status updates"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
