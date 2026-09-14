package com.lbs.schoolhelper.telemetry

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.CustomKeysAndValues
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.lbs.schoolhelper.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** The only Firebase SDK boundary. Debug traffic requires a dedicated QA config + build flag. */
@Singleton
class FirebaseTelemetrySink @Inject constructor(
    @param:ApplicationContext private val context: Context
) : TelemetrySink {
    private var analyticsEnabled = false
    private var diagnosticsEnabled = false
    private val available: Boolean get() = BuildConfig.TELEMETRY_ALLOWED && FirebaseApp.getApps(context).isNotEmpty()
    private val analytics get() = FirebaseAnalytics.getInstance(context)
    private val crashlytics get() = FirebaseCrashlytics.getInstance()

    override fun collection(analytics: Boolean, diagnostics: Boolean) {
        analyticsEnabled = available && analytics
        diagnosticsEnabled = available && diagnostics
        if (FirebaseApp.getApps(context).isEmpty()) return
        this.analytics.setAnalyticsCollectionEnabled(analyticsEnabled)
        this.analytics.setConsent(mapOf(
            FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to if (analyticsEnabled) FirebaseAnalytics.ConsentStatus.GRANTED else FirebaseAnalytics.ConsentStatus.DENIED,
            FirebaseAnalytics.ConsentType.AD_STORAGE to FirebaseAnalytics.ConsentStatus.DENIED,
            FirebaseAnalytics.ConsentType.AD_USER_DATA to FirebaseAnalytics.ConsentStatus.DENIED,
            FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to FirebaseAnalytics.ConsentStatus.DENIED
        ))
        crashlytics.setCrashlyticsCollectionEnabled(diagnosticsEnabled)
        if (!diagnosticsEnabled) crashlytics.deleteUnsentReports()
    }

    override fun event(name: String, parameters: Map<String, Any>) {
        if (analyticsEnabled) analytics.logEvent(name, bundle(parameters))
    }

    override fun context(parameters: Map<String, Any>) {
        if (diagnosticsEnabled) crashlytics.setCustomKeys(keys(parameters))
    }

    override fun exception(error: Throwable, parameters: Map<String, Any>) {
        if (diagnosticsEnabled) crashlytics.recordException(error, keys(parameters))
    }

    override fun clearAnalytics() {
        if (FirebaseApp.getApps(context).isEmpty()) return
        analytics.resetAnalyticsData()
        analytics.setDefaultEventParameters(null)
    }

    override fun clearDiagnostics() {
        if (FirebaseApp.getApps(context).isEmpty()) return
        crashlytics.setUserId("")
        crashlytics.setCustomKeys(keys(mapOf("school_code" to "", "office_code" to "", "school_kind" to "unknown", "setup_complete" to 0L, "screen" to "")))
        crashlytics.deleteUnsentReports()
    }

    private fun bundle(parameters: Map<String, Any>) = Bundle().apply {
        parameters.forEach { (key, value) -> when (value) {
            is String -> putString(key, value)
            is Long -> putLong(key, value)
        } }
    }

    private fun keys(parameters: Map<String, Any>): CustomKeysAndValues = CustomKeysAndValues.Builder().apply {
        parameters.forEach { (key, value) -> when (value) {
            is String -> putString(key, value)
            is Long -> putLong(key, value)
        } }
    }.build()
}
