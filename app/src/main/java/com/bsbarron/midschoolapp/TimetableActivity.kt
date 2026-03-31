package com.bsbarron.midschoolapp

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bsbarron.midschoolapp.data.model.TimetableItem
import com.bsbarron.midschoolapp.ui.timetable.TimetableViewModel
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TimetableActivity : AppCompatActivity() {
    private val viewModel: TimetableViewModel by viewModels()
    private lateinit var timetableContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_timetable)

        val backButton = findViewById<android.widget.ImageButton>(R.id.timetableBackButton)
        val previousDayButton = findViewById<TextView>(R.id.previousDayButton)
        val todayButton = findViewById<TextView>(R.id.todayButton)
        val nextDayButton = findViewById<TextView>(R.id.nextDayButton)
        val dateTitleText = findViewById<TextView>(R.id.timetableDateTitleText)
        val classInfoText = findViewById<TextView>(R.id.timetableClassInfoText)
        val statusText = findViewById<TextView>(R.id.timetableStatusText)
        timetableContainer = findViewById(R.id.timetableContainer)

        backButton.setOnClickListener { finish() }
        previousDayButton.setOnClickListener { viewModel.showPreviousDay() }
        todayButton.setOnClickListener { viewModel.showToday() }
        nextDayButton.setOnClickListener { viewModel.showNextDay() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    dateTitleText.text = state.dateTitle
                    classInfoText.text = state.classInfoText
                    statusText.text = state.statusText
                    timetableContainer.removeAllViews()
                    state.items.forEach { item ->
                        timetableContainer.addView(createTimetableRow(item))
                    }
                }
            }
        }
    }

    private fun createTimetableRow(item: TimetableItem): MaterialCardView {
        val context = this
        val card = MaterialCardView(context).apply {
            radius = resources.getDimension(R.dimen.timetable_card_radius)
            cardElevation = 0f
            strokeWidth = resources.getDimensionPixelSize(R.dimen.timetable_card_stroke)
            setCardBackgroundColor(getColor(R.color.surface_card))
            strokeColor = getColor(R.color.divider_soft)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = resources.getDimensionPixelSize(R.dimen.timetable_row_spacing)
            }
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                resources.getDimensionPixelSize(R.dimen.timetable_row_padding_horizontal),
                resources.getDimensionPixelSize(R.dimen.timetable_row_padding_vertical),
                resources.getDimensionPixelSize(R.dimen.timetable_row_padding_horizontal),
                resources.getDimensionPixelSize(R.dimen.timetable_row_padding_vertical)
            )
        }

        val periodBadge = TextView(context).apply {
            text = getString(R.string.timetable_period_format, item.period)
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.white))
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            background = getDrawable(R.drawable.ic_launcher_background)
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.timetable_period_badge_width),
                resources.getDimensionPixelSize(R.dimen.timetable_period_badge_height)
            )
        }

        val subjectText = TextView(context).apply {
            text = item.subject.ifBlank { getString(R.string.timetable_no_subject) }
            setTextColor(getColor(R.color.text_primary))
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.timetable_subject_margin_start)
            }
        }

        row.addView(periodBadge)
        row.addView(subjectText)
        card.addView(row)
        return card
    }
}
