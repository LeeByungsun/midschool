package com.lbs.schoolhelper.data.repository

import com.lbs.schoolhelper.data.model.MealInfo
import com.lbs.schoolhelper.data.model.NoticeFeed
import com.lbs.schoolhelper.data.model.SchoolEvent
import com.lbs.schoolhelper.data.model.SchoolInfo
import com.lbs.schoolhelper.data.model.TimetableItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface SchoolRepository {
    suspend fun searchSchools(query: String): Result<List<SchoolInfo>>
    suspend fun getMeals(date: String? = null): Result<List<MealInfo>>
    suspend fun getSchedules(date: String? = null): Result<List<SchoolEvent>>
    suspend fun getNotices(limit: Int = 3): Result<NoticeFeed>
    suspend fun getTimetable(grade: String, classroom: String, date: String? = null): Result<List<TimetableItem>>

    fun observeMeals(date: String? = null): Flow<Result<List<MealInfo>>> = flow {
        emit(getMeals(date))
    }

    fun observeSchedules(date: String? = null): Flow<Result<List<SchoolEvent>>> = flow {
        emit(getSchedules(date))
    }

    fun observeTimetable(
        grade: String,
        classroom: String,
        date: String? = null
    ): Flow<Result<List<TimetableItem>>> = flow {
        emit(getTimetable(grade, classroom, date))
    }
}
