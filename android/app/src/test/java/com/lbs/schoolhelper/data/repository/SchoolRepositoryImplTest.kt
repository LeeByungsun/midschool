package com.lbs.schoolhelper.data.repository

import com.lbs.schoolhelper.data.model.MealInfo
import com.lbs.schoolhelper.data.model.NoticeFeed
import com.lbs.schoolhelper.data.model.NoticePreview
import com.lbs.schoolhelper.data.model.SchoolInfo
import com.lbs.schoolhelper.data.model.SchoolEvent
import com.lbs.schoolhelper.data.model.TimetableItem
import com.lbs.schoolhelper.data.remote.NeisApiException
import com.lbs.schoolhelper.data.remote.NeisApiService
import com.lbs.schoolhelper.data.remote.NoticeApiService
import com.lbs.schoolhelper.data.remote.dto.MealRowDto
import com.lbs.schoolhelper.data.remote.dto.NeisHeadDto
import com.lbs.schoolhelper.data.remote.dto.NeisResponse
import com.lbs.schoolhelper.data.remote.dto.NeisResultDto
import com.lbs.schoolhelper.data.remote.dto.NeisSection
import com.lbs.schoolhelper.data.remote.dto.NoticeListResponseDto
import com.lbs.schoolhelper.data.remote.dto.NoticeSummaryDto
import com.lbs.schoolhelper.data.remote.dto.ScheduleRowDto
import com.lbs.schoolhelper.data.remote.dto.SchoolInfoRowDto
import com.lbs.schoolhelper.data.remote.dto.TimetableRowDto
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun <T> successResponse(rows: List<T>): NeisResponse<T> {
    val sections = listOf(
        NeisSection(
            head = listOf(
                NeisHeadDto(
                    result = NeisResultDto(code = "INFO-000", message = "OK"),
                    totalCount = rows.size
                )
            )
        ),
        NeisSection(row = rows)
    )
    return NeisResponse(
        mealServiceDietInfo = sections,
        schoolSchedule = sections,
        elsTimetable = sections,
        misTimetable = sections,
        hisTimetable = sections,
        schoolInfo = sections
    )
}

private fun <T> errorResponse(code: String, message: String): NeisResponse<T> {
    return NeisResponse(
        result = NeisResultDto(code = code, message = message)
    )
}

class SchoolRepositoryImplTest {

    @Test
    fun `getMeals uses selected school codes and cache keys`() = runBlocking {
        val apiService = FakeNeisApiService().apply {
            mealsResponse = successResponse(
                listOf(
                    MealRowDto(
                        mealDate = "20260519",
                        mealTypeName = "점심",
                        menu = "비빔밥",
                        calorieInfo = "700kcal"
                    )
                )
            )
        }
        val preferencesRepository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "3",
                schoolName = "미사중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        )
        val repository = SchoolRepositoryImpl(apiService, preferencesRepository, FakeNoticeApiService())

        val result = repository.getMeals("20260519")

        assertTrue(result.isSuccess)
        assertEquals("J10", apiService.lastMealOfficeCode)
        assertEquals("1234567", apiService.lastMealSchoolCode)
        assertEquals("J10", preferencesRepository.savedMealCacheArgs?.officeCode)
        assertEquals("1234567", preferencesRepository.savedMealCacheArgs?.schoolCode)
    }

    @Test
    fun `getMeals returns failure when NEIS root result contains error`() = runBlocking {
        val apiService = FakeNeisApiService().apply {
            mealsResponse = errorResponse("ERROR-300", "인증 실패")
        }
        val preferencesRepository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "3",
                schoolName = "미사중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        )
        val repository = SchoolRepositoryImpl(apiService, preferencesRepository, FakeNoticeApiService())

        val result = repository.getMeals("20260519")

        assertTrue(result.isFailure)
        assertEquals("나이스 인증키를 다시 확인해 주세요.", result.exceptionOrNull()?.message)
        assertTrue(result.exceptionOrNull() is NeisApiException)
        assertNull(preferencesRepository.savedMealCacheArgs)
    }

