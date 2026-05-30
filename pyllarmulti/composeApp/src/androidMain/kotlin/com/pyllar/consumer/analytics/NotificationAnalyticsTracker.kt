package com.pyllar.consumer.analytics

import android.content.Context
import com.pyllar.consumer.util.Log

object NotificationAnalyticsTracker {

    fun logDelivered(context: Context, notificationId: String?, action: String?, payload: Map<String, String>) {
        Log.d("NotificationAnalyticsTracker", "logDelivered - content_id: ${payload["content_id"]}")
        val params = buildParams(notificationId, action, payload).toMutableMap()
        params["event_type"] = "delivered"
        AnalyticsLogger.logEvent(context, "notification_delivered", params)
    }

    fun logClicked(context: Context, notificationId: String?, action: String?, payload: Map<String, String>) {
        Log.d("NotificationAnalyticsTracker", "logClicked - content_id: ${payload["content_id"]}")
        val params = buildParams(notificationId, action, payload).toMutableMap()
        params["event_type"] = "clicked"
        AnalyticsLogger.logEvent(context, "notification_clicked", params)
    }

    private fun buildParams(notificationId: String?, action: String?, payload: Map<String, String>): Map<String, Any?> {
        val contentId = payload["content_id"]?.takeIf { it.isNotBlank() } ?: "unknown"
        return mapOf(
            "notification_id" to (notificationId ?: payload["notification_id"] ?: "unknown"),
            "content_id" to contentId,
            "action" to (action ?: payload["action"] ?: "unknown"),
            "has_url" to (payload["url"]?.isNotBlank() == true),
            "has_route" to (payload["route"]?.isNotBlank() == true)
        )
    }
}
