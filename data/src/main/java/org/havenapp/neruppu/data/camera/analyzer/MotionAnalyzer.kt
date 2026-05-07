package org.havenapp.neruppu.data.camera.analyzer

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlin.math.abs

class MotionAnalyzer(
    private val onMotionDetected: (Double) -> Unit,
) : ImageAnalysis.Analyzer {

    private var referenceBuffer: ByteArray? = null
    private var bufferReuse: ByteArray? = null
    private var lastReferenceUpdateTime = 0L
    
    // Haven-grade tuning
    private val referenceUpdateIntervalMs = 500L // Compare against 0.5s ago to catch slow movement
    private val pixelDiffThreshold = 15 // Lowered for high sensitivity
    private val samplingStep = 4 // Higher resolution sampling (check 1 in 16 pixels)

    override fun analyze(image: ImageProxy) {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val width = image.width
        val height = image.height
        val now = System.currentTimeMillis()
        
        val size = buffer.remaining()
        if (bufferReuse == null || (bufferReuse!!.size != size)) {
            bufferReuse = ByteArray(size)
        }
        val currentData = bufferReuse!!
        buffer[currentData]
        
        // 1. Initial setup of reference buffer
        if (referenceBuffer == null || (referenceBuffer!!.size != currentData.size)) {
            referenceBuffer = ByteArray(currentData.size)
            System.arraycopy(currentData, 0, referenceBuffer!!, 0, currentData.size)
            lastReferenceUpdateTime = now
            image.close()
            return
        }

        // 2. Calculate motion relative to the LONG-TERM reference
        val motionLevel = calculateMotion(
            currentData, 
            referenceBuffer!!, 
            width, 
            height, 
            plane.rowStride, 
            plane.pixelStride
        )
        onMotionDetected(motionLevel)

        // 3. Update reference buffer periodically
        // If 500ms passed, we update the reference to adapt to lighting changes
        // But we only update if there's no major motion, to keep the background "clean"
        if (now - lastReferenceUpdateTime > referenceUpdateIntervalMs) {
            if (motionLevel < 5.0) { // Only update baseline if scene is relatively stable
                System.arraycopy(currentData, 0, referenceBuffer!!, 0, currentData.size)
                lastReferenceUpdateTime = now
                // Log.d("MotionAnalyzer", "Reference frame updated (Baseline refreshed)")
            }
        }
        
        image.close()
    }

    private fun calculateMotion(
        current: ByteArray, 
        reference: ByteArray, 
        width: Int, 
        height: Int, 
        rowStride: Int, 
        pixelStride: Int
    ): Double {
        var changedPixels = 0
        var totalSampled = 0
        
        for (y in 0 until height step samplingStep) {
            for (x in 0 until width step samplingStep) {
                val index = y * rowStride + x * pixelStride
                if (index < current.size) {
                    val diff = abs((current[index].toInt() and 0xFF) - (reference[index].toInt() and 0xFF))
                    
                    if (diff > pixelDiffThreshold) {
                        changedPixels++
                    }
                    totalSampled++
                }
            }
        }
        
        if (totalSampled == 0) return 0.0
        return (changedPixels.toDouble() / totalSampled) * 100.0
    }
}
