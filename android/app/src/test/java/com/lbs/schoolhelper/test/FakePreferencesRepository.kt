package com.lbs.schoolhelper.test

import com.lbs.schoolhelper.data.model.MealInfo
import com.lbs.schoolhelper.data.model.SchoolEvent
import com.lbs.schoolhelper.data.model.TimetableItem
import com.lbs.schoolhelper.data.repository.PreferencesRepository
import com.lbs.schoolhelper.data.repository.StudentInfo
import com.lbs.schoolhelper.data.repository.TimerDisplayMode
import com.lbs.schoolhelper.data.repository.TimerPreferenceState
import com.lbs.schoolhelper.data.repository.WidgetSettings

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
    val savedTimerStates = mutableListOf<TimerPreferenceState>()
    var clearTimerStateCallCount: Int = 0
        private set

    private val mealCache = mutableMapOf<MealCacheKey, List<MealInfo>>()
    private val timetableCache = mutableMapOf<TimetableCacheKey, List<TimetableItem>>()
    private val scheduleCache = mutableMapOf<ScheduleCacheKey, List<SchoolEvent>>()
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
        savedTimerStates += currentTimerState
    }

    override fun clearTimerState() {
        clearTimerStateCallCount += 1
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

    override fun saveScheduleCache(
        officeCode: String,
        schoolCode: String,
        date: String,
        events: List<SchoolEvent>
    ) {
        scheduleCache[ScheduleCacheKey(officeCode, schoolCode, date)] = events
    }

    override fun getScheduleCache(
        officeCode: String,
        schoolCode: String,
        date: String
    ): List<SchoolEvent>? {
        return scheduleCache[ScheduleCacheKey(officeCode, schoolCode, date)]
    }

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

    private data class ScheduleCacheKey(
        val officeCode: String,
        val schoolCode: String,
        val date: String
    )
}
