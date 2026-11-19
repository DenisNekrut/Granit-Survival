package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PerformanceMonitorTest {
    @Test fun calculatesFpsFramePercentileDrawCallsAndMemory() {
        val monitor = PerformanceMonitor(windowCapacity = 4)
        monitor.recordFrame(10_000_000L, 2)
        monitor.recordFrame(20_000_000L, 4)
        monitor.recordFrame(30_000_000L, 6)
        monitor.recordFrame(40_000_000L, 8)
        val snapshot = requireNotNull(monitor.snapshot(100_000_000L, 10, 20, 30, 40))
        assertEquals(40.0, snapshot.fps, 0.001)
        assertEquals(25.0, snapshot.averageFrameTimeMs, 0.001)
        assertEquals(40.0, snapshot.p95FrameTimeMs, 0.001)
        assertEquals(3, snapshot.droppedFrames)
        assertEquals(5.0, snapshot.averageDrawCalls, 0.001)
        assertEquals(30L, snapshot.nativeHeapAllocatedBytes)
        assertEquals(40, snapshot.pssKilobytes)
    }

    @Test fun resetsWindowAfterSnapshotAndIgnoresEmptyWindow() {
        val monitor = PerformanceMonitor()
        assertNull(monitor.snapshot(1_000_000_000L, 0, 0, 0, 0))
        monitor.recordFrame(16_000_000L, 3)
        assertEquals(1.0, requireNotNull(monitor.snapshot(1_000_000_000L, 1, 2, 3, 4)).fps, 0.001)
        assertNull(monitor.snapshot(1_000_000_000L, 1, 2, 3, 4))
    }
}
