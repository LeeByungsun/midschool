package com.lbs.schoolhelper.telemetry

import com.lbs.schoolhelper.data.repository.StudentInfo
import java.util.concurrent.CancellationException

/** Only typed, bounded values cross this API; never pass queries, URLs or response bodies. */
interface AppTelemetry {
    fun setCollection(analytics: Boolean, diagnostics: Boolean) {}
    fun clearAnalytics() {}
    fun clearDiagnostics() {}
    fun appOpened(entryPoint: EntryPoint) {}
    fun featureViewed(feature: Feature) {}
    fun schoolSaved(previous: StudentInfo) {}
    fun dataLoaded(feature: Feature, school: StudentInfo, outcome: LoadOutcome,
                   source: DataSource, durationMillis: Long, error: Throwable? = null) {}
    fun timerAction(action: TimerAction, durationMillis: Long) {}
    fun widgetAction(action: WidgetAction) {}
}

object NoOpTelemetry : AppTelemetry

/** Non-stable, coarse device metadata only. Never add Android ID, serial, advertising ID or user-agent. */
data class DeviceContext(
    val manufacturer: String,
    val model: String,
    val osSdk: Int,
    val appVersion: String
) {
    fun parameters(): Map<String, Any> = mapOf(
        "device_manufacturer" to manufacturer.telemetryValue(),
        "device_model" to model.telemetryValue(),
        "os_sdk" to osSdk.coerceIn(0, 999).toLong(),
        "app_version" to appVersion.telemetryValue()
    )

    private fun String.telemetryValue(): String =
        replace(Regex("[^A-Za-z0-9._ -]"), "_").take(48).ifBlank { "unknown" }
}

enum class Feature(val value: String) {
    HOME("home"), SETUP("setup"), SETTINGS("settings"), SCHOOL_SEARCH("school_search"),
    MEALS("meals"), TIMETABLE("timetable"), SCHEDULE("schedule"), NOTICES("notices"),
    TIMER("timer"), WIDGET_SETTINGS("widget_settings")
}
enum class EntryPoint(val value: String) { LAUNCHER("launcher"), WIDGET("widget"), NOTIFICATION("notification"), UNKNOWN("unknown") }
enum class LoadOutcome(val value: String) { SUCCESS("success"), EMPTY("empty"), FAILURE("failure") }
enum class DataSource(val value: String) { NETWORK("network"), CACHE("cache"), VALIDATION("validation"), CONFIG("config") }
enum class TimerAction(val value: String) { START("start"), PAUSE("pause"), RESET("reset"), PRESET("preset"), COMPLETE("complete") }
enum class WidgetAction(val value: String) { ENABLE("enable"), DISABLE("disable"), CONFIGURE("configure"), REFRESH("refresh"), OPEN("open") }

interface TelemetrySink {
    fun collection(analytics: Boolean, diagnostics: Boolean)
    fun event(name: String, parameters: Map<String, Any>)
    fun context(parameters: Map<String, Any>)
    fun exception(error: Throwable, parameters: Map<String, Any>)
    fun clearAnalytics()
    fun clearDiagnostics()
}

/** Prevents optional SDK/observer failures from changing product behavior. */
class SafeTelemetry(private val delegate: AppTelemetry) : AppTelemetry {
    private inline fun run(block: () -> Unit) {
        try {
            block()
        } catch (error: Exception) {
            if (error is CancellationException) throw error
        }
    }
    override fun setCollection(analytics: Boolean, diagnostics: Boolean) = run {
        delegate.setCollection(analytics, diagnostics)
    }
    override fun clearAnalytics() = run { delegate.clearAnalytics() }
    override fun clearDiagnostics() = run { delegate.clearDiagnostics() }
    override fun appOpened(entryPoint: EntryPoint) = run { delegate.appOpened(entryPoint) }
    override fun featureViewed(feature: Feature) = run { delegate.featureViewed(feature) }
    override fun schoolSaved(previous: com.lbs.schoolhelper.data.repository.StudentInfo) = run {
        delegate.schoolSaved(previous)
    }
    override fun dataLoaded(
        feature: Feature,
        school: com.lbs.schoolhelper.data.repository.StudentInfo,
        outcome: LoadOutcome,
        source: DataSource,
        durationMillis: Long,
        error: Throwable?
    ) = run { delegate.dataLoaded(feature, school, outcome, source, durationMillis, error) }
    override fun timerAction(action: TimerAction, durationMillis: Long) = run {
        delegate.timerAction(action, durationMillis)
    }
    override fun widgetAction(action: WidgetAction) = run { delegate.widgetAction(action) }
}
