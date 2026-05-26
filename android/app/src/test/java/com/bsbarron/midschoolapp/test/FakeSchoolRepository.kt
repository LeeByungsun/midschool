package com.bsbarron.midschoolapp.test

import com.bsbarron.midschoolapp.data.model.MealInfo
import com.bsbarron.midschoolapp.data.model.NoticeFeed
import com.bsbarron.midschoolapp.data.model.SchoolEvent
import com.bsbarron.midschoolapp.data.model.SchoolInfo
import com.bsbarron.midschoolapp.data.model.TimetableItem
import com.bsbarron.midschoolapp.data.repository.SchoolRepository
import kotlinx.coroutines.delay

class FakeSchoolRepository(
    var schoolSearchResult: Result<List<SchoolInfo>> = Result.success(emptyList()),
    var mealsResult: Result<List<MealInfo>> = Result.success(emptyList()),
    var schedulesResult: Result<List<SchoolEvent>> = Result.success(emptyList()),
    var noticesResult: Result<NoticeFeed> = Result.success(NoticeFeed()),
    var timetableResult: Result<List<TimetableItem>> = Result.success(emptyList())
) : SchoolRepository {
    var lastSearchQuery: String? = null
        private set
    val requestedSearchQueries = mutableListOf<String>()
    val searchResultsByQuery = mutableMapOf<String, Result<List<SchoolInfo>>>()
    val searchDelayMillisByQuery = mutableMapOf<String, Long>()
    var mealsCallCount: Int = 0
        private set
    val requestedMealDates = mutableListOf<String?>()
    val mealResultsByDate = mutableMapOf<String?, Result<List<MealInfo>>>()
    var schedulesCallCount: Int = 0
        private set
    var noticesCallCount: Int = 0
        private set
    var lastNoticeLimit: Int? = null
        private set

    override suspend fun searchSchools(query: String): Result<List<SchoolInfo>> {
        lastSearchQuery = query
        requestedSearchQueries += query
        searchDelayMillisByQuery[query]?.takeIf { it > 0L }?.let { delay(it) }
        return searchResultsByQuery[query] ?: schoolSearchResult
    }

    override suspend fun getMeals(date: String?): Result<List<MealInfo>> {
        mealsCallCount += 1
        requestedMealDates += date
        return mealResultsByDate[date] ?: mealsResult
    }

    override suspend fun getSchedules(date: String?): Result<List<SchoolEvent>> {
        schedulesCallCount += 1
        return schedulesResult
    }

    override suspend fun getNotices(limit: Int): Result<NoticeFeed> {
        noticesCallCount += 1
        lastNoticeLimit = limit
        return noticesResult
    }

    override suspend fun getTimetable(
        grade: String,
        classroom: String,
        date: String?
    ): Result<List<TimetableItem>> = timetableResult
}
