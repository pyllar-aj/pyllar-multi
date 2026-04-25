# Migration Progress

Track every file migrated from `/Users/aj/Projects/Pyllar/android` → `/Users/aj/Projects/pyllar-multi/pyllarmulti`.

**Format for each entry:**
```
- [x] `source/path/File.kt` → `target/sourceSet/path/File.kt` — brief note
```

Update this file after **every** completed item. Do not batch.

---

## Completed

### Infrastructure & Platform Abstraction
- [x] `Platform.kt` → `commonMain/.../Platform.kt` — expect declaration
- [x] `Platform.kt` → `androidMain/.../Platform.android.kt` — actual impl
- [x] `Platform.kt` → `iosMain/.../Platform.ios.kt` — actual impl
- [x] `platform/PlatformServices.kt` → `commonMain/.../platform/PlatformServices.kt` — interfaces for platform capabilities
- [x] `platform/AndroidPlatformServices.kt` → `androidMain/.../platform/AndroidPlatformServices.kt` — actual impl
- [x] `platform/AndroidPermissionManager.kt` → `androidMain/.../platform/AndroidPermissionManager.kt`
- [x] `platform/IosPlatformServices.kt` → `iosMain/.../platform/IosPlatformServices.kt`
- [x] `platform/IosPermissionManager.kt` → `iosMain/.../platform/IosPermissionManager.kt`

### Config / Environment
- [x] `config/Env.kt` → `commonMain/.../config/Env.kt` — expect getApiBaseUrl()
- [x] `config/Env.kt` → `androidMain/.../config/Env.android.kt` — actual, reads BuildConfig
- [x] `config/Env.kt` → `iosMain/.../config/Env.ios.kt` — actual, hardcoded release URL

### App Entry Points
- [x] `MainActivity.kt` → `androidMain/.../MainActivity.kt`
- [x] `PyllarApplication.kt` → `androidMain/.../PyllarApplication.kt`
- [x] (new) `MainViewController.kt` → `iosMain/.../MainViewController.kt` — SwiftUI entry

### Utilities
- [x] `util/Resource.kt` → `commonMain/.../util/Resource.kt` — Success/Error/Loading sealed class
- [x] `util/AppConstants.kt` → `commonMain/.../util/AppConstants.kt`
- [x] `util/TimeoutConfig.kt` → `commonMain/.../util/TimeoutConfig.kt`
- [x] `util/Log.kt` → `commonMain/.../util/Log.kt`
- [x] `util/PlatformLog.kt` → `commonMain/.../util/PlatformLog.kt` — expect
- [x] `util/PlatformLog.kt` → `androidMain/.../util/PlatformLog.android.kt` — actual
- [x] `util/PlatformLog.kt` → `iosMain/.../util/PlatformLog.ios.kt` — actual
- [x] `util/PlatformTime.kt` → `commonMain/.../util/PlatformTime.kt` — expect
- [x] `util/PlatformTime.kt` → `androidMain/.../util/PlatformTime.android.kt` — actual
- [x] `util/PlatformTime.kt` → `iosMain/.../util/PlatformTime.ios.kt` — actual

### Dependency Injection
- [x] `di/` → `commonMain/.../di/SharedModules.kt` — Koin sharedModule (Retrofit→Ktor, Hilt→Koin)
- [x] `di/` → `androidMain/.../di/AndroidModules.kt` — androidPlatformModule with Context deps
- [x] (new) `iosMain/.../di/IosModules.kt` — iOS Koin module
- [x] (new) `iosMain/.../di/KoinInit.kt` — iOS Koin bootstrap

### Navigation
- [x] `navigation/` → `commonMain/.../navigation/AppRoutes.kt` — sealed class route definitions
- [x] `navigation/` → `commonMain/.../navigation/ScreenRoutes.kt`
- [x] `navigation/` → `commonMain/.../navigation/ScreenNames.kt`
- [x] `navigation/NavigationConfiguration.kt` → `commonMain/.../navigation/NavigationConfiguration.kt` — adapted: removed Hilt/NavController, KMP-compatible config data classes
- [x] `navigation/NavigationExtensibility.kt` → `commonMain/.../navigation/NavigationExtensibility.kt` — adapted: NavController replaced with `(String)->Unit` callback pattern matching App.kt

