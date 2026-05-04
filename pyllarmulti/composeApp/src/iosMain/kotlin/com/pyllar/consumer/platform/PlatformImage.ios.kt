package com.pyllar.consumer.platform

import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import platform.UIKit.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.MediaPlayer.*
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
actual object PlatformImage {
    actual suspend fun captureToPng(coordinates: LayoutCoordinates): ByteArray? {
        val position = coordinates.positionInWindow()
        val size = coordinates.size
        
        val window = UIApplication.sharedApplication.keyWindow ?: return null
        val rootViewController = window.rootViewController ?: return null
        val view = rootViewController.view
        
        UIGraphicsBeginImageContextWithOptions(
            CGSizeMake(size.width.toDouble(), size.height.toDouble()),
            false,
            0.0
        )
        
        val context = UIGraphicsGetCurrentContext() ?: return null
        
        // Translate to capture only the specific area
        CGContextTranslateCTM(context, -position.x.toDouble(), -position.y.toDouble())
        
        view.drawViewHierarchyInRect(view.bounds, true)
        
        val image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        
        if (image == null) return null
        
        val data = UIImagePNGRepresentation(image) ?: return null
        val bytes = ByteArray(data.length.toInt())
        bytes.usePinned { pinned ->
            data.getBytes(pinned.addressOf(0), data.length)
        }
        return bytes
    }
}
