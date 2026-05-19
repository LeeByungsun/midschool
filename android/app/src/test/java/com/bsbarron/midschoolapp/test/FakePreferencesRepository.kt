package com.bsbarron.midschoolapp.test

import com.bsbarron.midschoolapp.data.model.MealInfo
import com.bsbarron.midschoolapp.data.model.TimetableItem
import com.bsbarron.midschoolapp.data.repository.PreferencesRepository
import com.bsbarron.midschoolapp.data.repository.StudentInfo
import com.bsbarron.midschoolapp.data.repository.TimerDisplayMode
import com.bsbarron.midschoolapp.data.repository.TimerPreferenceState
import com.bsbarron.midschoolapp.data.repository.WidgetSettings

class FakePreferencesRepository(
    studentInfo: StudentInfo = StudentInfo(),
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

    private val mealCache = mutableMapOf<MealCacheKey, List<MealInfo>>()
    private val timetableCache = mutableMapOf<TimetableCacheKey, List<TimetableItem>>()
    private val widgetSettings = mutableMapOf<Int, WidgetSettings>()

    override fun getStudentInfo(): StudentInfo = currentStudentInfo

    override fun hasStudentInfo(): Boolean = currentStudentInfo.isComplete()

    override fun saveStudentInfo(studentInfo: StudentInfo) {
        currentStudentInfo = studentInfo
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

    override fun saveMealCache(
        officeCode: String,
        schoolCode: String,
        date: String,
        meals: List<MealInfo>
    ) {
        mealCache[MealCacheKey(officeCode, schoolCode, date)] = meals
    }

    override fun getMealCache(
        officeCode: String,
        schoolCode: String,
        date: String
    ): List<MealInfo>? = mealCache[MealCacheKey(officeCode, schoolCode, date)]

    override fun saveTimetableCache(
        officeCode: String,
        schoolCode: String,
        grade: String,
        classroom: String,
        date: String,
        items: List<TimetableItem>
    ) {
        timetableCache[TimetableCacheKey(officeCode, schoolCode, grade, classroom, date)] = items
    }

    override fun getTimetableCache(
        officeCode: String,
        schoolCode: String,
        grade: String,
        classroom: String,
        date: String
    ): List<TimetableItem>? {
        return timetableCache[TimetableCacheKey(officeCode, schoolCode, grade, classroom, date)]
    }

    override fun getWidgetSettings(appWidgetId: Int): WidgetSettings {
        return widgetSettings[appWidgetId] ?: WidgetSettings()
    }

    override fun saveWidgetSettings(appWidgetId: Int, settings: WidgetSettings) {
        widgetSettings[appWidgetId] = settings
    }

    override fun clearWidgetSettings(appWidgetId: Int) {
        widgetSettings.remove(appWidgetId)
    }

    private data class MealCacheKey(
        val officeCode: String,
        val schoolCode: String,
        val date: String
    )

    private data class TimetableCacheKey(
        val officeCode: String,
        val schoolCode: String,
        val grade: String,
        val classroom: String,
        val date: String
    )
}
