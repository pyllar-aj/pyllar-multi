package com.pyllar.consumer.domain.repository

import com.pyllar.consumer.data.remote.dto.PanFetchRequestDto
import com.pyllar.consumer.data.remote.dto.PanFetchResponseDto
import com.pyllar.consumer.data.remote.dto.PanVerifyOtpRequestDto
import com.pyllar.consumer.data.remote.dto.PanVerifyOtpResponseDto
import com.pyllar.consumer.data.remote.dto.PreVerificationRequestDto
import com.pyllar.consumer.data.remote.dto.PreVerificationResponseDto
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.Flow

interface PreVerificationRepository {

    fun checkInvestorReadiness(
        panNumber: String
    ): Flow<Resource<PreVerificationResponseDto>>

    fun startAutomaticVerification(
        panNumber: String,
        name: String,
        accountNumber: String,
        ifscCode: String,
        accountType: String = "savings"
    ): Flow<Resource<PreVerificationResponseDto>>

    fun fetchVerificationStatus(
        preVerificationId: String
    ): Flow<Resource<PreVerificationResponseDto>>

    fun pollVerificationStatus(
        preVerificationId: String,
        maxAttempts: Int = 30,
        intervalSeconds: Long = 10
    ): Flow<Resource<PreVerificationResponseDto>>

    fun performManualVerification(
        panNumber: String,
        name: String,
        accountNumber: String,
        ifscCode: String,
        bankAccountProof: String,
        accountType: String = "savings"
    ): Flow<Resource<PreVerificationResponseDto>>

    fun initiatePanFetch(
        mobileNumber: String
    ): Flow<Resource<PanFetchResponseDto>>

    fun verifyOtpAndFetchPan(
        mobileNumber: String,
        prefillId: Long,
        otp: String
    ): Flow<Resource<PanVerifyOtpResponseDto>>
}