### Domain Repository Interfaces
- [x] `data/repository/AuthRepository.kt` → `commonMain/.../domain/repository/AuthRepository.kt`
- [x] `data/repository/OnboardingRepository.kt` → `commonMain/.../domain/repository/OnboardingRepository.kt`
- [x] `data/repository/MutualFundRepository.kt` → `commonMain/.../domain/repository/MutualFundRepository.kt`
- [x] `data/repository/FundDetailsRepository.kt` → `commonMain/.../domain/repository/FundDetailsRepository.kt`
- [x] (new) `domain/repository/DashboardRepository.kt` → `commonMain/.../domain/repository/DashboardRepository.kt`
- [x] `data/repository/RedemptionRepository.kt` → `commonMain/.../domain/repository/RedemptionRepository.kt`

### Domain Storage
- [x] `data/local/` → `commonMain/.../domain/storage/SessionStore.kt` — auth persistence interface
- [x] `data/local/` → `androidMain/.../domain/storage/AndroidSessionStore.kt` — DataStore actual
- [x] (new) `iosMain/.../domain/storage/IosSessionStore.kt` — iOS actual

### Domain Models
- [x] `data/remote/model/dto/AuthTokenDTO.kt` → `commonMain/.../domain/models/AuthToken.kt` — @Serializable
- [x] `data/remote/model/dto/AuthUserDTO.kt` → `commonMain/.../domain/models/AuthUserDTO.kt`
- [x] `domain/models/LoginCredentials.kt` → `commonMain/.../domain/models/LoginCredentials.kt`
- [x] `domain/models/MutualFundModels.kt` → `commonMain/.../domain/models/MutualFundModels.kt`
- [x] `domain/models/OtpVerificationRequest.kt` → `commonMain/.../domain/models/OtpVerificationRequest.kt`
- [x] `domain/models/PhoneVerificationRequest.kt` → `commonMain/.../domain/models/PhoneVerificationRequest.kt`
- [x] `domain/models/PhoneVerificationResponse.kt` → `commonMain/.../domain/models/PhoneVerificationResponse.kt`
- [x] (new) `domain/models/UpdateEmailResponse.kt` → `commonMain/.../domain/models/UpdateEmailResponse.kt`

### Crypto Layer
- [x] `data/remote/crypto/SecureSessionData.kt` → `commonMain/.../data/remote/crypto/SecureSessionData.kt`
- [x] `data/remote/crypto/Hkdf.kt` → `commonMain/.../data/remote/crypto/Hkdf.kt`
- [x] `data/remote/crypto/SecureHandshakeCoordinator.kt` → `commonMain/.../data/remote/crypto/SecureHandshakeCoordinator.kt`
- [x] `data/remote/crypto/SecureSessionStore.kt` → `commonMain/.../data/remote/crypto/SecureSessionStore.kt`
- [x] `data/remote/crypto/SecurePayloadCrypto.kt` → `commonMain/.../data/remote/crypto/SecurePayloadCrypto.kt` — expect
- [x] `data/remote/crypto/SecurePayloadCrypto.kt` → `androidMain/.../data/remote/crypto/SecurePayloadCrypto.android.kt` — actual
- [x] `data/remote/crypto/SecurePayloadCrypto.kt` → `iosMain/.../data/remote/crypto/SecurePayloadCrypto.ios.kt` — actual
- [x] `data/remote/crypto/DateTimeUtils.kt` → `commonMain/.../data/remote/crypto/DateTimeUtils.kt` — expect
- [x] `data/remote/crypto/DateTimeUtils.kt` → `androidMain/.../data/remote/crypto/DateTimeUtils.android.kt` — actual
- [x] `data/remote/crypto/DateTimeUtils.kt` → `iosMain/.../data/remote/crypto/DateTimeUtils.ios.kt` — actual
- [x] (new) `data/remote/crypto/IosCryptoBridge.kt` → `commonMain/.../data/remote/crypto/IosCryptoBridge.kt`
- [x] (new) `data/remote/model/crypto/SecureHandshakeRequestDto.kt` → `commonMain/.../data/remote/model/crypto/`
- [x] (new) `data/remote/model/crypto/SecureHandshakeResponseDto.kt` → `commonMain/.../data/remote/model/crypto/`
- [x] (new) `data/remote/model/crypto/SecurePayloadEnvelopeDto.kt` → `commonMain/.../data/remote/model/crypto/`

