package com.lbs.schoolhelper.data.repository

import android.content.Context
import com.lbs.schoolhelper.data.model.MealInfo
import com.lbs.schoolhelper.data.model.SchoolEvent
import com.lbs.schoolhelper.data.model.TimetableItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PreferencesRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val gson: Gson,
    private val userPreferencesStore: UserPreferencesStore
) : PreferencesRepository {

    private val sharedPreferences by lazy {
        context.getSharedPreferences(REPOSITORY_PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun getStudentInfo(): StudentInfo {
        return userPreferencesStore.getStudentInfo()
    }

    override fun hasStudentInfo(): Boolean {
        return userPreferencesStore.hasStudentInfo()
    }

    override fun saveStudentInfo(studentInfo: StudentInfo) {
        userPreferencesStore.saveStudentInfo(studentInfo)
    }

    override fun getTimerDisplayMode(): TimerDisplayMode {
        return when (userPreferencesStore.getTimerDisplayMode()) {
            TIMER_DISPLAY_RING -> TimerDisplayMode.RING
            else -> TimerDisplayMode.COUNT
        }
    }

    override fun saveTimerDisplayMode(displayMode: TimerDisplayMode) {
        val value = when (displayMode) {
            TimerDisplayMode.COUNT -> TIMER_DISPLAY_COUNT
            TimerDisplayMode.RING -> TIMER_DISPLAY_RING
        }
        userPreferencesStore.saveTimerDisplayMode(value)
    }

    override fun isTimerNotificationEnabled(): Boolean {
        return userPreferencesStore.isTimerNotificationEnabled()
    }

    override fun saveTimerNotificationEnabled(enabled: Boolean) {
        userPreferencesStore.saveTimerNotificationEnabled(enabled)
    }

    override fun isTimerVibrationEnabled(): Boolean {
        return userPreferencesStore.isTimerVibrationEnabled()
    }

    override fun saveTimerVibrationEnabled(enabled: Boolean) {
        userPreferencesStore.saveTimerVibrationEnabled(enabled)
    }

    override fun getTimerState(): TimerPreferenceState {
        val state = userPreferencesStore.getTimerState()
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
        userPreferencesStore.saveTimerState(
            presetName = presetName,
            totalMillis = totalMillis,
            remainingMillis = remainingMillis,
            targetAtMillis = targetAtMillis,
            isRunning = isRunning
        )
    }

    override fun clearTimerState() {
        userPreferencesStore.clearTimerState()
    }

    override fun saveMealCache(
        officeCode: String,
        schoolCode: String,
        date: String,
        meals: List<MealInfo>
    ) {
        sharedPreferences.edit()
            .putString(mealCacheKey(officeCode, schoolCode, date), gson.toJson(meals))
            .putLong(mealCacheTimestampKey(officeCode, schoolCode, date), System.currentTimeMillis())
            .apply()
    }

    override fun getMealCache(
        officeCode: String,
        schoolCode: String,
        date: String
    ): List<MealInfo>? {
        if (!isCacheFresh(mealCacheTimestampKey(officeCode, schoolCode, date), MEAL_CACHE_TTL_MILLIS)) {
            clearMealCache(officeCode, schoolCode, date)
            return null
        }
        val raw = sharedPreferences.getString(mealCacheKey(officeCode, schoolCode, date), null)
            ?: return null
        val type = object : TypeToken<List<MealInfo>>() {}.type
        return runCatching { gson.fromJson<List<MealInfo>>(raw, type) }.getOrNull()
    }

    override fun saveTimetableCache(
        officeCode: String,
        schoolCode: String,
        grade: String,
        classroom: String,
        date: String,
        items: List<TimetableItem>
    ) {
        sharedPreferences.edit()
            .putString(
                timetableCacheKey(officeCode, schoolCode, grade, classroom, date),
                gson.toJson(items)
            )
            .putLong(
                timetableCacheTimestampKey(officeCode, schoolCode, grade, classroom, date),
                System.currentTimeMillis()
            )
            .apply()
    }

    override fun getTimetableCache(
        officeCode: String,
        schoolCode: String,
        grade: String,
        classroom: String,
        date: String
    ): List<TimetableItem>? {
        if (!isCacheFresh(
                timetableCacheTimestampKey(officeCode, schoolCode, grade, classroom, date),
                TIMETABLE_CACHE_TTL_MILLIS
            )
        ) {
            clearTimetableCache(officeCode, schoolCode, grade, classroom, date)
            return null
        }
        val raw = sharedPreferences.getString(
            timetableCacheKey(officeCode, schoolCode, grade, classroom, date),
            null
        ) ?: return null
        val type = object : TypeToken<List<TimetableItem>>() {}.type
        return runCatching { gson.fromJson<List<TimetableItem>>(raw, type) }.getOrNull()
    }

    override fun saveScheduleCache(
        officeCode: String,
        schoolCode: String,
        date: String,
        events: List<SchoolEvent>
    ) {
        sharedPreferences.edit()
            .putString(
                scheduleCacheKey(officeCode, schoolCode, date),
                gson.toJson(events)
            )
            .putLong(
                scheduleCacheTimestampKey(officeCode, schoolCode, date),
                System.currentTimeMillis()
            )
            .apply()
    }

    override fun getScheduleCache(
        officeCode: String,
        schoolCode: String,
        date: String
    ): List<SchoolEvent>? {
        if (!isCacheFresh(
                scheduleCacheTimestampKey(officeCode, schoolCode, date),
                SCHEDULE_CACHE_TTL_MILLIS
            )
        ) {
            clearScheduleCache(officeCode, schoolCode, date)
            return null
        }
        val raw = sharedPreferences.getString(scheduleCacheKey(officeCode, schoolCode, date), null)
            ?: return null
        val type = object : TypeToken<List<SchoolEvent>>() {}.type
        return runCatching { gson.fromJson<List<SchoolEvent>>(raw, type) }
            .getOrNull()
    }

    override fun getWidgetSettings(appWidgetId: Int): WidgetSettings {
        return WidgetSettings(
            showTomorrowTimetable = sharedPreferences.getBoolean(widgetTomorrowKey(appWidgetId), true)
        )
    }

    override fun saveWidgetSettings(appWidgetId: Int, settings: WidgetSettings) {
        sharedPreferences.edit()
            .putBoolean(widgetTomorrowKey(appWidgetId), settings.showTomorrowTimetable)
            .apply()
    }

    override fun clearWidgetSettings(appWidgetId: Int) {
        sharedPreferences.edit()
            .remove(widgetTomorrowKey(appWidgetId))
            .remove(widgetMealKey(appWidgetId))
            .remove(widgetSubjectCountKey(appWidgetId))
            .apply()
    }

    private fun mealCacheKey(officeCode: String, schoolCode: String, date: String): String {
        return "meal_cache_${officeCode}_${schoolCode}_$date"
    }

    private fun mealCacheTimestampKey(officeCode: String, schoolCode: String, date: String): String {
        return "meal_cache_ts_${officeCode}_${schoolCode}_$date"
    }

    private fun timetableCacheKey(
        officeCode: String,
        schoolCode: String,
        grade: String,
        classroom: String,
        date: String
    ): String {
        return "timetable_cache_${officeCode}_${schoolCode}_${grade}_${classroom}_$date"
    }

    private fun timetableCacheTimestampKey(
        officeCode: String,
        schoolCode: String,
        grade: String,
        classroom: String,
        date: String
    ): String {
        return "timetable_cache_ts_${officeCode}_${schoolCode}_${grade}_${classroom}_$date"
    }

    private fun scheduleCacheKey(officeCode: String, schoolCode: String, date: String): String {
        return "schedule_cache_${officeCode}_${schoolCode}_$date"
    }

    private fun scheduleCacheTimestampKey(officeCode: String, schoolCode: String, date: String): String {
        return "schedule_cache_ts_${officeCode}_${schoolCode}_$date"
    }

    private fun widgetTomorrowKey(appWidgetId: Int): String = "widget_${appWidgetId}_show_tomorrow"

    private fun widgetMealKey(appWidgetId: Int): String = "widget_${appWidgetId}_show_meal"

    private fun widgetSubjectCountKey(appWidgetId: Int): String = "widget_${appWidgetId}_subject_count"

    private fun isCacheFresh(timestampKey: String, ttlMillis: Long): Boolean {
        val savedAt = sharedPreferences.getLong(timestampKey, 0L)
        if (savedAt <= 0L) {
            return false
        }
        return System.currentTimeMillis() - savedAt <= ttlMillis
    }

    private fun clearMealCache(officeCode: String, schoolCode: String, date: String) {
        sharedPreferences.edit()
            .remove(mealCacheKey(officeCode, schoolCode, date))
            .remove(mealCacheTimestampKey(officeCode, schoolCode, date))
            .apply()
    }

    private fun clearTimetableCache(
        officeCode: String,
        schoolCode: String,
        grade: String,
        classroom: String,
        date: String
    ) {
        sharedPreferences.edit()
            .remove(timetableCacheKey(officeCode, schoolCode, grade, classroom, date))
            .remove(timetableCacheTimestampKey(officeCode, schoolCode, grade, classroom, date))
            .apply()
    }

    private fun clearScheduleCache(
        officeCode: String,
        schoolCode: String,
        date: String
    ) {
        sharedPreferences.edit()
            .remove(scheduleCacheKey(officeCode, schoolCode, date))
            .remove(scheduleCacheTimestampKey(officeCode, schoolCode, date))
            .apply()
    }

    companion object {
        private const val REPOSITORY_PREFS_NAME = "midschool_repository_prefs"
        private const val TIMER_DISPLAY_COUNT = "count"
        private const val TIMER_DISPLAY_RING = "ring"
        private const val MEAL_CACHE_TTL_MILLIS = 12 * 60 * 60 * 1000L
        private const val TIMETABLE_CACHE_TTL_MILLIS = 24 * 60 * 60 * 1000L
        private const val SCHEDULE_CACHE_TTL_MILLIS = 12 * 60 * 60 * 1000L
    }
}
