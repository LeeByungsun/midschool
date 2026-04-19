package com.bsbarron.midschoolapp.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bsbarron.midschoolapp.databinding.ActivityWidgetConfigBinding
import com.bsbarron.midschoolapp.ui.widget.WidgetConfigViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WidgetConfigActivity : AppCompatActivity() {
    private val viewModel: WidgetConfigViewModel by viewModels()
    private lateinit var binding: ActivityWidgetConfigBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setResult(RESULT_CANCELED)
        binding = ActivityWidgetConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this

        val rootView = binding.root
        val initialTopPadding = rootView.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                initialTopPadding + systemBars.top,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        viewModel.loadSettings(appWidgetId)

        binding.widgetConfigBackButton.setOnClickListener { finish() }
        binding.widgetTomorrowSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateShowTomorrow(isChecked)
        }
        binding.widgetConfigSaveButton.setOnClickListener {
            lifecycleScope.launch {
                viewModel.saveSettings(appWidgetId)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        if (binding.widgetTomorrowSwitch.isChecked != state.showTomorrowTimetable) {
                            binding.widgetTomorrowSwitch.isChecked = state.showTomorrowTimetable
                        }
                    }
                }
                launch {
                    viewModel.saveEvent.collect {
                        MisSchoolWidgetProvider.requestWidgetUpdate(this@WidgetConfigActivity, appWidgetId)
                        val resultValue = Intent().apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        setResult(RESULT_OK, resultValue)
                        finish()
                    }
                }
            }
        }
    }
}
