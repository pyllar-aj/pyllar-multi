package com.pyllar.consumer.data.remote.crypto

interface SecureSessionStore {
    fun saveSession(session: SecureSessionData)
    fun getSession(): SecureSessionData?
    fun clear()
    fun getClientSessionId(): String
}

expect fun createSecureSessionStore(): SecureSessionStore
