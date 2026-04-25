# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Pyllar** is a Kotlin Multiplatform (KMP) consumer app for mutual fund investments, targeting Android and iOS. All shared UI is built with Compose Multiplatform. The package root is `com.pyllar.consumer`.

## Build Commands

```bash
# Build Android debug APK
./gradlew :composeApp:assembleDebug

# Build Android release APK
./gradlew :composeApp:assembleRelease

# Run all tests
./gradlew :composeApp:allTests

# Run common tests only
./gradlew :composeApp:jvmTest
```

For iOS, open `iosApp/` in Xcode and run from there.

## Architecture

Clean architecture with three layers inside `composeApp/src/commonMain/`:

- **`data/`** — Remote DTOs, data sources, and repository implementations
  - `data/remote/network/` — Ktor HTTP client (`PyllarApiClient`)
  - `data/remote/datasource/` — Raw API call wrappers
  - `data/repository/` — `*RepositoryImpl` classes
- **`domain/`** — Interfaces and domain models consumed by the presentation layer
  - `domain/repository/` — Repository interfaces
  - `domain/storage/SessionStore` — Auth/session persistence interface
- **`presentation/`** — ViewModels and Compose screens, organized by feature

## Dependency Injection (Koin)

- `di/SharedModules.kt` — `sharedModule` wires all shared singletons and ViewModels
- `androidMain/.../di/AndroidModules.kt` — `androidPlatformModule(Context)` provides Android-specific implementations
- `iosMain/.../di/IosModules.kt` / `KoinInit.kt` — iOS Koin bootstrap

**Platform implementations required by `sharedModule`:** `SessionStore`, `DeviceInfoProvider`, `PushTokenProvider`, `AnalyticsTracker`, `UpdateManager` — all defined as interfaces in `platform/PlatformServices.kt` and `domain/storage/SessionStore.kt`.

## Expect/Actual Pattern

Used for platform-specific behaviour. Common `expect` declarations:

| File | Purpose |
|---|---|
| `config/Env.kt` — `expect fun getApiBaseUrl()` | Android reads `BuildConfig.BASE_URL`; iOS hardcoded in `Env.ios.kt` |
| `Platform.kt` — `expect fun getPlatform()` | Returns platform name string |
| `data/remote/network/HttpClientProvider.kt` | Configures OkHttp (Android) vs Darwin (iOS) engine |
| `util/PlatformTime.kt`, `util/PlatformLog.kt` | Time/logging shims |

## Backend URLs

- **Debug (Android):** `http://10.222.186.213:8080` (set in `build.gradle.kts` `buildTypes.debug`)
- **Release:** `https://api.pyllar.in`
- **iOS:** always `https://api.pyllar.in` (`Env.ios.kt`)

## Navigation

Routes are defined as a sealed class `AppRoutes` in `navigation/AppRoutes.kt`. Every destination is an `object` with a `route` string and a `createRoute(...)` factory method. Use `createRoute()` when navigating — never construct route strings manually.

## State Handling

All repository calls return `Resource<T>` (`util/Resource.kt`), a sealed class with `Success`, `Error`, and `Loading` subclasses. `Error` carries an `ErrorType` enum (`NETWORK_ERROR`, `SERVER_ERROR`, `VALIDATION_ERROR`, `AUTHENTICATION_ERROR`), optional `NavigationInfo` for server-driven navigation, and optional `fieldErrors` for form validation.

## Key Dependencies

| Library | Version | Purpose |
|---|---|---|
| Compose Multiplatform | 1.10.0 | Shared UI |
| Kotlin | 2.3.0 | Language |
| Ktor | 3.0.2 | HTTP client |
| Koin | 4.0.0 | DI |
| kotlinx.serialization | 1.7.3 | JSON |
| Lifecycle ViewModel Compose | 2.9.6 | ViewModels |
