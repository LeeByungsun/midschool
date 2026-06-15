package com.lbs.schoolhelper.test

import com.lbs.schoolhelper.data.model.MealInfo
import com.lbs.schoolhelper.data.model.NoticeFeed
import com.lbs.schoolhelper.data.model.SchoolEvent
import com.lbs.schoolhelper.data.model.SchoolInfo
import com.lbs.schoolhelper.data.model.TimetableItem
import com.lbs.schoolhelper.data.repository.SchoolRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

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
    val mealFlowResultsByDate = mutableMapOf<String?, List<Result<List<MealInfo>>>>()
    val mealDelayMillisByDate = mutableMapOf<String?, Long>()
    val mealFlowEmissionDelayMillisByDate = mutableMapOf<String?, Long>()
    var schedulesCallCount: Int = 0
        private set
    val requestedScheduleDates = mutableListOf<String?>()
    val scheduleFlowResultsByDate = mutableMapOf<String?, List<Result<List<SchoolEvent>>>>()
    val scheduleFlowEmissionDelayMillisByDate = mutableMapOf<String?, Long>()
    var noticesCallCount: Int = 0
        private set
    var lastNoticeLimit: Int? = null
        private set
    val requestedTimetableArgs = mutableListOf<TimetableRequest>()
    val timetableFlowResultsByRequest = mutableMapOf<TimetableRequest, List<Result<List<TimetableItem>>>>()
    val timetableFlowEmissionDelayMillisByRequest = mutableMapOf<TimetableRequest, Long>()

    override suspend fun searchSchools(query: String): Result<List<SchoolInfo>> {
        lastSearchQuery = query
        requestedSearchQueries += query
        searchDelayMillisByQuery[query]?.takeIf { it > 0L }?.let { delay(it) }
        return searchResultsByQuery[query] ?: schoolSearchResult
    }

    override suspend fun getMeals(date: String?): Result<List<MealInfo>> {
        mealsCallCount += 1
        requestedMealDates += date
        mealDelayMillisByDate[date]?.takeIf { it > 0L }?.let { delay(it) }
        return mealResultsByDate[date] ?: mealsResult
    }

    override fun observeMeals(date: String?): Flow<Result<List<MealInfo>>> = flow {
        val flowResults = mealFlowResultsByDate[date]
        if (flowResults == null) {
            emit(getMeals(date))
            return@flow
        }

        mealsCallCount += 1
        requestedMealDates += date
        flowResults.forEachIndexed { index, result ->
            if (index > 0) {
                mealFlowEmissionDelayMillisByDate[date]?.takeIf { it > 0L }?.let { delay(it) }
            }
            emit(result)
        }
    }

    override suspend fun getSchedules(date: String?): Result<List<SchoolEvent>> {
        schedulesCallCount += 1
        requestedScheduleDates += date
        return schedulesResult
    }

    override fun observeSchedules(date: String?): Flow<Result<List<SchoolEvent>>> = flow {
        val flowResults = scheduleFlowResultsByDate[date]
        if (flowResults == null) {
            emit(getSchedules(date))
            return@flow
        }

        schedulesCallCount += 1
        requestedScheduleDates += date
        flowResults.forEachIndexed { index, result ->
            if (index > 0) {
                scheduleFlowEmissionDelayMillisByDate[date]?.takeIf { it > 0L }?.let { delay(it) }
            }
            emit(result)
        }
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
    ): Result<List<TimetableItem>> {
        requestedTimetableArgs += TimetableRequest(grade, classroom, date)
        return timetableResult
    }

    override fun observeTimetable(
        grade: String,
        classroom: String,
        date: String?
    ): Flow<Result<List<TimetableItem>>> = flow {
        val request = TimetableRequest(grade, classroom, date)
        val flowResults = timetableFlowResultsByRequest[request]
        if (flowResults == null) {
            emit(getTimetable(grade, classroom, date))
            return@flow
        }

        requestedTimetableArgs += request
        flowResults.forEachIndexed { index, result ->
            if (index > 0) {
                timetableFlowEmissionDelayMillisByRequest[request]?.takeIf { it > 0L }?.let { delay(it) }
            }
            emit(result)
        }
    }

    data class TimetableRequest(
        val grade: String,
        val classroom: String,
        val date: String?
    )
}
