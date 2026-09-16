package com.lbs.schoolhelper.ui.timer

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * In-process signal used by the home and detail timer screens.
 *
 * Both screens have their own ViewModel (and therefore their own UI state),
 * but they must react immediately when the other screen starts, pauses, or
 * resets the shared persisted session.
 */
object TimerSyncBus {
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()
    private val sessionLock = Any()
    @Volatile private var sessionRunning = false

    fun setRunning(running: Boolean) {
        sessionRunning = running
    }

    fun isRunning() = sessionRunning

    fun <T> withSessionLock(block: () -> T): T = synchronized(sessionLock, block)

    fun signal(sourceId: String) {
        _events.tryEmit(sourceId)
    }
}
