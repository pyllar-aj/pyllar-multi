package com.pyllar.consumer.util

/**
 * Minimal cross-platform logger.
 *
 * Kept intentionally small so networking can log fatal diagnostics on iOS Simulator
 * where `println` may not appear in unified logs.
 */
expect fun platformLog(message: String)

