package com.bsbarron.midschoolapp.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.bsbarron.midschoolapp.R
import com.bsbarron.midschoolapp.data.repository.PreferencesRepository
import com.bsbarron.midschoolapp.data.repository.WidgetSettings
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WidgetConfigActivity : AppCompatActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        setContentView(R.layout.activity_widget_config)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val backButton = findViewById<ImageButton>(R.id.widgetConfigBackButton)
        val tomorrowSwitch = findViewById<Switch>(R.id.widgetTomorrowSwitch)
        val saveButton = findViewById<Button>(R.id.widgetConfigSaveButton)

        val settings = preferencesRepository.getWidgetSettings(appWidgetId)
        tomorrowSwitch.isChecked = settings.showTomorrowTimetable

        backButton.setOnClickListener { finish() }
        saveButton.setOnClickListener {
            preferencesRepository.saveWidgetSettings(
                appWidgetId = appWidgetId,
                settings = WidgetSettings(
                    showTomorrowTimetable = tomorrowSwitch.isChecked
                )
            )

            MisSchoolWidgetProvider.requestWidgetUpdate(this, appWidgetId)

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}
