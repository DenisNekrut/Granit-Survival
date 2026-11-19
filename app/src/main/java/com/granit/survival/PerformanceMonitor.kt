package com.granit.survival

import kotlin.math.ceil

data class PerformanceSnapshot(
    val fps: Double,
    val averageFrameTimeMs: Double,
    val p95FrameTimeMs: Double,
    val droppedFrames: Int,
    val averageDrawCalls: Double,
    val javaHeapUsedBytes: Long,
    val javaHeapMaxBytes: Long,
    val nativeHeapAllocatedBytes: Long,
    val pssKilobytes: Int
) {
    fun toLogLine(): String =
        "fps=%.1f frameAvg=%.2fms frameP95=%.2fms dropped=%d drawCallsAvg=%.1f javaHeap=%d/%dMB nativeHeap=%dMB pss=%dMB".format(
            fps, averageFrameTimeMs, p95FrameTimeMs, droppedFrames, averageDrawCalls,
            javaHeapUsedBytes / MEBIBYTE, javaHeapMaxBytes / MEBIBYTE,
            nativeHeapAllocatedBytes / MEBIBYTE, pssKilobytes / 1024
        )

    private companion object { const val MEBIBYTE = 1024L * 1024L }
}

/** Rolling render telemetry. PSS includes process memory; GPU/vendor memory is not measured. */
class PerformanceMonitor(private val windowCapacity: Int = 120) {
    private val frameTimesNanos = LongArray(windowCapacity.coerceAtLeast(1))
    private var sampleCount = 0
    private var totalFrames = 0L
    private var totalFrameTimeNanos = 0L
    private var writeIndex = 0
    private var droppedFrames = 0
    private var drawCallsTotal = 0L

    init { require(windowCapacity > 0) }

    fun recordFrame(frameIntervalNanos: Long, drawCalls: Int) {
        val interval = frameIntervalNanos.coerceAtLeast(1L)
        frameTimesNanos[writeIndex] = interval
        writeIndex = (writeIndex + 1) % frameTimesNanos.size
        sampleCount = (sampleCount + 1).coerceAtMost(frameTimesNanos.size)
        totalFrames++
        totalFrameTimeNanos += interval
        if (interval > FRAME_BUDGET_NANOS) droppedFrames++
        drawCallsTotal += drawCalls.coerceAtLeast(0)
    }

    fun snapshot(
        windowDurationNanos: Long,
        javaHeapUsedBytes: Long,
        javaHeapMaxBytes: Long,
        nativeHeapAllocatedBytes: Long,
        pssKilobytes: Int
    ): PerformanceSnapshot? {
        if (totalFrames == 0L || sampleCount == 0 || windowDurationNanos <= 0L) return null
        val sorted = frameTimesNanos.copyOf(sampleCount).sortedArray()
        val p95Index = (ceil(sorted.size * 0.95).toInt() - 1).coerceIn(0, sorted.lastIndex)
        val elapsedSeconds = windowDurationNanos / NANOS_PER_SECOND
        val result = PerformanceSnapshot(
            fps = totalFrames / elapsedSeconds,
            averageFrameTimeMs = totalFrameTimeNanos.toDouble() / totalFrames / NANOS_PER_MILLISECOND,
            p95FrameTimeMs = sorted[p95Index].toDouble() / NANOS_PER_MILLISECOND,
            droppedFrames = droppedFrames,
            averageDrawCalls = drawCallsTotal.toDouble() / totalFrames,
            javaHeapUsedBytes = javaHeapUsedBytes.coerceAtLeast(0L),
            javaHeapMaxBytes = javaHeapMaxBytes.coerceAtLeast(0L),
            nativeHeapAllocatedBytes = nativeHeapAllocatedBytes.coerceAtLeast(0L),
            pssKilobytes = pssKilobytes.coerceAtLeast(0)
        )
        sampleCount = 0
        totalFrames = 0L
        totalFrameTimeNanos = 0L
        writeIndex = 0
        droppedFrames = 0
        drawCallsTotal = 0L
        return result
    }

    private companion object {
        const val FRAME_BUDGET_NANOS = 16_666_667L
        const val NANOS_PER_SECOND = 1_000_000_000.0
        const val NANOS_PER_MILLISECOND = 1_000_000.0
    }
}
