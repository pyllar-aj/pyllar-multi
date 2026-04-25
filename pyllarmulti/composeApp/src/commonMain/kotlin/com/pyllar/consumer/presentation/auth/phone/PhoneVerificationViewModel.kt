package com.pyllar.consumer.presentation.auth.phone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.parser.ErrorType
import com.pyllar.consumer.data.remote.requests.OtpRegistrationRequest
import com.pyllar.consumer.domain.models.AuthToken
import com.pyllar.consumer.domain.repository.AuthRepository
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.platform.DeviceInfoProvider
import com.pyllar.consumer.platform.PushTokenProvider
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PhoneVerificationViewModel(
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val pushTokenProvider: PushTokenProvider
) : ViewModel() {

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber

    private val _verificationResult = MutableStateFlow<Resource<AuthToken>?>(null)
    val verificationResult: StateFlow<Resource<AuthToken>?> = _verificationResult

    /** Prevents concurrent verification calls (double submit). */
    private val _isVerifying = MutableStateFlow(false)

    private val _hasLanguagePreference = MutableStateFlow(false)
    val hasLanguagePreference: StateFlow<Boolean> = _hasLanguagePreference

    init {
        viewModelScope.launch {
            val stored = sessionStore.getValue(LANGUAGE_PREFERENCE_KEY)
            _hasLanguagePreference.value = !stored.isNullOrBlank()
        }
    }

    fun updatePhoneNumber(number: String) {
        if (number.length <= 10 && number.all { it.isDigit() }) {
            _phoneNumber.value = number
        }
    }

    fun verifyPhoneNumber() {
        if (_phoneNumber.value.length != 10) {
            _verificationResult.value = Resource.Error("Please enter a valid 10-digit phone number", errorType = com.pyllar.consumer.data.remote.parser.ErrorType.VALIDATION_ERROR)
            return
        }
        if (_isVerifying.value) return

        _isVerifying.value = true
        viewModelScope.launch {
            try {
                _verificationResult.value = Resource.Loading()

                val deviceId = deviceInfoProvider.getDeviceId().orEmpty()
                val osName = deviceInfoProvider.getOsName()
                val osVersion = deviceInfoProvider.getOsVersion()
                val appVersion = deviceInfoProvider.getAppVersion().orEmpty()
                val pushToken = pushTokenProvider.getPushToken().orEmpty()

                val request = OtpRegistrationRequest(
                    phoneNumber = _phoneNumber.value,
                    name = "User",
                    deviceId = deviceId,
                    deviceType = osName.lowercase(),
                    deviceModel = "",
                    osVersion = osVersion,
                    appVersion = appVersion,
                    pushToken = pushToken
                )

                authRepository.sendOtp(request).collect {
                    _verificationResult.value = it
                }
            } finally {
                _isVerifying.value = false
            }
        }
    }

    /** Persist user language preference to local DB (key_value_store). Call when user selects language or taps Continue. */
    fun saveLanguagePreference(languageTag: String) {
        viewModelScope.launch {
            sessionStore.saveValue(LANGUAGE_PREFERENCE_KEY, languageTag)
            _hasLanguagePreference.value = true
        }
    }

    private companion object {
        const val LANGUAGE_PREFERENCE_KEY = "language_preference"
    }
}
