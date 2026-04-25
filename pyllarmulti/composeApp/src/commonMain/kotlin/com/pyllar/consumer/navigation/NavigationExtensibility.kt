package com.pyllar.consumer.navigation

/**
 * Extensibility framework for custom navigation handling.
 *
 * Adapted from the Android-only NavigationExtensibility:
 *  - Removed Hilt @Singleton / @Inject
 *  - Removed NavController (Android artifact) — handlers now receive a callback (String) -> Unit
 *  - Removed NavigationInfo import — interceptors now receive route strings and params
 *  - Patterns adapted to KMP callback-based navigation as defined in App.kt
 */
class NavigationExtensibility {

    // ─── Custom Route Handlers ────────────────────────────────────────────────

    /**
     * Handler for custom navigation routes.
     * [navigate] is a callback that accepts a route string; it maps to
     * the lambda callbacks passed down from App.kt.
     */
    interface CustomRouteHandler {
        fun handleRoute(
            screenId: String,
            params: Map<String, Any>?,
            navigate: (String) -> Unit
        ): Boolean

        fun getSupportedScreenIds(): Set<String>
        fun getPriority(): Int = 0
    }

    private val customRouteHandlers = mutableMapOf<String, MutableList<CustomRouteHandler>>()

    fun registerCustomRouteHandler(handler: CustomRouteHandler) {
        handler.getSupportedScreenIds().forEach { screenId ->
            val handlers = customRouteHandlers.getOrPut(screenId) { mutableListOf() }
            handlers.add(handler)
            handlers.sortByDescending { it.getPriority() }
        }
    }

    fun unregisterCustomRouteHandler(handler: CustomRouteHandler) {
        handler.getSupportedScreenIds().forEach { screenId ->
            customRouteHandlers[screenId]?.remove(handler)
            if (customRouteHandlers[screenId]?.isEmpty() == true) customRouteHandlers.remove(screenId)
        }
    }

    fun handleCustomRoute(
        screenId: String,
        params: Map<String, Any>?,
        navigate: (String) -> Unit
    ): Boolean {
        val handlers = customRouteHandlers[screenId] ?: return false
        for (handler in handlers) {
            try {
                if (handler.handleRoute(screenId, params, navigate)) return true
            } catch (e: Exception) {
                println("Error in custom route handler: ${e.message}")
            }
        }
        return false
    }

    fun getCustomRouteScreenIds(): Set<String> = customRouteHandlers.keys.toSet()
    fun hasCustomRouteHandler(screenId: String): Boolean =
        customRouteHandlers.containsKey(screenId) && customRouteHandlers[screenId]?.isNotEmpty() == true
    fun clearCustomRouteHandlers() = customRouteHandlers.clear()

    // ─── Route Validators ─────────────────────────────────────────────────────

    interface RouteValidator {
        fun isValidRoute(route: String, params: Map<String, Any>?): Boolean
        fun getPriority(): Int = 0
    }

    private val routeValidators = mutableListOf<RouteValidator>()

    fun registerRouteValidator(validator: RouteValidator) {
        routeValidators.add(validator)
        routeValidators.sortByDescending { it.getPriority() }
    }

    fun unregisterRouteValidator(validator: RouteValidator) = routeValidators.remove(validator)

    fun validateRoute(route: String, params: Map<String, Any>?): Boolean =
        routeValidators.all { validator ->
            try { validator.isValidRoute(route, params) }
            catch (e: Exception) { println("Error in route validator: ${e.message}"); false }
        }

    // ─── Navigation Interceptors ──────────────────────────────────────────────

    /**
     * Interceptor for navigation events.
     * Returns null to cancel navigation; returns a (possibly modified) route to continue.
     */
    interface NavigationInterceptor {
        fun interceptNavigation(
            route: String,
            params: Map<String, Any>?
        ): String?   // null = cancel; non-null = possibly modified route

        fun getPriority(): Int = 0
    }

    private val navigationInterceptors = mutableListOf<NavigationInterceptor>()

    fun registerNavigationInterceptor(interceptor: NavigationInterceptor) {
        navigationInterceptors.add(interceptor)
        navigationInterceptors.sortByDescending { it.getPriority() }
    }

    fun unregisterNavigationInterceptor(interceptor: NavigationInterceptor) =
        navigationInterceptors.remove(interceptor)

    fun processNavigationInterceptors(route: String, params: Map<String, Any>?): String? {
        var currentRoute: String? = route
        for (interceptor in navigationInterceptors) {
            currentRoute = currentRoute?.let { r ->
                try { interceptor.interceptNavigation(r, params) }
                catch (e: Exception) { println("Error in navigation interceptor: ${e.message}"); r }
            }
            if (currentRoute == null) break
        }
        return currentRoute
    }

    // ─── Stats ────────────────────────────────────────────────────────────────

    data class ExtensionStats(
        val customRouteHandlers: Int,
        val routeValidators: Int,
        val navigationInterceptors: Int,
        val supportedCustomScreenIds: Set<String>
    )

    fun getExtensionStats(): ExtensionStats = ExtensionStats(
        customRouteHandlers = customRouteHandlers.size,
        routeValidators = routeValidators.size,
        navigationInterceptors = navigationInterceptors.size,
        supportedCustomScreenIds = getCustomRouteScreenIds()
    )
}
