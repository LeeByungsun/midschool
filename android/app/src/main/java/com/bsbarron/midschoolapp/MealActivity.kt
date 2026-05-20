package com.bsbarron.midschoolapp

import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bsbarron.midschoolapp.databinding.ActivityMealBinding
import com.bsbarron.midschoolapp.ui.meal.MealDayUiModel
import com.bsbarron.midschoolapp.ui.meal.MealViewModel
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MealActivity : AppCompatActivity() {
    private val viewModel: MealViewModel by viewModels()
    private lateinit var binding: ActivityMealBinding
    private lateinit var mealContainer: LinearLayout

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

        mealContainer = binding.mealContainer
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

                    mealContainer.removeAllViews()
                    state.items.forEach { item ->
                        mealContainer.addView(createMealCard(item))
                    }
                }
            }
        }
    }

    private fun createMealCard(item: MealDayUiModel): MaterialCardView {
        val context = this
        val card = MaterialCardView(context).apply {
            radius = resources.getDimension(R.dimen.timetable_card_radius)
            cardElevation = 0f
            strokeWidth = resources.getDimensionPixelSize(R.dimen.timetable_card_stroke)
            setCardBackgroundColor(
                getColor(
                    if (item.isToday) {
                        R.color.brand_green_soft
                    } else {
                        R.color.surface_card
                    }
                )
            )
            strokeColor = getColor(R.color.divider_soft)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = resources.getDimensionPixelSize(R.dimen.timetable_row_spacing)
            }
        }

        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                resources.getDimensionPixelSize(R.dimen.timetable_row_padding_horizontal),
                resources.getDimensionPixelSize(R.dimen.timetable_row_padding_vertical),
                resources.getDimensionPixelSize(R.dimen.timetable_row_padding_horizontal),
                resources.getDimensionPixelSize(R.dimen.timetable_row_padding_vertical)
            )
        }

        val titleText = TextView(context).apply {
            text = item.dateLabel
            setTextColor(getColor(R.color.text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTypeface(typeface, Typeface.BOLD)
        }

        val detailText = TextView(context).apply {
            text = item.detailText
            setTextColor(getColor(R.color.text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setLineSpacing(0f, 1.15f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.timetable_row_spacing)
            }
        }

        column.addView(titleText)
        column.addView(detailText)
        card.addView(column)
        return card
    }
}