### Networking
- [x] `data/remote/network/HttpClientProvider.kt` → `commonMain/.../data/remote/network/HttpClientProvider.kt` — expect
- [x] `data/remote/network/HttpClientProvider.kt` → `androidMain/.../data/remote/network/HttpClientProvider.android.kt` — OkHttp actual
- [x] `data/remote/network/HttpClientProvider.kt` → `iosMain/.../data/remote/network/HttpClientProvider.ios.kt` — Darwin actual
- [x] `data/remote/services/ApiService.kt` → `commonMain/.../data/remote/network/PyllarApiClient.kt` — Retrofit→Ktor
- [x] `data/remote/datasource/AuthRemoteDataSource.kt` → `commonMain/.../data/remote/datasource/AuthRemoteDataSource.kt`

### Remote DTOs
- [x] `data/remote/dto/PanFetchDto.kt` → `commonMain/.../data/remote/dto/PanFetchDto.kt`
- [x] `data/remote/dto/PreVerificationDtos.kt` → `commonMain/.../data/remote/dto/PreVerificationDtos.kt`
- [x] `data/remote/model/dto/AuthTokenDTO.kt` → `commonMain/.../data/remote/model/dto/AuthTokenDTO.kt`
- [x] `data/remote/model/dto/AuthUserDTO.kt` → `commonMain/.../data/remote/model/dto/AuthUserDTO.kt`
- [x] `data/remote/model/dto/CheckPanResponse.kt` → `commonMain/.../data/remote/model/dto/CheckPanResponse.kt`
- [x] `data/remote/dto/StandardApiResponse.kt` → `commonMain/.../data/remote/model/dto/StandardApiResponseDto.kt` — Moshi→@Serializable
- [x] `data/remote/dto/NavigationInfo.kt` → `commonMain/.../data/remote/model/dto/NavigationInfo.kt`
- [x] `data/remote/dto/FieldError.kt` → `commonMain/.../data/remote/model/dto/FieldError.kt`
- [x] (new) `data/remote/model/dto/FieldErrorDto.kt`
- [x] (new) `data/remote/model/dto/NavigationInfoDto.kt`
- [x] (new) `data/remote/model/dto/ApiResponseDtos.kt`
- [x] (new) `data/remote/model/dto/StandardApiResponseDtoRaw.kt`
- [x] (new) `data/remote/model/dto/AssetAllocationDto.kt`
- [x] (new) `data/remote/model/dto/CalendarDayDto.kt`
- [x] (new) `data/remote/model/dto/DashboardResponseDto.kt`
- [x] (new) `data/remote/model/dto/DashboardV2Dtos.kt`
- [x] (new) `data/remote/model/dto/EsignCreateResponseDto.kt`
- [x] (new) `data/remote/model/dto/FundDetailsDtos.kt`
- [x] (new) `data/remote/model/dto/FundInvestmentDto.kt`
- [x] (new) `data/remote/model/dto/FundPerformanceDto.kt`
- [x] (new) `data/remote/model/dto/GoalProgressDto.kt`
- [x] (new) `data/remote/model/dto/MandateResponseDtos.kt`
- [x] (new) `data/remote/model/dto/PortfolioGrowthPointDto.kt`
- [x] (new) `data/remote/model/dto/PortfolioSummaryDto.kt`
- [x] (new) `data/remote/model/dto/ProfileDtos.kt`
- [x] (new) `data/remote/model/dto/RecentTransactionDto.kt`
- [x] (new) `data/remote/model/dto/RedemptionOtpResponseDto.kt`
- [x] (new) `data/remote/model/dto/RedemptionOtpVerifyRequestDto.kt`
- [x] (new) `data/remote/model/dto/RedemptionRequest.kt`
- [x] (new) `data/remote/model/dto/RedemptionResponse.kt`
- [x] (new) `data/remote/model/dto/ScreenDataResponseDto.kt`
- [x] (new) `data/remote/model/dto/SipDetailsDto.kt`
- [x] (new) `data/remote/model/dto/SipOverallSummaryDto.kt`
- [x] (new) `data/remote/model/dto/SipPerformanceDto.kt`
- [x] (new) `data/remote/model/dto/TransactionDetailsDtos.kt`
- [x] (new) `data/remote/model/dto/UpcomingSipDto.kt`
- [x] (new) `data/remote/model/dto/UserSummaryDto.kt`
- [x] (new) `data/remote/model/dto/AccountDeletionResponseDto.kt`
- [x] (new) `data/remote/model/dto/CreateDailySipRequestDto.kt`
- [x] `data/remote/model/ResponseMetadata.kt` → `commonMain/.../data/remote/model/ResponseMetadata.kt`
- [x] `data/remote/model/SmsDataRequest.kt` → `commonMain/.../data/remote/model/SmsDataRequest.kt`
- [x] `data/remote/model/User.kt` → `commonMain/.../data/remote/model/User.kt`
- [x] (new) `data/remote/model/MinimalKycModels.kt`
- [x] (new) `data/remote/model/AdditionalKycRequest.kt`
- [x] (new) `data/remote/model/UpdateEmailRequest.kt`

