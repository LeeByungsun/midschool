package com.lbs.schoolhelper.data.remote.dto

data class NoticeListResponseDto(
    val items: List<NoticeSummaryDto> = emptyList(),
    val message: String? = null
)

data class NoticeSummaryDto(
    val id: String = "",
    val title: String = "",
    val date: String = "",
    val author: String = "",
    val url: String = "",
    val sourceUrl: String = ""
)
