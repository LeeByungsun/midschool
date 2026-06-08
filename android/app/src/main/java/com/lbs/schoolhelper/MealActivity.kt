package com.lbs.schoolhelper

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lbs.schoolhelper.databinding.ActivityMealBinding
import androidx.recyclerview.widget.LinearLayoutManager
import com.lbs.schoolhelper.ui.meal.MealDayAdapter
import com.lbs.schoolhelper.ui.meal.MealViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
