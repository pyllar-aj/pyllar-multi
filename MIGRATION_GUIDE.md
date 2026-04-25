# Android → KMP Migration Guide

This document instructs an AI agent on how to migrate remaining code from the native Android project at `/Users/aj/Projects/Pyllar/android` into the Kotlin Multiplatform project at `/Users/aj/Projects/pyllar-multi/pyllarmulti`.

## Tracking Requirement

You **must** maintain a live progress file at `/Users/aj/Projects/pyllar-multi/MIGRATION_PROGRESS.md`.

Rules:
- **Before starting any work**, read `MIGRATION_PROGRESS.md` to understand what has already been done and what is pending.
- **After completing each file or task**, immediately move it from the `## Todo` section to the `## Completed` section in `MIGRATION_PROGRESS.md`. Do not batch updates — update after every single item.
- **When you discover new work** not already listed (e.g. a dependency pulled in by a migrated file), add it to `## Todo` before starting it.
- **Never re-migrate** anything already in `## Completed`.
- Keep entries specific: include the source file path, target file path, and a one-line note on what was changed (e.g. "Moshi → @Serializable, Hilt → Koin").

---

## Project Context

| | Native Android (`Pyllar`) | KMP (`pyllar-multi`) |
|---|---|---|
| Root package | `com.pyllar.consumer` | `com.pyllar.consumer` |
| UI | Jetpack Compose | Compose Multiplatform |
| HTTP | Retrofit 2.9.0 + Moshi | Ktor 3.0.2 |
| DI | Hilt 2.50 | Koin 4.0.0 |
| DB | Room 2.6.1 | Not yet migrated |
| Serialization | Moshi codegen | `kotlinx.serialization` |
| Storage | DataStore Preferences | `SessionStore` expect/actual |
| Min SDK | 23 | 24 |
| Kotlin | 1.9.23 | 2.3.0 |

The KMP project already has the core networking, crypto, auth, and repository layer migrated. The goal is to bring over all remaining screens, ViewModels, and supporting code so the app builds and runs on both Android and iOS.

---

## Source Set Rules

Every file you migrate must land in the correct source set:

- **`commonMain`** — business logic, ViewModels, domain models, DTOs, repository implementations, navigation. Anything that compiles for both platforms.
- **`androidMain`** — Android-only implementations: `actual` declarations, Firebase, UPI, SMS retriever, Room, DataStore.
- **`iosMain`** — iOS `actual` declarations only. No business logic.

When in doubt, start in `commonMain`. Only move to a platform source set when you hit a platform API that cannot be abstracted.

---

## Technology Mapping (what to replace with what)

### Networking
| Old (Retrofit) | New (Ktor) |
|---|---|
| `interface ApiService` with `@GET`/`@POST` annotations | Suspend functions in `PyllarApiClient.kt` using `client.get<T> {}` / `client.post<T> {}` |
| `Call<T>` / `Response<T>` | Direct return value or `ParsedResponse<T>` |
| `@Body`, `@Query`, `@Path` | Ktor `setBody()`, `parameter()`, string interpolation in URL |
| `Moshi` `@Json` annotations | `@Serializable` + `@SerialName` from `kotlinx.serialization` |
| `MoshiConverterFactory` | Already configured in `PyllarApiClient.kt` — add new endpoints there |

### Dependency Injection
| Old (Hilt) | New (Koin) |
|---|---|
| `@HiltViewModel` + `@Inject constructor` | `ViewModel` with constructor params; declare in `SharedModules.kt` via `viewModel { MyViewModel(get()) }` |
| `@Module @InstallIn(SingletonComponent)` | `module { single { ... } }` blocks in `SharedModules.kt` or `AndroidModules.kt` |
| `@Inject lateinit var` in Activities/Composables | `val vm: MyViewModel = koinViewModel()` in Composables |
| Hilt `@Provides` for Context-dependent deps | Put Context-requiring deps in `AndroidModules.kt` (`androidPlatformModule`) |

### Data Models / Serialization
| Old | New |
|---|---|
| `data class Foo(@Json(name="bar") val bar: String)` | `@Serializable data class Foo(@SerialName("bar") val bar: String)` |
| Moshi `JsonClass(generateAdapter = true)` | Remove — `@Serializable` handles this |
| Enums with Moshi adapters | `@Serializable enum class` — add `@SerialName` per value if JSON strings differ |

