package com.pyllar.consumer.presentation.loan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.requests.ConsentTextDto
import com.pyllar.consumer.data.remote.requests.LoanApplicantDto
import com.pyllar.consumer.data.remote.requests.LoanApplicationRequestDto
import com.pyllar.consumer.domain.repository.CommonRepository
import com.pyllar.consumer.domain.repository.LoanRepository
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive

/** The consent wording the loan form fetches: a notice, and the consents the user must tick. */
private const val LOAN_CONSENT_TYPES = "LOAN_DISCLAIMER,LOAN_BUREAU_PULL,LOAN_LENDER_SHARING"

private fun randomUuid(): String {
    val chars = "0123456789abcdef"
    val bytes = kotlin.random.Random.nextBytes(16)
    bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x40).toByte()
    bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
    val sb = StringBuilder()
    for (i in 0 until 16) {
        if (i == 4 || i == 6 || i == 8 || i == 10) sb.append('-')
        val b = bytes[i].toInt() and 0xff
        sb.append(chars[b shr 4])
        sb.append(chars[b and 0x0f])
    }
    return sb.toString()
}

data class LoanFormValues(
    val panName: String = "",
    val pan: String = "",
    val dob: String = "",
    val gender: String = "male",
    val contactNumber: String = "",
    val email: String = "",
    val addressL1: String = "",
    val addressL2: String = "",
    val pincode: String = "",
    val city: String = "",
    val state: String = "",
    val employmentType: String = "SALARIED",
    val companyName: String = "",
    val officialEmail: String = "",
    val incomeType: String = "MONTHLY",
    val income: String = "",
    val endUse: String = "travel"
)

/** The checkboxes the user must tick, in display order. */
fun List<ConsentTextDto>.tickable() = filter { it.requiresAcceptance }

/** True once the wording has loaded and every tickable text is ticked. */
fun consentsComplete(texts: List<ConsentTextDto>, accepted: Set<String>): Boolean {
    val tickable = texts.tickable()
    return tickable.isNotEmpty() && tickable.all { it.id in accepted }
}

data class LoanApplicationUiState(
    val values: LoanFormValues = LoanFormValues(),
    val consentTexts: List<ConsentTextDto> = emptyList(),
    val acceptedConsentIds: Set<String> = emptySet(),
    val consentsLoading: Boolean = true,
    val consentsFailed: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val submittedApplicationId: String? = null
)

