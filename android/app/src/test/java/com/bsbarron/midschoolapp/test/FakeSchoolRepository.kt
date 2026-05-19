package com.bsbarron.midschoolapp.test

import com.bsbarron.midschoolapp.data.model.MealInfo
import com.bsbarron.midschoolapp.data.model.SchoolEvent
import com.bsbarron.midschoolapp.data.model.SchoolInfo
import com.bsbarron.midschoolapp.data.model.TimetableItem
import com.bsbarron.midschoolapp.data.repository.SchoolRepository

class FakeSchoolRepository : SchoolRepository {
    var searchResult: Result<List<SchoolInfo>> = Result.success(emptyList())
    var mealsResult: Result<List<MealInfo>> = Result.success(emptyList())
    var schedulesResult: Result<List<SchoolEvent>> = Result.success(emptyList())
    var timetableResult: Result<List<TimetableItem>> = Result.success(emptyList())

    val searchQueries = mutableListOf<String>()

    override suspend fun searchSchools(query: String): Result<List<SchoolInfo>> {
        searchQueries += query
        return searchResult
    }

    override suspend fun getMeals(date: String?): Result<List<MealInfo>> = mealsResult

    override suspend fun getSchedules(date: String?): Result<List<SchoolEvent>> = schedulesResult

    override suspend fun getTimetable(
        grade: String,
        classroom: String,
        date: String?
    ): Result<List<TimetableItem>> = timetableResult
}
