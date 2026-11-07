package com.pyllar.consumer.data.repository

import com.pyllar.consumer.data.remote.network.PyllarApiClient
import com.pyllar.consumer.data.remote.requests.ConsentTextDto
import com.pyllar.consumer.data.remote.requests.LoanApplicationRequestDto
import com.pyllar.consumer.data.remote.requests.LoanApplicationResponseDto
import com.pyllar.consumer.data.remote.requests.LoanApplicationViewDto
import com.pyllar.consumer.data.remote.requests.LoanContinueDto
import com.pyllar.consumer.data.remote.requests.LoanDetailsDto
import com.pyllar.consumer.data.remote.requests.LoanEntryDto
import com.pyllar.consumer.domain.repository.LoanRepository
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LoanRepositoryImpl(
    private val apiClient: PyllarApiClient
) : LoanRepository {

    override suspend fun getLoanEntrySync(): Resource<LoanEntryDto> {
        return apiClient.get<LoanEntryDto>("api/loans/entry")
    }

    override fun getLoanEntry(): Flow<Resource<LoanEntryDto>> = flow {
        emit(Resource.Loading())
        emit(getLoanEntrySync())
    }

    override suspend fun startLoanApplicationSync(request: LoanApplicationRequestDto): Resource<LoanApplicationResponseDto> {
        return apiClient.post<LoanApplicationResponseDto, LoanApplicationRequestDto>("api/loans/applications", request)
    }

    override fun startLoanApplication(request: LoanApplicationRequestDto): Flow<Resource<LoanApplicationResponseDto>> = flow {
        emit(Resource.Loading())
        emit(startLoanApplicationSync(request))
    }

    override suspend fun getLoanApplicationSync(id: String): Resource<LoanApplicationViewDto> {
        return apiClient.get<LoanApplicationViewDto>("api/loans/applications/$id")
    }

    override fun getLoanApplication(id: String): Flow<Resource<LoanApplicationViewDto>> = flow {
        emit(Resource.Loading())
        emit(getLoanApplicationSync(id))
    }

    override suspend fun continueLoanOfferSync(id: String, offerId: String): Resource<LoanContinueDto> {
        return apiClient.post<LoanContinueDto, Unit?>("api/loans/applications/$id/offers/$offerId/continue", null)
    }

    override fun continueLoanOffer(id: String, offerId: String): Flow<Resource<LoanContinueDto>> = flow {
        emit(Resource.Loading())
        emit(continueLoanOfferSync(id, offerId))
    }

    override suspend fun getLoanDetailsSync(id: String): Resource<LoanDetailsDto> {
        return apiClient.get<LoanDetailsDto>("api/loans/applications/$id/details")
    }

    override fun getLoanDetails(id: String): Flow<Resource<LoanDetailsDto>> = flow {
        emit(Resource.Loading())
        emit(getLoanDetailsSync(id))
    }

    override suspend fun getConsentTextsSync(types: String): Resource<List<ConsentTextDto>> {
        return apiClient.get<List<ConsentTextDto>>("api/consents/texts?types=$types")
    }

    override fun getConsentTexts(types: String): Flow<Resource<List<ConsentTextDto>>> = flow {
        emit(Resource.Loading())
        emit(getConsentTextsSync(types))
    }
}

