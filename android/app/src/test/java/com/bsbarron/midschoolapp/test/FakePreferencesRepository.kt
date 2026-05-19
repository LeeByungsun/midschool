package com.bsbarron.midschoolapp.test

import com.bsbarron.midschoolapp.data.model.MealInfo
import com.bsbarron.midschoolapp.data.model.TimetableItem
import com.bsbarron.midschoolapp.data.repository.PreferencesRepository
import com.bsbarron.midschoolapp.data.repository.StudentInfo
import com.bsbarron.midschoolapp.data.repository.TimerDisplayMode
import com.bsbarron.midschoolapp.data.repository.TimerPreferenceState
import com.bsbarron.midschoolapp.data.repository.WidgetSettings

class FakePreferencesRepository(
    studentInfo: StudentInfo = StudentInfo("", ""),
    timerDisplayMode: TimerDisplayMode = TimerDisplayMode.COUNT,
    notificationEnabled: Boolean = true,
    vibrationEnabled: Boolean = true,
    timerState: TimerPreferenceState = TimerPreferenceState(
        presetName = "FOCUS",
        totalMillis = 0L,
        remainingMillis = 0L,
        targetAtMillis = 0L,
        isRunning = false
    )
) : PreferencesRepository {
    var currentStudentInfo: StudentInfo = studentInfo
        private set
    var currentTimerDisplayMode: TimerDisplayMode = timerDisplayMode
        private set
    var currentNotificationEnabled: Boolean = notificationEnabled
        private set
    var currentVibrationEnabled: Boolean = vibrationEnabled
        private set
    var currentTimerState: TimerPreferenceState = timerState
        private set

    val savedStudentInfoCalls = mutableListOf<StudentInfo>()
    val savedTimerDisplayModes = mutableListOf<TimerDisplayMode>()
    val savedNotificationEnabledValues = mutableListOf<Boolean>()
    val savedVibrationEnabledValues = mutableListOf<Boolean>()

    private val mealCache = mutableMapOf<String, List<MealInfo>>()
    private val timetableCache = mutableMapOf<Triple<String, String, String>, List<TimetableItem>>()
    private val widgetSettings = mutableMapOf<Int, WidgetSettings>()

    override fun getStudentInfo(): StudentInfo = currentStudentInfo

    override fun hasStudentInfo(): Boolean {
        return currentStudentInfo.grade.isNotBlank() && currentStudentInfo.classroom.isNotBlank()
    }

    override fun saveStudentInfo(grade: String, classroom: String) {
        currentStudentInfo = StudentInfo(grade = grade, classroom = classroom)
        savedStudentInfoCalls += currentStudentInfo
    }

    override fun getTimerDisplayMode(): TimerDisplayMode = currentTimerDisplayMode

    override fun saveTimerDisplayMode(displayMode: TimerDisplayMode) {
        currentTimerDisplayMode = displayMode
        savedTimerDisplayModes += displayMode
    }

    override fun isTimerNotificationEnabled(): Boolean = currentNotificationEnabled

    override fun saveTimerNotificationEnabled(enabled: Boolean) {
        currentNotificationEnabled = enabled
        savedNotificationEnabledValues += enabled
    }

    override fun isTimerVibrationEnabled(): Boolean = currentVibrationEnabled

    override fun saveTimerVibrationEnabled(enabled: Boolean) {
        currentVibrationEnabled = enabled
        savedVibrationEnabledValues += enabled
    }

    override fun getTimerState(): TimerPreferenceState = currentTimerState

    override fun saveTimerState(
        presetName: String,
        totalMillis: Long,
        remainingMillis: Long,
        targetAtMillis: Long,
        isRunning: Boolean
    ) {
        currentTimerState = TimerPreferenceState(
            presetName = presetName,
            totalMillis = totalMillis,
            remainingMillis = remainingMillis,
            targetAtMillis = targetAtMillis,
            isRunning = isRunning
        )
    }

    override fun clearTimerState() {
        currentTimerState = TimerPreferenceState(
            presetName = "FOCUS",
            totalMillis = 0L,
            remainingMillis = 0L,
            targetAtMillis = 0L,
            isRunning = false
        )
    }

    override fun saveMealCache(date: String, meals: List<MealInfo>) {
        mealCache[date] = meals
    }

    override fun getMealCache(date: String): List<MealInfo>? = mealCache[date]

    override fun saveTimetableCache(
        grade: String,
        classroom: String,
        date: String,
        items: List<TimetableItem>
    ) {
        timetableCache[Triple(grade, classroom, date)] = items
    }

    override fun getTimetableCache(
        grade: String,
        classroom: String,
        date: String
    ): List<TimetableItem>? = timetableCache[Triple(grade, classroom, date)]

    override fun getWidgetSettings(appWidgetId: Int): WidgetSettings {
        return widgetSettings[appWidgetId] ?: WidgetSettings()
    }

    override fun saveWidgetSettings(appWidgetId: Int, settings: WidgetSettings) {
        widgetSettings[appWidgetId] = settings
    }

    override fun clearWidgetSettings(appWidgetId: Int) {
        widgetSettings.remove(appWidgetId)
    }
}
