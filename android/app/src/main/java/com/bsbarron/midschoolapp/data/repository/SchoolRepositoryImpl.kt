package com.bsbarron.midschoolapp.data.repository

import com.bsbarron.midschoolapp.R
import com.bsbarron.midschoolapp.data.model.MealInfo
import com.bsbarron.midschoolapp.data.model.SchoolEvent
import com.bsbarron.midschoolapp.data.model.SchoolInfo
import com.bsbarron.midschoolapp.data.model.TimetableItem
import com.bsbarron.midschoolapp.data.remote.NeisApiException
import com.bsbarron.midschoolapp.data.remote.NeisApiService
import com.bsbarron.midschoolapp.data.remote.dto.NeisResultDto
import com.bsbarron.midschoolapp.data.remote.dto.NeisSection
import javax.inject.Inject

class SchoolRepositoryImpl @Inject constructor(
    private val apiService: NeisApiService,
    private val preferencesRepository: PreferencesRepository
) : SchoolRepository {

    override suspend fun searchSchools(query: String): Result<List<SchoolInfo>> = runCatching {
        val trimmedQuery = query.trim()
        require(trimmedQuery.length >= MIN_SCHOOL_QUERY_LENGTH) {
            "학교 이름은 두 글자 이상 입력해 주세요."
        }

        extractRows(
            sections = apiService.getSchools(query = trimmedQuery).schoolInfo,
            dataLabel = "학교 검색"
        )
            .map { row ->
                SchoolInfo(
                    officeCode = row.officeCode,
                    officeName = row.officeName.orEmpty(),
                    schoolCode = row.schoolCode,
                    schoolName = row.schoolName.orEmpty(),
                    schoolKind = row.schoolKind.orEmpty(),
                    location = row.location.orEmpty(),
                    jurisdiction = row.jurisdiction.orEmpty(),
                    foundation = row.foundation.orEmpty(),
                    roadAddress = row.roadAddress.orEmpty(),
                    telephone = row.telephone.orEmpty(),
                    homepage = row.homepage.orEmpty()
                )
            }
            .filter { school ->
                school.schoolKind == ELEMENTARY_SCHOOL_KIND || school.schoolKind == MIDDLE_SCHOOL_KIND
            }
    }

    override suspend fun getMeals(date: String?): Result<List<MealInfo>> {
        val studentInfo = selectedStudentInfo().getOrElse { return Result.failure(it) }
        val cacheKey = date
        val networkResult = runCatching {
            extractRows(
                sections = apiService.getMeals(
                    officeCode = studentInfo.officeCode,
                    schoolCode = studentInfo.schoolCode,
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

        networkResult.getOrNull()?.let { meals ->
            if (!cacheKey.isNullOrBlank()) {
                preferencesRepository.saveMealCache(studentInfo.officeCode, studentInfo.schoolCode, cacheKey, meals)
            }
            return Result.success(meals)
        }

        val cachedMeals = cacheKey?.let {
            preferencesRepository.getMealCache(studentInfo.officeCode, studentInfo.schoolCode, it)
        }
        return if (!cachedMeals.isNullOrEmpty()) {
            Result.success(cachedMeals)
        } else {
            Result.failure(networkResult.exceptionOrNull() ?: IllegalStateException("급식 정보를 불러오지 못했어요."))
        }
    }

    override suspend fun getSchedules(date: String?): Result<List<SchoolEvent>> {
        val studentInfo = selectedStudentInfo().getOrElse { return Result.failure(it) }
        return runCatching {
            extractRows(
                sections = apiService.getSchedules(
                    officeCode = studentInfo.officeCode,
                    schoolCode = studentInfo.schoolCode,
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
    }

    override suspend fun getTimetable(
        grade: String,
        classroom: String,
        date: String?
    ): Result<List<TimetableItem>> {
        val studentInfo = selectedStudentInfo().getOrElse { return Result.failure(it) }
        val cacheKey = date
        val networkResult = runCatching {
            val timetableSections = when (studentInfo.schoolKind) {
                ELEMENTARY_SCHOOL_KIND -> apiService.getElementaryTimetable(
                    officeCode = studentInfo.officeCode,
                    schoolCode = studentInfo.schoolCode,
                    grade = grade,
                    classroom = classroom,
                    date = date
                ).elsTimetable

                else -> apiService.getMiddleTimetable(
                    officeCode = studentInfo.officeCode,
                    schoolCode = studentInfo.schoolCode,
                    grade = grade,
                    classroom = classroom,
                    date = date
                ).misTimetable
            }

            extractRows(
                sections = timetableSections,
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

        networkResult.getOrNull()?.let { items ->
            if (!cacheKey.isNullOrBlank()) {
                preferencesRepository.saveTimetableCache(
                    officeCode = studentInfo.officeCode,
                    schoolCode = studentInfo.schoolCode,
                    grade = grade,
                    classroom = classroom,
                    date = cacheKey,
                    items = items
                )
            }
            return Result.success(items)
        }

        val cachedItems = cacheKey?.let {
            preferencesRepository.getTimetableCache(
                officeCode = studentInfo.officeCode,
                schoolCode = studentInfo.schoolCode,
                grade = grade,
                classroom = classroom,
                date = it
            )
        }
        return if (!cachedItems.isNullOrEmpty()) {
            Result.success(cachedItems)
        } else {
            Result.failure(networkResult.exceptionOrNull() ?: IllegalStateException("시간표 정보를 불러오지 못했어요."))
        }
    }

    private fun selectedStudentInfo(): Result<StudentInfo> {
        val studentInfo = preferencesRepository.getStudentInfo()
        return if (studentInfo.hasSchoolSelection()) {
            Result.success(studentInfo)
        } else {
            Result.failure(IllegalStateException("설정에서 학교를 먼저 선택해 주세요."))
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

    companion object {
        private const val MIN_SCHOOL_QUERY_LENGTH = 2
        private const val ELEMENTARY_SCHOOL_KIND = "초등학교"
        private const val MIDDLE_SCHOOL_KIND = "중학교"
    }
}
