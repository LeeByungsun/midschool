package com.lbs.schoolhelper.ui.home

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lbs.schoolhelper.R
import com.lbs.schoolhelper.data.model.HomeContentStatus
import com.lbs.schoolhelper.data.model.HomeNoticeCardState
import com.lbs.schoolhelper.data.model.HomeUiState
import com.lbs.schoolhelper.data.model.MealInfo
import com.lbs.schoolhelper.data.model.NoticeFeed
import com.lbs.schoolhelper.data.model.NoticePreview
import com.lbs.schoolhelper.data.model.SchoolEvent
import com.lbs.schoolhelper.data.repository.PreferencesRepository
import com.lbs.schoolhelper.data.repository.SchoolRepository
import com.lbs.schoolhelper.util.isVisibleSchedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel private constructor(
    application: Application,
    private val schoolRepository: SchoolRepository,
    private val preferencesRepository: PreferencesRepository,
    private val textResolver: (Int, Array<out Any?>) -> String
) : AndroidViewModel(application) {

    private var loadHomeJob: Job? = null
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val _noticeActionEvent = MutableSharedFlow<HomeNoticeAction>(extraBufferCapacity = 1)
    val noticeActionEvent: SharedFlow<HomeNoticeAction> = _noticeActionEvent.asSharedFlow()

    @Inject
    constructor(
        application: Application,
        schoolRepository: SchoolRepository,
        preferencesRepository: PreferencesRepository
    ) : this(application, schoolRepository, preferencesRepository, defaultTextResolver(application))

    companion object {
        fun createForTest(
            application: Application,
            schoolRepository: SchoolRepository,
            preferencesRepository: PreferencesRepository,
            textResolver: (Int, Array<out Any?>) -> String
        ): HomeViewModel {
            return HomeViewModel(
                application,
                schoolRepository,
                preferencesRepository,
                textResolver
            )
        }

        private fun defaultTextResolver(application: Application): (Int, Array<out Any?>) -> String {
            val context = application.applicationContext
            return { id, formatArgs ->
                if (formatArgs.isEmpty()) {
                    context.getString(id)
                } else {
                    context.getString(id, *formatArgs)
                }
            }
        }
    }

    init {
        refreshHeader()
    }

    private fun resolveString(@StringRes id: Int): String = textResolver(id, emptyArray<Any?>())

    private fun resolveString(@StringRes id: Int, vararg args: Any?): String = textResolver(id, args)

    fun refreshHeader() {
        val studentInfo = preferencesRepository.getStudentInfo()
        val grade = studentInfo.grade
        val classroom = studentInfo.classroom
        val hasSchoolSelection = studentInfo.hasSchoolSelection()
        _uiState.update {
            it.copy(
                isSchoolConfigured = hasSchoolSelection,
                schoolName = studentInfo.schoolName.ifBlank {
                    resolveString(R.string.home_school_name_placeholder)
                },
                dateLabel = LocalDate.now().format(
                    DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)
                ),
                classSummary = if (!hasSchoolSelection) {
                    resolveString(R.string.home_school_not_set_hint)
                } else if (grade.isNotBlank() && classroom.isNotBlank()) {
                    resolveString(R.string.home_student_info_format, grade, classroom)
                } else {
                    resolveString(R.string.home_semester_label)
                },
                notices = resolveHeaderNoticeState(
                    current = it.notices,
                    hasSchoolSelection = hasSchoolSelection
                )
            )
        }
    }

    fun loadHomeData() {
        loadHomeJob?.cancel()
        loadHomeJob = viewModelScope.launch {
            val today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
            refreshHeader()

            _uiState.update {
                it.copy(
                    todaySummaryText = resolveString(R.string.home_today_summary_loading),
                    todayStatus = HomeContentStatus.LOADING,
                    mealSummary = resolveString(R.string.home_meal_loading),
                    mealMeta = "",
                    mealStatus = HomeContentStatus.LOADING,
                    eventSummary = resolveString(R.string.home_schedule_loading),
                    scheduleStatus = HomeContentStatus.LOADING
                )
            }

            val studentInfo = preferencesRepository.getStudentInfo()
            if (!studentInfo.hasSchoolSelection()) {
                _uiState.update {
                    it.copy(
                        isSchoolConfigured = false,
                        todaySummaryText = resolveString(R.string.home_school_not_set_summary),
                        todayStatus = HomeContentStatus.NOT_CONFIGURED,
                        mealSummary = resolveString(R.string.home_meal_missing_school),
                        mealMeta = "",
                        mealStatus = HomeContentStatus.NOT_CONFIGURED,
                        eventSummary = resolveString(R.string.home_schedule_missing_school),
                        scheduleStatus = HomeContentStatus.NOT_CONFIGURED,
                        notices = buildSetupRequiredNoticeState()
                    )
                }
                return@launch
            }

            launch {
                val noticesResult = schoolRepository.getNotices(limit = 3)
                _uiState.update { current ->
                    current.copy(notices = buildNoticeCardState(noticesResult))
                }
            }

            launch {
                schoolRepository.observeMeals(today).collect { result ->
                    updateHomeSections(
                        mealUi = buildMealUi(
                            meal = result.getOrNull()?.firstOrNull(),
                            error = result.exceptionOrNull()
                        )
                    )
                }
            }

            launch {
                schoolRepository.observeSchedules(today.take(6)).collect { result ->
                    updateHomeSections(
                        scheduleUi = buildScheduleUi(
                            events = result.getOrNull().orEmpty(),
                            error = result.exceptionOrNull()
                        )
                    )
                }
            }
        }
    }

    private fun updateHomeSections(
        mealUi: HomeSectionUi? = null,
        scheduleUi: HomeSectionUi? = null
    ) {
        _uiState.update { current ->
            val mealStatus = mealUi?.status ?: current.mealStatus
            val scheduleStatus = scheduleUi?.status ?: current.scheduleStatus
            val todayStatus = resolveTodayStatus(mealStatus, scheduleStatus)

            current.copy(
                isSchoolConfigured = true,
                todaySummaryText = resolveTodaySummary(todayStatus),
                todayStatus = todayStatus,
                mealSummary = mealUi?.summary ?: current.mealSummary,
                mealMeta = mealUi?.meta ?: current.mealMeta,
                mealStatus = mealStatus,
                eventSummary = scheduleUi?.summary ?: current.eventSummary,
                scheduleStatus = scheduleStatus
            )
        }
    }

    private fun resolveTodayStatus(
        mealStatus: HomeContentStatus,
        scheduleStatus: HomeContentStatus
    ): HomeContentStatus {
        return when {
            mealStatus == HomeContentStatus.ERROR ||
                scheduleStatus == HomeContentStatus.ERROR -> HomeContentStatus.ERROR
            mealStatus == HomeContentStatus.LOADING ||
                scheduleStatus == HomeContentStatus.LOADING -> HomeContentStatus.LOADING
            mealStatus == HomeContentStatus.EMPTY &&
                scheduleStatus == HomeContentStatus.EMPTY -> HomeContentStatus.EMPTY
            else -> HomeContentStatus.SUCCESS
        }
    }

    private fun resolveTodaySummary(status: HomeContentStatus): String {
        return when (status) {
            HomeContentStatus.NOT_CONFIGURED -> resolveString(R.string.home_school_not_set_summary)
            HomeContentStatus.LOADING -> resolveString(R.string.home_today_summary_loading)
            HomeContentStatus.ERROR -> resolveString(R.string.home_today_summary_error)
            HomeContentStatus.EMPTY -> resolveString(R.string.home_today_summary_empty)
            HomeContentStatus.SUCCESS -> resolveString(R.string.home_today_summary_body)
        }
    }

    fun onNoticeActionClicked() {
        val noticeState = uiState.value.notices
        when {
            noticeState.requiresSetup -> _noticeActionEvent.tryEmit(HomeNoticeAction.OpenSetup)
            !noticeState.latestNoticeUrl.isNullOrBlank() -> {
                _noticeActionEvent.tryEmit(HomeNoticeAction.OpenUrl(noticeState.latestNoticeUrl))
            }
        }
    }

    private fun buildMealUi(meal: MealInfo?, error: Throwable?): HomeSectionUi {
        if (error != null) {
            return HomeSectionUi(
                status = HomeContentStatus.ERROR,
                summary = resolveString(R.string.meal_error_day),
                meta = ""
            )
        }

        if (meal == null) {
            return HomeSectionUi(
                status = HomeContentStatus.EMPTY,
                summary = resolveString(R.string.home_meal_empty),
                meta = resolveString(R.string.home_meal_empty_meta)
            )
        }

        val mealSummary = meal.menu
            .let(::formatMealMenu)
            .trim()
            .ifBlank { resolveString(R.string.home_meal_empty) }
        val mealMeta = listOfNotNull(
            meal.mealType.takeIf { it.isNotBlank() },
            meal.calorieInfo.takeIf { it.isNotBlank() }
        ).joinToString(" • ")
            .ifBlank { resolveString(R.string.home_meal_empty_meta) }

        return HomeSectionUi(
            status = HomeContentStatus.SUCCESS,
            summary = mealSummary,
            meta = mealMeta
        )
    }

    private fun buildScheduleUi(events: List<SchoolEvent>, error: Throwable?): HomeSectionUi {
        if (error != null) {
            return HomeSectionUi(
                status = HomeContentStatus.ERROR,
                summary = resolveString(R.string.home_schedule_error),
                meta = ""
            )
        }

        val eventSummary = events
            .filter { it.isVisibleSchedule() }
            .mapNotNull { event ->
                val eventDate = runCatching {
                    LocalDate.parse(event.date, DateTimeFormatter.BASIC_ISO_DATE)
                }.getOrNull() ?: return@mapNotNull null

                if (eventDate.isBefore(LocalDate.now())) {
                    return@mapNotNull null
                }

                val title = event.title.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val formattedDate = eventDate.format(
                    DateTimeFormatter.ofPattern("M/d(E)", Locale.KOREAN)
                )
                val detail = event.description.takeIf { it.isNotBlank() }

                buildString {
                    append(formattedDate)
                    append("  ")
                    append(title)
                    if (detail != null) {
                        append("\n")
                        append(detail)
                    }
                }
            }
            .take(3)
            .joinToString("\n\n")

        return if (eventSummary.isBlank()) {
            HomeSectionUi(
                status = HomeContentStatus.EMPTY,
                summary = resolveString(R.string.home_schedule_empty)
            )
        } else {
            HomeSectionUi(
                status = HomeContentStatus.SUCCESS,
                summary = eventSummary
            )
        }
    }

    private fun buildSetupRequiredNoticeState(): HomeNoticeCardState {
        return HomeNoticeCardState(
            summary = resolveString(R.string.home_notice_setup_required),
            actionText = resolveString(R.string.home_setup_button),
            actionEnabled = true,
            latestNoticeUrl = null,
            requiresSetup = true
        )
    }

    private fun buildUnavailableNoticeState(summary: String): HomeNoticeCardState {
        return HomeNoticeCardState(
            summary = summary,
            actionText = resolveString(R.string.home_notice_unavailable_button),
            actionEnabled = false,
            latestNoticeUrl = null,
            requiresSetup = false
        )
    }

    private fun resolveHeaderNoticeState(
        current: HomeNoticeCardState,
        hasSchoolSelection: Boolean
    ): HomeNoticeCardState {
        if (!hasSchoolSelection) {
            return buildSetupRequiredNoticeState()
        }

        return if (current.requiresSetup || isUninitializedNoticeState(current)) {
            buildUnavailableNoticeState(resolveString(R.string.home_notice_empty))
        } else {
            current
        }
    }

    private fun isUninitializedNoticeState(state: HomeNoticeCardState): Boolean {
        return state.summary.isBlank() &&
            state.actionText.isBlank() &&
            !state.actionEnabled &&
            state.latestNoticeUrl == null &&
            !state.requiresSetup
    }

    private fun buildNoticeCardState(noticesResult: Result<NoticeFeed>): HomeNoticeCardState {
        return noticesResult.fold(
            onSuccess = { noticeFeed ->
                val previewLines = noticeFeed.items
                    .take(3)
                    .map(::formatNoticePreviewLine)
                    .filter { it.isNotBlank() }

                when {
                    previewLines.isNotEmpty() -> HomeNoticeCardState(
                        summary = previewLines.joinToString("\n"),
                        actionText = resolveString(R.string.home_notice_open_button),
                        actionEnabled = true,
                        latestNoticeUrl = noticeFeed.items.firstOrNull()?.let { notice ->
                            notice.sourceUrl.ifBlank { notice.url }
                        },
                        requiresSetup = false
                    )

                    !noticeFeed.message.isNullOrBlank() -> HomeNoticeCardState(
                        summary = noticeFeed.message,
                        actionText = resolveString(R.string.home_notice_unavailable_button),
                        actionEnabled = false,
                        latestNoticeUrl = null,
                        requiresSetup = false
                    )

                    else -> buildUnavailableNoticeState(resolveString(R.string.home_notice_empty))
                }
            },
            onFailure = { error ->
                buildUnavailableNoticeState(
                    error.message ?: resolveString(R.string.home_notice_error)
                )
            }
        )
    }

    private fun formatNoticePreviewLine(notice: NoticePreview): String {
        val date = notice.date.takeIf { it.isNotBlank() }
        return if (date != null) {
            resolveString(R.string.home_notice_preview_format, date, notice.title)
        } else {
            notice.title
        }
    }

    private fun formatMealMenu(rawMenu: String): String {
        return rawMenu
            .replace(Regex("<br\\s*/?>"), "\n")
            .replace(Regex("[ \\t]+"), " ")
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map(::formatMealLine)
            .joinToString("\n")
    }

    private fun formatMealLine(line: String): String {
        val match = Regex("^(.*?)(\\(([^)]*)\\))?$").matchEntire(line.trim()) ?: return line.trim()
        val name = match.groupValues[1].trim()
        val allergy = match.groupValues.getOrNull(3)?.trim().orEmpty()

        return if (allergy.isNotBlank()) {
            "$name ($allergy)"
        } else {
            name
        }
    }

    private data class HomeSectionUi(
        val status: HomeContentStatus,
        val summary: String,
        val meta: String = ""
    )
}

sealed interface HomeNoticeAction {
    data object OpenSetup : HomeNoticeAction
    data class OpenUrl(val url: String) : HomeNoticeAction
}
