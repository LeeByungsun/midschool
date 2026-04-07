package com.bsbarron.midschoolapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.bsbarron.midschoolapp.R
import com.bsbarron.midschoolapp.data.repository.PreferencesRepository
import com.bsbarron.midschoolapp.data.repository.SchoolRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MisSchoolWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var schoolRepository: SchoolRepository

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        fun createBaseViews(): RemoteViews {
            return RemoteViews(context.packageName, R.layout.widget_home).apply {
                val today = LocalDate.now()
                setTextViewText(
                    R.id.widgetDateText,
                    today.format(DateTimeFormatter.ofPattern("🗓️ 오늘 (M/d)", Locale.KOREAN))
                )
                val intent = Intent(context, MisSchoolWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, appWidgetId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.widgetRefreshButton, pendingIntent)
            }
        }

        val loadingViews = createBaseViews()
        loadingViews.setTextViewText(R.id.widgetTimetableText, "불러오는 중...")
        loadingViews.setTextViewText(R.id.widgetTomorrowTimetableText, "불러오는 중...")
        loadingViews.setTextViewText(R.id.widgetMealText, "")
        appWidgetManager.updateAppWidget(appWidgetId, loadingViews)

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val studentInfo = preferencesRepository.getStudentInfo()
                val grade = studentInfo.grade
                val classroom = studentInfo.classroom

                if (grade.isBlank() || classroom.isBlank()) {
                    val setupViews = createBaseViews()
                    setupViews.setTextViewText(
                        R.id.widgetTimetableText,
                        context.getString(R.string.widget_requires_student_info)
                    )
                    setupViews.setTextViewText(
                        R.id.widgetTomorrowTimetableText,
                        context.getString(R.string.widget_requires_student_info)
                    )
                    setupViews.setTextViewText(R.id.widgetMealText, "")
                    appWidgetManager.updateAppWidget(appWidgetId, setupViews)
                    return@launch
                }

                val today = LocalDate.now()
                val todayStr = today.format(DateTimeFormatter.BASIC_ISO_DATE)
                val tomorrow = today.plusDays(1)
                val tomorrowStr = tomorrow.format(DateTimeFormatter.BASIC_ISO_DATE)

                val timetableResultToday = schoolRepository.getTimetable(grade, classroom, todayStr)
                val timetableResultTomorrow = schoolRepository.getTimetable(grade, classroom, tomorrowStr)
                val mealsResult = schoolRepository.getMeals(todayStr)

                val timetableTextToday = formatTimetableText(
                    result = timetableResultToday,
                    context = context
                )
                val timetableTextTomorrow = formatTimetableText(
                    result = timetableResultTomorrow,
                    context = context
                )
                val mealText = formatMealText(
                    result = mealsResult,
                    context = context
                )

                val finalViews = createBaseViews()
                finalViews.setTextViewText(R.id.widgetTimetableText, timetableTextToday)
                finalViews.setTextViewText(R.id.widgetTomorrowTimetableText, timetableTextTomorrow)
                finalViews.setTextViewText(R.id.widgetMealText, mealText)
                appWidgetManager.updateAppWidget(appWidgetId, finalViews)

            } catch (e: Exception) {
                val errViews = createBaseViews()
                errViews.setTextViewText(
                    R.id.widgetTimetableText,
                    context.getString(R.string.widget_load_error)
                )
                errViews.setTextViewText(
                    R.id.widgetTomorrowTimetableText,
                    e.message ?: context.getString(R.string.widget_retry_hint)
                )
                errViews.setTextViewText(R.id.widgetMealText, context.getString(R.string.widget_retry_hint))
                appWidgetManager.updateAppWidget(appWidgetId, errViews)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun formatTimetableText(
        result: Result<List<com.bsbarron.midschoolapp.data.model.TimetableItem>>,
        context: Context
    ): String {
        result.exceptionOrNull()?.message?.let { return it }
        val items = result.getOrNull().orEmpty()
            .sortedBy { it.period.toIntOrNull() ?: Int.MAX_VALUE }
            .mapNotNull { item -> item.subject.takeIf { subject -> subject.isNotBlank() } }

        if (items.isEmpty()) {
            return context.getString(R.string.widget_no_classes)
        }

        return items.take(WIDGET_TIMETABLE_MAX_SUBJECTS).joinToString(" • ").let { subjectText ->
            if (items.size > WIDGET_TIMETABLE_MAX_SUBJECTS) {
                "$subjectText ${context.getString(R.string.widget_more_suffix)}"
            } else {
                subjectText
            }
        }
    }

    private fun formatMealText(
        result: Result<List<com.bsbarron.midschoolapp.data.model.MealInfo>>,
        context: Context
    ): String {
        result.exceptionOrNull()?.message?.let { return it }
        val firstMeal = result.getOrNull()?.firstOrNull()
            ?: return context.getString(R.string.widget_no_meal)

        return firstMeal.menu
            .replace(Regex("<br\\s*/?>"), " ")
            .replace(Regex("[ \t]+"), " ")
            .trim()
            .ifBlank { context.getString(R.string.widget_no_meal) }
    }

    companion object {
        const val ACTION_REFRESH = "com.bsbarron.midschoolapp.widget.ACTION_REFRESH"
        private const val WIDGET_TIMETABLE_MAX_SUBJECTS = 4
    }
}
