package com.bsbarron.midschoolapp.data.repository

import android.content.Context
import com.bsbarron.midschoolapp.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PreferencesRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : PreferencesRepository {

    override fun getStudentInfo(): StudentInfo {
        return StudentInfo(
            grade = UserPreferences.getGrade(context),
            classroom = UserPreferences.getClassroom(context)
        )
    }

    override fun hasStudentInfo(): Boolean {
        return UserPreferences.hasStudentInfo(context)
    }

    override fun saveStudentInfo(grade: String, classroom: String) {
        UserPreferences.saveStudentInfo(context, grade, classroom)
    }

    override fun getTimerDisplayMode(): TimerDisplayMode {
        return when (UserPreferences.getTimerDisplayMode(context)) {
            UserPreferences.TIMER_DISPLAY_RING -> TimerDisplayMode.RING
            else -> TimerDisplayMode.COUNT
        }
    }

    override fun saveTimerDisplayMode(displayMode: TimerDisplayMode) {
        val value = when (displayMode) {
            TimerDisplayMode.COUNT -> UserPreferences.TIMER_DISPLAY_COUNT
            TimerDisplayMode.RING -> UserPreferences.TIMER_DISPLAY_RING
        }
        UserPreferences.saveTimerDisplayMode(context, value)
    }

    override fun isTimerNotificationEnabled(): Boolean {
        return UserPreferences.isTimerNotificationEnabled(context)
    }

    override fun saveTimerNotificationEnabled(enabled: Boolean) {
        UserPreferences.saveTimerNotificationEnabled(context, enabled)
    }

    override fun isTimerVibrationEnabled(): Boolean {
        return UserPreferences.isTimerVibrationEnabled(context)
    }

    override fun saveTimerVibrationEnabled(enabled: Boolean) {
        UserPreferences.saveTimerVibrationEnabled(context, enabled)
    }

    override fun getTimerState(): TimerPreferenceState {
        val state = UserPreferences.getTimerState(context)
        return TimerPreferenceState(
            presetName = state.presetName,
            totalMillis = state.totalMillis,
            remainingMillis = state.remainingMillis,
            targetAtMillis = state.targetAtMillis,
            isRunning = state.isRunning
        )
    }

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