### Remote Requests
- [x] `data/remote/requests/AuthRequests.kt` → `commonMain/.../data/remote/requests/AuthRequests.kt`
- [x] `data/remote/requests/CheckPanRequest.kt` → `commonMain/.../data/remote/requests/CheckPanRequest.kt`
- [x] `data/remote/requests/MutualFundRequests.kt` → `commonMain/.../data/remote/requests/MutualFundRequests.kt`
- [x] `data/remote/requests/MandateRequests.kt` → `commonMain/.../data/remote/requests/MandateRequests.kt`
- [x] `data/remote/requests/CreateNomineeRequest.kt` → `commonMain/.../data/remote/requests/CreateNomineeRequest.kt`
- [x] (new) `data/remote/requests/CreateNomineeRequestV2.kt`
- [x] (new) `data/remote/requests/AccountDeletionRequestDto.kt`
- [x] (new) `data/remote/requests/GoalSelectionRequest.kt`
- [x] (new) `data/remote/requests/HelperCodeRequest.kt`
- [x] (new) `data/remote/requests/NomineeDetailsRequest.kt`
- [x] (new) `data/remote/requests/TransactionDetailsRequest.kt`

### Remote Parser
- [x] `data/remote/parser/ErrorType.kt` → `commonMain/.../data/remote/parser/ErrorType.kt`
- [x] `data/remote/parser/ParsedResponse.kt` → `commonMain/.../data/remote/parser/ParsedResponse.kt`

### Repository Implementations
- [x] `data/repository/AuthRepositoryImpl.kt` → `commonMain/.../data/repository/AuthRepositoryImpl.kt` — Retrofit→Ktor, Hilt→Koin
- [x] `data/repository/OnboardingRepositoryImpl.kt` → `commonMain/.../data/repository/OnboardingRepositoryImpl.kt`
- [x] `data/repository/MutualFundRepositoryImpl.kt` → `commonMain/.../data/repository/MutualFundRepositoryImpl.kt`
- [x] `data/repository/FundDetailsRepositoryImpl.kt` → `commonMain/.../data/repository/FundDetailsRepositoryImpl.kt`
- [x] `data/repository/RedemptionRepositoryImpl.kt` → `commonMain/.../data/repository/RedemptionRepositoryImpl.kt`
- [x] (new) `data/repository/DashboardRepositoryImpl.kt` → `commonMain/.../data/repository/DashboardRepositoryImpl.kt`

