package com.pyllar.consumer.data.remote.crypto

import com.pyllar.consumer.data.remote.model.crypto.SecureHandshakeRequestDto
import com.pyllar.consumer.data.remote.model.crypto.SecureHandshakeResponseDto
import com.pyllar.consumer.data.remote.network.PyllarApiClient
import com.pyllar.consumer.platform.DeviceInfoProvider
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SecureHandshakeCoordinator(
    private val apiClientUrlProvider: () -> String, // Provide base URL dynamically to avoid circular injection
    private val sessionStore: SecureSessionStore,
    private val deviceInfoProvider: DeviceInfoProvider
) {

    private val lock = Mutex()
    private val crypto = SecurePayloadCrypto()

    suspend fun ensureSession(): SecureSessionData {
        sessionStore.getSession()?.takeIf { !it.isExpired() }?.let { return it }
        return lock.withLock {
            sessionStore.getSession()?.takeIf { !it.isExpired() } ?: performHandshake()
        }
    }

    fun invalidateSession() {
        sessionStore.clear()
    }

    private suspend fun performHandshake(): SecureSessionData {
        val clientSessionId = sessionStore.getClientSessionId()
        val platform = deviceInfoProvider.getOsName().lowercase()
        
        val (request, context) = crypto.generateHandshakeRequest(clientSessionId, platform)

        // Temporary client to perform handshake plain
        val apiClient = PyllarApiClient(apiClientUrlProvider())
        
        val responseResource = apiClient.post<SecureHandshakeResponseDto, SecureHandshakeRequestDto>(
            path = "api/crypto/handshake",
            body = request
        )
        
        when (responseResource) {
            is Resource.Success -> {
                val payload = responseResource.data ?: throw SecureChannelException("Handshake response missing body")
                val session = crypto.deriveKeysAndSession(payload, context, clientSessionId)
                sessionStore.saveSession(session)
                return session
            }
            is Resource.Error -> {
                sessionStore.clear()
                throw SecureChannelException("Handshake failed: ${responseResource.message}")
            }
            is Resource.Loading -> {
                throw IllegalStateException("Unexpected loading state")
            }
        }
    }
}
