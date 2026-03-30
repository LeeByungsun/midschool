package com.bsbarron.midschoolapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bsbarron.midschoolapp.data.AppContainer
import com.bsbarron.midschoolapp.ui.home.HomeViewModel
import com.bsbarron.midschoolapp.ui.home.HomeViewModelFactory
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        homeViewModel = ViewModelProvider(
            this,
            HomeViewModelFactory(AppContainer.schoolRepository)
        )[HomeViewModel::class.java]

        val classSummaryText: TextView = findViewById(R.id.classSummaryText)
        val todaySummaryText: TextView = findViewById(R.id.todaySummaryText)
        val mealMenuText: TextView = findViewById(R.id.mealMenuText)
        val scheduleTitleText: TextView = findViewById(R.id.scheduleTitleText)
        val settingsButton: MaterialButton = findViewById(R.id.settingsButton)
        val timetableButton: MaterialButton = findViewById(R.id.openTimetableButton)

        classSummaryText.text = getString(
            R.string.home_student_info_format,
            UserPreferences.getGrade(this),
            UserPreferences.getClassroom(this)
        )

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        timetableButton.setOnClickListener {
            startActivity(Intent(this, TimetableActivity::class.java))
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.uiState.collect { state ->
                    if (state.mealSummary.isNotBlank()) {
                        mealMenuText.text = state.mealSummary.replace("<br/>", "\n")
                    }

                    if (state.eventSummary.isNotBlank()) {
                        scheduleTitleText.text = state.eventSummary
                    }

                    if (state.errorMessage != null) {
                        todaySummaryText.text = getString(R.string.home_today_summary_error)
                    }
                }
            }
        }

        homeViewModel.loadHomeData()
    }

    override fun onResume() {
        super.onResume()

        val classSummaryText: TextView = findViewById(R.id.classSummaryText)
        classSummaryText.text = getString(
            R.string.home_student_info_format,
            UserPreferences.getGrade(this),
            UserPreferences.getClassroom(this)
        )
    }
}