### Local Storage
| Old | New |
|---|---|
| Room `@Entity`, `@Dao`, `@Database` | No shared Room equivalent yet — see [Room Migration](#room-and-datastore) below |
| DataStore `Preferences` | `SessionStore` interface (expect/actual) in `domain/storage/` |
| `PyllarDatabase.kt` | Remains in `androidMain` for now; extract access behind a `commonMain` interface |

### ViewModels
| Old | New |
|---|---|
| `import androidx.lifecycle.ViewModel` | `import androidx.lifecycle.ViewModel` — same import works in `commonMain` via `androidx.lifecycle` multiplatform artifact |
| `viewModelScope.launch` | `viewModelScope.launch` — identical in KMP |
| `LiveData<T>` | Replace with `StateFlow<T>` / `MutableStateFlow<T>` |
| `MutableLiveData` | `MutableStateFlow` |

### Navigation
| Old | New |
|---|---|
| `NavController`, `NavHost` with string routes | `AppRoutes` sealed class in `navigation/AppRoutes.kt` — add new routes there |
| `navController.navigate("route")` | Pass lambda callbacks from `App.kt` down to composables (existing pattern) |

---

## Migration Checklist by Layer

Work top-to-bottom (domain → data → presentation). Each layer builds on the one below.

### 1. Domain Models (`domain/models/`)

Source: `android/app/src/main/java/com/pyllar/consumer/domain/models/`

- [ ] `AuthToken.kt` — add `@Serializable`, remove Moshi annotations
- [ ] `LoginCredentials.kt`
- [ ] `OtpVerificationRequest.kt`
- [ ] `PhoneVerificationRequest.kt` / `PhoneVerificationResponse.kt`
- [ ] `MutualFundModels.kt` — large file; split into logical sub-files if >200 lines

Target: `composeApp/src/commonMain/kotlin/com/pyllar/consumer/domain/models/`

### 2. Remote DTOs (`data/remote/dto/` and `data/remote/model/dto/`)

Source: `android/app/src/main/java/com/pyllar/consumer/data/remote/dto/`  
Source: `android/app/src/main/java/com/pyllar/consumer/data/remote/model/dto/`

- [ ] All `*Dto.kt` and `*Response.kt` files
- [ ] `StandardApiResponse.kt`, `NavigationInfo.kt`, `FieldError.kt`
- [ ] Existing `data/remote/model/dto/` files in KMP — check for duplicates before adding

Replace all Moshi annotations with `@Serializable` / `@SerialName`.

Target: `composeApp/src/commonMain/kotlin/com/pyllar/consumer/data/remote/dto/`

### 3. API Endpoints

Source: `android/app/src/main/java/com/pyllar/consumer/data/remote/services/` (Retrofit `ApiService`)

- [ ] For every Retrofit endpoint not yet in `PyllarApiClient.kt`, add a corresponding suspend function
- [ ] Group endpoints by domain (auth, mutual fund, onboarding, etc.)

Target: `composeApp/src/commonMain/kotlin/com/pyllar/consumer/data/remote/network/PyllarApiClient.kt`

### 4. Repository Implementations

Source: `android/app/src/main/java/com/pyllar/consumer/data/repository/`

Check each against what already exists in `composeApp/src/commonMain/kotlin/com/pyllar/consumer/data/repository/`:

- [ ] `AuthRepositoryImpl.kt` — partially migrated; diff and fill gaps
- [ ] `CommonRepository.kt` — evaluate if logic belongs in a shared util or specific repo
- [ ] `SimpleAuthRepository.kt` — merge or replace `AuthRepositoryImpl` if redundant
- [ ] `OnboardingRepositoryImpl.kt` — partially migrated
- [ ] `MutualFundRepositoryImpl.kt` — partially migrated
- [ ] `FundDetailsRepositoryImpl.kt` — partially migrated
- [ ] `RedemptionRepositoryImpl.kt` — partially migrated
- [ ] `BaseRepository.kt` — check if the KMP error handling in `parser/` already covers this

Register any new repos in `di/SharedModules.kt`.

### 5. Onboarding Data Layer

Source: `android/app/src/main/java/com/pyllar/consumer/data/local/`

- [ ] Extract `OnboardingRepository` and `OnboardingStateStore` logic into a `commonMain` interface
- [ ] Keep Room DAOs (`OnboardingAttemptDao`, `OnboardingStateDao`, `UserProfileDao`, `KeyValueDao`) in `androidMain` behind that interface
- [ ] Move `KeyValueConstants.kt` to `commonMain` (no Android dependencies)

### 6. Navigation Routes

Source: `android/app/src/main/java/com/pyllar/consumer/navigation/`

- [ ] Map every destination in `NavigationConfiguration.kt` to a route object in `AppRoutes.kt` / `ScreenRoutes.kt`
- [ ] For `NavigationExtensibility.kt` patterns, adapt to the existing callback-based navigation in `App.kt`

### 7. ViewModels

Source: `android/app/src/main/java/com/pyllar/consumer/presentation/`

Migrate to `composeApp/src/commonMain/kotlin/com/pyllar/consumer/presentation/`:

- [ ] `auth/login/AuthViewModel.kt`
- [ ] `mutualfund/onboarding/OnboardingViewModel.kt`
- [ ] `mutualfund/portfolio/PortfolioViewModel.kt`
- [ ] `mutualfund/sip/SipViewModel.kt`
- [ ] `mutualfund/upi/UpiAccountLinkingViewModel.kt`
- [ ] `mutualfund/upi/UpiMandateSetupViewModel.kt`

For each ViewModel:
1. Replace `LiveData` with `StateFlow`
2. Remove `@HiltViewModel` / `@Inject` — add to Koin `viewModel { }` block
3. Remove any direct Android context usage — inject through a `PlatformServices` interface if needed
4. UPI ViewModels: UPI is Android-only; put the ViewModel in `androidMain` and expose a no-op/stub via expect/actual for iOS

### 8. Compose Screens

Source: `android/app/src/main/java/com/pyllar/consumer/presentation/` (`*Screen.kt`, `*View.kt` composables)

Migrate to `commonMain`. Follow these rules:

- Use only Compose Multiplatform APIs (no `android.widget.*`, no `LocalContext.current` in shared code)
- For Android-only UI (UPI screens, SMS consent UI): place in `androidMain` and conditionally include in the nav graph via an expect/actual factory
- Replace `Accompanist` Pager → `HorizontalPager` from `foundation` (already in CMP)
- Replace `Accompanist` SwipeRefresh → `PullRefreshIndicator` from CMP
- Lottie animations: use an expect/actual `LottieAnimation` composable (Lottie CMP exists; iOS uses a no-op or Lottie Swift bridge)
- YouTube player: Android-only — wrap in an `androidMain` composable

### 9. UI Components and Theme

Source: `android/app/src/main/java/com/pyllar/consumer/presentation/ui/`

- [ ] Move shared components from `ui/components/` to `commonMain/presentation/ui/components/`
- [ ] Move theme files (`TrueWhite.kt`, colors, typography) to `commonMain/presentation/ui/theme/`
- [ ] Remove any `android.graphics.Color` imports — use Compose `Color` only

### 10. Push Notifications

Source: `android/app/src/main/java/com/pyllar/consumer/push/TokenStore.kt`  
Source: `android/app/src/main/java/com/pyllar/consumer/PyllarFirebaseMessagingService`

- [ ] `TokenStore.kt` — already exists in `androidMain`; verify it is current
- [ ] Firebase Messaging: stays in `androidMain` entirely — no iOS equivalent in this repo yet
- [ ] Define a `PushTokenProvider` interface in `commonMain` if the token needs to be read from shared code

### 11. Analytics

Source: `android/app/src/main/java/com/pyllar/consumer/analytics/`

- [ ] Define an `AnalyticsLogger` interface in `commonMain`
- [ ] Keep the Firebase/Clarity implementation in `androidMain/presentation/analytics/AnalyticsLogger.kt` (already exists)
- [ ] Add a no-op iOS implementation in `iosMain`
- [ ] Register via Koin in the respective platform modules

### 12. In-App Update / Review

Source: `android/app/src/main/java/com/pyllar/consumer/update/`

- [ ] Android-only; keep entirely in `androidMain`
- [ ] Expose a `AppUpdateManager` interface in `commonMain` if update prompts are triggered from shared ViewModel logic

---

## Room and DataStore

Room is not yet available as a multiplatform library stable enough for production. Strategy:

1. Keep `PyllarDatabase.kt` and all DAOs in `androidMain`
2. Define repository interfaces in `commonMain` that abstract the DAOs
3. The `androidMain` implementations inject the Room DAOs
4. For iOS, implement the same interfaces with in-memory or file-based storage as a placeholder

DataStore Preferences → already handled by `SessionStore` expect/actual. Extend this pattern for any new persistent keys.

---

## DI Registration Pattern

After migrating each component, register it in the correct Koin module:

```kotlin
// composeApp/src/commonMain/kotlin/com/pyllar/consumer/di/SharedModules.kt
val sharedModule = module {
    // Repositories
    single<AuthRepository> { AuthRepositoryImpl(get()) }

    // ViewModels
    viewModel { AuthViewModel(get()) }
    viewModel { PortfolioViewModel(get()) }
}

// composeApp/src/androidMain/kotlin/com/pyllar/consumer/di/AndroidModules.kt
fun androidPlatformModule(context: Context) = module {
    // Android-only deps that need Context
    single { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
}
```

---

## Expect / Actual Pattern

When a `commonMain` file needs platform-specific behavior, use expect/actual:

```kotlin
// commonMain
expect fun getPlatformName(): String

// androidMain
actual fun getPlatformName(): String = "Android"

// iosMain
actual fun getPlatformName(): String = "iOS"
```

Existing expect/actual contracts to be aware of:
- `Platform.kt` — platform name
- `config/Env.kt` — `getApiBaseUrl()`
- `data/remote/network/HttpClientProvider.kt` — Ktor engine
- `data/remote/crypto/SecurePayloadCrypto.kt` — encryption
- `data/remote/crypto/DateTimeUtils.kt` — time handling
- `domain/storage/SessionStore.kt` — auth persistence
- `util/PlatformLog.kt`, `PlatformTime.kt`
- `presentation/auth/permission/EmailInputSection.kt`

---

## File-by-File Migration Steps

For each file migrated:

1. **Read** the source file at `/Users/aj/Projects/Pyllar/android/app/src/main/java/com/pyllar/consumer/<path>`
2. **Determine target source set** (commonMain / androidMain / iosMain) using the rules above
3. **Adapt** the code:
   - Swap Hilt → Koin (remove annotations, add to module)
   - Swap Retrofit → Ktor
   - Swap Moshi → `@Serializable`
   - Swap `LiveData` → `StateFlow`
   - Remove Android-only imports where the code is going to `commonMain`
4. **Write** to `composeApp/src/<sourceSet>/kotlin/com/pyllar/consumer/<path>`
5. **Register** in Koin if it's a repository or ViewModel
6. **Add to navigation** if it's a new screen

---

## What Is Already Done (Do Not Re-migrate)

The following are already present and working in `pyllar-multi`:

- `data/remote/network/PyllarApiClient.kt` — Ktor client
- `data/remote/crypto/` — full secure session / HKDF crypto layer
- `data/repository/AuthRepositoryImpl.kt`, `OnboardingRepositoryImpl.kt`, `MutualFundRepositoryImpl.kt`, `FundDetailsRepositoryImpl.kt`, `DashboardRepositoryImpl.kt`, `RedemptionRepositoryImpl.kt`
- `domain/repository/` — all interfaces
- `domain/storage/SessionStore.kt` + platform implementations
- `presentation/auth/phone/OtpVerificationViewModel.kt`, `PhoneVerificationViewModel.kt`
- `presentation/auth/permission/PermissionViewModel.kt`
- `di/SharedModules.kt`, `AndroidModules.kt`, `IosModules.kt`
- `navigation/AppRoutes.kt`, `ScreenRoutes.kt`, `ScreenNames.kt`
- `util/Resource.kt`, `PlatformLog.kt`, `PlatformTime.kt`, `AppConstants.kt`
- `config/Env.kt` (expect/actual)
- `push/TokenStore.kt` (androidMain)

---

## Gradle — Adding New Dependencies

The version catalog is at `pyllarmulti/gradle/libs.versions.toml`. Add new versions and libraries there, then reference them in `composeApp/build.gradle.kts`.

Do **not** add Hilt, Retrofit, Moshi, or Accompanist. They are Android-only and replaced.

If a KMP-compatible library is needed (e.g., Lottie CMP, SQLDelight for Room replacement), add it to `commonMain` dependencies. Platform-specific libraries go in the `androidMain` or `iosMain` source set dependency blocks.

---

## Build Verification

After each migration batch:

```bash
cd /Users/aj/Projects/pyllar-multi/pyllarmulti

# Check shared code compiles
./gradlew :composeApp:compileKotlinIosArm64

# Check Android builds
./gradlew :composeApp:assembleDebug

# Check iOS framework
./gradlew :composeApp:linkDebugFrameworkIosArm64
```

Fix compile errors before moving to the next layer. Never leave expect declarations without actual implementations.
