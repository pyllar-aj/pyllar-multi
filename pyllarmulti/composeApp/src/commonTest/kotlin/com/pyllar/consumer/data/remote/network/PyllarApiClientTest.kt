package com.pyllar.consumer.data.remote.network
 
import com.pyllar.consumer.data.remote.crypto.SecureHandshakeCoordinator
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.platform.DeviceInfoProvider
import com.pyllar.consumer.domain.models.AuthToken
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
 
class PyllarApiClientTest {
 
    private lateinit var apiClient: PyllarApiClient
    
    private val mockSessionStore = object : SessionStore {
        override suspend fun saveUserSession(userId: String, email: String, phone: String, authToken: String, fullName: String) {}
        override suspend fun getCurrentToken(): String = "test-token"
        override suspend fun getCurrentUserId(): String = "test-user-id"
        override suspend fun getCurrentEmail(): String = ""
        override suspend fun getCurrentPhone(): String = ""
        override suspend fun getCurrentFullName(): String = ""
        override suspend fun isLoggedIn(): Boolean = true
        override suspend fun logout() {}
        override suspend fun saveToken(token: String) {}
        override suspend fun saveUserId(userId: String) {}
        override suspend fun savePhone(phone: String) {}
        override suspend fun saveValue(key: String, value: String) {}
        override suspend fun getValue(key: String): String? = null
    }
 
    private val mockDeviceInfoProvider = object : DeviceInfoProvider {
        override fun getDeviceId(): String = "test-device"
        override fun getAppVersion(): String = "1.0.0"
        override fun getOsName(): String = "ios"
        override fun getOsVersion(): String = "17.0"
    }
 
    private val mockHandshakeCoordinator = object : SecureHandshakeCoordinator({ "http://localhost" }, object : com.pyllar.consumer.data.remote.crypto.SecureSessionStore {
        override fun getSession(): com.pyllar.consumer.data.remote.crypto.SecureSessionData? = null
        override fun saveSession(session: com.pyllar.consumer.data.remote.crypto.SecureSessionData) {}
        override fun clear() {}
        override fun getClientSessionId(): String = "test-session-id"
    }, mockDeviceInfoProvider) {
        override suspend fun ensureSession(): com.pyllar.consumer.data.remote.crypto.SecureSessionData {
            return com.pyllar.consumer.data.remote.crypto.SecureSessionData(
                handshakeId = "test-handshake-id",
                encryptionKey = byteArrayOf(1, 2, 3),
                hmacKey = byteArrayOf(4, 5, 6),
                expiresAt = "2099-01-01T00:00:00Z",
                clientSessionId = "test-session-id"
            )
        }
        override fun invalidateSession() {}
    }
 
    @Test
    fun testHeaderAttachment() = runTest {
        val mockEngine = MockEngine { request ->
            assertTrue(request.headers.contains(HttpHeaders.Authorization), "Missing Authorization header")
            assertEquals("Bearer test-token", request.headers[HttpHeaders.Authorization])
            respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
        }
 
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
 
        apiClient = PyllarApiClient(
            baseUrl = "http://localhost",
            testClient = client,
            injectedHandshakeCoordinator = mockHandshakeCoordinator,
            injectedSessionStore = mockSessionStore,
            injectedDeviceInfoProvider = mockDeviceInfoProvider
        )
        
        apiClient.post<JsonObject, JsonObject>("test", JsonObject(emptyMap()))
    }
}