    @Test
    fun `observeMeals emits cached meals first and then changed network meals`() = runBlocking {
        val cachedMeals = listOf(
            MealInfo(
                date = "20260519",
                mealType = "중식",
                menu = "캐시 메뉴",
                calorieInfo = "600kcal"
            )
        )
        val networkMeals = listOf(
            MealInfo(
                date = "20260519",
                mealType = "중식",
                menu = "최신 메뉴",
                calorieInfo = "700kcal"
            )
        )
        val apiService = FakeNeisApiService().apply {
            mealsResponse = successResponse(
                listOf(
                    MealRowDto(
                        mealDate = "20260519",
                        mealTypeName = "중식",
                        menu = "최신 메뉴",
                        calorieInfo = "700kcal"
                    )
                )
            )
        }
        val preferencesRepository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "3",
                schoolName = "미사중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        ).apply {
            mealCache[MealCacheKey("J10", "1234567", "20260519")] = cachedMeals
        }
        val repository = SchoolRepositoryImpl(apiService, preferencesRepository, FakeNoticeApiService())

        val emissions = repository.observeMeals("20260519").toList()

        assertEquals(2, emissions.size)
        assertEquals(cachedMeals, emissions[0].getOrThrow())
        assertEquals(networkMeals, emissions[1].getOrThrow())
        assertEquals(networkMeals, preferencesRepository.savedMealCacheArgs?.meals)
    }

    @Test
    fun `getSchedules uses selected school codes`() = runBlocking {
        val apiService = FakeNeisApiService().apply {
            schedulesResponse = successResponse(
                listOf(
                    ScheduleRowDto(
                        date = "20260519",
                        title = "체육대회",
                        description = "운동장"
                    )
                )
            )
        }
        val preferencesRepository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "3",
                schoolName = "미사중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        )
        val repository = SchoolRepositoryImpl(apiService, preferencesRepository, FakeNoticeApiService())

        val result = repository.getSchedules("202605")

        assertTrue(result.isSuccess)
        assertEquals("J10", apiService.lastScheduleOfficeCode)
        assertEquals("1234567", apiService.lastScheduleSchoolCode)
    }

    @Test
    fun `getSchedules saves schedules to cache on success`() = runBlocking {
        val apiService = FakeNeisApiService().apply {
            schedulesResponse = successResponse(
                listOf(
                    ScheduleRowDto(
                        date = "20260519",
                        title = "체육대회",
                        description = "운동장"
                    )
                )
            )
        }
        val preferencesRepository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "3",
                schoolName = "미사중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        )
        val repository = SchoolRepositoryImpl(apiService, preferencesRepository, FakeNoticeApiService())

        val result = repository.getSchedules("202605")

        assertTrue(result.isSuccess)
        assertEquals(
            listOf(
                SchoolEvent(
                    date = "20260519",
                    title = "체육대회",
                    description = "운동장"
                )
            ),
            preferencesRepository.savedScheduleCacheEvents
        )
    }

    @Test
    fun `getSchedules returns cached schedules when network fails`() = runBlocking {
        val apiService = FakeNeisApiService().apply {
            schedulesResponse = successResponse(emptyList())
            failSchedules = true
        }
        val cacheEvents = listOf(
            SchoolEvent(date = "20260519", title = "대체행사", description = "캐시에서 복구")
        )
        val preferencesRepository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "3",
                schoolName = "미사중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        ).apply {
            scheduleCache[ScheduleCacheKey("J10", "1234567", "202605")] = cacheEvents
        }
        val repository = SchoolRepositoryImpl(apiService, preferencesRepository, FakeNoticeApiService())

        val result = repository.getSchedules("202605")

        assertTrue(result.isSuccess)
        assertEquals(cacheEvents, result.getOrThrow())
    }

    @Test
    fun `getSchedules returns cached empty schedules when network fails`() = runBlocking {
        val apiService = FakeNeisApiService().apply {
            schedulesResponse = successResponse(emptyList())
            failSchedules = true
        }
        val preferencesRepository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "3",
                schoolName = "미사중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        ).apply {
            scheduleCache[ScheduleCacheKey("J10", "1234567", "202605")] = emptyList()
        }
        val repository = SchoolRepositoryImpl(apiService, preferencesRepository, FakeNoticeApiService())

        val result = repository.getSchedules("202605")

        assertTrue(result.isSuccess)
        assertEquals(emptyList<SchoolEvent>(), result.getOrThrow())
    }

    @Test
    fun `observeSchedules emits cached schedule first and skips identical network schedule`() = runBlocking {
        val cachedEvents = listOf(
            SchoolEvent(date = "20260519", title = "체육대회", description = "운동장")
        )
        val apiService = FakeNeisApiService().apply {
            schedulesResponse = successResponse(
                listOf(
                    ScheduleRowDto(
                        date = "20260519",
                        title = "체육대회",
                        description = "운동장"
                    )
                )
            )
        }
        val preferencesRepository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "3",
                schoolName = "미사중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        ).apply {
            scheduleCache[ScheduleCacheKey("J10", "1234567", "202605")] = cachedEvents
        }
        val repository = SchoolRepositoryImpl(apiService, preferencesRepository, FakeNoticeApiService())

        val emissions = repository.observeSchedules("202605").toList()

        assertEquals(1, emissions.size)
        assertEquals(cachedEvents, emissions[0].getOrThrow())
        assertEquals(cachedEvents, preferencesRepository.savedScheduleCacheEvents)
    }

    @Test
    fun `getSchedules returns failure when both network and cache miss`() = runBlocking {
        val apiService = FakeNeisApiService().apply {
            schedulesResponse = successResponse(emptyList())
            failSchedules = true
        }
        val preferencesRepository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "3",
                schoolName = "미사중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        )
        val repository = SchoolRepositoryImpl(apiService, preferencesRepository, FakeNoticeApiService())

        val result = repository.getSchedules("202605")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getSchedules returns failure when NEIS root result contains validation error`() = runBlocking {
        val apiService = FakeNeisApiService().apply {
            schedulesResponse = errorResponse("INFO-100", "필수 파라미터 누락")
        }
        val preferencesRepository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "3",
                schoolName = "미사중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        )
        val repository = SchoolRepositoryImpl(apiService, preferencesRepository, FakeNoticeApiService())

        val result = repository.getSchedules("202605")

        assertTrue(result.isFailure)
        assertEquals("학사 일정 조회에 필요한 값이 누락되었어요.", result.exceptionOrNull()?.message)
        assertTrue(result.exceptionOrNull() is NeisApiException)
    }

    @Test
    fun `getTimetable uses elementary endpoint for elementary school`() = runBlocking {
        val apiService = FakeNeisApiService().apply {
            elementaryTimetableResponse = successResponse(
                listOf(
                    TimetableRowDto(
                        date = "20260519",
                        period = "1",
                        subject = "국어",
                        grade = "3",
                        classroom = "2"
                    )
                )
            )
        }
        val preferencesRepository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "3",
                classroom = "2",
                schoolName = "미사초등학교",
                officeCode = "J10",
                schoolCode = "7654321",
                schoolKind = "초등학교"
            )
        )
        val repository = SchoolRepositoryImpl(apiService, preferencesRepository, FakeNoticeApiService())

        val result = repository.getTimetable("3", "2", "20260519")

        assertTrue(result.isSuccess)
        assertTrue(apiService.elementaryCalled)
        assertFalse(apiService.middleCalled)
    }

    @Test
    fun `getTimetable uses middle endpoint for middle school`() = runBlocking {
        val apiService = FakeNeisApiService().apply {
            middleTimetableResponse = successResponse(
                listOf(
                    TimetableRowDto(
                        date = "20260519",
                        period = "1",
                        subject = "과학",
                        grade = "3",
                        classroom = "2"
                    )
                )
            )
        }
        val preferencesRepository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "3",
                classroom = "2",
                schoolName = "미사중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        )
        val repository = SchoolRepositoryImpl(apiService, preferencesRepository, FakeNoticeApiService())

        val result = repository.getTimetable("3", "2", "20260519")

        assertTrue(result.isSuccess)
        assertFalse(apiService.elementaryCalled)
        assertTrue(apiService.middleCalled)
    }

    @Test
    fun `getTimetable uses high school endpoint for high school`() = runBlocking {
        val apiService = FakeNeisApiService().apply {
            highTimetableResponse = successResponse(
                listOf(
                    TimetableRowDto(
                        date = "20260519",
                        period = "1",
                        subject = "진로활동",
                        grade = "2",
                        classroom = "4"
                    )
                )
            )
        }
        val preferencesRepository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "2",
                classroom = "4",
                schoolName = "미사고등학교",
                officeCode = "J10",
                schoolCode = "4444444",
                schoolKind = "고등학교"
            )
        )
        val repository = SchoolRepositoryImpl(apiService, preferencesRepository, FakeNoticeApiService())

        val result = repository.getTimetable("2", "4", "20260519")

        assertTrue(result.isSuccess)
        assertFalse(apiService.elementaryCalled)
        assertFalse(apiService.middleCalled)
        assertTrue(apiService.highCalled)
    }

    @Test
    fun `getTimetable replaces cached timetable when network returns empty rows`() = runBlocking {
        val apiService = FakeNeisApiService().apply {
            middleTimetableResponse = successResponse(emptyList())
        }
        val cachedItems = listOf(
            TimetableItem(
                date = "20260519",
                period = "1",
                subject = "과학",
                grade = "3",
                classroom = "2"
            )
        )
        val preferencesRepository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "3",
                classroom = "2",
                schoolName = "미사중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        ).apply {
            timetableCache[TimetableCacheKey("J10", "1234567", "3", "2", "20260519")] = cachedItems
        }
        val repository = SchoolRepositoryImpl(apiService, preferencesRepository, FakeNoticeApiService())

        val result = repository.getTimetable("3", "2", "20260519")

        assertTrue(result.isSuccess)
        assertEquals(emptyList<TimetableItem>(), result.getOrThrow())
        assertEquals(emptyList<TimetableItem>(), preferencesRepository.savedTimetableCacheArgs?.items)
    }

    @Test
    fun `observeTimetable emits cached timetable first and then changed network timetable`() = runBlocking {
        val cachedItems = listOf(
            TimetableItem(
                date = "20260519",
                period = "1",
                subject = "과학",
                grade = "3",
                classroom = "2"
            )
        )
        val networkItems = listOf(
            TimetableItem(
                date = "20260519",
                period = "1",
                subject = "영어",
                grade = "3",
                classroom = "2"
            )
        )
        val apiService = FakeNeisApiService().apply {
            middleTimetableResponse = successResponse(
                listOf(
                    TimetableRowDto(
                        date = "20260519",
                        period = "1",
                        subject = "영어",
                        grade = "3",
                        classroom = "2"
                    )
                )
            )
        }
        val preferencesRepository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "3",
                classroom = "2",
                schoolName = "미사중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        ).apply {
            timetableCache[TimetableCacheKey("J10", "1234567", "3", "2", "20260519")] = cachedItems
        }
        val repository = SchoolRepositoryImpl(apiService, preferencesRepository, FakeNoticeApiService())

        val emissions = repository.observeTimetable("3", "2", "20260519").toList()

        assertEquals(2, emissions.size)
        assertEquals(cachedItems, emissions[0].getOrThrow())
        assertEquals(networkItems, emissions[1].getOrThrow())
        assertEquals(networkItems, preferencesRepository.savedTimetableCacheArgs?.items)
    }

    @Test
    fun `getTimetable returns cached empty timetable when network fails`() = runBlocking {
        val apiService = FakeNeisApiService().apply {
            failMiddleTimetable = true
        }
        val preferencesRepository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "3",
                classroom = "2",
                schoolName = "미사중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        ).apply {
            timetableCache[TimetableCacheKey("J10", "1234567", "3", "2", "20260519")] = emptyList()
        }
        val repository = SchoolRepositoryImpl(apiService, preferencesRepository, FakeNoticeApiService())

        val result = repository.getTimetable("3", "2", "20260519")

        assertTrue(result.isSuccess)
        assertEquals(emptyList<TimetableItem>(), result.getOrThrow())
    }

    @Test
    fun `searchSchools filters to elementary, middle and high schools`() = runBlocking {
        val apiService = FakeNeisApiService().apply {
            schoolInfoResponse = successResponse(
                listOf(
                    SchoolInfoRowDto(
                        officeCode = "J10",
                        officeName = "경기",
                        schoolCode = "1",
                        schoolName = "미사초등학교",
                        schoolKind = "초등학교",
                        location = null,
                        jurisdiction = null,
                        foundation = null,
                        roadAddress = null,
                        telephone = null,
                        homepage = null
                    ),
                    SchoolInfoRowDto(
                        officeCode = "J10",
                        officeName = "경기",
                        schoolCode = "2",
                        schoolName = "미사중학교",
                        schoolKind = "중학교",
                        location = null,
                        jurisdiction = null,
                        foundation = null,
                        roadAddress = null,
                        telephone = null,
                        homepage = null
                    ),
                    SchoolInfoRowDto(
                        officeCode = "J10",
                        officeName = "경기",
                        schoolCode = "3",
                        schoolName = "미사고등학교",
                        schoolKind = "고등학교",
                        location = null,
                        jurisdiction = null,
                        foundation = null,
                        roadAddress = null,
                        telephone = null,
                        homepage = null
                    )
                )
            )
        }
        val repository = SchoolRepositoryImpl(apiService, FakePreferencesRepository(), FakeNoticeApiService())

        val result = repository.searchSchools("미사")

        assertTrue(result.isSuccess)
        assertEquals(
            listOf("초등학교", "중학교", "고등학교"),
            result.getOrThrow().map(SchoolInfo::schoolKind)
        )
    }

    @Test
    fun `getSchedules fails when school selection is missing`() = runBlocking {
        val repository = SchoolRepositoryImpl(FakeNeisApiService(), FakePreferencesRepository(), FakeNoticeApiService())

        val result = repository.getSchedules("202605")

        assertTrue(result.isFailure)
        assertEquals("설정에서 학교를 먼저 선택해 주세요.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getNotices maps notice payload for selected school`() = runBlocking {
        val noticeApiService = FakeNoticeApiService().apply {
            noticesResponse = NoticeListResponseDto(
                items = listOf(
                    NoticeSummaryDto(
                        id = "1",
                        title = "체험학습 안내",
                        date = "2026-05-22",
                        author = "교무실",
                        url = "https://school.example/notices/1",
                        sourceUrl = "https://school.example/notices"
                    )
                )
            )
        }
        val preferencesRepository = FakePreferencesRepository(
            studentInfo = StudentInfo(
                grade = "1",
                classroom = "3",
                schoolName = "미사중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        )
        val repository = SchoolRepositoryImpl(FakeNeisApiService(), preferencesRepository, noticeApiService)

        val result = repository.getNotices(limit = 3)

        assertTrue(result.isSuccess)
        assertEquals("J10", noticeApiService.lastOfficeCode)
        assertEquals("1234567", noticeApiService.lastSchoolCode)
        assertEquals(3, noticeApiService.lastLimit)
        assertEquals(
            NoticeFeed(
                items = listOf(
                    NoticePreview(
                        id = "1",
                        title = "체험학습 안내",
                        date = "2026-05-22",
                        author = "교무실",
                        url = "https://school.example/notices/1",
                        sourceUrl = "https://school.example/notices"
                    )
                ),
                message = null
            ),
            result.getOrThrow()
        )
    }

    private class FakeNeisApiService : NeisApiService {
        var mealsResponse: NeisResponse<MealRowDto> = successResponse(emptyList())
        var schedulesResponse: NeisResponse<ScheduleRowDto> = successResponse(emptyList())
        var elementaryTimetableResponse: NeisResponse<TimetableRowDto> = successResponse(emptyList())
        var middleTimetableResponse: NeisResponse<TimetableRowDto> = successResponse(emptyList())
        var highTimetableResponse: NeisResponse<TimetableRowDto> = successResponse(emptyList())
        var schoolInfoResponse: NeisResponse<SchoolInfoRowDto> = successResponse(emptyList())
        var failSchedules = false
        var failElementaryTimetable = false
        var failMiddleTimetable = false
        var failHighTimetable = false

        var lastMealOfficeCode: String? = null
        var lastMealSchoolCode: String? = null
        var lastScheduleOfficeCode: String? = null
        var lastScheduleSchoolCode: String? = null
        var elementaryCalled = false
        var middleCalled = false
        var highCalled = false

        override suspend fun getMeals(
            apiKey: String,
            type: String,
            pageIndex: Int,
            pageSize: Int,
            officeCode: String,
            schoolCode: String,
            date: String?
        ): NeisResponse<MealRowDto> {
            lastMealOfficeCode = officeCode
            lastMealSchoolCode = schoolCode
            return mealsResponse
        }

        override suspend fun getSchedules(
            apiKey: String,
            type: String,
            pageIndex: Int,
            pageSize: Int,
            officeCode: String,
            schoolCode: String,
            date: String?
        ): NeisResponse<ScheduleRowDto> {
            if (failSchedules) throw IllegalStateException("schedule api error")
            lastScheduleOfficeCode = officeCode
            lastScheduleSchoolCode = schoolCode
            return schedulesResponse
        }

        override suspend fun getElementaryTimetable(
            apiKey: String,
            type: String,
            pageIndex: Int,
            pageSize: Int,
            officeCode: String,
            schoolCode: String,
            grade: String,
            classroom: String,
            date: String?
        ): NeisResponse<TimetableRowDto> {
            if (failElementaryTimetable) throw IllegalStateException("elementary timetable api error")
            elementaryCalled = true
            return elementaryTimetableResponse
        }

        override suspend fun getMiddleTimetable(
            apiKey: String,
            type: String,
            pageIndex: Int,
            pageSize: Int,
            officeCode: String,
            schoolCode: String,
            grade: String,
            classroom: String,
            date: String?
        ): NeisResponse<TimetableRowDto> {
            if (failMiddleTimetable) throw IllegalStateException("timetable api error")
            middleCalled = true
            return middleTimetableResponse
        }

        override suspend fun getHighTimetable(
            apiKey: String,
            type: String,
            pageIndex: Int,
            pageSize: Int,
            officeCode: String,
            schoolCode: String,
            grade: String,
            classroom: String,
            date: String?
        ): NeisResponse<TimetableRowDto> {
            if (failHighTimetable) throw IllegalStateException("high timetable api error")
            highCalled = true
            return highTimetableResponse
        }

        override suspend fun getSchools(
            apiKey: String,
            type: String,
            pageIndex: Int,
            pageSize: Int,
            query: String
        ): NeisResponse<SchoolInfoRowDto> = schoolInfoResponse
    }

    private class FakeNoticeApiService : NoticeApiService {
        var noticesResponse: NoticeListResponseDto = NoticeListResponseDto()
        var lastOfficeCode: String? = null
        var lastSchoolCode: String? = null
        var lastLimit: Int? = null

        override suspend fun getNotices(
            officeCode: String,
            schoolCode: String,
            limit: Int
        ): NoticeListResponseDto {
            lastOfficeCode = officeCode
            lastSchoolCode = schoolCode
            lastLimit = limit
            return noticesResponse
        }
    }

    private class FakePreferencesRepository(
        private var studentInfo: StudentInfo = StudentInfo()
    ) : PreferencesRepository {
        var savedMealCacheArgs: MealCacheArgs? = null
        var savedScheduleCacheEvents: List<SchoolEvent>? = null
        var savedTimetableCacheArgs: TimetableCacheArgs? = null
        val mealCache = mutableMapOf<MealCacheKey, List<MealInfo>>()
        val scheduleCache = mutableMapOf<ScheduleCacheKey, List<SchoolEvent>>()
        val timetableCache = mutableMapOf<TimetableCacheKey, List<TimetableItem>>()

        override fun getStudentInfo(): StudentInfo = studentInfo

        override fun hasStudentInfo(): Boolean = studentInfo.isComplete()

        override fun saveStudentInfo(studentInfo: StudentInfo) {
            this.studentInfo = studentInfo
        }

        override fun getTimerDisplayMode(): TimerDisplayMode = TimerDisplayMode.COUNT

        override fun saveTimerDisplayMode(displayMode: TimerDisplayMode) = Unit

        override fun isTimerNotificationEnabled(): Boolean = true

        override fun saveTimerNotificationEnabled(enabled: Boolean) = Unit

        override fun isTimerVibrationEnabled(): Boolean = true

        override fun saveTimerVibrationEnabled(enabled: Boolean) = Unit

        override fun getTimerState(): TimerPreferenceState {
            return TimerPreferenceState("FOCUS", 0L, 0L, 0L, false)
        }

        override fun saveTimerState(
            presetName: String,
            totalMillis: Long,
            remainingMillis: Long,
            targetAtMillis: Long,
            isRunning: Boolean
        ) = Unit

        override fun clearTimerState() = Unit

        override fun saveMealCache(
            officeCode: String,
            schoolCode: String,
            date: String,
            meals: List<MealInfo>
        ) {
            savedMealCacheArgs = MealCacheArgs(officeCode, schoolCode, date, meals)
            mealCache[MealCacheKey(officeCode, schoolCode, date)] = meals
        }

        override fun getMealCache(
            officeCode: String,
            schoolCode: String,
            date: String
        ): List<MealInfo>? = mealCache[MealCacheKey(officeCode, schoolCode, date)]

        override fun saveScheduleCache(
            officeCode: String,
            schoolCode: String,
            date: String,
            events: List<SchoolEvent>
        ) {
            savedScheduleCacheEvents = events
            scheduleCache[ScheduleCacheKey(officeCode, schoolCode, date)] = events
        }

        override fun getScheduleCache(
            officeCode: String,
            schoolCode: String,
            date: String
        ): List<SchoolEvent>? {
            return scheduleCache[ScheduleCacheKey(officeCode, schoolCode, date)]
        }

        override fun saveTimetableCache(
            officeCode: String,
            schoolCode: String,
            grade: String,
            classroom: String,
            date: String,
            items: List<TimetableItem>
        ) {
            savedTimetableCacheArgs = TimetableCacheArgs(officeCode, schoolCode, grade, classroom, date, items)
            timetableCache[TimetableCacheKey(officeCode, schoolCode, grade, classroom, date)] = items
        }

        override fun getTimetableCache(
            officeCode: String,
            schoolCode: String,
            grade: String,
            classroom: String,
            date: String
        ): List<TimetableItem>? {
            return timetableCache[TimetableCacheKey(officeCode, schoolCode, grade, classroom, date)]
        }

        override fun getWidgetSettings(appWidgetId: Int): WidgetSettings = WidgetSettings()

        override fun saveWidgetSettings(appWidgetId: Int, settings: WidgetSettings) = Unit

        override fun clearWidgetSettings(appWidgetId: Int) = Unit
    }

    private data class MealCacheArgs(
        val officeCode: String,
        val schoolCode: String,
        val date: String,
        val meals: List<MealInfo>
    )

    private data class MealCacheKey(
        val officeCode: String,
        val schoolCode: String,
        val date: String
    )

    private data class ScheduleCacheKey(
        val officeCode: String,
        val schoolCode: String,
        val date: String
    )

    private data class TimetableCacheArgs(
        val officeCode: String,
        val schoolCode: String,
        val grade: String,
        val classroom: String,
        val date: String,
        val items: List<TimetableItem>
    )

    private data class TimetableCacheKey(
        val officeCode: String,
        val schoolCode: String,
        val grade: String,
        val classroom: String,
        val date: String
    )
}
