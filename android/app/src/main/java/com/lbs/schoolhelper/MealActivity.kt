package com.lbs.schoolhelper

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lbs.schoolhelper.databinding.ActivityMealBinding
import androidx.recyclerview.widget.LinearLayoutManager
import com.lbs.schoolhelper.ui.meal.MealDayAdapter
import com.lbs.schoolhelper.ui.meal.MealViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.lbs.schoolhelper.util.applySystemBarPadding

@AndroidEntryPoint
class MealActivity : AppCompatActivity() {
    private val viewModel: MealViewModel by viewModels()
    private lateinit var binding: ActivityMealBinding
    private val mealAdapter = MealDayAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMealBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this

        binding.root.applySystemBarPadding()
        binding.mealRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.mealRecyclerView.adapter = mealAdapter
        binding.mealRecyclerView.itemAnimator = null
        binding.mealBackButton.setOnClickListener { finish() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.mealWeekTitleText.text = state.weekTitle
                    binding.mealStatusText.text = state.statusText
                    binding.mealStatusText.visibility = if (state.statusText.isBlank()) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }

                    mealAdapter.submitList(state.items)
                }
            }
        }
    }
}
