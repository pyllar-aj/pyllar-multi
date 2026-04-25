package com.pyllar.consumer.data.remote.crypto

expect class SecureSessionStore() {
    fun saveSession(session: SecureSessionData)
    fun getSession(): SecureSessionData?
    fun clear()
    fun getClientSessionId(): String
}
