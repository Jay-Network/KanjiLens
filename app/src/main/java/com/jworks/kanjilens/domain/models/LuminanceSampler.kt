package com.jworks.kanjilens.domain.models

import android.graphics.Rect
import java.nio.ByteBuffer

object LuminanceSampler {

    private const val GRID_SIZE = 5 // 5x5 = 25 sample points

    /**
     * Sample average luminance from the Y-plane of a YUV_420_888 frame
     * within the given bounding box (in ML Kit rotated coordinates).
     *
     * @param yBuffer Read-only view of the Y plane buffer
     * @param rowStride Row stride of the Y plane
     * @param pixelStride Pixel stride of the Y plane (usually 1)
     * @param imageW Sensor image width (imageProxy.width)
     * @param imageH Sensor image height (imageProxy.height)
     * @param bounds Bounding box in ML Kit rotated coordinate space
     * @param rotation Image rotation degrees (0, 90, 180, 270)
     * @return Average luminance (0-255), or null on failure
     */
    fun sampleLuminance(
        yBuffer: ByteBuffer,
        rowStride: Int,
        pixelStride: Int,
        imageW: Int,
        imageH: Int,
        bounds: Rect,
        rotation: Int
    ): Int? {
        if (bounds.isEmpty || imageW <= 0 || imageH <= 0) return null
        if (rowStride <= 0 || pixelStride <= 0) return null

        // ML Kit reports bounds in the rotated coordinate space.
        // Determine the rotated image dimensions.
        val rotatedW: Int
        val rotatedH: Int
        when (rotation) {
            90, 270 -> { rotatedW = imageH; rotatedH = imageW }
            else -> { rotatedW = imageW; rotatedH = imageH }
        }

        var sum = 0L
        var count = 0

        for (gy in 0 until GRID_SIZE) {
            for (gx in 0 until GRID_SIZE) {
                // Sample point in ML Kit (rotated) coords
                val mlX = bounds.left + (bounds.width() * (gx + 0.5f) / GRID_SIZE).toInt()
                val mlY = bounds.top + (bounds.height() * (gy + 0.5f) / GRID_SIZE).toInt()

                // Bounds check in rotated space
                if (mlX < 0 || mlX >= rotatedW || mlY < 0 || mlY >= rotatedH) continue

                // Map to raw sensor buffer coordinates
                val (bufX, bufY) = mapToBufferCoords(mlX, mlY, imageW, imageH, rotation)
                    ?: continue

                // Final bounds check in sensor space
                if (bufX < 0 || bufX >= imageW || bufY < 0 || bufY >= imageH) continue

                val offset = bufY * rowStride + bufX * pixelStride
                if (offset < 0 || offset >= yBuffer.capacity()) continue

                sum += (yBuffer.get(offset).toInt() and 0xFF)
                count++
            }
        }

        return if (count > 0) (sum / count).toInt() else null
    }

    /**
     * Sample average luminance from the center 20% of the frame (global, not per-element).
     * Uses a 5x5 grid of sample points from the frame center to determine background brightness.
     * This avoids sampling inside text bounding boxes (which would hit ink, not background).
     *
     * @param yBuffer Read-only view of the Y plane buffer
     * @param rowStride Row stride of the Y plane
     * @param pixelStride Pixel stride of the Y plane (usually 1)
     * @param imageW Sensor image width (imageProxy.width)
     * @param imageH Sensor image height (imageProxy.height)
     * @param rotation Image rotation degrees (0, 90, 180, 270)
     * @return Average luminance (0-255), or null on failure
     */
    fun sampleGlobalLuminance(
        yBuffer: ByteBuffer,
        rowStride: Int,
        pixelStride: Int,
        imageW: Int,
        imageH: Int,
        rotation: Int
    ): Int? {
        if (imageW <= 0 || imageH <= 0) return null
        if (rowStride <= 0 || pixelStride <= 0) return null

        // Sample from center 20% of the rotated (display-oriented) frame
        val rotatedW: Int
        val rotatedH: Int
        when (rotation) {
            90, 270 -> { rotatedW = imageH; rotatedH = imageW }
            else -> { rotatedW = imageW; rotatedH = imageH }
        }

        val centerX = rotatedW / 2f
        val centerY = rotatedH / 2f
        val regionW = rotatedW * 0.2f
        val regionH = rotatedH * 0.2f
        val regionLeft = centerX - regionW / 2f
        val regionTop = centerY - regionH / 2f

        var sum = 0L
        var count = 0

        for (gy in 0 until GRID_SIZE) {
            for (gx in 0 until GRID_SIZE) {
                val mlX = (regionLeft + regionW * (gx + 0.5f) / GRID_SIZE).toInt()
                val mlY = (regionTop + regionH * (gy + 0.5f) / GRID_SIZE).toInt()

                if (mlX < 0 || mlX >= rotatedW || mlY < 0 || mlY >= rotatedH) continue

                val (bufX, bufY) = mapToBufferCoords(mlX, mlY, imageW, imageH, rotation)
                    ?: continue

                if (bufX < 0 || bufX >= imageW || bufY < 0 || bufY >= imageH) continue

                val offset = bufY * rowStride + bufX * pixelStride
                if (offset < 0 || offset >= yBuffer.capacity()) continue

                sum += (yBuffer.get(offset).toInt() and 0xFF)
                count++
            }
        }

        return if (count > 0) (sum / count).toInt() else null
    }

    /**
     * Map ML Kit rotated coordinates back to raw sensor buffer coordinates.
     *
     * ML Kit applies rotation so that (0,0) is always top-left of the
     * correctly-oriented image. We need to reverse that to index into
     * the raw sensor buffer which is always in sensor orientation.
     *
     * @param mlX X in ML Kit rotated space
     * @param mlY Y in ML Kit rotated space
     * @param origW Sensor image width (imageProxy.width)
     * @param origH Sensor image height (imageProxy.height)
     * @param rotation Rotation degrees (0, 90, 180, 270)
     * @return (bufferX, bufferY) or null if rotation is unknown
     */
    fun mapToBufferCoords(mlX: Int, mlY: Int, origW: Int, origH: Int, rotation: Int): Pair<Int, Int>? {
        return when (rotation) {
            0 -> Pair(mlX, mlY)
            90 -> Pair(mlY, origH - 1 - mlX)
            180 -> Pair(origW - 1 - mlX, origH - 1 - mlY)
            270 -> Pair(origW - 1 - mlY, mlX)
            else -> null
        }
    }
}
