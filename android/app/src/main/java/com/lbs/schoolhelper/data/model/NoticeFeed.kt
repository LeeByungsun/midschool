package com.lbs.schoolhelper.data.model

data class NoticeFeed(
    val items: List<NoticePreview> = emptyList(),
    val message: String? = null
)

data class NoticePreview(
    val id: String,
    val title: String,
    val date: String,
    val author: String,
    val url: String,
    val sourceUrl: String
)
