package com.bsbarron.midschoolapp

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bsbarron.midschoolapp.databinding.ActivityTimetableBinding
import com.bsbarron.midschoolapp.ui.timetable.TimetableAdapter
import com.bsbarron.midschoolapp.ui.timetable.TimetableViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TimetableActivity : AppCompatActivity() {
    private val viewModel: TimetableViewModel by viewModels()
    private lateinit var binding: ActivityTimetableBinding
    private val timetableAdapter = TimetableAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTimetableBinding.inflate(layoutInflater)
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
        binding.timetableRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.timetableRecyclerView.adapter = timetableAdapter
        binding.timetableRecyclerView.itemAnimator = null

        binding.timetableBackButton.setOnClickListener { finish() }
        binding.previousDayButton.setOnClickListener { viewModel.showPreviousDay() }
        binding.todayButton.setOnClickListener { viewModel.showToday() }
        binding.nextDayButton.setOnClickListener { viewModel.showNextDay() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 날짜가 바뀔 때마다 기존 행을 비우고 현재 상태의 시간표를 다시 그린다.
                viewModel.uiState.collect { state ->
                    binding.timetableDateTitleText.text = state.dateTitle
                    binding.timetableClassInfoText.text = state.classInfoText
                    binding.timetableLessonCountText.text = state.lessonCountText
                    binding.timetableStatusText.text = state.statusText
                    binding.timetableStatusText.visibility = if (state.statusText.isBlank()) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }
                    binding.todayButton.visibility = if (state.showTodayButton) View.VISIBLE else View.GONE
                    timetableAdapter.submitList(state.items)
                }
            }
        }
    }
}
