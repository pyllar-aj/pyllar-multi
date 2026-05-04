package com.pyllar.consumer.platform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntSize
import com.pyllar.consumer.MainActivity
import java.io.ByteArrayOutputStream

actual object PlatformImage {
    actual suspend fun captureToPng(coordinates: LayoutCoordinates): ByteArray? {
        try {
            val size = coordinates.size
            if (size.width <= 0 || size.height <= 0) {
                com.pyllar.consumer.util.Log.e("PlatformImage", "Invalid signature box dimensions: ${size.width}x${size.height}")
                return null
            }

            val rootView = findRootView()
            if (rootView == null) {
                com.pyllar.consumer.util.Log.e("PlatformImage", "Root view not found for signature capture")
                return null
            }

            val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            val position = coordinates.positionInWindow()
            canvas.translate(-position.x, -position.y)
            rootView.draw(canvas)
            
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            return stream.toByteArray()
        } catch (e: Exception) {
            com.pyllar.consumer.util.Log.e("PlatformImage", "Error capturing signature: ${e.message}", e)
            return null
        }
    }
    
    private fun findRootView(): View? {
        return MainActivity.instance?.window?.decorView?.findViewById(android.R.id.content)
    }
}
