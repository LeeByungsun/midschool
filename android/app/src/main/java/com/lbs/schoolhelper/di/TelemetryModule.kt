package com.lbs.schoolhelper.di

import android.os.Build
import com.lbs.schoolhelper.BuildConfig
import com.lbs.schoolhelper.data.repository.PreferencesRepository
import com.lbs.schoolhelper.telemetry.AppTelemetry
import com.lbs.schoolhelper.telemetry.DeviceContext
import com.lbs.schoolhelper.telemetry.FirebaseTelemetrySink
import com.lbs.schoolhelper.telemetry.TelemetryReporter
import com.lbs.schoolhelper.telemetry.SafeTelemetry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TelemetryModule {
    @Provides @Singleton
    fun provideTelemetry(sink: FirebaseTelemetrySink, preferences: PreferencesRepository): AppTelemetry =
        SafeTelemetry(
            TelemetryReporter(
                sink = sink,
                studentInfo = preferences::getStudentInfo,
                deviceContext = DeviceContext(
                    manufacturer = Build.MANUFACTURER.orEmpty(),
                    model = Build.MODEL.orEmpty(),
                    osSdk = Build.VERSION.SDK_INT,
                    appVersion = BuildConfig.VERSION_NAME
                ),
                environment = BuildConfig.TELEMETRY_ENVIRONMENT
            )
        )
}
