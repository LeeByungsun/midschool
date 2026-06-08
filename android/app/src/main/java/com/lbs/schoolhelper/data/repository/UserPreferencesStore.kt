package com.lbs.schoolhelper.data.repository

import android.content.Context
import com.lbs.schoolhelper.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface UserPreferencesStore {
    fun getStudentInfo(): StudentInfo
    fun hasStudentInfo(): Boolean
    fun saveStudentInfo(studentInfo: StudentInfo)
    fun getTimerDisplayMode(): String
    fun saveTimerDisplayMode(displayMode: String)
    fun isTimerNotificationEnabled(): Boolean
    fun saveTimerNotificationEnabled(enabled: Boolean)
    fun isTimerVibrationEnabled(): Boolean
    fun saveTimerVibrationEnabled(enabled: Boolean)
    fun getTimerState(): UserPreferences.TimerState
    fun saveTimerState(
        presetName: String,
        totalMillis: Long,
        remainingMillis: Long,
        targetAtMillis: Long,
        isRunning: Boolean
    )
    fun clearTimerState()
}

class AndroidUserPreferencesStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) : UserPreferencesStore {
    override fun getStudentInfo(): StudentInfo = UserPreferences.getStudentInfo(context)

    override fun hasStudentInfo(): Boolean = UserPreferences.hasStudentInfo(context)

    override fun saveStudentInfo(studentInfo: StudentInfo) {
        UserPreferences.saveStudentInfo(context, studentInfo)
    }

    override fun getTimerDisplayMode(): String = UserPreferences.getTimerDisplayMode(context)

    override fun saveTimerDisplayMode(displayMode: String) {
        UserPreferences.saveTimerDisplayMode(context, displayMode)
    }

    override fun isTimerNotificationEnabled(): Boolean = UserPreferences.isTimerNotificationEnabled(context)

    override fun saveTimerNotificationEnabled(enabled: Boolean) {
        UserPreferences.saveTimerNotificationEnabled(context, enabled)
    }

    override fun isTimerVibrationEnabled(): Boolean = UserPreferences.isTimerVibrationEnabled(context)

    override fun saveTimerVibrationEnabled(enabled: Boolean) {
        UserPreferences.saveTimerVibrationEnabled(context, enabled)
    }

    override fun getTimerState(): UserPreferences.TimerState = UserPreferences.getTimerState(context)

    override fun saveTimerState(
        presetName: String,
        totalMillis: Long,
        remainingMillis: Long,
        targetAtMillis: Long,
        isRunning: Boolean
    ) {
        UserPreferences.saveTimerState(
            context = context,
            presetName = presetName,
            totalMillis = totalMillis,
            remainingMillis = remainingMillis,
            targetAtMillis = targetAtMillis,
            isRunning = isRunning
        )
    }

    override fun clearTimerState() {
        UserPreferences.clearTimerState(context)
    }
}
