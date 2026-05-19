package com.bsbarron.midschoolapp.data.repository

import com.bsbarron.midschoolapp.data.model.MealInfo
<<<<<<< HEAD
import com.bsbarron.midschoolapp.data.model.SchoolEvent
=======
>>>>>>> 46966ee (task: add android school selection settings)
import com.bsbarron.midschoolapp.data.model.TimetableItem
import com.bsbarron.midschoolapp.data.remote.NeisApiService
import com.bsbarron.midschoolapp.data.remote.dto.MealRowDto
import com.bsbarron.midschoolapp.data.remote.dto.NeisHeadDto
import com.bsbarron.midschoolapp.data.remote.dto.NeisResponse
import com.bsbarron.midschoolapp.data.remote.dto.NeisResultDto
import com.bsbarron.midschoolapp.data.remote.dto.NeisSection
import com.bsbarron.midschoolapp.data.remote.dto.ScheduleRowDto
<<<<<<< HEAD
import com.bsbarron.midschoolapp.data.remote.dto.TimetableRowDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        schoolInfo = sections
    )
}

class SchoolRepositoryImplTest {

    @Test
    fun `getMeals uses selected school codes`() = runBlocking {
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
        val repository = SchoolRepositoryImpl(apiService, preferencesRepository)

        val result = repository.getMeals("20260519")

        assertTrue(result.isSuccess)
        assertEquals("J10", apiService.lastMealOfficeCode)
        assertEquals("1234567", apiService.lastMealSchoolCode)
        assertEquals("J10", preferencesRepository.savedMealCacheArgs?.officeCode)
        assertEquals("1234567", preferencesRepository.savedMealCacheArgs?.schoolCode)
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
        val repository = SchoolRepositoryImpl(apiService, preferencesRepository)

        val result = repository.getTimetable("3", "2", "20260519")

        assertTrue(result.isSuccess)
        assertTrue(apiService.elementaryCalled)
        assertFalse(apiService.middleCalled)
    }

    @Test
    fun `searchSchools filters to elementary and middle schools`() = runBlocking {
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
        val repository = SchoolRepositoryImpl(apiService, FakePreferencesRepository())

        val result = repository.searchSchools("미사")

        assertTrue(result.isSuccess)
        assertEquals(listOf("초등학교", "중학교"), result.getOrThrow().map { it.schoolKind })
    }

    @Test
    fun `getSchedules fails when school selection is missing`() = runBlocking {
        val repository = SchoolRepositoryImpl(FakeNeisApiService(), FakePreferencesRepository())

        val result = repository.getSchedules("202605")

        assertTrue(result.isFailure)
        assertEquals("설정에서 학교를 먼저 선택해 주세요.", result.exceptionOrNull()?.message)
    }

    private class FakeNeisApiService : NeisApiService {
        var mealsResponse: NeisResponse<MealRowDto> = NeisResponse()
        var schedulesResponse: NeisResponse<ScheduleRowDto> = NeisResponse()
        var timetableResponse: NeisResponse<TimetableRowDto> = NeisResponse()

        var lastMealsRequest: MealsRequest? = null
        var lastSchedulesRequest: SchedulesRequest? = null
        var lastTimetableRequest: TimetableRequest? = null
=======
import com.bsbarron.midschoolapp.data.remote.dto.SchoolInfoRowDto
import com.bsbarron.midschoolapp.data.remote.dto.TimetableRowDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        schoolInfo = sections
    )
}

class SchoolRepositoryImplTest {

    @Test
    fun `getMeals uses selected school codes`() = runBlocking {
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
        val repository = SchoolRepositoryImpl(apiService, preferencesRepository)

        val result = repository.getMeals("20260519")

        assertTrue(result.isSuccess)
        assertEquals("J10", apiService.lastMealOfficeCode)
        assertEquals("1234567", apiService.lastMealSchoolCode)
        assertEquals("J10", preferencesRepository.savedMealCacheArgs?.officeCode)
        assertEquals("1234567", preferencesRepository.savedMealCacheArgs?.schoolCode)
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
        val repository = SchoolRepositoryImpl(apiService, preferencesRepository)

        val result = repository.getTimetable("3", "2", "20260519")

        assertTrue(result.isSuccess)
        assertTrue(apiService.elementaryCalled)
        assertFalse(apiService.middleCalled)
    }

