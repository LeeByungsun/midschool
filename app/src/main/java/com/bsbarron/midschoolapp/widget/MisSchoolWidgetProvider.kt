package com.bsbarron.midschoolapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.bsbarron.midschoolapp.R
import com.bsbarron.midschoolapp.data.repository.PreferencesRepository
import com.bsbarron.midschoolapp.data.repository.SchoolRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MisSchoolWidgetProvider : AppWidgetProvider() {

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

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val preferencesRepository = dependencies(context).preferencesRepository()
        appWidgetIds.forEach(preferencesRepository::clearWidgetSettings)
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val schoolRepository = dependencies(context).schoolRepository()
        val preferencesRepository = dependencies(context).preferencesRepository()

        fun createBaseViews(): RemoteViews {
            return RemoteViews(context.packageName, R.layout.widget_home).apply {
                val today = LocalDate.now()
                val widgetSettings = preferencesRepository.getWidgetSettings(appWidgetId)
                setTextViewText(
                    R.id.widgetDateText,
                    today.format(DateTimeFormatter.ofPattern("🗓️ 오늘 (M/d)", Locale.KOREAN))
                )
                setViewVisibility(
                    R.id.widgetTomorrowSection,
                    if (widgetSettings.showTomorrowTimetable) View.VISIBLE else View.GONE
                )
                setViewVisibility(
                    R.id.widgetTimetableDivider,
                    if (widgetSettings.showTomorrowTimetable) View.VISIBLE else View.GONE
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

                val configIntent = Intent(context, WidgetConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val configPendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId + CONFIG_REQUEST_CODE_OFFSET,
                    configIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.widgetSettingsButton, configPendingIntent)
            }
        }

        val loadingViews = createBaseViews()
        loadingViews.setTextViewText(R.id.widgetTimetableText, context.getString(R.string.widget_loading))
        loadingViews.setTextViewText(
            R.id.widgetTomorrowTimetableText,
            context.getString(R.string.widget_loading)
        )
        appWidgetManager.updateAppWidget(appWidgetId, loadingViews)

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val studentInfo = preferencesRepository.getStudentInfo()
                val widgetSettings = preferencesRepository.getWidgetSettings(appWidgetId)
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
                    appWidgetManager.updateAppWidget(appWidgetId, setupViews)
                    return@launch
                }

                val today = LocalDate.now()
                val todayStr = today.format(DateTimeFormatter.BASIC_ISO_DATE)
                val tomorrow = today.plusDays(1)
                val tomorrowStr = tomorrow.format(DateTimeFormatter.BASIC_ISO_DATE)

                val timetableResultToday = schoolRepository.getTimetable(grade, classroom, todayStr)
                val timetableResultTomorrow = schoolRepository.getTimetable(grade, classroom, tomorrowStr)

                val timetableTextToday = formatTimetableText(
                    result = timetableResultToday,
                    context = context
                )
                val timetableTextTomorrow = formatTimetableText(
                    result = timetableResultTomorrow,
                    context = context
                )

                val finalViews = createBaseViews()
                finalViews.setTextViewText(R.id.widgetTimetableText, timetableTextToday)
                if (widgetSettings.showTomorrowTimetable) {
                    finalViews.setTextViewText(R.id.widgetTomorrowTimetableText, timetableTextTomorrow)
                }
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
            .mapNotNull { item ->
                item.subject.takeIf { subject -> subject.isNotBlank() }?.let { subject ->
                    val period = item.period.takeIf { it.isNotBlank() } ?: "?"
                    "${period}교시 ${subject.truncatedWidgetSubject()}"
                }
            }

        if (items.isEmpty()) {
            return context.getString(R.string.widget_no_classes)
        }

        return items.joinToString("\n")
    }

    private fun String.truncatedWidgetSubject(): String {
        return if (length > 6) take(5) else this
    }

    companion object {
        const val ACTION_REFRESH = "com.bsbarron.midschoolapp.widget.ACTION_REFRESH"
        private const val CONFIG_REQUEST_CODE_OFFSET = 10_000

        fun requestWidgetUpdate(context: Context, appWidgetId: Int) {
            val intent = Intent(context, MisSchoolWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            }
            context.sendBroadcast(intent)
        }

        private fun dependencies(context: Context): WidgetProviderEntryPoint {
            return EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetProviderEntryPoint::class.java
            )
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetProviderEntryPoint {
    fun schoolRepository(): SchoolRepository
    fun preferencesRepository(): PreferencesRepository
}
