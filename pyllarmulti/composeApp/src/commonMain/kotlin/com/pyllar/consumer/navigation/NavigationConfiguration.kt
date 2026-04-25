package com.pyllar.consumer.navigation

/**
 * Navigation behavior configuration for the KMP app.
 *
 * Adapted from the Android-only NavigationConfiguration:
 *  - Removed Hilt @Singleton / @Inject — instantiated once and held in Koin if needed
 *  - Removed NavController references (Android artifact) — KMP uses callback-based navigation
 *  - Removed NavigationAction import (replaces with plain string error screen key)
 */
class NavigationConfiguration {

    data class NavigationBehaviorConfig(
        val enableAutomaticNavigation: Boolean = true,
        val enableNavigationInterceptors: Boolean = true,
        val enableCustomRouteHandlers: Boolean = true,
        val enableRouteValidation: Boolean = true,
        val navigationTimeout: Long = 5000L,
        val maxRetryAttempts: Int = 3,
        val enableNavigationLogging: Boolean = false,
        val enableNavigationAnalytics: Boolean = true,
        val fallbackScreen: String? = null,
        val errorScreen: String = "error",
        val enableDeepLinking: Boolean = true,
        val enableBackStackManagement: Boolean = true
    )

    data class ScreenMappingConfig(
        val enableServerScreenMapping: Boolean = true,
        val enableCustomScreenMapping: Boolean = true,
        val enableDirectRouteMapping: Boolean = true,
        val caseSensitiveMapping: Boolean = false,
        val maxCustomMappings: Int = 100
    )

    data class ErrorHandlingConfig(
        val enableGlobalErrorHandling: Boolean = true,
        val enableCustomErrorHandlers: Boolean = true,
        val enableErrorRecovery: Boolean = true,
        val enableErrorLogging: Boolean = true,
        val enableErrorAnalytics: Boolean = true,
        val maxErrorRetries: Int = 3,
        val errorRetryDelay: Long = 1000L,
        val enableFallbackNavigation: Boolean = true,
        val showErrorToUser: Boolean = true,
        val enableErrorReporting: Boolean = false
    )

    data class PerformanceConfig(
        val enableNavigationCaching: Boolean = true,
        val navigationCacheSize: Int = 20,
        val enableLazyLoading: Boolean = true,
        val maxConcurrentNavigations: Int = 1,
        val navigationQueueSize: Int = 10
    )

    data class SecurityConfig(
        val enableRouteValidation: Boolean = true,
        val enableParameterSanitization: Boolean = true,
        val enableDeepLinkValidation: Boolean = true,
        val allowedDeepLinkDomains: Set<String> = emptySet(),
        val blockedRoutes: Set<String> = emptySet(),
        val requireAuthenticationForRoutes: Set<String> = emptySet(),
        val maxParameterLength: Int = 1000,
        val enableInputValidation: Boolean = true
    )

    private var behaviorConfig = NavigationBehaviorConfig()
    private var screenMappingConfig = ScreenMappingConfig()
    private var errorHandlingConfig = ErrorHandlingConfig()
    private var performanceConfig = PerformanceConfig()
    private var securityConfig = SecurityConfig()

    fun updateBehaviorConfig(config: NavigationBehaviorConfig) { behaviorConfig = config }
    fun getBehaviorConfig(): NavigationBehaviorConfig = behaviorConfig

    fun updateScreenMappingConfig(config: ScreenMappingConfig) { screenMappingConfig = config }
    fun getScreenMappingConfig(): ScreenMappingConfig = screenMappingConfig

    fun updateErrorHandlingConfig(config: ErrorHandlingConfig) { errorHandlingConfig = config }
    fun getErrorHandlingConfig(): ErrorHandlingConfig = errorHandlingConfig

    fun updatePerformanceConfig(config: PerformanceConfig) { performanceConfig = config }
    fun getPerformanceConfig(): PerformanceConfig = performanceConfig

    fun updateSecurityConfig(config: SecurityConfig) { securityConfig = config }
    fun getSecurityConfig(): SecurityConfig = securityConfig

    // ─── Validation ─────────────────────────────────────────────────────────

    data class ConfigurationValidationResult(
        val isValid: Boolean,
        val errors: List<String>,
        val warnings: List<String>
    )

    fun validateConfiguration(): ConfigurationValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (behaviorConfig.navigationTimeout <= 0) errors.add("Navigation timeout must be positive")
        if (behaviorConfig.maxRetryAttempts < 0) errors.add("Max retry attempts cannot be negative")
        if (performanceConfig.navigationCacheSize <= 0) warnings.add("Navigation cache size should be positive for optimal performance")
        if (performanceConfig.maxConcurrentNavigations <= 0) errors.add("Max concurrent navigations must be positive")
        if (securityConfig.maxParameterLength <= 0) errors.add("Max parameter length must be positive")

        return ConfigurationValidationResult(isValid = errors.isEmpty(), errors = errors, warnings = warnings)
    }

    // ─── Preset helpers ──────────────────────────────────────────────────────

    companion object {
        fun development(): NavigationConfiguration = NavigationConfiguration().also { cfg ->
            cfg.updateBehaviorConfig(cfg.getBehaviorConfig().copy(
                enableNavigationLogging = true,
                enableNavigationAnalytics = false,
                maxRetryAttempts = 1,
                navigationTimeout = 10_000L
            ))
            cfg.updateErrorHandlingConfig(cfg.getErrorHandlingConfig().copy(
                enableErrorLogging = true,
                enableErrorAnalytics = false,
                enableErrorReporting = true,
                showErrorToUser = true,
                maxErrorRetries = 1
            ))
        }

        fun production(): NavigationConfiguration = NavigationConfiguration().also { cfg ->
            cfg.updateBehaviorConfig(cfg.getBehaviorConfig().copy(
                enableNavigationLogging = false,
                enableNavigationAnalytics = true,
                maxRetryAttempts = 3,
                navigationTimeout = 5_000L
            ))
            cfg.updateSecurityConfig(cfg.getSecurityConfig().copy(
                enableRouteValidation = true,
                enableParameterSanitization = true,
                enableDeepLinkValidation = true,
                enableInputValidation = true
            ))
        }
    }
}
