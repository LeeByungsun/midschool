package com.bsbarron.midschoolapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsbarron.midschoolapp.data.model.HomeUiState
import com.bsbarron.midschoolapp.data.repository.SchoolRepository
import com.bsbarron.midschoolapp.util.isVisibleSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeViewModel(
    private val schoolRepository: SchoolRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadHomeData() {
        viewModelScope.launch {
            val today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val mealsResult = schoolRepository.getMeals(today)
            val schedulesResult = schoolRepository.getSchedules(today.take(6))

            val firstMeal = mealsResult.getOrNull()?.firstOrNull()
            val mealSummary = firstMeal?.menu
                ?.let(::formatMealMenu)
                ?.trim()
                .orEmpty()
            val mealMeta = listOfNotNull(
                firstMeal?.mealType?.takeIf { it.isNotBlank() },
                firstMeal?.calorieInfo?.takeIf { it.isNotBlank() }
            ).joinToString(" • ")
            val eventSummary = schedulesResult.getOrNull()
                .orEmpty()
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
                        if (!detail.isNullOrBlank()) {
                            append("\n")
                            append(detail)
                        }
                    }
                }
                .take(3)
                .joinToString("\n\n")
            val errorMessage = mealsResult.exceptionOrNull()?.message
                ?: schedulesResult.exceptionOrNull()?.message

            _uiState.update {
                it.copy(
                    mealSummary = mealSummary.ifBlank { "오늘은 등록된 급식이 없어요." },
                    mealMeta = mealMeta.ifBlank { "급식 없음" },
                    eventSummary = eventSummary.ifBlank { "이번 달에 남아 있는 학사 일정이 없어요." },
                    isLoading = false,
                    errorMessage = errorMessage
                )
            }
        }
    }

    private fun formatMealMenu(rawMenu: String): String {
        return rawMenu
            .replace(Regex("<br\\s*/?>"), "\n")
            .replace(Regex("[ \t]+"), " ")
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
}
