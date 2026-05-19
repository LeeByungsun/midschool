package com.bsbarron.midschoolapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bsbarron.midschoolapp.data.model.SchoolInfo
import com.bsbarron.midschoolapp.databinding.ActivitySetupBinding
import com.bsbarron.midschoolapp.ui.setup.SetupUiState
import com.bsbarron.midschoolapp.ui.setup.SetupViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SetupActivity : AppCompatActivity() {
    private val viewModel: SetupViewModel by viewModels()
    private lateinit var binding: ActivitySetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this

        bindClicks()
        bindState()
    }

    private fun bindClicks() {
        binding.searchSchoolButton.setOnClickListener {
            viewModel.updateSchoolQuery(binding.schoolQueryInput.text.toString())
            viewModel.searchSchools()
        }

        binding.saveStudentInfoButton.setOnClickListener {
            viewModel.updateSchoolQuery(binding.schoolQueryInput.text.toString())
            viewModel.updateGrade(binding.gradeInput.text.toString().trim())
            viewModel.updateClassroom(binding.classInput.text.toString().trim())
            lifecycleScope.launch {
                viewModel.saveStudentInfo()
            }
        }
    }

    private fun bindState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect(::renderState)
                }
                launch {
                    viewModel.messageEvent.collect { messageRes ->
                        Toast.makeText(this@SetupActivity, messageRes, Toast.LENGTH_SHORT).show()
                    }
                }
                launch {
                    viewModel.navigationEvent.collect {
                        startActivity(Intent(this@SetupActivity, MainActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }

    private fun renderState(state: SetupUiState) {
        if (binding.schoolQueryInput.text.toString() != state.schoolQuery) {
            binding.schoolQueryInput.setText(state.schoolQuery)
        }
        if (binding.gradeInput.text.toString() != state.grade) {
            binding.gradeInput.setText(state.grade)
        }
        if (binding.classInput.text.toString() != state.classroom) {
            binding.classInput.setText(state.classroom)
        }

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
