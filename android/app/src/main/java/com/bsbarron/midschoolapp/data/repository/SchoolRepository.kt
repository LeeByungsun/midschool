package com.bsbarron.midschoolapp.data.repository

import com.bsbarron.midschoolapp.data.model.MealInfo
import com.bsbarron.midschoolapp.data.model.SchoolEvent
import com.bsbarron.midschoolapp.data.model.SchoolInfo
import com.bsbarron.midschoolapp.data.model.TimetableItem

interface SchoolRepository {
    suspend fun searchSchools(query: String): Result<List<SchoolInfo>>
    suspend fun getMeals(date: String? = null): Result<List<MealInfo>>
    suspend fun getSchedules(date: String? = null): Result<List<SchoolEvent>>
    suspend fun getTimetable(grade: String, classroom: String, date: String? = null): Result<List<TimetableItem>>
}