### Presentation — Auth Screens
- [x] `presentation/auth/phone/` → `commonMain/.../presentation/auth/phone/PhoneVerificationScreen.kt`
- [x] `presentation/auth/phone/` → `commonMain/.../presentation/auth/phone/OtpVerificationScreen.kt`
- [x] `presentation/auth/phone/` → `commonMain/.../presentation/auth/phone/PhoneVerificationViewModel.kt` — LiveData→StateFlow, Hilt→Koin
- [x] `presentation/auth/phone/` → `commonMain/.../presentation/auth/phone/OtpVerificationViewModel.kt`
- [x] `presentation/auth/permission/` → `commonMain/.../presentation/auth/permission/PermissionViewModel.kt`
- [x] `presentation/auth/permission/` → `commonMain/.../presentation/auth/permission/MinimalPermissionScreen.kt`
- [x] `presentation/auth/permission/` → `commonMain/.../presentation/auth/permission/EmailInputSection.kt` — expect
- [x] `presentation/auth/permission/` → `androidMain/.../presentation/auth/permission/EmailInputSection.android.kt` — actual
- [x] `presentation/auth/permission/` → `iosMain/.../presentation/auth/permission/EmailInputSection.ios.kt` — actual

### Presentation — Dashboard
- [x] `presentation/dashboard/` → `commonMain/.../presentation/dashboard/InvestmentDashboardV2ViewModel.kt`
- [x] `presentation/dashboard/` → `commonMain/.../presentation/dashboard/InvestmentDashboardModels.kt`

### Presentation — UI Components
- [x] `presentation/ui/components/` → `commonMain/.../presentation/ui/components/TimeoutState.kt`

### Analytics
- [x] `analytics/AnalyticsLogger.kt` → `commonMain/.../analytics/AnalyticsLogger.kt` — expect PlatformAnalyticsLogger object
- [x] `analytics/AnalyticsLogger.kt` → `androidMain/.../analytics/AnalyticsLogger.kt` — Firebase + Clarity actual impl
- [x] (new) `iosMain/.../analytics/AnalyticsLogger.kt` — no-op iOS actual

### Push Notifications
- [x] `push/TokenStore.kt` → `androidMain/.../push/TokenStore.kt` — full DataStore TokenStore (was stub, now updated)
- [x] `push/PyllarFirebaseMessagingService.kt` → `androidMain/.../push/PyllarFirebaseMessagingService.kt` — stays in androidMain, AnalyticsLogger→PlatformAnalyticsLogger

### Onboarding Local Data Layer (SharedPreferences — Room pending)
- [x] `data/local/KeyValueConstants.kt` → `commonMain/.../data/local/KeyValueConstants.kt` — no Android deps, safe to share
- [x] (new) `commonMain/.../data/local/LocalOnboardingStore.kt` — interface + OnboardingStep + OnboardingStateSnapshot
- [x] `data/local/OnboardingRepository.kt` → `androidMain/.../data/local/AndroidLocalOnboardingStore.kt` — SharedPreferences impl (Room deferred until dep added)
- [x] `data/local/OnboardingStateStore.kt` — OnboardingStep/OnboardingState extracted to commonMain LocalOnboardingStore.kt
- [x] (new) `iosMain/.../data/local/IosLocalOnboardingStore.kt` — in-memory iOS placeholder
- [ ] Room entities/DAOs/PyllarDatabase — created as reference but removed from androidMain (Room not in KMP gradle yet)
  - TODO: add `implementation("androidx.room:room-runtime:2.x.x")` + KSP compiler to androidMain deps to re-enable Room

