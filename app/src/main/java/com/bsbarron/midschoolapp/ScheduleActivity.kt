package com.bsbarron.midschoolapp

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bsbarron.midschoolapp.data.repository.SchoolRepository
import com.bsbarron.midschoolapp.util.isVisibleSchedule
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ScheduleActivity : AppCompatActivity() {
    @Inject lateinit var schoolRepository: SchoolRepository
    private lateinit var monthTitleText: TextView
    private lateinit var scheduleListText: TextView
    private var currentMonth: YearMonth = YearMonth.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_schedule)

        val backButton: ImageButton = findViewById(R.id.scheduleBackButton)
        val previousButton: TextView = findViewById(R.id.previousMonthButton)
        val nextButton: TextView = findViewById(R.id.nextMonthButton)
        monthTitleText = findViewById(R.id.scheduleMonthTitleText)
        scheduleListText = findViewById(R.id.scheduleListText)

        backButton.setOnClickListener { finish() }
        previousButton.setOnClickListener {
            currentMonth = currentMonth.minusMonths(1)
            loadSchedule()
        }
        nextButton.setOnClickListener {
            currentMonth = currentMonth.plusMonths(1)
            loadSchedule()
        }

        loadSchedule()
    }

    private fun loadSchedule() {
        monthTitleText.text = currentMonth.format(
            DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN)
        )
        scheduleListText.text = getString(R.string.schedule_loading)

        lifecycleScope.launch {
            val monthKey = currentMonth.format(DateTimeFormatter.ofPattern("yyyyMM"))
            val result = schoolRepository.getSchedules(monthKey)
            val schedules = result.getOrDefault(emptyList())
                .filter { it.isVisibleSchedule() }
                .sortedBy { it.date }

            scheduleListText.text = if (schedules.isEmpty()) {
                getString(R.string.schedule_empty_month)
            } else {
                schedules.joinToString("\n\n") { event ->
                    val dateLabel = runCatching {
                        LocalDate.parse(event.date, DateTimeFormatter.BASIC_ISO_DATE).format(
                            DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
                        )
                    }.getOrDefault(event.date)

                    buildString {
                        append(dateLabel)
                        append("\n")
                        append(event.title.ifBlank { getString(R.string.schedule_no_title) })
                        if (event.description.isNotBlank()) {
                            append("\n")
                            append(event.description)
                        }
                    }
                }
            }
        }
    }
}
