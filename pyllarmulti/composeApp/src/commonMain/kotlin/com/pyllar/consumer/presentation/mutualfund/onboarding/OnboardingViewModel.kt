package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.domain.models.BankAccountRequest
import com.pyllar.consumer.domain.models.InvestorFormData
import com.pyllar.consumer.domain.models.InvestorOnboardingRequest
import com.pyllar.consumer.domain.models.OnboardingResponse
import com.pyllar.consumer.domain.repository.MutualFundRepository
import com.pyllar.consumer.data.remote.parser.ErrorType
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the investor onboarding flow.
 *
 * Migrated from Android-only (Hilt + SimpleDateFormat) to:
 *  - Koin-injected (registered in SharedModules)
 *  - StateFlow instead of LiveData
 *  - commonMain — date validation uses Regex instead of java.text.SimpleDateFormat
 */
class OnboardingViewModel(
    private val mutualFundRepository: MutualFundRepository
) : ViewModel() {

    private val _onboardingResult = MutableStateFlow<Resource<OnboardingResponse>?>(null)
    val onboardingResult: StateFlow<Resource<OnboardingResponse>?> = _onboardingResult

    private val _formData = MutableStateFlow(InvestorFormData())
    val formData: StateFlow<InvestorFormData> = _formData

    private val _panError = MutableStateFlow<String?>(null)
    val panError: StateFlow<String?> = _panError
    private val _dobError = MutableStateFlow<String?>(null)
    val dobError: StateFlow<String?> = _dobError
    private val _ifscError = MutableStateFlow<String?>(null)
    val ifscError: StateFlow<String?> = _ifscError
    private val _pincodeError = MutableStateFlow<String?>(null)
    val pincodeError: StateFlow<String?> = _pincodeError

    fun updateFormData(newData: InvestorFormData) {
        _formData.value = newData
    }

    fun onboardInvestor(userId: String) {
        val currentFormData = _formData.value

        if (!isFormValid(currentFormData)) {
            _onboardingResult.value = Resource.Error(
                "Please fill all required fields",
                errorType = ErrorType.VALIDATION_ERROR
            )
            return
        }

        viewModelScope.launch {
            _onboardingResult.value = Resource.Loading()

            val request = InvestorOnboardingRequest(
                firstName = currentFormData.firstName,
                lastName = currentFormData.lastName,
                middleName = currentFormData.middleName.takeIf { it.isNotBlank() },
                gender = currentFormData.gender,
                panNumber = currentFormData.panNumber,
                dateOfBirth = currentFormData.dateOfBirth,
                addressLine1 = currentFormData.addressLine1,
                city = currentFormData.city,
                state = currentFormData.state,
                pincode = currentFormData.pincode,
                occupation = currentFormData.occupation,
                incomeRange = currentFormData.incomeRange,
                bankAccount = BankAccountRequest(
                    accountNumber = currentFormData.bankAccountNumber,
                    ifscCode = currentFormData.ifscCode,
                    accountHolderName = currentFormData.accountHolderName,
                    accountType = currentFormData.accountType,
                    bankName = currentFormData.bankName
                )
            )
            mutualFundRepository.onboardInvestor(userId, request).collect {
                _onboardingResult.value = it
            }
        }
    }

    fun validatePan(pan: String) {
        val regex = Regex("^[A-Z]{5}[0-9]{4}[A-Z]")
        _panError.value = if (pan.isNotEmpty() && !regex.matches(pan)) "Invalid" else null
    }

    /** Validates date in YYYY-MM-DD format using Regex (no java.text.SimpleDateFormat). */
    fun validateDob(dob: String) {
        if (dob.isEmpty()) { _dobError.value = null; return }
        val regex = Regex("^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$")
        _dobError.value = if (regex.matches(dob)) null else "Invalid"
    }

    fun validateIfsc(ifsc: String) {
        val regex = Regex("^[A-Z]{4}0[A-Z0-9]{6}")
        _ifscError.value = if (ifsc.isNotEmpty() && !regex.matches(ifsc)) "Invalid" else null
    }

    fun validatePincode(pin: String) {
        val regex = Regex("^[0-9]{6}")
        _pincodeError.value = if (pin.isNotEmpty() && !regex.matches(pin)) "Invalid" else null
    }

    private fun isFormValid(formData: InvestorFormData): Boolean {
        validatePan(formData.panNumber)
        validateDob(formData.dateOfBirth)
        validateIfsc(formData.ifscCode)
        validatePincode(formData.pincode)
        return formData.firstName.isNotBlank() &&
            formData.lastName.isNotBlank() &&
            formData.gender.isNotBlank() &&
            formData.panNumber.isNotBlank() && _panError.value == null &&
            formData.dateOfBirth.isNotBlank() && _dobError.value == null &&
            formData.addressLine1.isNotBlank() &&
            formData.city.isNotBlank() &&
            formData.state.isNotBlank() &&
            formData.pincode.isNotBlank() && _pincodeError.value == null &&
            formData.occupation.isNotBlank() &&
            formData.incomeRange.isNotBlank() &&
            formData.bankAccountNumber.isNotBlank() &&
            formData.ifscCode.isNotBlank() && _ifscError.value == null &&
            formData.accountHolderName.isNotBlank() &&
            formData.accountType.isNotBlank() &&
            formData.bankName.isNotBlank()
    }
}
