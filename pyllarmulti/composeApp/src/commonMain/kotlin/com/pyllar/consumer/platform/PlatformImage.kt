package com.pyllar.consumer.platform

import androidx.compose.ui.layout.LayoutCoordinates

/**
 * Platform-specific image utilities.
 */
expect object PlatformImage {
    /**
     * Captures a screenshot of the area defined by the given coordinates.
     * Returns the PNG bytes.
     */
    suspend fun captureToPng(coordinates: LayoutCoordinates): ByteArray?
}
