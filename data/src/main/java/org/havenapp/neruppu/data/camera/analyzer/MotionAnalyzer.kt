package org.havenapp.neruppu.data.camera.analyzer

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import kotlin.math.abs

class MotionAnalyzer(
    private val onMotionDetected: (Double) -> Unit,
    private val sensitivity: Int = 15
) : ImageAnalysis.Analyzer {

    internal var referenceBuffer: ByteArray? = null
    private var lastAnalysisTime = 0L
    private val targetFps = 5
    private val frameIntervalMs = 1000L / targetFps

    private val outWidth = 320
    private val outHeight = 240

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        
        if (now - lastAnalysisTime < frameIntervalMs) {
            image.close()
            return
        }
        lastAnalysisTime = now

        val currentRef = referenceBuffer
        val width = image.width
        val height = image.height
        val plane = image.planes[0]
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val buffer = plane.buffer

        val size = width * height
        var ref = currentRef
        if (ref == null || ref.size != size) {
            val newRef = ByteArray(size)
            fillBuffer(buffer, newRef, width, height, rowStride, pixelStride)
            referenceBuffer = newRef
            image.close()
            return
        }

        var changedPixels = 0
        var totalSampled = 0

        val xStep = width / outWidth
        val yStep = height / outHeight

        for (y in 0 until outHeight) {
            for (x in 0 until outWidth) {
                val srcX = x * xStep
                val srcY = y * yStep
                val srcIndex = srcY * rowStride + srcX * pixelStride
                
                if (srcIndex < buffer.capacity()) {
                    val currentVal = buffer.get(srcIndex).toInt() and 0xFF
                    val refIndex = srcY * width + srcX
                    val referenceVal = ref[refIndex].toInt() and 0xFF
                    
                    val diff = abs(currentVal - referenceVal)

                    if (diff > sensitivity * 2) {
                        changedPixels++
                    }
                    
                    totalSampled++
                    
                    val adaptRate = 0.05f
                    val blended = (adaptRate * currentVal + (1f - adaptRate) * referenceVal).toInt()
                    ref[refIndex] = blended.toByte()
                }
            }
        }

        val motionLevel = if (totalSampled > 0) (changedPixels.toDouble() / totalSampled) * 100.0 else 0.0
        onMotionDetected(motionLevel)

        image.close()
    }

    fun cleanup() {
        referenceBuffer = null
    }

    private fun fillBuffer(src: ByteBuffer, dst: ByteArray, w: Int, h: Int, rowStride: Int, pixelStride: Int) {
        if (pixelStride == 1 && rowStride == w) {
            src.get(dst, 0, dst.size)
            return
        }
        for (y in 0 until h) {
            val srcPos = y * rowStride
            val dstPos = y * w
            if (pixelStride == 1) {
                src.position(srcPos)
                src.get(dst, dstPos, w.coerceAtMost(dst.size - dstPos))
            } else {
                for (x in 0 until w) {
                    dst[dstPos + x] = src.get(srcPos + x * pixelStride)
                }
            }
        }
    }
}
