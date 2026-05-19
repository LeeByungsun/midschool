package com.bsbarron.midschoolapp.data.repository

import com.bsbarron.midschoolapp.data.model.MealInfo
import com.bsbarron.midschoolapp.data.model.SchoolInfo
import com.bsbarron.midschoolapp.data.model.TimetableItem

interface PreferencesRepository {
    fun getStudentInfo(): StudentInfo
    fun hasStudentInfo(): Boolean
    fun saveStudentInfo(studentInfo: StudentInfo)
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
    fun saveMealCache(officeCode: String, schoolCode: String, date: String, meals: List<MealInfo>)
    fun getMealCache(officeCode: String, schoolCode: String, date: String): List<MealInfo>?
    fun saveTimetableCache(
        officeCode: String,
        schoolCode: String,
        grade: String,
        classroom: String,
        date: String,
        items: List<TimetableItem>
    )
    fun getTimetableCache(
        officeCode: String,
        schoolCode: String,
        grade: String,
        classroom: String,
        date: String
    ): List<TimetableItem>?
    fun getWidgetSettings(appWidgetId: Int): WidgetSettings
    fun saveWidgetSettings(appWidgetId: Int, settings: WidgetSettings)
    fun clearWidgetSettings(appWidgetId: Int)
}

data class StudentInfo(
    val grade: String = "",
    val classroom: String = "",
    val schoolName: String = "",
    val officeCode: String = "",
    val schoolCode: String = "",
    val schoolKind: String = ""
) {
    fun hasClassroomInfo(): Boolean = grade.isNotBlank() && classroom.isNotBlank()

    fun hasSchoolSelection(): Boolean {
        return schoolName.isNotBlank() &&
            officeCode.isNotBlank() &&
            schoolCode.isNotBlank() &&
            schoolKind.isNotBlank()
    }

    fun isComplete(): Boolean = hasClassroomInfo() && hasSchoolSelection()

    fun toSchoolInfo(): SchoolInfo {
        return SchoolInfo(
            officeCode = officeCode,
            schoolCode = schoolCode,
            schoolName = schoolName,
            schoolKind = schoolKind
        )
    }
}

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

data class WidgetSettings(
    val showTomorrowTimetable: Boolean = true
)