### ViewModels — Migrated
- [x] `presentation/auth/login/AuthViewModel.kt` → `commonMain/.../presentation/auth/login/AuthViewModel.kt` — LiveData→StateFlow, Hilt→Koin
- [x] `presentation/mutualfund/onboarding/OnboardingViewModel.kt` → `commonMain/.../presentation/mutualfund/onboarding/OnboardingViewModel.kt` — SimpleDateFormat→Regex
- [x] `presentation/mutualfund/portfolio/PortfolioViewModel.kt` → `commonMain/.../presentation/mutualfund/portfolio/PortfolioViewModel.kt`
- [x] `presentation/mutualfund/sip/SipViewModel.kt` → `commonMain/.../presentation/mutualfund/sip/SipViewModel.kt`
- [x] `presentation/mutualfund/upi/UpiAccountLinkingViewModel.kt` → `androidMain` (UPI is Android-only) — Hilt→Koin
- [x] `presentation/mutualfund/upi/UpiMandateSetupViewModel.kt` → `androidMain` (UPI is Android-only) — java.util.Calendar→System.currentTimeMillis()

### In-App Update / Review
- [x] `update/InAppUpdateManager.kt` → `androidMain/.../update/AndroidUpdateManager.kt` — implements shared UpdateManager interface, Hilt removed, AnalyticsLogger→PlatformAnalyticsLogger

### Repository — Audit & Merge (Evaluated)
- [x] `data/repository/SimpleAuthRepository.kt` — **not present** in source project (only PreVerificationRepository.kt exists); no migration needed
- [x] `data/repository/CommonRepository.kt` — **not present** in source project; no migration needed
- [x] `data/repository/BaseRepository.kt` — **not present** in source project; `parser/ParsedResponse.kt` already covers error handling; no migration needed

### Misc
- [x] (new) `com/pyllar/otp/OtpField.kt` — shared OTP input component

---

## Todo

### Compose Screens — Not Yet Migrated
- [ ] `presentation/auth/login/` screens → `commonMain/.../presentation/auth/login/`
- [ ] `presentation/auth/signup/` screens → `commonMain/.../presentation/auth/signup/`
- [ ] `presentation/mutualfund/onboarding/` screens → `commonMain/.../presentation/mutualfund/onboarding/`
- [ ] `presentation/mutualfund/portfolio/` screens → `commonMain/.../presentation/mutualfund/portfolio/`
- [ ] `presentation/mutualfund/sip/` screens → `commonMain/.../presentation/mutualfund/sip/`
- [ ] `presentation/mutualfund/upi/` screens → `androidMain` (UPI Android-only)
- [ ] `presentation/mutualfund/details/` screens → `commonMain`
- [ ] `presentation/home/` screens → `commonMain`
- [ ] `presentation/dashboard/` remaining screens → `commonMain`
- [ ] `presentation/profile/` screens → `commonMain`
- [ ] `presentation/support/` screens → `commonMain`
- [ ] `presentation/notification/` screens → `commonMain`

### UI Components & Theme
- [ ] `presentation/ui/components/` remaining components → `commonMain/.../presentation/ui/components/` — remove android.graphics imports
- [ ] `presentation/ui/theme/TrueWhite.kt` → `commonMain/.../presentation/ui/theme/`

---

## Discovered During Migration

> Add items here as you find unlisted dependencies while migrating.

- `data/repository/PreVerificationRepository.kt` (Android source) depends on `PreVerificationApiService` (Retrofit), not yet ported to Ktor — needs evaluation if this endpoint is actively used
- `UpiAccountLinkingViewModel` and `UpiMandateSetupViewModel` depend on a `UpiService` class not present in the KMP target; `UpiService` logic was replaced with inline PM queries and SDK placeholder comments
- `PyllarFirebaseMessagingService` references `R.mipmap.ic_launcher` — replaced with `android.R.drawable.ic_dialog_info` as a temporary fallback until the drawable resource is confirmed present
