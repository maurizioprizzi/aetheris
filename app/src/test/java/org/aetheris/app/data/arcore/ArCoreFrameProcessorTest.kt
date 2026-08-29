package org.aetheris.app.data.arcore

import com.google.ar.core.Frame
import com.google.ar.core.PointCloud
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class ArCoreFrameProcessorTest {

    private lateinit var processor: ArCoreFrameProcessor
    private val frame: Frame = mockk()
    private val pointCloud: PointCloud = mockk(relaxed = true)

    @Before
    fun setUp() {
        processor = ArCoreFrameProcessor(confidenceThreshold = 0.3f)
    }

    @Test
    fun `processPointCloud should filter out points below confidence threshold`() {
        // [X, Y, Z, Confidence]
        val rawData = floatArrayOf(
            1.0f, 2.0f, 3.0f, 0.8f,  // Válido (0.8 >= 0.3)
            4.0f, 5.0f, 6.0f, 0.1f   // Inválido (0.1 < 0.3)
        )
        val buffer = ByteBuffer.allocateDirect(rawData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(rawData)
        buffer.position(0)

        every { frame.acquirePointCloud() } returns pointCloud
        every { pointCloud.points } returns buffer

        val result = processor.processPointCloud(frame)

        assertThat(result).hasSize(1)
        assertThat(result[0].x).isEqualTo(1.0f)
        assertThat(result[0].y).isEqualTo(2.0f)
        assertThat(result[0].z).isEqualTo(3.0f)
        verify { pointCloud.release() }
    }

    @Test
    fun `processPointCloud should return empty list when buffer is null`() {
        every { frame.acquirePointCloud() } returns pointCloud
        every { pointCloud.points } returns null

        val result = processor.processPointCloud(frame)

        assertThat(result).isEmpty()
        verify { pointCloud.release() }
    }
}