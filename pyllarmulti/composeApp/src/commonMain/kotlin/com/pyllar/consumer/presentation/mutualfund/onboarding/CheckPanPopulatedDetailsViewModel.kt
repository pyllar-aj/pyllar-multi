package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.domain.repository.PreVerificationRepository
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CheckPanPopulatedDetailsViewModel(
    private val preVerificationRepository: PreVerificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<Resource<Unit>?>(null)
    val uiState: StateFlow<Resource<Unit>?> = _uiState.asStateFlow()

    fun submitDetails(
        userId: String,
        preVerificationId: String?,
        name: String,
        gender: String,
        dateOfBirth: String,
        fatherName: String,
        maritalStatus: String,
        permanentAddress: String,
        correspondenceAddress: String
    ) {
        viewModelScope.launch {
            _uiState.value = Resource.Loading()
            // Map the details to the pre-verification update if needed, 
            // or use a specific repository method.
            // For now, we simulate success or use PreVerificationRepository if it has a generic update.
            _uiState.value = Resource.Success(Unit)
        }
    }
}
