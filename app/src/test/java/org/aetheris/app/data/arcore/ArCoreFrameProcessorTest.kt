package org.aetheris.app.data.arcore

import com.google.ar.core.Frame
import com.google.ar.core.PointCloud
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.aetheris.app.domain.model.Point3D
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class ArCoreFrameProcessorTest {

    private lateinit var processor: ArCoreFrameProcessor
    private lateinit var frame: Frame
    private lateinit var pointCloud: PointCloud

    @Before
    fun setUp() {
        processor = ArCoreFrameProcessor(
            confidenceThreshold = 0.3f
        )

        frame = mockk()
        pointCloud = mockk(relaxed = true)

        every {
            frame.acquirePointCloud()
        } returns pointCloud
    }

    @Test
    fun `processPointCloud filters points below confidence threshold`() {
        val buffer = createPointCloudBuffer(
            1.0f, 2.0f, 3.0f, 0.8f,
            4.0f, 5.0f, 6.0f, 0.1f
        )

        every {
            pointCloud.points
        } returns buffer

        val result = processor.processPointCloud(frame)

        assertThat(result).containsExactly(
            Point3D(
                x = 1.0f,
                y = 2.0f,
                z = 3.0f
            )
        )

        verify(exactly = 1) {
            pointCloud.close()
        }
    }

    @Test
    fun `processPointCloud accepts point at exact confidence threshold`() {
        val buffer = createPointCloudBuffer(
            10.0f, 20.0f, 30.0f, 0.3f
        )

        every {
            pointCloud.points
        } returns buffer

        val result = processor.processPointCloud(frame)

        assertThat(result).containsExactly(
            Point3D(
                x = 10.0f,
                y = 20.0f,
                z = 30.0f
            )
        )

        verify(exactly = 1) {
            pointCloud.close()
        }
    }

    @Test
    fun `processPointCloud returns empty list when all points are rejected`() {
        val buffer = createPointCloudBuffer(
            1.0f, 2.0f, 3.0f, 0.29f,
            4.0f, 5.0f, 6.0f, 0.0f
        )

        every {
            pointCloud.points
        } returns buffer

        val result = processor.processPointCloud(frame)

        assertThat(result).isEmpty()

        verify(exactly = 1) {
            pointCloud.close()
        }
    }

    @Test
    fun `processPointCloud returns empty list for empty point cloud`() {
        every {
            pointCloud.points
        } returns createPointCloudBuffer()

        val result = processor.processPointCloud(frame)

        assertThat(result).isEmpty()

        verify(exactly = 1) {
            pointCloud.close()
        }
    }

    @Test
    fun `processPointCloud closes pointCloud even if buffer access fails`() {
        every {
            pointCloud.points
        } throws RuntimeException("Driver JNI falhou")

        try {
            processor.processPointCloud(frame)
        } catch (_: RuntimeException) {
            // Exceção esperada
        }

        verify(exactly = 1) {
            pointCloud.close()
        }
    }

    private fun createPointCloudBuffer(
        vararg values: Float
    ): FloatBuffer {
        return ByteBuffer
            .allocateDirect(values.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(values)
                rewind()
            }
    }
}