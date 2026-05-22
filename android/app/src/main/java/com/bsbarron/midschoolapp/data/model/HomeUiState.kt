package com.bsbarron.midschoolapp.data.model

enum class HomeContentStatus {
    NOT_CONFIGURED,
    LOADING,
    EMPTY,
    ERROR,
    SUCCESS
}

data class HomeUiState(
    val schoolName: String = "",
    val dateLabel: String = "",
    val classSummary: String = "",
    val isSchoolConfigured: Boolean = false,
    val todaySummaryText: String = "",
    val todayStatus: HomeContentStatus = HomeContentStatus.NOT_CONFIGURED,
    val mealSummary: String = "",
    val mealMeta: String = "",
    val mealStatus: HomeContentStatus = HomeContentStatus.NOT_CONFIGURED,
    val eventSummary: String = "",
    val scheduleStatus: HomeContentStatus = HomeContentStatus.NOT_CONFIGURED,
    val notices: HomeNoticeCardState = HomeNoticeCardState()
)

data class HomeNoticeCardState(
    val summary: String = "",
    val actionText: String = "",
    val actionEnabled: Boolean = false,
    val latestNoticeUrl: String? = null,
    val requiresSetup: Boolean = false
)
