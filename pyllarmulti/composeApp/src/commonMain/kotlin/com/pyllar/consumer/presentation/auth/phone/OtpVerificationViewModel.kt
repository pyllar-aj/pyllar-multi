package com.pyllar.consumer.presentation.auth.phone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.parser.ErrorType
import com.pyllar.consumer.data.remote.requests.OtpRegistrationRequest
import com.pyllar.consumer.data.remote.requests.OtpVerificationRequest
import com.pyllar.consumer.domain.models.AuthUserDTO
import com.pyllar.consumer.domain.repository.AuthRepository
import com.pyllar.consumer.platform.DeviceInfoProvider
import com.pyllar.consumer.platform.PushTokenProvider
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OtpVerificationViewModel(
    private val authRepository: AuthRepository,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val pushTokenProvider: PushTokenProvider
) : ViewModel() {

    private val _verificationResult = MutableStateFlow<Resource<AuthUserDTO>?>(null)
    val verificationResult: StateFlow<Resource<AuthUserDTO>?> = _verificationResult

    private val _otp = MutableStateFlow("")
    val otp: StateFlow<String> = _otp

    private val _phoneNumber = MutableStateFlow("")

    private val _resendResult = MutableStateFlow<Resource<String>?>(null)
    val resendResult: StateFlow<Resource<String>?> = _resendResult

    /** Prevents concurrent verify OTP calls (double submit). */
    private val _isVerifying = MutableStateFlow(false)

    private val _otpRef = MutableStateFlow<String?>(null)
    val otpRef: StateFlow<String?> = _otpRef

    fun setPhoneNumber(number: String) {
        _phoneNumber.value = number
    }

    fun setOtpRef(ref: String?) {
        _otpRef.value = ref
    }

    fun updateOtp(code: String) {
        if (code.length <= 6 && code.all { it.isDigit() }) {
            _otp.value = code
        }
    }

    fun verifyOtp() {
        if (_otp.value.length != 6) {
            _verificationResult.value = Resource.Error(
                "Please enter a valid 6-digit OTP",
                errorType = ErrorType.VALIDATION_ERROR
            )
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

                com.pyllar.consumer.util.platformLog("OtpVerificationViewModel: Verifying OTP for ${_phoneNumber.value} with id: ${_otpRef.value}")
                val request = OtpVerificationRequest(
                    phoneNumber = _phoneNumber.value,
                    otp = _otp.value,
                    id = _otpRef.value,
                    deviceId = deviceId,
                    deviceType = osName.lowercase(),
                    deviceModel = "",
                    osVersion = osVersion,
                    appVersion = appVersion,
                    pushToken = pushToken
                )
                authRepository.verifyOtp(request).collect { result ->
                    _verificationResult.value = result
                }
            } finally {
                _isVerifying.value = false
            }
        }
    }

    fun resendOtp() {
        viewModelScope.launch {
            _resendResult.value = Resource.Loading()

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

            authRepository.sendOtp(request).collect { result ->
                if (result is Resource.Success) {
                    _otpRef.value = result.data?.otpRef
                }
                _resendResult.value = when (result) {
                    is Resource.Success -> Resource.Success("OTP sent successfully")
                    is Resource.Error -> Resource.Error(result.message ?: "Failed to send OTP")
                    is Resource.Loading -> Resource.Loading()
                }
            }
        }
    }
}
