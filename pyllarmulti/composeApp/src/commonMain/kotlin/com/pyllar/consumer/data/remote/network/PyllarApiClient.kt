package com.pyllar.consumer.data.remote.network

import com.pyllar.consumer.data.remote.model.dto.StandardApiResponseDtoRaw
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.data.remote.parser.ErrorType
import com.pyllar.consumer.util.platformLog
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.takeFrom
import io.ktor.http.path
import io.ktor.http.encodedPath
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import com.pyllar.consumer.data.remote.crypto.SecureHandshakeCoordinator
import com.pyllar.consumer.data.remote.crypto.SecurePayloadCrypto
import com.pyllar.consumer.data.remote.crypto.SecureChannelException
import com.pyllar.consumer.data.remote.model.crypto.SecurePayloadEnvelopeDto
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.platform.DeviceInfoProvider

class PyllarApiClient(
    @PublishedApi internal val baseUrl: String,
    @PublishedApi internal val testClient: io.ktor.client.HttpClient? = null,
    private val injectedHandshakeCoordinator: SecureHandshakeCoordinator? = null,
    private val injectedSessionStore: SessionStore? = null,
    private val injectedDeviceInfoProvider: DeviceInfoProvider? = null
) : KoinComponent {

    @PublishedApi
    internal val client = testClient ?: createHttpClient()

    @PublishedApi
    internal val handshakeCoordinator: SecureHandshakeCoordinator by lazy { 
        injectedHandshakeCoordinator ?: inject<SecureHandshakeCoordinator>().value 
    }

    @PublishedApi
    internal val crypto = SecurePayloadCrypto()

    @PublishedApi
    internal val sessionStore: SessionStore by lazy { 
        injectedSessionStore ?: inject<SessionStore>().value 
    }

    @PublishedApi
    internal val deviceInfoProvider: DeviceInfoProvider by lazy { 
        injectedDeviceInfoProvider ?: inject<DeviceInfoProvider>().value 
    }

    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    suspend inline fun <reified T> get(
        path: String,
        noinline configure: HttpRequestBuilder.() -> Unit = {}
    ): Resource<T> {
        return executeRequest<T, Unit>(path, method = "GET", body = null, configure = configure)
    }

    suspend inline fun <reified T, reified B> post(
        path: String,
        body: B,
        noinline configure: HttpRequestBuilder.() -> Unit = {}
    ): Resource<T> {
        return executeRequest<T, B>(path, method = "POST", body = body, configure = configure)
    }

    suspend inline fun <reified T, reified B> put(
        path: String,
        body: B,
        noinline configure: HttpRequestBuilder.() -> Unit = {}
    ): Resource<T> {
        return executeRequest<T, B>(path, method = "PUT", body = body, configure = configure)
    }

    suspend inline fun <reified T, reified B> patch(
        path: String,
        body: B,
        noinline configure: HttpRequestBuilder.() -> Unit = {}
    ): Resource<T> {
        return executeRequest<T, B>(path, method = "PATCH", body = body, configure = configure)
    }

    @PublishedApi
    internal suspend inline fun <reified T, reified B> executeRequest(
        path: String,
        method: String,
        body: B?,
        noinline configure: HttpRequestBuilder.() -> Unit = {}
    ): Resource<T> {
        return try {
            val isHandshake = path.contains("api/crypto/handshake")
            
            // Bypass encryption for handshake itself
            if (isHandshake) {
                return executeRawRequest<T, B>(path, method, body, configure)
            }

            // Ensure secure session is established
            val session = handshakeCoordinator.ensureSession()
            
            // Fetch session info OUTSIDE the request block to ensure reliable suspension handling
            val authToken = sessionStore.getCurrentToken()
            val sessionUserId = sessionStore.getCurrentUserId()
            
            platformLog("PyllarApiClient: \ud83d\udd10 Preparing request with authToken: ${if (authToken.isNotBlank()) "PRESENT (${authToken.take(10)}...)" else "MISSING"}, sessionUserId: $sessionUserId")

            val response: HttpResponse = client.request {
                url {
                    takeFrom(baseUrl)
                    val cleanPath = if (path.startsWith("/")) path.substring(1) else path
                    // Ensure baseUrl ends with / before appending cleanPath
                    val baseWithTrailing = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
                    takeFrom(baseWithTrailing + cleanPath)
                }
                this.method = HttpMethod.parse(method)
                
                var timestamp = crypto.createFreshHeaders(session).second
                
                if (method.uppercase() in listOf("POST", "PUT", "PATCH") && body != null) {
                    val plainJson = json.encodeToString(serializer<B>(), body)
                    val envelope = crypto.encrypt(plainJson.encodeToByteArray(), session)
                    timestamp = envelope.timestampUtc
                    setBody(TextContent(
                        json.encodeToString(serializer<SecurePayloadEnvelopeDto>(), envelope),
                        ContentType.Application.Json
                    ))
                }
                
                header("X-Handshake-Id", session.handshakeId)
                header("X-Timestamp-Utc", timestamp)
                
                // Mandatory headers for backend controllers
                val appVersion = deviceInfoProvider.getAppVersion() ?: "1.0.0"
                val osName = deviceInfoProvider.getOsName()
                val osVersion = deviceInfoProvider.getOsVersion()
                val deviceId = deviceInfoProvider.getDeviceId() ?: ""
                
                header("app_version", appVersion)
                header("X-App-Version", appVersion)
                header("app_name", "pyllar-consumer")
                header("X-App-Name", "pyllar-consumer")
                header("os", osName)
                header("X-OS", osName)
                header("os_version", osVersion)
                header("X-OS-Version", osVersion)
                header("device_id", deviceId)
                header("X-Device-Id", deviceId)
                header("platform", osName.lowercase())
                header("X-Platform", osName.lowercase())
                header("utm_source", "direct")
                header("utm_medium", "mobile")
                header("utm_campaign", "app")
                
                // Add Authorization header if token exists
                if (authToken.isNotBlank()) {
                    this.header(HttpHeaders.Authorization, "Bearer $authToken")
                    platformLog("PyllarApiClient: \u2705 Added Authorization header")
                } else {
                    platformLog("PyllarApiClient: \u26a0\ufe0f Skipping Authorization header (token is blank)")
                }

                // Add User ID header for user context tracking
                if (sessionUserId.isNotBlank()) {
                    header("X-User-Id", sessionUserId)
                    header("Session-User-Id", sessionUserId)
                    platformLog("PyllarApiClient: \u2705 Added X-User-Id and Session-User-Id headers")
                }
                
                configure()
                
                platformLog("HTTPSecure(Pyllar) REQUEST: $method ${this.url.buildString()}")
            }

            if (response.status.value == 401) {
                handshakeCoordinator.invalidateSession()
                return Resource.Error(
                    message = "Secure session expired or invalid. Please try again.",
                    errorType = ErrorType.AUTHENTICATION_ERROR
                )
            }

            val responseText = response.bodyAsText()
            platformLog("HTTPSecure(Pyllar) RESPONSE RAW (Status: ${response.status.value}): $responseText")
            
            // Server always responds with envelope for secure endpoints
            val envelope: SecurePayloadEnvelopeDto? = try {
                json.decodeFromString(serializer<SecurePayloadEnvelopeDto>(), responseText)
            } catch (e: Exception) {
                platformLog("HTTPSecure(Pyllar) Envelope parse failed (might be plain JSON): ${e.message}")
                null
            }

            if (envelope == null) {
                // Return plain parsing if not encrypted
                val parsed: StandardApiResponseDtoRaw = try {
                    json.decodeFromString(serializer<StandardApiResponseDtoRaw>(), responseText)
                } catch (e: Exception) {
                    platformLog("HTTPSecure(Pyllar) Plain JSON parse failed: ${e.message}")
                    StandardApiResponseDtoRaw(status = response.status.value.toString(), message = responseText)
                }
                return parseStandardResponse<T>(parsed)
            }

            platformLog("HTTPSecure(Pyllar) DECRYPTING response envelope...")
            val decryptedBytes = try {
                crypto.decrypt(envelope, session)
            } catch (e: Exception) {
                platformLog("HTTPSecure(Pyllar) DECRYPTION FAILED: ${e::class.simpleName}: ${e.message}")
                throw e
            }
            
            val decryptedString = decryptedBytes.decodeToString()
            platformLog("PyllarApiClient: \uD83D\uDD13 Decrypted Response: $decryptedString")
            val parsed: StandardApiResponseDtoRaw = json.decodeFromString(decryptedString)
            
            parseStandardResponse<T>(parsed)
            
        } catch (e: Exception) {
            platformLog("HTTPSecure(Pyllar) TOP-LEVEL ERROR: ${e::class.simpleName}: ${e.message}")
            if (e is SecureChannelException) {
                platformLog("HTTPSecure(Pyllar) SecureChannelException details: ${e.message}")
            }
            Resource.Error(
                message = e.message ?: "Network error",
                errorType = ErrorType.NETWORK_ERROR
            )
        }
    }

    @PublishedApi
    internal suspend inline fun <reified T, reified B> executeRawRequest(
        path: String,
        method: String,
        body: B?,
        noinline configure: HttpRequestBuilder.() -> Unit = {}
    ): Resource<T> {
        val response: HttpResponse = client.request("$baseUrl/$path") {
            this.method = HttpMethod.parse(method)
            if (body != null) {
                setBody(TextContent(
                    json.encodeToString(serializer<B>(), body),
                    ContentType.Application.Json
                ))
            }
            val appVersion = deviceInfoProvider.getAppVersion() ?: "1.0.0"
            val deviceId = deviceInfoProvider.getDeviceId() ?: ""
            val sessionUserId = sessionStore.getCurrentUserId()

            header("X-App-Version", appVersion)
            header("X-App-Name", "pyllar-consumer")
            header("X-Device-Id", deviceId)
            header("device_id", deviceId)

            if (sessionUserId.isNotBlank()) {
                header("X-User-Id", sessionUserId)
                header("Session-User-Id", sessionUserId)
                platformLog("PyllarApiClient: ✅ [RawRequest] Added X-User-Id and Session-User-Id: $sessionUserId")
            }

            configure()
        }
        return if (response.status.value in 200..299) {
            Resource.Success(response.body<T>())
        } else {
            val errorBody = try { response.bodyAsText() } catch (e: Exception) { "Network Error" }
            Resource.Error(errorBody)
        }
    }

    @PublishedApi
    internal inline fun <reified T> parseStandardResponse(parsed: StandardApiResponseDtoRaw): Resource<T> {
        return if (parsed.isSuccess) {
            val typedData: T? = parsed.data?.let {
                json.decodeFromJsonElement(serializer<T>(), it)
            }
            Resource.Success(
                data = typedData,
                navigation = parsed.navigation,
                fieldErrors = parsed.errors
            )
        } else {
            Resource.Error(
                message = parsed.message ?: "Unknown error",
                navigation = parsed.navigation,
                fieldErrors = parsed.errors,
                errorType = mapStatusToErrorType(parsed.status)
            )
        }
    }

    @PublishedApi
    internal fun mapStatusToErrorType(status: String): ErrorType {
        return when (status.uppercase()) {
            "VALIDATION_ERROR" -> ErrorType.VALIDATION_ERROR
            "UNAUTHORIZED" -> ErrorType.AUTHENTICATION_ERROR
            "NOT_FOUND" -> ErrorType.NOT_FOUND_ERROR
            "SERVER_ERROR" -> ErrorType.SERVER_ERROR
            "NETWORK_ERROR" -> ErrorType.NETWORK_ERROR
            else -> ErrorType.UNKNOWN_ERROR
        }
    }
}
