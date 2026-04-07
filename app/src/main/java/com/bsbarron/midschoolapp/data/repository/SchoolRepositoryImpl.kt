package com.bsbarron.midschoolapp.data.repository

import com.bsbarron.midschoolapp.data.model.MealInfo
import com.bsbarron.midschoolapp.data.model.SchoolEvent
import com.bsbarron.midschoolapp.data.model.TimetableItem
import com.bsbarron.midschoolapp.data.remote.NeisApiException
import com.bsbarron.midschoolapp.data.remote.NeisApiService
import com.bsbarron.midschoolapp.data.remote.dto.NeisResultDto
import com.bsbarron.midschoolapp.data.remote.dto.NeisSection
import javax.inject.Inject
import javax.inject.Named

class SchoolRepositoryImpl @Inject constructor(
    private val apiService: NeisApiService,
    @param:Named("officeCode") private val officeCode: String,
    @param:Named("schoolCode") private val schoolCode: String
) : SchoolRepository {

    override suspend fun getMeals(date: String?): Result<List<MealInfo>> = runCatching {
        extractRows(
            sections = apiService.getMeals(
                officeCode = officeCode,
                schoolCode = schoolCode,
                date = date
            ).mealServiceDietInfo,
            dataLabel = "급식"
        )
            .map { row ->
                MealInfo(
                    date = row.mealDate,
                    mealType = row.mealTypeName.orEmpty(),
                    menu = row.menu.orEmpty(),
                    calorieInfo = row.calorieInfo.orEmpty()
                )
            }
    }

    override suspend fun getSchedules(date: String?): Result<List<SchoolEvent>> = runCatching {
        extractRows(
            sections = apiService.getSchedules(
                officeCode = officeCode,
                schoolCode = schoolCode,
                date = date
            ).schoolSchedule,
            dataLabel = "학사 일정"
        )
            .map { row ->
                SchoolEvent(
                    date = row.date,
                    title = row.title.orEmpty(),
                    description = row.description.orEmpty()
                )
            }
    }

    override suspend fun getTimetable(
        grade: String,
        classroom: String,
        date: String?
    ): Result<List<TimetableItem>> = runCatching {
        extractRows(
            sections = apiService.getTimetable(
                officeCode = officeCode,
                schoolCode = schoolCode,
                grade = grade,
                classroom = classroom,
                date = date
            ).misTimetable,
            dataLabel = "시간표"
        )
            .map { row ->
                TimetableItem(
                    date = row.date,
                    period = row.period.orEmpty(),
                    subject = row.subject.orEmpty(),
                    grade = row.grade.orEmpty(),
                    classroom = row.classroom.orEmpty()
                )
            }
    }

    private fun <T> extractRows(
        sections: List<NeisSection<T>>?,
        dataLabel: String
    ): List<T> {
        val result = sections.orEmpty()
            .firstNotNullOfOrNull { section ->
                section.head.orEmpty().firstNotNullOfOrNull { it.result }
            }

        validateResult(result, dataLabel)
        return sections.orEmpty().getOrNull(1)?.row.orEmpty()
    }

    private fun validateResult(result: NeisResultDto?, dataLabel: String) {
        val code = result?.code.orEmpty()
        if (code.isBlank() || code == "INFO-000" || code == "INFO-200") {
            return
        }

        val message = when (code) {
            "INFO-100" -> "$dataLabel 조회에 필요한 값이 누락되었어요."
            "INFO-300" -> "$dataLabel 데이터가 준비되지 않았어요."
            "ERROR-300" -> "나이스 인증키를 다시 확인해 주세요."
            "ERROR-336" -> "요청 횟수가 많아 잠시 후 다시 시도해 주세요."
            else -> result?.message?.takeIf { it.isNotBlank() } ?: "$dataLabel 정보를 불러오지 못했어요."
        }
        throw NeisApiException(code = code, message = message)
    }
}
