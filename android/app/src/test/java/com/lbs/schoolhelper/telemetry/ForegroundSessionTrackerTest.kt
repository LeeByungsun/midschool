package com.lbs.schoolhelper.telemetry

import org.junit.Assert.assertEquals
import org.junit.Test

class ForegroundSessionTrackerTest {
    @Test fun `navigation and rotation do not duplicate app visits but background return does`() {
        val visits = mutableListOf<EntryPoint>()
        val tracker = ForegroundSessionTracker { visits += it }
        tracker.started(EntryPoint.LAUNCHER)
        tracker.started(EntryPoint.UNKNOWN)
        tracker.stopped(false)
        tracker.stopped(true)
        tracker.started(EntryPoint.UNKNOWN)
        assertEquals(listOf(EntryPoint.LAUNCHER), visits)
        tracker.stopped(false)
        tracker.started(EntryPoint.WIDGET)
        assertEquals(listOf(EntryPoint.LAUNCHER, EntryPoint.WIDGET), visits)
    }
}
