package com.pyllar.consumer.domain.repository

import com.pyllar.consumer.domain.models.InvestorOnboardingRequest
import com.pyllar.consumer.domain.models.LumpsumPurchaseRequest
import com.pyllar.consumer.domain.models.PortfolioResponse
import com.pyllar.consumer.domain.models.SipCreationRequest
import com.pyllar.consumer.domain.models.SipResponse
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.data.remote.model.dto.ConsentOtpVerificationResponseDto
import com.pyllar.consumer.data.remote.model.dto.RedemptionOtpVerifyRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject

interface MutualFundRepository {
    fun onboardInvestor(
        userId: String,
        request: InvestorOnboardingRequest
    ): Flow<Resource<com.pyllar.consumer.domain.models.OnboardingResponse>>

    fun createSip(
        userId: String,
        request: SipCreationRequest
    ): Flow<Resource<SipResponse>>

    fun createLumpsumPurchase(
        userId: String,
        request: LumpsumPurchaseRequest
    ): Flow<Resource<SipResponse>>

    fun getPortfolio(userId: String): Flow<Resource<PortfolioResponse>>

    fun sendConsentOtp(
        userId: String,
        phoneNumber: String? = null
    ): Flow<Resource<JsonObject>>

    fun verifyConsentOtp(
        request: RedemptionOtpVerifyRequestDto
    ): Flow<Resource<ConsentOtpVerificationResponseDto>>
}

