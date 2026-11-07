package com.pyllar.consumer.domain.repository

import com.pyllar.consumer.data.remote.requests.ConsentTextDto
import com.pyllar.consumer.data.remote.requests.LoanApplicationRequestDto
import com.pyllar.consumer.data.remote.requests.LoanApplicationResponseDto
import com.pyllar.consumer.data.remote.requests.LoanApplicationViewDto
import com.pyllar.consumer.data.remote.requests.LoanContinueDto
import com.pyllar.consumer.data.remote.requests.LoanDetailsDto
import com.pyllar.consumer.data.remote.requests.LoanEntryDto
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.Flow

interface LoanRepository {
    fun getLoanEntry(): Flow<Resource<LoanEntryDto>>
    suspend fun getLoanEntrySync(): Resource<LoanEntryDto>

    fun startLoanApplication(request: LoanApplicationRequestDto): Flow<Resource<LoanApplicationResponseDto>>
    suspend fun startLoanApplicationSync(request: LoanApplicationRequestDto): Resource<LoanApplicationResponseDto>

    fun getLoanApplication(id: String): Flow<Resource<LoanApplicationViewDto>>
    suspend fun getLoanApplicationSync(id: String): Resource<LoanApplicationViewDto>

    fun continueLoanOffer(id: String, offerId: String): Flow<Resource<LoanContinueDto>>
    suspend fun continueLoanOfferSync(id: String, offerId: String): Resource<LoanContinueDto>

    fun getLoanDetails(id: String): Flow<Resource<LoanDetailsDto>>
    suspend fun getLoanDetailsSync(id: String): Resource<LoanDetailsDto>

    fun getConsentTexts(types: String): Flow<Resource<List<ConsentTextDto>>>
    suspend fun getConsentTextsSync(types: String): Resource<List<ConsentTextDto>>
}

