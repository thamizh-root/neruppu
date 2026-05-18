package org.havenapp.neruppu.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class AudioRecorderTest {

    private val context = mockk<Context>(relaxed = true)
    private lateinit var audioRecorder: AudioRecorder
    private var mockRecorder: MediaRecorder? = null

    @Before
    fun setup() {
        mockkStatic(Uri::class)
        mockkConstructor(MediaRecorder::class)

        val testDir = File(System.getProperty("java.io.tmpdir"), "neruppu_test_${System.currentTimeMillis()}").apply { mkdirs() }
        every { context.filesDir } returns testDir

        audioRecorder = AudioRecorder(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `TC-AR-06 stopRecording without start returns null`() = runBlocking {
        val uri = audioRecorder.stopRecording()
        assertNull(uri)
    }

    @Test
    fun `TC-AR-07 getMaxAmplitude returns 0 when no recorder`() {
        assertEquals(0, audioRecorder.getMaxAmplitude())
    }
}