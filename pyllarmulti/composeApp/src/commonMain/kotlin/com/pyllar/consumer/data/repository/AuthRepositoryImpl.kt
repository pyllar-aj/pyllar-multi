package com.pyllar.consumer.data.repository

import com.pyllar.consumer.data.remote.datasource.AuthRemoteDataSource
import com.pyllar.consumer.data.remote.model.UpdateEmailRequest
import com.pyllar.consumer.data.remote.requests.OtpRegistrationRequest
import com.pyllar.consumer.data.remote.requests.OtpVerificationRequest
import com.pyllar.consumer.domain.models.AuthToken
import com.pyllar.consumer.domain.models.AuthUserDTO
import com.pyllar.consumer.domain.models.UpdateEmailResponse
import com.pyllar.consumer.domain.repository.AuthRepository
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.data.remote.parser.ErrorType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AuthRepositoryImpl(
    private val remote: AuthRemoteDataSource,
    private val sessionStore: SessionStore
) : AuthRepository {

    override fun checkPreviousAuthUser(): Flow<Resource<AuthToken>> = flow {
        val token = sessionStore.getAuthToken()
        if (token != null) {
            emit(Resource.Success(token))
        } else {
            emit(Resource.Error("No previous authentication found"))
        }
    }

    override fun sendOtp(request: OtpRegistrationRequest): Flow<Resource<AuthToken>> = flow {
        emit(Resource.Loading())
        when (val result = remote.sendOtp(request)) {
            is Resource.Success -> {
                val data = result.data
                if (data != null) {
                    val authToken = AuthToken(
                        token = "",
                        userId = data.actualUserId ?: "",
                        auth_token = "",
                        otpRef = data.ref,
                        phoneNumber = data.phoneNumber
                    )
                    emit(Resource.Success(authToken, result.navigation, result.fieldErrors))
                } else {
                    emit(
                        Resource.Error(
                            "Empty response data",
                            result.navigation,
                            result.fieldErrors,
                            ErrorType.UNKNOWN_ERROR
                        )
                    )
                }
            }

            is Resource.Error -> {
                emit(
                    Resource.Error(
                        result.message ?: "Unknown error",
                        result.navigation,
                        result.fieldErrors,
                        result.errorType
                    )
                )
            }

            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    override fun verifyOtp(request: OtpVerificationRequest): Flow<Resource<AuthUserDTO>> = flow {
        emit(Resource.Loading())
        when (val result = remote.verifyOtp(request)) {
            is Resource.Success -> {
                val data = result.data
                if (data != null) {
                    val domainUser = AuthUserDTO(
                        token = data.authToken,
                        userId = data.userId,
                        phoneNumber = data.phoneNumber,
                        email = "",
                        firstName = "",
                        lastName = "",
                        newUser = true
                    )
                    // Persist token via SessionStore
                    sessionStore.saveAuthToken(
                        AuthToken(
                            auth_token = data.authToken,
                            token = data.authToken,
                            userId = data.userId
                        )
                    )
                    emit(Resource.Success(domainUser, result.navigation, result.fieldErrors))
                } else {
                    emit(
                        Resource.Error(
                            "Empty response data",
                            result.navigation,
                            result.fieldErrors,
                            ErrorType.UNKNOWN_ERROR
                        )
                    )
                }
            }

            is Resource.Error -> {
                emit(
                    Resource.Error(
                        result.message ?: "Unknown error",
                        result.navigation,
                        result.fieldErrors,
                        result.errorType
                    )
                )
            }

            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    override fun updateEmail(email: String, userId: String): Flow<Resource<UpdateEmailResponse>> = flow {
        emit(Resource.Loading())
        val request = UpdateEmailRequest(email = email, userId = userId)
        when (val result = remote.updateEmail(request)) {
            is Resource.Success -> {
                val data = result.data
                if (data != null) {
                    val domain = UpdateEmailResponse(
                        email = data.email,
                        message = data.message,
                        isUpdated = data.updated,
                        isNewUser = data.newUser ?: false,
                        isMismatch = data.mismatch ?: false,
                        isError = data.error ?: false
                    )
                    sessionStore.saveUserSession(userId = userId, email = email)
                    emit(Resource.Success(domain, result.navigation, result.fieldErrors))
                } else {
                    emit(
                        Resource.Error(
                            "Empty response data",
                            result.navigation,
                            result.fieldErrors,
                            ErrorType.UNKNOWN_ERROR
                        )
                    )
                }
            }
            is Resource.Error -> {
                emit(
                    Resource.Error(
                        result.message ?: "Unknown error",
                        result.navigation,
                        result.fieldErrors,
                        result.errorType
                    )
                )
            }
            is Resource.Loading -> emit(Resource.Loading())
        }
    }
}

