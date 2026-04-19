package com.bsbarron.midschoolapp.widget

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object WidgetDateFormatter {
    private const val HEADER_PREFIX = "\uD83D\uDCC5"

    fun formatHeaderDate(date: LocalDate, locale: Locale = Locale.KOREAN): String {
        val formatter = DateTimeFormatter.ofPattern("M월 d일 (E)", locale)
        return "$HEADER_PREFIX ${date.format(formatter)}"
    }
}
