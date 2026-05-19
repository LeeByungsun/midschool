package com.bsbarron.midschoolapp.data.repository

import com.bsbarron.midschoolapp.data.model.MealInfo
import com.bsbarron.midschoolapp.data.model.SchoolEvent
import com.bsbarron.midschoolapp.data.model.TimetableItem
import com.bsbarron.midschoolapp.data.remote.NeisApiService
import com.bsbarron.midschoolapp.data.remote.dto.MealRowDto
import com.bsbarron.midschoolapp.data.remote.dto.NeisHeadDto
import com.bsbarron.midschoolapp.data.remote.dto.NeisResponse
import com.bsbarron.midschoolapp.data.remote.dto.NeisResultDto
import com.bsbarron.midschoolapp.data.remote.dto.NeisSection
import com.bsbarron.midschoolapp.data.remote.dto.ScheduleRowDto
import com.bsbarron.midschoolapp.data.remote.dto.TimetableRowDto
import com.bsbarron.midschoolapp.test.FakePreferencesRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SchoolRepositoryImplTest {

    @Test
    fun getMeals_usesInjectedSchoolCodesAndCachesMealsByDate() = runBlocking {
        val apiService = FakeNeisApiService().apply {
            mealsResponse = mealResponse(
                MealRowDto(
                    mealDate = "20260519",
                    mealTypeName = "중식",
                    menu = "김밥",
                    calorieInfo = "700kcal"
                )
            )
        }
        val preferencesRepository = FakePreferencesRepository()
        val repository = SchoolRepositoryImpl(
            apiService = apiService,
            preferencesRepository = preferencesRepository,
            officeCode = "J01",
            schoolCode = "1234567"
        )

        val result = repository.getMeals("20260519")

        assertEquals(MealsRequest(officeCode = "J01", schoolCode = "1234567", date = "20260519"), apiService.lastMealsRequest)
        assertEquals(
            listOf(
                MealInfo(
                    date = "20260519",
                    mealType = "중식",
                    menu = "김밥",
                    calorieInfo = "700kcal"
                )
            ),
            result.getOrThrow()
        )
        assertEquals(result.getOrThrow(), preferencesRepository.getMealCache("20260519"))
    }

    @Test
    fun getSchedules_usesInjectedSchoolCodesForScheduleRequests() = runBlocking {
        val apiService = FakeNeisApiService().apply {
            schedulesResponse = scheduleResponse(
                ScheduleRowDto(
                    date = "20260519",
                    title = "체육대회",
                    description = "운동장"
                )
            )
        }
        val repository = SchoolRepositoryImpl(
            apiService = apiService,
            preferencesRepository = FakePreferencesRepository(),
            officeCode = "B10",
            schoolCode = "7654321"
        )

        val result = repository.getSchedules("202605")

        assertEquals(SchedulesRequest(officeCode = "B10", schoolCode = "7654321", date = "202605"), apiService.lastSchedulesRequest)
        assertEquals(
            listOf(
                SchoolEvent(
                    date = "20260519",
                    title = "체육대회",
                    description = "운동장"
                )
            ),
            result.getOrThrow()
        )
    }

    @Test
    fun getTimetable_usesInjectedSchoolCodesAndCachesByStudentAndDate() = runBlocking {
        val apiService = FakeNeisApiService().apply {
            timetableResponse = timetableResponse(
                TimetableRowDto(
                    date = "20260519",
                    period = "1",
                    subject = "수학",
                    grade = "2",
                    classroom = "3"
                )
            )
        }
        val preferencesRepository = FakePreferencesRepository()
        val repository = SchoolRepositoryImpl(
            apiService = apiService,
            preferencesRepository = preferencesRepository,
            officeCode = "C10",
            schoolCode = "2468101"
        )

        val result = repository.getTimetable(
            grade = "2",
            classroom = "3",
            date = "20260519"
        )

        assertEquals(
            TimetableRequest(
                officeCode = "C10",
                schoolCode = "2468101",
                grade = "2",
                classroom = "3",
                date = "20260519"
            ),
            apiService.lastTimetableRequest
        )
        assertEquals(
            listOf(
                TimetableItem(
                    date = "20260519",
                    period = "1",
                    subject = "수학",
                    grade = "2",
                    classroom = "3"
                )
            ),
            result.getOrThrow()
        )
        assertEquals(
            result.getOrThrow(),
            preferencesRepository.getTimetableCache("2", "3", "20260519")
        )
    }

    private fun mealResponse(vararg rows: MealRowDto): NeisResponse<MealRowDto> {
        return NeisResponse(
            mealServiceDietInfo = listOf(
                successHeadSection(),
                NeisSection(row = rows.toList())
            )
        )
    }

    private fun scheduleResponse(vararg rows: ScheduleRowDto): NeisResponse<ScheduleRowDto> {
        return NeisResponse(
            schoolSchedule = listOf(
                successHeadSection(),
                NeisSection(row = rows.toList())
            )
        )
    }

    private fun timetableResponse(vararg rows: TimetableRowDto): NeisResponse<TimetableRowDto> {
        return NeisResponse(
            misTimetable = listOf(
                successHeadSection(),
                NeisSection(row = rows.toList())
            )
        )
    }

    private fun <T> successHeadSection(): NeisSection<T> {
        return NeisSection(
            head = listOf(
                NeisHeadDto(
                    result = NeisResultDto(code = "INFO-000", message = "OK")
                )
            )
        )
    }

    private data class MealsRequest(
        val officeCode: String,
        val schoolCode: String,
        val date: String?
    )

    private data class SchedulesRequest(
        val officeCode: String,
        val schoolCode: String,
        val date: String?
    )

    private data class TimetableRequest(
        val officeCode: String,
        val schoolCode: String,
        val grade: String,
        val classroom: String,
        val date: String?
    )

    private class FakeNeisApiService : NeisApiService {
        var mealsResponse: NeisResponse<MealRowDto> = NeisResponse()
        var schedulesResponse: NeisResponse<ScheduleRowDto> = NeisResponse()
        var timetableResponse: NeisResponse<TimetableRowDto> = NeisResponse()

        var lastMealsRequest: MealsRequest? = null
        var lastSchedulesRequest: SchedulesRequest? = null
        var lastTimetableRequest: TimetableRequest? = null

        override suspend fun getMeals(
            apiKey: String,
            type: String,
            pageIndex: Int,
            pageSize: Int,
            officeCode: String,
            schoolCode: String,
            date: String?
        ): NeisResponse<MealRowDto> {
            lastMealsRequest = MealsRequest(
                officeCode = officeCode,
                schoolCode = schoolCode,
                date = date
            )
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
            lastSchedulesRequest = SchedulesRequest(
                officeCode = officeCode,
                schoolCode = schoolCode,
                date = date
            )
            return schedulesResponse
        }

        override suspend fun getTimetable(
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
            lastTimetableRequest = TimetableRequest(
                officeCode = officeCode,
                schoolCode = schoolCode,
                grade = grade,
                classroom = classroom,
                date = date
            )
            return timetableResponse
        }
    }
}
