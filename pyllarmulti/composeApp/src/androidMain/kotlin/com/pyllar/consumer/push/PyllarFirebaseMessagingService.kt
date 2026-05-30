package com.pyllar.consumer.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pyllar.consumer.MainActivity
import com.pyllar.consumer.R
import com.pyllar.consumer.analytics.AnalyticsLogger
import com.pyllar.consumer.analytics.NotificationAnalyticsTracker
import com.pyllar.consumer.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class PyllarFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "general"
        private const val CHANNEL_NAME = "Pyllar Updates"
        private const val CHANNEL_DESCRIPTION = "General Pyllar notifications"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("PyllarFMS", "New FCM token: $token")
        CoroutineScope(Dispatchers.IO).launch {
            TokenStore.saveToken(applicationContext, token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data ?: emptyMap()
        val notificationId = data["notification_id"] ?: UUID.randomUUID().toString()
        val action = data["action"]

        Log.d("PyllarFMS", "FCM message data keys: ${data.keys}")
        Log.d("PyllarFMS", "content_id from FCM data: ${data["content_id"]}")

        NotificationAnalyticsTracker.logDelivered(applicationContext, notificationId, action, data)

        if (action == "CHECK_UPDATE") {
            CoroutineScope(Dispatchers.IO).launch {
                val prefs = applicationContext.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("pending_update_check", true)
                    .putLong("last_update_check_time", System.currentTimeMillis())
                    .apply()
                Log.d("PyllarFMS", "CHECK_UPDATE notification received - flag stored")

                AnalyticsLogger.logEvent(
                    applicationContext,
                    "update_check_notification_received",
                    mapOf("notification_id" to notificationId, "source" to "fcm_notification")
                )
            }
        }

        runBlocking { showNotification(message, data, notificationId) }
    }

    private suspend fun showNotification(message: RemoteMessage, data: Map<String, String>, notificationId: String) {
        val imageUrl = data["image_url"]
        val bitmap = if (!imageUrl.isNullOrBlank()) getBitmapFromUrl(imageUrl) else null
        processNotification(message, data, notificationId, bitmap)
    }

    private fun processNotification(
        message: RemoteMessage,
        data: Map<String, String>,
        notificationId: String,
        bitmap: Bitmap?
    ) {
        createNotificationChannel()

        val title = data["title"] ?: message.notification?.title ?: getString(R.string.app_name)
        val body = data["body"] ?: message.notification?.body ?: ""

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            data.forEach { (key, value) -> putExtra(key, value) }
            putExtra("notification_id", notificationId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        if (bitmap != null) {
            builder.setLargeIcon(bitmap)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null as Bitmap?)
                    .setSummaryText(body)
            )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        try {
            NotificationManagerCompat.from(this).notify(notificationId.hashCode(), builder.build())
        } catch (e: SecurityException) {
            Log.e("PyllarFMS", "Missing POST_NOTIFICATIONS permission", e)
        } catch (e: Exception) {
            Log.e("PyllarFMS", "Error showing notification", e)
        }
    }

    private suspend fun getBitmapFromUrl(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(imageUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent", "Pyllar-Android-App")
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null

            connection.inputStream.use { input ->
                val bitmap = BitmapFactory.decodeStream(input.buffered())
                if (bitmap != null && (bitmap.width > 1920 || bitmap.height > 1080)) {
                    val ratio = minOf(1920f / bitmap.width, 1080f / bitmap.height)
                    Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
                } else {
                    bitmap
                }
            }
        } catch (e: Exception) {
            Log.e("PyllarFMS", "Error downloading notification image: ${e.message}", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = CHANNEL_DESCRIPTION
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
