package com.flavo_oculus.air_force

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AirForceFCMService: FirebaseMessagingService() {


    companion object {
        const val CHANNEL_ID = "galactic_wars_channel_id"
        const val SILENT_CHANNEL_ID = "galactic_wars_silent_channel_id"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("onNewToken", token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        try {
            val notification = message.notification
            if(notification != null){
                val title = notification.title ?: ""
                val text = notification.body ?: ""
                showFCMNotification(title = title, text = text)
            }
        } catch (e: Exception) {
            Log.d("WheelOfFortuneFCMS", e.message.toString())
        }
    }

    private fun showFCMNotification(title: String, text: String) {
        val intent = Intent(baseContext, MainActivity::class.java)
        val channelId = "notification_channel"
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra("is_from_notification", true)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(
                baseContext, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
            )
        } else {
            PendingIntent.getActivity(
                baseContext, 0, intent, PendingIntent.FLAG_ONE_SHOT
            )
        }
        val notification = NotificationCompat.Builder(baseContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Firebase notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(0, notification.build())
    }
}