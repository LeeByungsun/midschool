package com.bsbarron.midschoolapp.data.repository

interface PreferencesRepository {
    fun getStudentInfo(): StudentInfo
    fun hasStudentInfo(): Boolean
    fun saveStudentInfo(grade: String, classroom: String)
    fun getTimerDisplayMode(): TimerDisplayMode
    fun saveTimerDisplayMode(displayMode: TimerDisplayMode)
    fun isTimerNotificationEnabled(): Boolean
    fun saveTimerNotificationEnabled(enabled: Boolean)
    fun isTimerVibrationEnabled(): Boolean
    fun saveTimerVibrationEnabled(enabled: Boolean)
    fun getTimerState(): TimerPreferenceState
    fun saveTimerState(
        presetName: String,
        totalMillis: Long,
        remainingMillis: Long,
        targetAtMillis: Long,
        isRunning: Boolean
    )
    fun clearTimerState()
}

data class StudentInfo(
    val grade: String,
    val classroom: String
)

enum class TimerDisplayMode {
    COUNT,
    RING
}

data class TimerPreferenceState(
    val presetName: String,
    val totalMillis: Long,
    val remainingMillis: Long,
    val targetAtMillis: Long,
    val isRunning: Boolean
)
