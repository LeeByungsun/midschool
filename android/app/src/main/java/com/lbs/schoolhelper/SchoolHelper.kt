package com.lbs.schoolhelper

import android.app.Application
import com.lbs.schoolhelper.data.repository.PreferencesRepository
import com.lbs.schoolhelper.telemetry.AppTelemetry
import com.lbs.schoolhelper.telemetry.TelemetryLifecycleCallbacks
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SchoolHelperApplication : Application() {
    @Inject lateinit var telemetry: AppTelemetry
    @Inject lateinit var preferences: PreferencesRepository
    override fun onCreate() {
        super.onCreate()
        telemetry.setCollection(preferences.isAnalyticsEnabled(), preferences.isDiagnosticsEnabled())
        registerActivityLifecycleCallbacks(TelemetryLifecycleCallbacks(telemetry))
    }
}
