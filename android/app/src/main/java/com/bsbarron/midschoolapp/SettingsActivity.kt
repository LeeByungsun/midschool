package com.bsbarron.midschoolapp

import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bsbarron.midschoolapp.data.model.SchoolInfo
import com.bsbarron.midschoolapp.databinding.ActivitySettingsBinding
import com.bsbarron.midschoolapp.ui.settings.SettingsUiState
import com.bsbarron.midschoolapp.ui.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
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

        binding.backButton.setOnClickListener { finish() }
        binding.searchSchoolButton.setOnClickListener {
            viewModel.updateSchoolQuery(binding.settingsSchoolQueryInput.text.toString())
            viewModel.searchSchools()
        }

        binding.saveSettingsButton.setOnClickListener {
            viewModel.updateSchoolQuery(binding.settingsSchoolQueryInput.text.toString())
            viewModel.updateGrade(binding.settingsGradeInput.text.toString().trim())
            viewModel.updateClassroom(binding.settingsClassInput.text.toString().trim())
            viewModel.updateDisplayMode(binding.timerDisplayRingRadio.isChecked)
            viewModel.updateNotificationEnabled(binding.timerNotificationSwitch.isChecked)
            viewModel.updateVibrationEnabled(binding.timerVibrationSwitch.isChecked)
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
        if (binding.settingsSchoolQueryInput.text.toString() != state.schoolQuery) {
            binding.settingsSchoolQueryInput.setText(state.schoolQuery)
        }
        if (binding.settingsGradeInput.text.toString() != state.grade) {
            binding.settingsGradeInput.setText(state.grade)
        }
        if (binding.settingsClassInput.text.toString() != state.classroom) {
            binding.settingsClassInput.setText(state.classroom)
        }
        binding.timerDisplayCountRadio.isChecked = !state.isRingMode
        binding.timerDisplayRingRadio.isChecked = state.isRingMode
        binding.timerNotificationSwitch.isChecked = state.notificationEnabled
        binding.timerVibrationSwitch.isChecked = state.vibrationEnabled
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
