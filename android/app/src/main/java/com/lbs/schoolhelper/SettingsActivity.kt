package com.lbs.schoolhelper

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lbs.schoolhelper.data.model.SchoolInfo
import com.lbs.schoolhelper.databinding.ActivitySettingsBinding
import com.lbs.schoolhelper.ui.settings.SettingsUiState
import com.lbs.schoolhelper.ui.settings.SettingsViewModel
import com.lbs.schoolhelper.ui.settings.UnsavedProfileDecision
import com.lbs.schoolhelper.util.applySystemBarPadding
import com.lbs.schoolhelper.util.enableSchoolEdgeToEdge
import com.lbs.schoolhelper.util.ExternalUrlOpener
import com.lbs.schoolhelper.util.requestVisibleAboveKeyboard
import com.google.android.gms.oss.licenses.v2.OssLicensesMenuActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var binding: ActivitySettingsBinding
    private var isRenderingState = false
    private var selectorProfileIds: List<String?> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableSchoolEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this

        binding.root.applySystemBarPadding()
        binding.settingsVersionText.text = AppVersionLabel.format(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE
        )
        bindActions()
        bindState()

        if (intent.getBooleanExtra(EXTRA_START_ADD_PROFILE, false)) {
            viewModel.startAddingProfile()
        }
    }

    private fun bindActions() {
        binding.backButton.setOnClickListener { requestClose() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = requestClose()
        })

        binding.settingsProfileSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isRenderingState) return
                selectorProfileIds.getOrNull(position)?.let(viewModel::selectEditingProfile)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        binding.addProfileButton.setOnClickListener {
            captureProfileForm()
            viewModel.startAddingProfile()
        }
        binding.deleteProfileButton.setOnClickListener { showDeleteConfirmation() }

        binding.settingsProfileNameInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                viewModel.updateDisplayName(binding.settingsProfileNameInput.text.toString())
                binding.settingsSchoolQueryInput.requestFocus()
                true
            } else {
                false
            }
        }
        binding.settingsProfileNameInput.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.requestVisibleAboveKeyboard()
            } else if (!isRenderingState) {
                viewModel.updateDisplayName(binding.settingsProfileNameInput.text.toString())
            }
        }
        binding.settingsSchoolQueryInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.updateSchoolQuery(binding.settingsSchoolQueryInput.text.toString())
                viewModel.searchSchools()
                true
            } else {
                false
            }
        }
        binding.settingsGradeInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.settingsClassInput.requestFocus()
                true
            } else {
                false
            }
        }
        binding.settingsClassInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.settingsClassInput.clearFocus()
                true
            } else {
                false
            }
        }
        binding.settingsSchoolQueryInput.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.requestVisibleAboveKeyboard()
            } else if (!isRenderingState) {
                viewModel.updateSchoolQuery(binding.settingsSchoolQueryInput.text.toString())
            }
        }
        binding.settingsGradeInput.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.requestVisibleAboveKeyboard()
            } else if (!isRenderingState) {
                viewModel.updateGrade(binding.settingsGradeInput.text.toString().trim())
            }
        }
        binding.settingsClassInput.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.requestVisibleAboveKeyboard()
            } else if (!isRenderingState) {
                viewModel.updateClassroom(binding.settingsClassInput.text.toString().trim())
            }
        }

        binding.searchSchoolButton.setOnClickListener {
            viewModel.updateSchoolQuery(binding.settingsSchoolQueryInput.text.toString())
            viewModel.searchSchools()
        }
        binding.analyticsSwitch.setOnCheckedChangeListener { _, checked ->
            if (!isRenderingState) viewModel.updateAnalyticsEnabled(checked)
        }
        binding.diagnosticsSwitch.setOnCheckedChangeListener { _, checked ->
            if (!isRenderingState) viewModel.updateDiagnosticsEnabled(checked)
        }
        binding.settingsTelemetryPrivacyPolicyButton.setOnClickListener { openPrivacyPolicy() }
        binding.settingsPrivacyPolicyButton.setOnClickListener { openPrivacyPolicy() }
        binding.openSourceLicensesButton.setOnClickListener {
            OssLicensesMenuActivity.setActivityTitle(getString(R.string.open_source_licenses))
            startActivity(Intent(this, OssLicensesMenuActivity::class.java))
        }
        binding.saveSettingsButton.setOnClickListener {
            captureProfileForm()
            viewModel.updateDisplayMode(binding.timerDisplayRingRadio.isChecked)
            lifecycleScope.launch { viewModel.saveSettings() }
        }
    }

    private fun openPrivacyPolicy() {
        if (!ExternalUrlOpener.open(this, getString(R.string.privacy_policy_url))) {
            Toast.makeText(this, R.string.legal_link_open_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::renderState) }
                launch {
                    viewModel.messageEvent.collect { messageRes ->
                        Toast.makeText(this@SettingsActivity, messageRes, Toast.LENGTH_SHORT).show()
                    }
                }
                launch { viewModel.closeEvent.collect { finish() } }
                launch { viewModel.unsavedProfileChangesEvent.collect { showUnsavedChangesDialog() } }
            }
        }
    }

    private fun captureProfileForm() {
        viewModel.updateDisplayName(binding.settingsProfileNameInput.text.toString())
        viewModel.updateSchoolQuery(binding.settingsSchoolQueryInput.text.toString())
        viewModel.updateGrade(binding.settingsGradeInput.text.toString().trim())
        viewModel.updateClassroom(binding.settingsClassInput.text.toString().trim())
    }

    private fun requestClose() {
        captureProfileForm()
        viewModel.requestClose()
    }

    private fun showUnsavedChangesDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_unsaved_title)
            .setMessage(R.string.settings_unsaved_message)
            .setPositiveButton(R.string.settings_unsaved_save) { _, _ ->
                lifecycleScope.launch {
                    viewModel.resolveUnsavedProfileChanges(UnsavedProfileDecision.SAVE)
                }
            }
            .setNegativeButton(R.string.settings_unsaved_discard) { _, _ ->
                lifecycleScope.launch {
                    viewModel.resolveUnsavedProfileChanges(UnsavedProfileDecision.DISCARD)
                }
            }
            .setNeutralButton(R.string.settings_unsaved_cancel) { _, _ ->
                lifecycleScope.launch {
                    viewModel.resolveUnsavedProfileChanges(UnsavedProfileDecision.CANCEL)
                }
            }
            .setOnCancelListener {
                lifecycleScope.launch {
                    viewModel.resolveUnsavedProfileChanges(UnsavedProfileDecision.CANCEL)
                }
            }
            .show()
    }

    private fun showDeleteConfirmation() {
        val profileName = viewModel.uiState.value.displayName
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_profile_delete_title)
            .setMessage(getString(R.string.settings_profile_delete_message, profileName))
            .setPositiveButton(R.string.settings_profile_delete_confirm) { _, _ ->
                lifecycleScope.launch { viewModel.deleteEditingProfile() }
            }
            .setNegativeButton(R.string.settings_unsaved_cancel, null)
            .show()
    }

    private fun renderState(state: SettingsUiState) {
        isRenderingState = true
        renderProfileSelector(state)
        if (binding.settingsProfileNameInput.text.toString() != state.displayName) {
            binding.settingsProfileNameInput.setText(state.displayName)
        }
        if (binding.settingsSchoolQueryInput.text.toString() != state.schoolQuery) {
            binding.settingsSchoolQueryInput.setText(state.schoolQuery)
        }
        if (binding.settingsGradeInput.text.toString() != state.grade) {
            binding.settingsGradeInput.setText(state.grade)
        }
        if (binding.settingsClassInput.text.toString() != state.classroom) {
            binding.settingsClassInput.setText(state.classroom)
        }
        binding.deleteProfileButton.isVisible = state.canDeleteProfile
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

    private fun renderProfileSelector(state: SettingsUiState) {
        val adding = state.editingProfileId == null
        selectorProfileIds = buildList {
            if (adding) add(null)
            addAll(state.profiles.map { it.id })
        }
        val labels = buildList {
            if (adding) add(getString(R.string.settings_profile_new))
            addAll(state.profiles.map { it.displayName })
        }
        binding.settingsProfileSelector.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            labels
        )
        val selectedPosition = selectorProfileIds.indexOf(state.editingProfileId).coerceAtLeast(0)
        binding.settingsProfileSelector.setSelection(selectedPosition, false)
        binding.settingsProfileSelector.isEnabled = state.profiles.isNotEmpty()
    }

    private fun renderSchoolResults(schools: List<SchoolInfo>, selectedSchool: SchoolInfo?) {
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
        return listOfNotNull(school.schoolName, meta.ifBlank { null }, address).joinToString("\n")
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

    companion object {
        const val EXTRA_START_ADD_PROFILE = "start_add_profile"
    }
}
