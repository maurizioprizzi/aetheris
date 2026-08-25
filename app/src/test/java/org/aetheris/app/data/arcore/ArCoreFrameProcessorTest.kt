package org.aetheris.app.data.arcore

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ArCoreFrameProcessorTest {

    private val processor = ArCoreFrameProcessor(minConfidenceThreshold = 0.5f)

    @Test
    fun `when buffer contains points above threshold then extract them successfully`() {
        val rawData = floatArrayOf(
            1.0f, 2.0f, 3.0f, 0.8f,  // Ponto 1: Válido (0.8 >= 0.5)
            4.0f, 5.0f, 6.0f, 0.2f   // Ponto 2: Descartado (0.2 < 0.5)
        )

        val byteBuffer = ByteBuffer.allocateDirect(rawData.size * 4).order(ByteOrder.nativeOrder())
        val floatBuffer = byteBuffer.asFloatBuffer().put(rawData)
        floatBuffer.rewind()

        val extracted = processor.extractPointCloud(floatBuffer)

        assertThat(extracted).hasSize(1)
        assertThat(extracted.first().x).isEqualTo(1.0f)
        assertThat(extracted.first().y).isEqualTo(2.0f)
        assertThat(extracted.first().z).isEqualTo(3.0f)
    }

    @Test
    fun `when buffer is null then return empty list`() {
        val extracted = processor.extractPointCloud(null)
        assertThat(extracted).isEmpty()
    }
}