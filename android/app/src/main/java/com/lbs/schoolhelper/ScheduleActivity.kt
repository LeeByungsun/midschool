package com.lbs.schoolhelper

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lbs.schoolhelper.databinding.ActivityScheduleBinding
import com.lbs.schoolhelper.ui.schedule.ScheduleViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.lbs.schoolhelper.util.applySystemBarPadding

@AndroidEntryPoint
class ScheduleActivity : AppCompatActivity() {
    private val viewModel: ScheduleViewModel by viewModels()
    private lateinit var binding: ActivityScheduleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this

        binding.root.applySystemBarPadding()

        binding.scheduleBackButton.setOnClickListener { finish() }
        binding.previousMonthButton.setOnClickListener { viewModel.showPreviousMonth() }
        binding.nextMonthButton.setOnClickListener { viewModel.showNextMonth() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.scheduleMonthTitleText.text = state.monthTitle
                    binding.scheduleListText.text = state.scheduleText
                }
            }
        }
    }
}
