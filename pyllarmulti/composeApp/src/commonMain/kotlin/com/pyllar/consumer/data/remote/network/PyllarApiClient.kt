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
import io.ktor.http.content.TextContent
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import com.pyllar.consumer.data.remote.crypto.SecureHandshakeCoordinator
import com.pyllar.consumer.data.remote.crypto.SecurePayloadCrypto
import com.pyllar.consumer.data.remote.model.crypto.SecurePayloadEnvelopeDto
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PyllarApiClient(
    @PublishedApi internal val baseUrl: String
) : KoinComponent {

    @PublishedApi
    internal val client = createHttpClient()

    @PublishedApi
    internal val handshakeCoordinator: SecureHandshakeCoordinator by inject()

    @PublishedApi
    internal val crypto = SecurePayloadCrypto()

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

            val response: HttpResponse = client.request("$baseUrl/$path") {
                this.method = HttpMethod.parse(method)
                
                val session = handshakeCoordinator.ensureSession()
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
                header("X-App-Version", "1.0.0")
                header("X-App-Name", "pyllar-consumer")
                configure()
            }

            if (response.status.value == 401) {
                handshakeCoordinator.invalidateSession()
                return Resource.Error(
                    message = "Secure session expired or invalid. Please try again.",
                    errorType = ErrorType.AUTHENTICATION_ERROR
                )
            }

            val responseText = response.bodyAsText()
            
            // Server always responds with envelope for secure endpoints
            val envelope: SecurePayloadEnvelopeDto? = try {
                json.decodeFromString(serializer<SecurePayloadEnvelopeDto>(), responseText)
            } catch (e: Exception) {
                null
            }

            if (envelope == null) {
                // Return plain parsing if not encrypted
                val parsed: StandardApiResponseDtoRaw = try {
                    json.decodeFromString(serializer<StandardApiResponseDtoRaw>(), responseText)
                } catch (e: Exception) {
                    StandardApiResponseDtoRaw(status = response.status.value.toString(), message = responseText)
                }
                return parseStandardResponse<T>(parsed)
            }

            val decryptedBytes = crypto.decrypt(envelope, session)
            val decryptedString = decryptedBytes.decodeToString()
            val parsed: StandardApiResponseDtoRaw = json.decodeFromString(decryptedString)
            
            parseStandardResponse<T>(parsed)
            
        } catch (e: Exception) {
            platformLog("HTTPSecure(Pyllar) ERROR: ${e::class.simpleName}: ${e.message}")
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
            header("X-App-Version", "1.0.0")
            header("X-App-Name", "pyllar-consumer")
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
