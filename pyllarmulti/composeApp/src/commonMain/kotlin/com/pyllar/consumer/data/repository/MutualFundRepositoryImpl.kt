package com.pyllar.consumer.data.repository

import com.pyllar.consumer.data.remote.model.dto.OnboardingResponseDto
import com.pyllar.consumer.data.remote.model.dto.PortfolioResponseDto
import com.pyllar.consumer.data.remote.model.dto.SipResponseDto
import com.pyllar.consumer.data.remote.network.PyllarApiClient
import com.pyllar.consumer.domain.models.InvestorOnboardingRequest
import com.pyllar.consumer.domain.models.LumpsumPurchaseRequest
import com.pyllar.consumer.domain.models.OnboardingResponse
import com.pyllar.consumer.domain.models.PortfolioResponse
import com.pyllar.consumer.domain.models.SipCreationRequest
import com.pyllar.consumer.domain.models.SipResponse
import com.pyllar.consumer.data.remote.model.dto.ConsentOtpVerificationResponseDto
import com.pyllar.consumer.data.remote.model.dto.RedemptionOtpVerifyRequestDto
import com.pyllar.consumer.domain.repository.MutualFundRepository
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonObject

class MutualFundRepositoryImpl(
    private val apiClient: PyllarApiClient
) : MutualFundRepository {

    override fun onboardInvestor(
        userId: String,
        request: InvestorOnboardingRequest
    ): Flow<Resource<OnboardingResponse>> = flow {
        emit(Resource.Loading())
        when (val result =
            apiClient.post<OnboardingResponseDto, InvestorOnboardingRequest>(
                path = "api/mutual-fund/onboard?userId=$userId",
                body = request
            )
        ) {
            is Resource.Success -> {
                val dto = result.data
                if (dto != null) {
                    val mapped = OnboardingResponse(
                        investorProfileId = dto.investorId.toIntOrNull() ?: 0,
                        investmentAccountId = dto.userId.hashCode(),
                        message = dto.message
                    )
                    emit(
                        Resource.Success(
                            data = mapped,
                            navigation = result.navigation,
                            fieldErrors = result.fieldErrors
                        )
                    )
                } else {
                    emit(
                        Resource.Error(
                            message = "Empty response data",
                            navigation = result.navigation,
                            fieldErrors = result.fieldErrors,
                            errorType = result.errorType
                        )
                    )
                }
            }

            is Resource.Error -> emit(
                Resource.Error(
                    message = result.message ?: "",
                    navigation = result.navigation,
                    fieldErrors = result.fieldErrors,
                    errorType = result.errorType
                )
            )

            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    override fun createSip(
        userId: String,
        request: SipCreationRequest
    ): Flow<Resource<SipResponse>> = flow {
        emit(Resource.Loading())
        when (val result =
            apiClient.post<SipResponseDto, SipCreationRequest>(
                path = "api/mutual-fund/sip?userId=$userId",
                body = request
            )
        ) {
            is Resource.Success -> {
                val dto = result.data
                if (dto != null) {
                    val mapped = SipResponse(
                        sipId = dto.sipId,
                        amount = dto.amount,
                        frequency = request.frequency,
                        startDate = request.startDate,
                        status = dto.status,
                        sourceRefId = dto.sipId
                    )
                    emit(
                        Resource.Success(
                            data = mapped,
                            navigation = result.navigation,
                            fieldErrors = result.fieldErrors
                        )
                    )
                } else {
                    emit(
                        Resource.Error(
                            message = "Empty response data",
                            navigation = result.navigation,
                            fieldErrors = result.fieldErrors,
                            errorType = result.errorType
                        )
                    )
                }
            }

            is Resource.Error -> emit(
                Resource.Error(
                    message = result.message ?: "",
                    navigation = result.navigation,
                    fieldErrors = result.fieldErrors,
                    errorType = result.errorType
                )
            )

            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    override fun createLumpsumPurchase(
        userId: String,
        request: LumpsumPurchaseRequest
    ): Flow<Resource<SipResponse>> = flow {
        emit(Resource.Loading())
        when (val result =
            apiClient.post<SipResponseDto, LumpsumPurchaseRequest>(
                path = "api/mutual-fund/lumpsum-purchase?userId=$userId",
                body = request
            )
        ) {
            is Resource.Success -> {
                val dto = result.data
                if (dto != null) {
                    val mapped = SipResponse(
                        sipId = dto.sipId,
                        amount = dto.amount,
                        frequency = "MONTHLY",
                        startDate = "",
                        status = dto.status,
                        sourceRefId = dto.sipId
                    )
                    emit(
                        Resource.Success(
                            data = mapped,
                            navigation = result.navigation,
                            fieldErrors = result.fieldErrors
                        )
                    )
                } else {
                    emit(
                        Resource.Error(
                            message = "Empty response data",
                            navigation = result.navigation,
                            fieldErrors = result.fieldErrors,
                            errorType = result.errorType
                        )
                    )
                }
            }

            is Resource.Error -> emit(
                Resource.Error(
                    message = result.message ?: "",
                    navigation = result.navigation,
                    fieldErrors = result.fieldErrors,
                    errorType = result.errorType
                )
            )

            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    override fun getPortfolio(userId: String): Flow<Resource<PortfolioResponse>> = flow {
        emit(Resource.Loading())
        when (val result =
            apiClient.get<PortfolioResponseDto>(
                path = "api/mutual-fund/portfolio?userId=$userId"
            )
        ) {
            is Resource.Success -> {
                val dto = result.data
                if (dto != null) {
                    val mapped = PortfolioResponse(
                        totalSips = 0,
                        activeSips = 0,
                        totalInvestmentAccounts = 1,
                        totalBankAccounts = 1,
                        totalPurchases = dto.holdings.size,
                        sipOrders = emptyList(),
                        purchaseOrders = emptyList()
                    )
                    emit(
                        Resource.Success(
                            data = mapped,
                            navigation = result.navigation,
                            fieldErrors = result.fieldErrors
                        )
                    )
                } else {
                    emit(
                        Resource.Error(
                            message = "Empty response data",
                            navigation = result.navigation,
                            fieldErrors = result.fieldErrors,
                            errorType = result.errorType
                        )
                    )
                }
            }

            is Resource.Error -> emit(
                Resource.Error(
                    message = result.message ?: "",
                    navigation = result.navigation,
                    fieldErrors = result.fieldErrors,
                    errorType = result.errorType
                )
            )

            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    override fun sendConsentOtp(
        userId: String,
        phoneNumber: String?
    ): Flow<Resource<JsonObject>> = flow {
        emit(Resource.Loading())
        val url = if (phoneNumber != null) "api/mf-purchase-plans/consent-otp-send?userId=$userId&phoneNumber=$phoneNumber" 
                  else "api/mf-purchase-plans/consent-otp-send?userId=$userId"
        val result = apiClient.post<JsonObject, String>(
            path = url,
            body = ""
        )
        emit(result)
    }

    override fun verifyConsentOtp(
        request: RedemptionOtpVerifyRequestDto
    ): Flow<Resource<ConsentOtpVerificationResponseDto>> = flow {
        emit(Resource.Loading())
        val result = apiClient.post<ConsentOtpVerificationResponseDto, RedemptionOtpVerifyRequestDto>(
            path = "api/mf-purchase-plans/consent-otp-verify",
            body = request
        )
        emit(result)
    }
}

