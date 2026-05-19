package com.bsbarron.midschoolapp

import android.content.Context
import com.bsbarron.midschoolapp.data.repository.StudentInfo

object UserPreferences {
    private const val PREFS_NAME = "midschool_prefs"
    private const val KEY_GRADE = "grade"
    private const val KEY_CLASSROOM = "classroom"
    private const val KEY_SCHOOL_NAME = "school_name"
    private const val KEY_OFFICE_CODE = "office_code"
    private const val KEY_SCHOOL_CODE = "school_code"
    private const val KEY_SCHOOL_KIND = "school_kind"
    private const val KEY_TIMER_DISPLAY_MODE = "timer_display_mode"
    private const val KEY_TIMER_NOTIFICATION_ENABLED = "timer_notification_enabled"
    private const val KEY_TIMER_VIBRATION_ENABLED = "timer_vibration_enabled"
    private const val KEY_TIMER_TARGET_AT = "timer_target_at"
    private const val KEY_TIMER_TOTAL_MILLIS = "timer_total_millis"
    private const val KEY_TIMER_REMAINING_MILLIS = "timer_remaining_millis"
    private const val KEY_TIMER_RUNNING = "timer_running"
    private const val KEY_TIMER_PRESET = "timer_preset"

    const val TIMER_DISPLAY_COUNT = "count"
    const val TIMER_DISPLAY_RING = "ring"

    fun saveStudentInfo(context: Context, studentInfo: StudentInfo) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_GRADE, studentInfo.grade)
            .putString(KEY_CLASSROOM, studentInfo.classroom)
            .putString(KEY_SCHOOL_NAME, studentInfo.schoolName)
            .putString(KEY_OFFICE_CODE, studentInfo.officeCode)
            .putString(KEY_SCHOOL_CODE, studentInfo.schoolCode)
            .putString(KEY_SCHOOL_KIND, studentInfo.schoolKind)
            .apply()
    }

    fun getStudentInfo(context: Context): StudentInfo {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return StudentInfo(
            grade = preferences.getString(KEY_GRADE, "") ?: "",
            classroom = preferences.getString(KEY_CLASSROOM, "") ?: "",
            schoolName = preferences.getString(KEY_SCHOOL_NAME, "") ?: "",
            officeCode = preferences.getString(KEY_OFFICE_CODE, "") ?: "",
            schoolCode = preferences.getString(KEY_SCHOOL_CODE, "") ?: "",
            schoolKind = preferences.getString(KEY_SCHOOL_KIND, "") ?: ""
        )
    }

    fun getGrade(context: Context): String = getStudentInfo(context).grade

    fun getClassroom(context: Context): String = getStudentInfo(context).classroom

    fun hasStudentInfo(context: Context): Boolean {
        return getStudentInfo(context).isComplete()
    }

    fun saveTimerDisplayMode(context: Context, displayMode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TIMER_DISPLAY_MODE, displayMode)
            .apply()
    }

    fun getTimerDisplayMode(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TIMER_DISPLAY_MODE, TIMER_DISPLAY_COUNT)
            ?: TIMER_DISPLAY_COUNT
    }

    fun saveTimerNotificationEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_TIMER_NOTIFICATION_ENABLED, enabled)
            .apply()
    }

    fun isTimerNotificationEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_TIMER_NOTIFICATION_ENABLED, true)
    }

    fun saveTimerVibrationEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_TIMER_VIBRATION_ENABLED, enabled)
            .apply()
    }

    fun isTimerVibrationEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_TIMER_VIBRATION_ENABLED, true)
    }

    fun saveTimerState(
        context: Context,
        presetName: String,
        totalMillis: Long,
        remainingMillis: Long,
        targetAtMillis: Long,
        isRunning: Boolean
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TIMER_PRESET, presetName)
            .putLong(KEY_TIMER_TOTAL_MILLIS, totalMillis)
            .putLong(KEY_TIMER_REMAINING_MILLIS, remainingMillis)
            .putLong(KEY_TIMER_TARGET_AT, targetAtMillis)
            .putBoolean(KEY_TIMER_RUNNING, isRunning)
            .apply()
    }

    fun getTimerState(context: Context): TimerState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return TimerState(
            presetName = prefs.getString(KEY_TIMER_PRESET, "FOCUS") ?: "FOCUS",
            totalMillis = prefs.getLong(KEY_TIMER_TOTAL_MILLIS, 40L * 60L * 1000L),
            remainingMillis = prefs.getLong(KEY_TIMER_REMAINING_MILLIS, 40L * 60L * 1000L),
            targetAtMillis = prefs.getLong(KEY_TIMER_TARGET_AT, 0L),
            isRunning = prefs.getBoolean(KEY_TIMER_RUNNING, false)
        )
    }

    fun clearTimerState(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_TIMER_TARGET_AT)
            .remove(KEY_TIMER_TOTAL_MILLIS)
            .remove(KEY_TIMER_REMAINING_MILLIS)
            .remove(KEY_TIMER_RUNNING)
            .remove(KEY_TIMER_PRESET)
            .apply()
    }

    data class TimerState(
        val presetName: String,
        val totalMillis: Long,
        val remainingMillis: Long,
        val targetAtMillis: Long,
        val isRunning: Boolean
    )
}
