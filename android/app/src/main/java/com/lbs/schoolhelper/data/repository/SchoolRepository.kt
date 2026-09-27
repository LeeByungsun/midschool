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
    suspend fun getMeals(student: StudentInfo, date: String? = null): Result<List<MealInfo>>
    suspend fun getSchedules(student: StudentInfo, date: String? = null): Result<List<SchoolEvent>>
    suspend fun getNotices(student: StudentInfo, limit: Int = 3): Result<NoticeFeed>
    suspend fun getTimetable(student: StudentInfo, date: String? = null): Result<List<TimetableItem>>

    fun observeMeals(student: StudentInfo, date: String? = null): Flow<Result<List<MealInfo>>> = flow {
        emit(getMeals(student, date))
    }

    fun observeSchedules(student: StudentInfo, date: String? = null): Flow<Result<List<SchoolEvent>>> = flow {
        emit(getSchedules(student, date))
    }

    fun observeTimetable(
        student: StudentInfo,
        date: String? = null
    ): Flow<Result<List<TimetableItem>>> = flow {
        emit(getTimetable(student, date))
    }
}
