package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.lifecycle.ViewModel
import com.pyllar.consumer.domain.repository.AuthRepository
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.Flow

class SignatureViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    fun uploadSignatureFile(bytes: ByteArray, kycAttemptId: String): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.EsignCreateResponseDto>> {
        return authRepository.uploadSignatureFile(bytes, kycAttemptId)
    }
}
