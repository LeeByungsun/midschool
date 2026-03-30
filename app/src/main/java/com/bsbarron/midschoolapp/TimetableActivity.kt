package com.bsbarron.midschoolapp

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bsbarron.midschoolapp.data.AppContainer
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class TimetableActivity : AppCompatActivity() {
    private lateinit var dateTitleText: TextView
    private lateinit var classInfoText: TextView
    private lateinit var statusText: TextView
    private lateinit var timetableContainer: LinearLayout
    private var currentDate: LocalDate = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_timetable)

        val backButton: ImageButton = findViewById(R.id.timetableBackButton)
        val previousDayButton: TextView = findViewById(R.id.previousDayButton)
        val todayButton: TextView = findViewById(R.id.todayButton)
        val nextDayButton: TextView = findViewById(R.id.nextDayButton)
        dateTitleText = findViewById(R.id.timetableDateTitleText)
        classInfoText = findViewById(R.id.timetableClassInfoText)
        statusText = findViewById(R.id.timetableStatusText)
        timetableContainer = findViewById(R.id.timetableContainer)

        backButton.setOnClickListener { finish() }
        previousDayButton.setOnClickListener {
            currentDate = currentDate.minusDays(1)
            loadTimetable()
        }
        todayButton.setOnClickListener {
            currentDate = LocalDate.now()
            loadTimetable()
        }
        nextDayButton.setOnClickListener {
            currentDate = currentDate.plusDays(1)
            loadTimetable()
        }

        loadTimetable()
    }

    private fun loadTimetable() {
        val grade = UserPreferences.getGrade(this)
        val classroom = UserPreferences.getClassroom(this)

        dateTitleText.text = currentDate.format(
            DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)
        )
        classInfoText.text = if (grade.isNotBlank() && classroom.isNotBlank()) {
            getString(R.string.home_student_info_format, grade, classroom)
        } else {
            getString(R.string.timetable_missing_student_info)
        }

        if (grade.isBlank() || classroom.isBlank()) {
            timetableContainer.removeAllViews()
            statusText.text = getString(R.string.timetable_missing_student_info)
            return
        }

        statusText.text = getString(R.string.timetable_loading)
        timetableContainer.removeAllViews()

        lifecycleScope.launch {
            val result = AppContainer.schoolRepository.getTimetable(
                grade = grade,
                classroom = classroom,
                date = currentDate.format(DateTimeFormatter.BASIC_ISO_DATE)
            )

            val items = result.getOrDefault(emptyList())
                .sortedWith(
                    compareBy(
                        { it.period.toIntOrNull() ?: Int.MAX_VALUE },
                        { it.period }
                    )
                )

            when {
                result.isFailure -> {
                    statusText.text = getString(R.string.timetable_error)
                }

                items.isEmpty() -> {
                    statusText.text = getString(R.string.timetable_empty)
                }

                else -> {
                    statusText.text = ""
                    items.forEach { item ->
                        timetableContainer.addView(createTimetableRow(item.period, item.subject))
                    }
                }
            }
        }
    }

    private fun createTimetableRow(period: String, subject: String): MaterialCardView {
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
            text = getString(R.string.timetable_period_format, period)
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
            text = subject.ifBlank { getString(R.string.timetable_no_subject) }
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
