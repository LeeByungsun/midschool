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
                    setupViews.setTextViewText(R.id.widgetTimetableText, "학년/반 설정 필요")
                    setupViews.setTextViewText(R.id.widgetTomorrowTimetableText, "학년/반 설정 필요")
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

                val timetableTextToday = timetableResultToday.getOrNull()?.takeIf { it.isNotEmpty() }?.let { items ->
                    items.joinToString(" • ") { it.subject }
                } ?: "수업 없음"
                
                val timetableTextTomorrow = timetableResultTomorrow.getOrNull()?.takeIf { it.isNotEmpty() }?.let { items ->
                    items.joinToString(" • ") { it.subject }
                } ?: "수업 없음"

                val firstMeal = mealsResult.getOrNull()?.firstOrNull()
                val mealText = firstMeal?.menu?.replace(Regex("<br\\s*/?>"), " ")?.replace(Regex("[ \t]+"), " ")
                    ?: "오늘은 등록된 급식이 없어요."

                val finalViews = createBaseViews()
                finalViews.setTextViewText(R.id.widgetTimetableText, timetableTextToday)
                finalViews.setTextViewText(R.id.widgetTomorrowTimetableText, timetableTextTomorrow)
                finalViews.setTextViewText(R.id.widgetMealText, mealText)
                appWidgetManager.updateAppWidget(appWidgetId, finalViews)

            } catch (e: Exception) {
                val errViews = createBaseViews()
                errViews.setTextViewText(R.id.widgetTimetableText, "에러명: ${e.javaClass.simpleName}")
                errViews.setTextViewText(R.id.widgetTomorrowTimetableText, e.message ?: "원인 알 수 없음")
                errViews.setTextViewText(R.id.widgetMealText, "새로고침을 눌러 다시 시도하세요")
                appWidgetManager.updateAppWidget(appWidgetId, errViews)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.bsbarron.midschoolapp.widget.ACTION_REFRESH"
    }
}
