package com.lbs.schoolhelper.telemetry

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lbs.schoolhelper.data.repository.PreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Explicit QA-only entry point used to verify Analytics and Crashlytics delivery on a test device. */
@AndroidEntryPoint
class QaTelemetryProbeActivity : AppCompatActivity() {
    @Inject lateinit var telemetry: AppTelemetry
    @Inject lateinit var preferences: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Refresh Crashlytics context before recording; lifecycle tracking logs the foreground entry.
        telemetry.featureViewed(Feature.SETTINGS)
        telemetry.dataLoaded(
            feature = Feature.SETTINGS,
            school = preferences.getStudentInfo(),
            outcome = LoadOutcome.FAILURE,
            source = DataSource.CONFIG,
            durationMillis = 0L,
            error = IllegalStateException(QA_PROBE_ERROR)
        )

        val enabledChannels = buildList {
            if (preferences.isAnalyticsEnabled()) add("Analytics")
            if (preferences.isDiagnosticsEnabled()) add("Crashlytics")
        }
        Toast.makeText(
            this,
            if (enabledChannels.isEmpty()) "먼저 설정에서 분석 또는 오류 진단에 동의해 주세요."
            else "QA 전송 요청: ${enabledChannels.joinToString()}",
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    private companion object {
        const val QA_PROBE_ERROR = "qa_nonfatal_probe"
    }
}
