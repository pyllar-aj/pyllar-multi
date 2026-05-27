package com.pyllar.consumer.push

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Shared manager for push notification tokens and incoming notification payloads.
 * This can be updated from platform-specific code (e.g. AppDelegate on iOS)
 * and read by shared ViewModels or App routing.
 */
object PushTokenManager {
    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token

    private val _lastNotificationPayload = MutableStateFlow<String?>(null)
    val lastNotificationPayload: StateFlow<String?> = _lastNotificationPayload

    fun setToken(token: String) {
        _token.value = token
    }

    fun getPushToken(): String? = _token.value

    fun setNotificationPayload(payload: String) {
        _lastNotificationPayload.value = payload
    }

    fun clearNotificationPayload() {
        _lastNotificationPayload.value = null
    }
}

