package com.lbs.schoolhelper.widget

import com.lbs.schoolhelper.data.model.TimetableItem

internal sealed class WidgetTimetableContent {
    data class Lessons(val text: String) : WidgetTimetableContent()
    object NoClasses : WidgetTimetableContent()
    object LoadError : WidgetTimetableContent()
}

/** Converts repository output into safe, display-ready widget content. */
internal object WidgetTimetableContentResolver {
    fun resolve(result: Result<List<TimetableItem>>): WidgetTimetableContent {
        if (result.isFailure) return WidgetTimetableContent.LoadError

        val lessons = result.getOrNull().orEmpty()
            .sortedBy { it.period.toIntOrNull() ?: Int.MAX_VALUE }
            .mapNotNull { item ->
                item.subject.trim().takeIf { it.isNotEmpty() }?.let { subject ->
                    val period = item.period.trim().takeIf { it.isNotEmpty() } ?: "?"
                    "${period}교시 ${subject.take(6)}"
                }
            }

        return if (lessons.isEmpty()) {
            WidgetTimetableContent.NoClasses
        } else {
            WidgetTimetableContent.Lessons(lessons.joinToString("\n"))
        }
    }
}
