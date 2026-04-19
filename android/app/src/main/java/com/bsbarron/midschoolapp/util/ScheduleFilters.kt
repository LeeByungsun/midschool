package com.bsbarron.midschoolapp.util

import com.bsbarron.midschoolapp.data.model.SchoolEvent

fun SchoolEvent.isVisibleSchedule(): Boolean {
    val blockedKeywords = listOf("토요휴업일")
    return blockedKeywords.none { keyword ->
        title.contains(keyword) || description.contains(keyword)
    }
}
