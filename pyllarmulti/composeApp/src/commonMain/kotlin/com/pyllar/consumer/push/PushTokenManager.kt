package com.pyllar.consumer.push

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Shared manager for push notification tokens.
 * This can be updated from platform-specific code (e.g. AppDelegate on iOS)
 * and read by shared ViewModels.
 */
object PushTokenManager {
    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token

    fun setToken(token: String) {
        _token.value = token
    }

    fun getPushToken(): String? = _token.value
}
