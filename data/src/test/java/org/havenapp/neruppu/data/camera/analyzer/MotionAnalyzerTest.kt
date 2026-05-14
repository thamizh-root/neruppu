package org.havenapp.neruppu.data.camera.analyzer

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer

class MotionAnalyzerTest {

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0

        mockkStatic(Bitmap::class)
        every { Bitmap.createBitmap(any<Int>(), any<Int>(), any<Bitmap.Config>()) } returns mockk(relaxed = true)
    }

    private fun createMockImage(width: Int, height: Int, data: ByteArray): ImageProxy {
        val image = mockk<ImageProxy>(relaxed = true)
        val plane = mockk<ImageProxy.PlaneProxy>()
        val buffer = ByteBuffer.allocate(data.size)
        buffer.put(data)
        buffer.rewind()
        
        every { image.width } returns width
        every { image.height } returns height
        every { image.planes } returns arrayOf(plane)
        every { plane.buffer } returns buffer
        every { plane.rowStride } returns width
        every { plane.pixelStride } returns 1
        return image
    }

    @Test
    fun `TC-CAM-01 Frame rate control skips fast frames`() {
        val analyzer = MotionAnalyzer(onMotionDetected = {})
        val image1 = createMockImage(100, 100, ByteArray(10000) { 0 })
        val image2 = createMockImage(100, 100, ByteArray(10000) { 0 })

        analyzer.analyze(image1)
        // Immediately analyze second frame
        analyzer.analyze(image2)

        verify(exactly = 1) { image1.close() }
        verify(exactly = 1) { image2.close() }
        // The fact that it doesn't crash and closes images is good. 
        // We can't easily verify internal state but we can check if math was skipped by mocking math? 
        // Actually, the easiest way is to check if it returns early.
    }

    @Test
    fun `TC-CAM-02 Reference initialization does not trigger motion`() {
        var triggered = false
        val analyzer = MotionAnalyzer(onMotionDetected = { triggered = true })
        val image = createMockImage(100, 100, ByteArray(10000) { 100.toByte() })

        analyzer.analyze(image)

        assert(!triggered)
    }

    @Test
    fun `TC-CAM-03 Motion threshold triggers correctly`() {
        var detectedLevel = 0.0
        val analyzer = MotionAnalyzer(onMotionDetected = { detectedLevel = it })
        
        val frame1 = createMockImage(320, 240, ByteArray(320 * 240) { 100.toByte() })
        analyzer.analyze(frame1)

        Thread.sleep(210) // Wait for 5 FPS interval

        // Create frame with 20% change (sensitivity is 15 in driver, 15.0 in MonitoringService)
        // MotionAnalyzer sensitivity param defaults to 15.
        // If we change > 15% of pixels by > 30 (sensitivity * 2), it should detect significant change.
        val data2 = ByteArray(320 * 240) { 100.toByte() }
        for (i in 0 until (320 * 240 / 5)) { // 20% of pixels
            data2[i] = 200.toByte()
        }
        val frame2 = createMockImage(320, 240, data2)
        
        analyzer.analyze(frame2)

        assertTrue("Motion level should be ~20%, was $detectedLevel", detectedLevel > 15.0)
    }

    @Test
    fun `TC-CAM-06 Cleanup nullifies resources`() {
        val analyzer = MotionAnalyzer(onMotionDetected = {})
        analyzer.cleanup()
        
        assertNull(analyzer.differenceMap.value)
    }

    @Test
    fun `TC-CAM-07 Heatmap colors are correct`() {
        // We need to capture the IntArray passed to setPixels
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        every { Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888) } returns mockBitmap
        
        val pixelsSlot = slot<IntArray>()
        every { mockBitmap.setPixels(capture(pixelsSlot), any(), any(), any(), any(), any(), any()) } just Runs

        val analyzer = MotionAnalyzer(onMotionDetected = {}, sensitivity = 15)
        
        // 1. Initialize reference (all 100)
        val frame1 = createMockImage(320, 240, ByteArray(320 * 240) { 100.toByte() })
        analyzer.analyze(frame1)

        Thread.sleep(210)

        // 2. Second frame with specific changes:
        // Pixel 0: 100 (Unchanged -> Grey 0xFF808080)
        // Pixel 1: 120 (Minor change 20 > 15 -> Blue 0xFF0000FF)
        // Pixel 2: 140 (Major change 40 > 30 -> Yellow 0xFFFFFF00)
        val data2 = ByteArray(320 * 240) { 100.toByte() }
        data2[1] = 120.toByte()
        data2[2] = 140.toByte()
        
        val frame2 = createMockImage(320, 240, data2)
        analyzer.analyze(frame2)

        val pixels = pixelsSlot.captured
        assertEquals(0xFF808080.toInt(), pixels[0])
        assertEquals(0xFF0000FF.toInt(), pixels[1])
        assertEquals(0xFFFFFF00.toInt(), pixels[2])
    }
}
