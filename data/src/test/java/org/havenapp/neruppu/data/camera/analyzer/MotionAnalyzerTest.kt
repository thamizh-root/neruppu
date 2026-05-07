package org.havenapp.neruppu.data.camera.analyzer

import androidx.camera.core.ImageProxy
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

class MotionAnalyzerTest {

    @Test
    fun `test motion detection calculation`() {
        var detectedLevel = 0.0
        val analyzer = MotionAnalyzer { level ->
            detectedLevel = level
        }

        // Mock ImageProxy
        val image1 = mockk<ImageProxy>(relaxed = true)
        val plane1 = mockk<ImageProxy.PlaneProxy>()
        val buffer1 = ByteBuffer.allocate(100 * 100)
        buffer1.put(ByteArray(100 * 100) { 0 })
        buffer1.rewind()
        
        every { image1.width } returns 100
        every { image1.height } returns 100
        every { image1.planes } returns arrayOf(plane1)
        every { plane1.buffer } returns buffer1
        every { plane1.rowStride } returns 100
        every { plane1.pixelStride } returns 1
        
        analyzer.analyze(image1)

        val image2 = mockk<ImageProxy>(relaxed = true)
        val plane2 = mockk<ImageProxy.PlaneProxy>()
        val buffer2 = ByteBuffer.allocate(100 * 100)
        // Set some pixels to 255 to simulate motion
        val data2 = ByteArray(100 * 100) { 0 }
        for (i in 0 until 500) data2[i * 8] = 255.toByte() // Change some pixels
        buffer2.put(data2)
        buffer2.rewind()

        every { image2.width } returns 100
        every { image2.height } returns 100
        every { image2.planes } returns arrayOf(plane2)
        every { plane2.buffer } returns buffer2
        every { plane2.rowStride } returns 100
        every { plane2.pixelStride } returns 1

        analyzer.analyze(image2)

        assertTrue("Motion level should be greater than 0, was $detectedLevel", detectedLevel > 0)
    }
}
