package com.bsbarron.midschoolapp.test

import com.bsbarron.midschoolapp.data.model.MealInfo
import com.bsbarron.midschoolapp.data.model.SchoolEvent
import com.bsbarron.midschoolapp.data.model.SchoolInfo
import com.bsbarron.midschoolapp.data.model.TimetableItem
import com.bsbarron.midschoolapp.data.repository.SchoolRepository

class FakeSchoolRepository(
    var schoolSearchResult: Result<List<SchoolInfo>> = Result.success(emptyList()),
    var mealsResult: Result<List<MealInfo>> = Result.success(emptyList()),
    var schedulesResult: Result<List<SchoolEvent>> = Result.success(emptyList()),
    var timetableResult: Result<List<TimetableItem>> = Result.success(emptyList())
) : SchoolRepository {
    var lastSearchQuery: String? = null
        private set

    override suspend fun searchSchools(query: String): Result<List<SchoolInfo>> {
        lastSearchQuery = query
        return schoolSearchResult
    }

    override suspend fun getMeals(date: String?): Result<List<MealInfo>> = mealsResult

    override suspend fun getSchedules(date: String?): Result<List<SchoolEvent>> = schedulesResult

    override suspend fun getTimetable(
        grade: String,
        classroom: String,
        date: String?
    ): Result<List<TimetableItem>> = timetableResult
}
