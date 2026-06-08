package com.lbs.schoolhelper.util

import com.lbs.schoolhelper.data.model.SchoolEvent

fun SchoolEvent.isVisibleSchedule(): Boolean {
    val blockedKeywords = listOf("토요휴업일")
    return blockedKeywords.none { keyword ->
        title.contains(keyword) || description.contains(keyword)
    }
}
