package com.bsbarron.midschoolapp.data.repository

import com.bsbarron.midschoolapp.BuildConfig
import com.bsbarron.midschoolapp.R
import com.bsbarron.midschoolapp.data.model.MealInfo
import com.bsbarron.midschoolapp.data.model.NoticeFeed
import com.bsbarron.midschoolapp.data.model.NoticePreview
import com.bsbarron.midschoolapp.data.model.SchoolEvent
import com.bsbarron.midschoolapp.data.model.SchoolInfo
import com.bsbarron.midschoolapp.data.model.TimetableItem
import com.bsbarron.midschoolapp.data.remote.NeisApiException
import com.bsbarron.midschoolapp.data.remote.NeisApiService
import com.bsbarron.midschoolapp.data.remote.NoticeApiService
import com.bsbarron.midschoolapp.data.remote.dto.NeisResultDto
import com.bsbarron.midschoolapp.data.remote.dto.NeisSection
import java.io.IOException
import javax.inject.Inject
import retrofit2.HttpException

class SchoolRepositoryImpl @Inject constructor(
    private val apiService: NeisApiService,
    private val preferencesRepository: PreferencesRepository,
    private val noticeApiService: NoticeApiService
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
                school.schoolKind == ELEMENTARY_SCHOOL_KIND ||
                    school.schoolKind == MIDDLE_SCHOOL_KIND ||
                    school.schoolKind == HIGH_SCHOOL_KIND
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
        val cacheKey = date
        val networkResult = runCatching {
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

        networkResult.getOrNull()?.let { schedules ->
            if (!cacheKey.isNullOrBlank()) {
                preferencesRepository.saveScheduleCache(
                    studentInfo.officeCode,
                    studentInfo.schoolCode,
                    cacheKey,
                    schedules
                )
            }
            return Result.success(schedules)
        }

        val cachedSchedules = cacheKey?.let {
            preferencesRepository.getScheduleCache(studentInfo.officeCode, studentInfo.schoolCode, it)
        }
        return if (cachedSchedules != null) {
            Result.success(cachedSchedules)
        } else {
            Result.failure(networkResult.exceptionOrNull() ?: IllegalStateException("학사 일정을 불러오지 못했어요."))
        }
    }

    override suspend fun getNotices(limit: Int): Result<NoticeFeed> {
        val studentInfo = selectedStudentInfo().getOrElse { return Result.failure(it) }
        if (BuildConfig.WEB_BASE_URL.isBlank()) {
            return Result.failure(IllegalStateException("가정통신문 서버 주소가 설정되지 않았어요."))
        }

        return try {
            val response = noticeApiService.getNotices(
                officeCode = studentInfo.officeCode,
                schoolCode = studentInfo.schoolCode,
                limit = limit.coerceIn(1, 10)
            )
            Result.success(
                NoticeFeed(
                    items = response.items.map { item ->
                        NoticePreview(
                            id = item.id,
                            title = item.title,
                            date = item.date,
                            author = item.author,
                            url = item.url,
                            sourceUrl = item.sourceUrl
                        )
                    },
                    message = response.message?.trim()?.takeIf { it.isNotBlank() }
                )
            )
        } catch (error: Exception) {
            Result.failure(mapNoticeError(error))
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

                MIDDLE_SCHOOL_KIND -> apiService.getMiddleTimetable(
                    officeCode = studentInfo.officeCode,
                    schoolCode = studentInfo.schoolCode,
                    grade = grade,
                    classroom = classroom,
                    date = date
                ).misTimetable

                HIGH_SCHOOL_KIND -> apiService.getHighTimetable(
                    officeCode = studentInfo.officeCode,
                    schoolCode = studentInfo.schoolCode,
                    grade = grade,
                    classroom = classroom,
                    date = date
                ).hisTimetable

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

    private fun mapNoticeError(error: Exception): Throwable {
        return when (error) {
            is HttpException -> {
                val message = when (error.code()) {
                    404 -> "학교 홈페이지 주소를 찾지 못해 가정통신문을 불러오지 못했어요."
                    504 -> "가정통신문 조회가 지연되어 잠시 후 다시 시도해 주세요."
                    else -> "가정통신문을 불러오지 못했어요."
                }
                IllegalStateException(message, error)
            }

            is IOException -> IllegalStateException("가정통신문 서버에 연결할 수 없어요.", error)
            else -> error
        }
    }

    companion object {
        private const val MIN_SCHOOL_QUERY_LENGTH = 2
        private const val ELEMENTARY_SCHOOL_KIND = "초등학교"
        private const val MIDDLE_SCHOOL_KIND = "중학교"
        private const val HIGH_SCHOOL_KIND = "고등학교"
    }
}
