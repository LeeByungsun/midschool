package com.lbs.schoolhelper.telemetry

import com.google.gson.JsonParseException
import com.lbs.schoolhelper.data.remote.NeisApiException
import com.lbs.schoolhelper.data.repository.StudentInfo
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.CancellationException
import retrofit2.HttpException

/** Thread-safe policy boundary. SDK failure is deliberately isolated from product behavior. */
class TelemetryReporter(
    private val sink: TelemetrySink,
    private val studentInfo: () -> StudentInfo,
    private val deviceContext: DeviceContext,
    private val environment: String
) : AppTelemetry {
    private var analyticsEnabled = false
    private var diagnosticsEnabled = false
    private val reportedErrors = mutableSetOf<String>()

    @Synchronized override fun setCollection(analytics: Boolean, diagnostics: Boolean) {
        val revoked = (analyticsEnabled && !analytics) || (diagnosticsEnabled && !diagnostics)
        analyticsEnabled = analytics
        diagnosticsEnabled = diagnostics
        safely { sink.collection(analytics, diagnostics) }
        if (revoked) {
            if (!analytics) safely { sink.clearAnalytics() }
            if (!diagnostics) safely { sink.clearDiagnostics() }
            reportedErrors.clear()
        }
        refreshContext()
    }

    @Synchronized override fun appOpened(entryPoint: EntryPoint) {
        refreshContext()
        event("app_open_context", deviceContext.parameters() + ("entry_point" to entryPoint.value))
        if (entryPoint == EntryPoint.WIDGET) widgetAction(WidgetAction.OPEN)
    }

    @Synchronized override fun featureViewed(feature: Feature) {
        refreshContext(mapOf("screen" to feature.value))
        event("feature_view", mapOf("feature" to feature.value))
    }

    @Synchronized override fun schoolSaved(previous: StudentInfo) {
        val current = studentInfo()
        refreshContext()
        if (!current.hasSchoolSelection()) return
        when {
            !previous.hasSchoolSelection() -> event("school_selected")
            previous.officeCode != current.officeCode || previous.schoolCode != current.schoolCode -> event("school_changed")
        }
    }

    @Synchronized override fun dataLoaded(
        feature: Feature, school: StudentInfo, outcome: LoadOutcome,
        source: DataSource, durationMillis: Long, error: Throwable?
    ) {
        if (error is CancellationException) return
        val code = errorCode(error)
        // The captured request school overrides the current UI school for in-flight requests.
        val parameters = schoolContext(school) + mapOf(
            "feature" to feature.value, "outcome" to outcome.value, "source" to source.value,
            "duration_ms" to durationMillis.coerceAtLeast(0L), "error_code" to code,
            "api" to if (feature == Feature.NOTICES) "notices" else "neis"
        )
        event("data_load_result", parameters)
        if (!diagnosticsEnabled || error == null || source == DataSource.VALIDATION ||
            error is IOException || (error is HttpException && error.code() in listOf(404, 408, 429))) return
        val signature = "${feature.value}:$code"
        if (reportedErrors.size >= 8 || !reportedErrors.add(signature)) return
        val sanitized = TelemetryException(signature).apply { stackTrace = error.stackTrace }
        safely { sink.exception(sanitized, parameters + ("environment" to environment)) }
    }

    @Synchronized override fun timerAction(action: TimerAction, durationMillis: Long) {
        event("timer_action", mapOf("action" to action.value, "duration_ms" to durationMillis.coerceAtLeast(0L)))
    }

    @Synchronized override fun widgetAction(action: WidgetAction) {
        event("widget_action", mapOf("action" to action.value, "widget_type" to "school_dashboard"))
    }

    private fun refreshContext(extra: Map<String, Any> = emptyMap()) {
        if (diagnosticsEnabled) safely {
            sink.context(schoolContext(studentInfo()) + deviceContext.parameters() + ("environment" to environment) + extra)
        }
    }

    private fun event(name: String, extra: Map<String, Any> = emptyMap()) {
        if (analyticsEnabled) safely { sink.event(name, schoolContext(studentInfo()) + ("environment" to environment) + extra) }
    }

    private fun schoolContext(info: StudentInfo): Map<String, Any> = mapOf(
        "office_code" to info.officeCode.takeIf { it.matches(Regex("[A-Z][0-9]{2}")) }.orEmpty(),
        "school_code" to info.schoolCode.takeIf { it.matches(Regex("[0-9]{1,10}")) }.orEmpty(),
        "school_kind" to when (info.schoolKind) { "초등학교" -> "elementary"; "중학교" -> "middle"; "고등학교" -> "high"; else -> "unknown" },
        "setup_complete" to if (info.isComplete()) 1L else 0L
    )

    private fun errorCode(error: Throwable?): String = when (error) {
        null -> "none"
        is SocketTimeoutException -> "timeout"
        is HttpException -> "http_${error.code()}"
        is NeisApiException -> if (error.code.matches(Regex("(INFO|ERROR)-[0-9]{3}"))) "neis_${error.code}" else "neis_unknown"
        is IOException -> "network"
        is JsonParseException -> "parse"
        else -> "unexpected"
    }

    private inline fun safely(action: () -> Unit) {
        try { action() } catch (error: Exception) {
            if (error is CancellationException) throw error
            // Never recurse into diagnostics while the telemetry SDK itself is failing.
        }
    }
}

private class TelemetryException(message: String) : RuntimeException(message)