    @Test
    fun `searchSchools filters to elementary and middle schools`() = runBlocking {
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
        val repository = SchoolRepositoryImpl(apiService, FakePreferencesRepository())

        val result = repository.searchSchools("미사")

        assertTrue(result.isSuccess)
        assertEquals(listOf("초등학교", "중학교"), result.getOrThrow().map { it.schoolKind })
    }

    @Test
    fun `getSchedules fails when school selection is missing`() = runBlocking {
        val repository = SchoolRepositoryImpl(FakeNeisApiService(), FakePreferencesRepository())

        val result = repository.getSchedules("202605")

        assertTrue(result.isFailure)
        assertEquals("설정에서 학교를 먼저 선택해 주세요.", result.exceptionOrNull()?.message)
    }

    private class FakeNeisApiService : NeisApiService {
        var mealsResponse: NeisResponse<MealRowDto> = successResponse(emptyList())
        var scheduleResponse: NeisResponse<ScheduleRowDto> = successResponse(emptyList())
        var elementaryTimetableResponse: NeisResponse<TimetableRowDto> = successResponse(emptyList())
        var middleTimetableResponse: NeisResponse<TimetableRowDto> = successResponse(emptyList())
        var schoolInfoResponse: NeisResponse<SchoolInfoRowDto> = successResponse(emptyList())
        var lastMealOfficeCode: String? = null
        var lastMealSchoolCode: String? = null
        var elementaryCalled = false
        var middleCalled = false
>>>>>>> 46966ee (task: add android school selection settings)

        override suspend fun getMeals(
            apiKey: String,
            type: String,
            pageIndex: Int,
            pageSize: Int,
            officeCode: String,
            schoolCode: String,
            date: String?
        ): NeisResponse<MealRowDto> {
<<<<<<< HEAD
            lastMealsRequest = MealsRequest(
                officeCode = officeCode,
                schoolCode = schoolCode,
                date = date
            )
=======
            lastMealOfficeCode = officeCode
            lastMealSchoolCode = schoolCode
>>>>>>> 46966ee (task: add android school selection settings)
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
<<<<<<< HEAD
        ): NeisResponse<ScheduleRowDto> {
            lastSchedulesRequest = SchedulesRequest(
                officeCode = officeCode,
                schoolCode = schoolCode,
                date = date
            )
            return schedulesResponse
        }

        override suspend fun getTimetable(
=======
        ): NeisResponse<ScheduleRowDto> = scheduleResponse

        override suspend fun getElementaryTimetable(
>>>>>>> 46966ee (task: add android school selection settings)
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
<<<<<<< HEAD
            lastTimetableRequest = TimetableRequest(
                officeCode = officeCode,
                schoolCode = schoolCode,
                grade = grade,
                classroom = classroom,
                date = date
            )
            return timetableResponse
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
            middleCalled = true
            return middleTimetableResponse
        }

        override suspend fun getSchools(
            apiKey: String,
            type: String,
            pageIndex: Int,
            pageSize: Int,
            query: String
        ): NeisResponse<SchoolInfoRowDto> = schoolInfoResponse
    }
=======
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
            middleCalled = true
            return middleTimetableResponse
        }

        override suspend fun getSchools(
            apiKey: String,
            type: String,
            pageIndex: Int,
            pageSize: Int,
            query: String
        ): NeisResponse<SchoolInfoRowDto> = schoolInfoResponse
    }

    private class FakePreferencesRepository(
        private var studentInfo: StudentInfo = StudentInfo()
    ) : PreferencesRepository {
        var savedMealCacheArgs: MealCacheArgs? = null

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
        }

        override fun getMealCache(
            officeCode: String,
            schoolCode: String,
            date: String
        ): List<MealInfo>? = null

        override fun saveTimetableCache(
            officeCode: String,
            schoolCode: String,
            grade: String,
            classroom: String,
            date: String,
            items: List<TimetableItem>
        ) = Unit

        override fun getTimetableCache(
            officeCode: String,
            schoolCode: String,
            grade: String,
            classroom: String,
            date: String
        ): List<TimetableItem>? = null

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
>>>>>>> 46966ee (task: add android school selection settings)
}
