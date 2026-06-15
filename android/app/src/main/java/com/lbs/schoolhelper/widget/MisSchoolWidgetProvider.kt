package com.lbs.schoolhelper.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.lbs.schoolhelper.R
import com.lbs.schoolhelper.SplashActivity
import com.lbs.schoolhelper.data.repository.PreferencesRepository
import com.lbs.schoolhelper.data.repository.SchoolRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger

class MisSchoolWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        WidgetMidnightScheduler.scheduleNext(context)
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                WidgetMidnightScheduler.scheduleNext(context)
                val pendingResult = goAsync()
                updateWidgets(
                    context = context,
                    appWidgetManager = appWidgetManager,
                    appWidgetIds = appWidgetIdsFromIntent(context, appWidgetManager, intent),
                    onComplete = pendingResult::finish
                )
            }

            ACTION_REFRESH -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                val pendingResult = goAsync()
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    updateAppWidget(
                        context = context,
                        appWidgetManager = appWidgetManager,
                        appWidgetId = appWidgetId,
                        onComplete = pendingResult::finish
                    )
                } else {
                    pendingResult.finish()
                }
            }

            WidgetMidnightScheduler.ACTION_MIDNIGHT_REFRESH -> {
                val pendingResult = goAsync()
                updateAllWidgets(
                    context = context,
                    appWidgetManager = appWidgetManager,
                    onComplete = {
                        WidgetMidnightScheduler.scheduleNext(context)
                        pendingResult.finish()
                    }
                )
            }

            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED -> {
                WidgetMidnightScheduler.scheduleNext(context)
                val pendingResult = goAsync()
                updateAllWidgets(
                    context = context,
                    appWidgetManager = appWidgetManager,
                    onComplete = pendingResult::finish
                )
            }

            else -> {
                super.onReceive(context, intent)
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetMidnightScheduler.scheduleNext(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetMidnightScheduler.cancel(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val preferencesRepository = dependencies(context).preferencesRepository()
        appWidgetIds.forEach(preferencesRepository::clearWidgetSettings)
    }

    companion object {
        const val ACTION_REFRESH = "com.lbs.schoolhelper.widget.ACTION_REFRESH"
        private const val CONFIG_REQUEST_CODE_OFFSET = 10_000
        private const val OPEN_APP_REQUEST_CODE_OFFSET = 20_000

        fun requestWidgetUpdate(context: Context, appWidgetId: Int) {
            WidgetMidnightScheduler.scheduleNext(context)
            val intent = Intent(context, MisSchoolWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            }
            context.sendBroadcast(intent)
        }

        fun updateAllWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            onComplete: (() -> Unit)? = null
        ) {
            updateWidgets(
                context = context,
                appWidgetManager = appWidgetManager,
                appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponentName(context)),
                onComplete = onComplete
            )
        }

        private fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
            onComplete: (() -> Unit)? = null
        ) {
            if (appWidgetIds.isEmpty()) {
                onComplete?.invoke()
                return
            }

            val dependencies = dependencies(context)
            val schoolRepository = dependencies.schoolRepository()
            val preferencesRepository = dependencies.preferencesRepository()
            val remainingUpdates = AtomicInteger(appWidgetIds.size)
            appWidgetIds.forEach { appWidgetId ->
                updateAppWidget(
                    context = context,
                    appWidgetManager = appWidgetManager,
                    appWidgetId = appWidgetId,
                    schoolRepository = schoolRepository,
                    preferencesRepository = preferencesRepository,
                    onComplete = {
                        if (remainingUpdates.decrementAndGet() == 0) {
                            onComplete?.invoke()
                        }
                    }
                )
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            schoolRepository: SchoolRepository = dependencies(context).schoolRepository(),
            preferencesRepository: PreferencesRepository = dependencies(context).preferencesRepository(),
            onComplete: (() -> Unit)? = null
        ) {
            fun createBaseViews(): RemoteViews {
                return RemoteViews(context.packageName, R.layout.widget_home).apply {
                    val today = LocalDate.now()
                    val widgetSettings = preferencesRepository.getWidgetSettings(appWidgetId)
                    setTextViewText(
                        R.id.widgetDateText,
                        WidgetDateFormatter.formatHeaderDate(today)
                    )
                    setViewVisibility(
                        R.id.widgetTomorrowSection,
                        if (widgetSettings.showTomorrowTimetable) View.VISIBLE else View.GONE
                    )
                    setViewVisibility(
                        R.id.widgetTimetableDivider,
                        if (widgetSettings.showTomorrowTimetable) View.VISIBLE else View.GONE
                    )
                    val openAppIntent = context.packageManager
                        .getLaunchIntentForPackage(context.packageName)
                        ?.apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        ?: Intent(context, SplashActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                    val openAppPendingIntent = PendingIntent.getActivity(
                        context,
                        appWidgetId + OPEN_APP_REQUEST_CODE_OFFSET,
                        openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setOnClickPendingIntent(R.id.widgetContainer, openAppPendingIntent)

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

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val studentInfo = preferencesRepository.getStudentInfo()
                    val widgetSettings = preferencesRepository.getWidgetSettings(appWidgetId)
                    val grade = studentInfo.grade
                    val classroom = studentInfo.classroom

                    if (!studentInfo.isComplete()) {
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

                    var timetableTextToday = context.getString(R.string.widget_loading)
                    var timetableTextTomorrow = context.getString(R.string.widget_loading)
                    val publishMutex = Mutex()

                    suspend fun publishTimetableTexts() {
                        publishMutex.withLock {
                            val finalViews = createBaseViews()
                            finalViews.setTextViewText(R.id.widgetTimetableText, timetableTextToday)
                            if (widgetSettings.showTomorrowTimetable) {
                                finalViews.setTextViewText(R.id.widgetTomorrowTimetableText, timetableTextTomorrow)
                            }
                            appWidgetManager.updateAppWidget(appWidgetId, finalViews)
                        }
                    }

                    coroutineScope {
                        val jobs = mutableListOf(
                            launch {
                                schoolRepository.observeTimetable(grade, classroom, todayStr).collect { result ->
                                    timetableTextToday = formatTimetableText(result = result, context = context)
                                    publishTimetableTexts()
                                }
                            }
                        )

                        if (widgetSettings.showTomorrowTimetable) {
                            jobs += launch {
                                schoolRepository.observeTimetable(grade, classroom, tomorrowStr).collect { result ->
                                    timetableTextTomorrow = formatTimetableText(result = result, context = context)
                                    publishTimetableTexts()
                                }
                            }
                        }

                        jobs.joinAll()
                    }

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
                    onComplete?.invoke()
                }
            }
        }

        private fun formatTimetableText(
            result: Result<List<com.lbs.schoolhelper.data.model.TimetableItem>>,
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

        private fun dependencies(context: Context): WidgetProviderEntryPoint {
            return EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetProviderEntryPoint::class.java
            )
        }

        private fun appWidgetIdsFromIntent(
            context: Context,
            appWidgetManager: AppWidgetManager,
            intent: Intent
        ): IntArray {
            return intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ?.takeIf { it.isNotEmpty() }
                ?: appWidgetManager.getAppWidgetIds(widgetComponentName(context))
        }

        private fun widgetComponentName(context: Context): ComponentName {
            return ComponentName(context, MisSchoolWidgetProvider::class.java)
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetProviderEntryPoint {
    fun schoolRepository(): SchoolRepository
    fun preferencesRepository(): PreferencesRepository
}
