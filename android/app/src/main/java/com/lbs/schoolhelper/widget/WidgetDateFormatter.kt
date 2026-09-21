package com.lbs.schoolhelper.widget

import android.content.Context
import com.lbs.schoolhelper.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object WidgetDateFormatter {
    private const val HEADER_PREFIX = "\uD83D\uDCC5"

    fun formatHeaderDate(context: Context, date: LocalDate, locale: Locale = Locale.KOREAN): String {
        val formatter = DateTimeFormatter.ofPattern(
            context.getString(R.string.date_format_day_with_short_weekday),
            locale
        )
        return "$HEADER_PREFIX ${date.format(formatter)}"
    }
}
