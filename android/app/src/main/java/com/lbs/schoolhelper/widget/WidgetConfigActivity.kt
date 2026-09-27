package com.lbs.schoolhelper.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lbs.schoolhelper.databinding.ActivityWidgetConfigBinding
import com.lbs.schoolhelper.ui.widget.WidgetConfigViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.lbs.schoolhelper.util.applySystemBarPadding
import com.lbs.schoolhelper.util.enableSchoolEdgeToEdge

@AndroidEntryPoint
class WidgetConfigActivity : AppCompatActivity() {
    private val viewModel: WidgetConfigViewModel by viewModels()
    private lateinit var binding: ActivityWidgetConfigBinding
    private var isRenderingState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableSchoolEdgeToEdge()
        setResult(RESULT_CANCELED)
        binding = ActivityWidgetConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this

        binding.root.applySystemBarPadding()

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
            if (!isRenderingState) viewModel.updateShowTomorrow(isChecked)
        }
        binding.widgetProfileSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isRenderingState) viewModel.uiState.value.profiles.getOrNull(position)?.let {
                    viewModel.updateSelectedProfile(it.id)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
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
                        isRenderingState = true
                        binding.widgetProfileSelector.adapter = ArrayAdapter(
                            this@WidgetConfigActivity,
                            android.R.layout.simple_spinner_dropdown_item,
                            state.profiles.map { it.displayName }
                        )
                        val selectedIndex = state.profiles.indexOfFirst { it.id == state.selectedProfileId }
                        if (selectedIndex >= 0) binding.widgetProfileSelector.setSelection(selectedIndex, false)
                        if (binding.widgetTomorrowSwitch.isChecked != state.showTomorrowTimetable) {
                            binding.widgetTomorrowSwitch.isChecked = state.showTomorrowTimetable
                        }
                        isRenderingState = false
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
