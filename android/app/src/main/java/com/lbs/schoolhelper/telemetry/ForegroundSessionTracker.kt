package com.lbs.schoolhelper.telemetry

/** Application-wide activity visibility, independent of screen navigation or rotation. Main thread only. */
class ForegroundSessionTracker(private val onForeground: (EntryPoint) -> Unit) {
    private var startedActivities = 0
    private var foreground = false
    fun started(entryPoint: EntryPoint) {
        startedActivities++
        if (!foreground) {
            foreground = true
            onForeground(entryPoint)
        }
    }
    fun stopped(changingConfigurations: Boolean) {
        startedActivities = (startedActivities - 1).coerceAtLeast(0)
        if (startedActivities == 0 && !changingConfigurations) foreground = false
    }
}
