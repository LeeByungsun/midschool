package com.lbs.schoolhelper

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lbs.schoolhelper.data.model.SchoolInfo
import com.lbs.schoolhelper.databinding.ActivitySettingsBinding
import com.lbs.schoolhelper.ui.settings.SettingsUiState
import com.lbs.schoolhelper.ui.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.lbs.schoolhelper.util.applySystemBarPadding
import com.lbs.schoolhelper.util.enableSchoolEdgeToEdge

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var binding: ActivitySettingsBinding
    private var isRenderingState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableSchoolEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this

        binding.root.applySystemBarPadding()
        binding.settingsVersionText.text = AppVersionLabel.format(BuildConfig.VERSION_NAME)

        binding.backButton.setOnClickListener { finish() }
        binding.searchSchoolButton.setOnClickListener {
            viewModel.updateSchoolQuery(binding.settingsSchoolQueryInput.text.toString())
            viewModel.searchSchools()
        }
        binding.pomodoroFocusMinutesInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) viewModel.updatePomodoroFocusMinutes(binding.pomodoroFocusMinutesInput.text.toString())
        }
        binding.pomodoroShortBreakMinutesInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) viewModel.updatePomodoroShortBreakMinutes(binding.pomodoroShortBreakMinutesInput.text.toString())
        }
        binding.pomodoroLongBreakMinutesInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) viewModel.updatePomodoroLongBreakMinutes(binding.pomodoroLongBreakMinutesInput.text.toString())
        }
        binding.pomodoroRoundsGroup.setOnCheckedChangeListener { _, checkedId ->
            viewModel.updatePomodoroRounds(
                when (checkedId) {
                    R.id.pomodoroRound1 -> 1
                    R.id.pomodoroRound2 -> 2
                    R.id.pomodoroRound3 -> 3
                    else -> 4
                }
            )
        }

        binding.analyticsSwitch.setOnCheckedChangeListener { _, checked ->
            if (!isRenderingState) viewModel.updateAnalyticsEnabled(checked)
        }
        binding.diagnosticsSwitch.setOnCheckedChangeListener { _, checked ->
            if (!isRenderingState) viewModel.updateDiagnosticsEnabled(checked)
        }
        binding.saveSettingsButton.setOnClickListener {
            viewModel.updateSchoolQuery(binding.settingsSchoolQueryInput.text.toString())
            viewModel.updateGrade(binding.settingsGradeInput.text.toString().trim())
            viewModel.updateClassroom(binding.settingsClassInput.text.toString().trim())
            viewModel.updateDisplayMode(binding.timerDisplayRingRadio.isChecked)
            lifecycleScope.launch {
                viewModel.saveSettings()
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect(::renderState)
                }
                launch {
                    viewModel.messageEvent.collect { messageRes ->
                        Toast.makeText(this@SettingsActivity, messageRes, Toast.LENGTH_SHORT).show()
                    }
                }
                launch {
                    viewModel.closeEvent.collect {
                        finish()
                    }
                }
            }
        }
    }

    private fun renderState(state: SettingsUiState) {
        isRenderingState = true
        if (binding.settingsSchoolQueryInput.text.toString() != state.schoolQuery) {
            binding.settingsSchoolQueryInput.setText(state.schoolQuery)
        }
        if (binding.settingsGradeInput.text.toString() != state.grade) {
            binding.settingsGradeInput.setText(state.grade)
        }
        if (binding.settingsClassInput.text.toString() != state.classroom) {
            binding.settingsClassInput.setText(state.classroom)
        }
        if (binding.pomodoroFocusMinutesInput.text.toString() != state.pomodoroFocusMinutes.toString()) {
            binding.pomodoroFocusMinutesInput.setText(state.pomodoroFocusMinutes.toString())
        }
        if (binding.pomodoroShortBreakMinutesInput.text.toString() != state.pomodoroShortBreakMinutes.toString()) {
            binding.pomodoroShortBreakMinutesInput.setText(state.pomodoroShortBreakMinutes.toString())
        }
        if (binding.pomodoroLongBreakMinutesInput.text.toString() != state.pomodoroLongBreakMinutes.toString()) {
            binding.pomodoroLongBreakMinutesInput.setText(state.pomodoroLongBreakMinutes.toString())
        }
        binding.pomodoroRoundsGroup.check(
            when (state.pomodoroRounds) {
                1 -> R.id.pomodoroRound1
                2 -> R.id.pomodoroRound2
                3 -> R.id.pomodoroRound3
                else -> R.id.pomodoroRound4
            }
        )
        binding.timerDisplayCountRadio.isChecked = !state.isRingMode
        binding.timerDisplayRingRadio.isChecked = state.isRingMode
        binding.analyticsSwitch.isChecked = state.analyticsEnabled
        binding.diagnosticsSwitch.isChecked = state.diagnosticsEnabled
        binding.searchSchoolButton.isEnabled = !state.isSearching
        binding.searchSchoolButton.text = getString(
            if (state.isSearching) R.string.setup_school_search_loading else R.string.setup_school_search_button
        )
        binding.schoolSearchMessageText.isVisible = state.searchMessage.isNotBlank()
        binding.schoolSearchMessageText.text = state.searchMessage
        binding.selectedSchoolSummaryText.text = state.selectedSchool?.let(::formatSelectedSchool)
            ?: getString(R.string.setup_school_selected_empty)
        binding.schoolResultsLabel.isVisible = state.schoolResults.isNotEmpty()
        binding.schoolResultsGroup.isVisible = state.schoolResults.isNotEmpty()
        renderSchoolResults(state.schoolResults, state.selectedSchool)
        isRenderingState = false
    }

    private fun renderSchoolResults(
        schools: List<SchoolInfo>,
        selectedSchool: SchoolInfo?
    ) {
        binding.schoolResultsGroup.removeAllViews()
        schools.forEach { school ->
            val radioButton = RadioButton(this).apply {
                id = View.generateViewId()
                text = formatSchoolOption(school)
                isChecked = selectedSchool?.officeCode == school.officeCode &&
                    selectedSchool.schoolCode == school.schoolCode
                setOnClickListener { viewModel.selectSchool(school) }
            }
            binding.schoolResultsGroup.addView(radioButton)
        }
    }

    private fun formatSchoolOption(school: SchoolInfo): String {
        val meta = listOfNotNull(
            school.schoolKind.takeIf { it.isNotBlank() },
            school.officeName.takeIf { it.isNotBlank() }
        ).joinToString(" • ")
        val address = school.roadAddress.takeIf { it.isNotBlank() }
        return listOfNotNull(school.schoolName, meta.ifBlank { null }, address)
            .joinToString("\n")
    }

    private fun formatSelectedSchool(school: SchoolInfo): String {
        return listOfNotNull(
            school.schoolName,
            listOfNotNull(
                school.schoolKind.takeIf { it.isNotBlank() },
                school.officeName.takeIf { it.isNotBlank() }
            ).joinToString(" • ").ifBlank { null }
        ).joinToString("\n")
    }

}