class LoanApplicationViewModel(
    private val loanRepository: LoanRepository,
    private val commonRepository: CommonRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoanApplicationUiState())
    val uiState: StateFlow<LoanApplicationUiState> = _uiState.asStateFlow()

    // One id per form session: a double tap or retry returns the same application from the server.
    private val clientRequestId = randomUuid()

    init {
        prefill()
        loadConsents()
    }

    /** The wording comes from the server, so what the user ticks is exactly what the server stores. */
    fun loadConsents() {
        _uiState.value = _uiState.value.copy(consentsLoading = true, consentsFailed = false)
        viewModelScope.launch {
            val result = loanRepository.getConsentTextsSync(LOAN_CONSENT_TYPES)
            val texts = (result as? Resource.Success)?.data.orEmpty()
            _uiState.value = _uiState.value.copy(
                consentTexts = texts,
                acceptedConsentIds = emptySet(),
                consentsLoading = false,
                consentsFailed = texts.tickable().isEmpty()
            )
        }
    }

    /** Fills the form from what Pyllar already knows. Only blank fields are filled, so anything the user typed wins. */
    private fun prefill() {
        viewModelScope.launch {
            commonRepository.fetchScreenData("LoanApplication").collect { result ->
                if (result !is Resource.Success) return@collect
                val data = result.data?.data ?: return@collect
                fun text(key: String) = (data[key] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
                _uiState.value = _uiState.value.let { state ->
                    val v = state.values
                    state.copy(
                        values = v.copy(
                            panName = v.panName.ifBlank { text("panName") ?: "" },
                            pan = v.pan.ifBlank { text("pan") ?: "" },
                            dob = v.dob.ifBlank { text("dob") ?: "" },
                            contactNumber = v.contactNumber.ifBlank { text("contactNumber") ?: "" },
                            email = v.email.ifBlank { text("email") ?: "" },
                            pincode = v.pincode.ifBlank { text("pincode") ?: "" },
                            city = v.city.ifBlank { text("city") ?: "" },
                            state = v.state.ifBlank { text("state") ?: "" },
                            addressL1 = v.addressL1.ifBlank { text("addressL1") ?: "" },
                            addressL2 = v.addressL2.ifBlank { text("addressL2") ?: "" },
                            companyName = v.companyName.ifBlank { text("companyName") ?: "" },
                            officialEmail = v.officialEmail.ifBlank { text("officialEmail") ?: "" },
                            income = v.income.ifBlank { text("income") ?: "" },
                            // These two have defaults, so they can't be told apart from user input; the prefill lands
                            // as the form opens, before the user reaches them.
                            employmentType = text("employmentType") ?: v.employmentType,
                            incomeType = text("incomeType") ?: v.incomeType,
                            gender = text("gender") ?: v.gender,
                            endUse = text("endUse") ?: v.endUse,
                        )
                    )
                }
            }
        }
    }

    fun update(transform: (LoanFormValues) -> LoanFormValues) {
        _uiState.value = _uiState.value.copy(values = transform(_uiState.value.values), errorMessage = null)
    }

    fun setConsent(textId: String, accepted: Boolean) {
        val current = _uiState.value.acceptedConsentIds
        _uiState.value = _uiState.value.copy(acceptedConsentIds = if (accepted) current + textId else current - textId)
    }

    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting || !consentsComplete(state.consentTexts, state.acceptedConsentIds)) return
        val v = state.values
        val salaried = v.employmentType == "SALARIED"
        val request = LoanApplicationRequestDto(
            clientRequestId = clientRequestId,
            consentTextIds = state.consentTexts.tickable().map { it.id },
            applicant = LoanApplicantDto(
                contactNumber = v.contactNumber, pan = v.pan, panName = v.panName.trim(), dob = v.dob,
                gender = v.gender, email = v.email.trim(), pincode = v.pincode, city = v.city.trim(),
                state = v.state.trim(), addressL1 = v.addressL1.trim(), addressL2 = v.addressL2.trim(),
                employmentType = v.employmentType, income = v.income.toLongOrNull() ?: 0L,
                incomeType = v.incomeType,
                companyName = if (salaried) v.companyName.trim() else null,
                officialEmail = if (salaried) v.officialEmail.trim() else null,
                endUse = v.endUse, bureauConsent = true
            )
        )
        _uiState.value = state.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            val result = loanRepository.startLoanApplicationSync(request)
            _uiState.value = when (result) {
                is Resource.Success -> {
                    val id = result.data?.applicationId
                    if (id != null) {
                        _uiState.value.copy(isSubmitting = false, submittedApplicationId = id)
                    } else {
                        _uiState.value.copy(isSubmitting = false, errorMessage = "Something went wrong. Please try again.")
                    }
                }
                is Resource.Error -> {
                    if (result.message == "CONSENT_OUTDATED") loadConsents()
                    _uiState.value.copy(isSubmitting = false, errorMessage = friendlyError(result.message))
                }
                else -> _uiState.value.copy(isSubmitting = false)
            }
        }
    }

    fun onSubmittedHandled() {
        _uiState.value = _uiState.value.copy(submittedApplicationId = null)
    }

    private fun friendlyError(code: String?): String = when (code) {
        "TOO_MANY_ACTIVE" -> "You already have loan applications in progress."
        "PROVIDER_UNAVAILABLE" -> "Loans are not available right now. Please try again later."
        "CONSENT_REQUIRED" -> "Please accept the consent to continue."
        "CONSENT_OUTDATED" -> "The consent wording was just updated. Please read it and accept again."
        else -> "Could not submit your application. Please try again."
    }
}

