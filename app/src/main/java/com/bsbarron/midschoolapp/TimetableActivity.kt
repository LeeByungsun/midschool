package com.bsbarron.midschoolapp

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bsbarron.midschoolapp.data.model.TimetableItem
import com.bsbarron.midschoolapp.databinding.ActivityTimetableBinding
import com.bsbarron.midschoolapp.ui.timetable.TimetableViewModel
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.ShapeAppearanceModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TimetableActivity : AppCompatActivity() {
    private val viewModel: TimetableViewModel by viewModels()
    private lateinit var binding: ActivityTimetableBinding
    // 시간표 행은 런타임에 동적으로 추가되므로 컨테이너를 필드로 보관한다.
    private lateinit var timetableContainer: LinearLayout

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

        timetableContainer = binding.timetableContainer

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
        // XML RecyclerView 없이도 교시별 카드를 즉석에서 만들 수 있도록
        // 카드, 배지, 과목명을 코드로 조합한다.
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
            setTextColor(getColor(R.color.brand_navy))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.timetable_period_badge_width),
                resources.getDimensionPixelSize(R.dimen.timetable_period_badge_height)
            )
        }

        val badgeCard = MaterialCardView(context).apply {
            radius = resources.getDimension(R.dimen.timetable_period_badge_radius)
            cardElevation = 0f
            strokeWidth = resources.getDimensionPixelSize(R.dimen.timetable_card_stroke)
            setCardBackgroundColor(getColor(R.color.white))
            strokeColor = getColor(R.color.divider_soft)
            shapeAppearanceModel = ShapeAppearanceModel.builder()
                .setAllCornerSizes(resources.getDimension(R.dimen.timetable_period_badge_radius))
                .build()
            layoutParams = periodBadge.layoutParams
            addView(periodBadge)
        }

        val subjectText = TextView(context).apply {
            text = item.subject.ifBlank { getString(R.string.timetable_no_subject) }
            setTextColor(getColor(R.color.text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.timetable_subject_margin_start)
            }
        }

        row.addView(badgeCard)
        row.addView(subjectText)
        card.addView(row)
        return card
    }
}
