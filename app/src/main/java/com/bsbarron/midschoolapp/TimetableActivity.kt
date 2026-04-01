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
    // 시간표 행은 런타임에 동적으로 추가되므로 컨테이너를 필드로 보관한다.
    private lateinit var timetableContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_timetable)

        // 날짜 탐색 버튼과 결과 표시 영역을 분리해서 읽기 쉽게 연결한다.
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
                // 날짜가 바뀔 때마다 기존 행을 비우고 현재 상태의 시간표를 다시 그린다.
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
