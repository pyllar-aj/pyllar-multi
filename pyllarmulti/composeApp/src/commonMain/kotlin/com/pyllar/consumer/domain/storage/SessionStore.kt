package com.pyllar.consumer.domain.storage

import com.pyllar.consumer.domain.models.AuthToken

interface SessionStore {
    suspend fun saveUserSession(
        userId: String,
        email: String = "",
        phone: String = "",
        authToken: String = "",
        fullName: String = ""
    )

    suspend fun getCurrentToken(): String
    suspend fun getCurrentUserId(): String
    suspend fun getCurrentEmail(): String
    suspend fun getCurrentPhone(): String
    suspend fun getCurrentFullName(): String

    suspend fun isLoggedIn(): Boolean
    suspend fun logout()

    suspend fun saveToken(token: String)
    suspend fun saveUserId(userId: String)
    suspend fun savePhone(phone: String)

    suspend fun saveValue(key: String, value: String)
    suspend fun getValue(key: String): String?

    suspend fun getAuthToken(): AuthToken? {
        val token = getCurrentToken()
        val userId = getCurrentUserId()
        return if (token.isNotBlank()) {
            AuthToken(auth_token = token, token = token, userId = userId)
        } else null
    }

    suspend fun saveAuthToken(authToken: AuthToken) {
        saveToken(authToken.token)
        authToken.userId?.let { saveUserId(it) }
        authToken.phoneNumber?.let { savePhone(it) }
    }
}
