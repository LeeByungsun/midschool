package com.bsbarron.midschoolapp.data.repository

import com.bsbarron.midschoolapp.data.model.MealInfo
import com.bsbarron.midschoolapp.data.model.SchoolEvent
import com.bsbarron.midschoolapp.data.model.TimetableItem
import com.bsbarron.midschoolapp.data.remote.NeisApiService
import javax.inject.Inject
import javax.inject.Named

class SchoolRepositoryImpl @Inject constructor(
    private val apiService: NeisApiService,
    @param:Named("officeCode") private val officeCode: String,
    @param:Named("schoolCode") private val schoolCode: String
) : SchoolRepository {

    override suspend fun getMeals(date: String?): Result<List<MealInfo>> = runCatching {
        apiService.getMeals(
            officeCode = officeCode,
            schoolCode = schoolCode,
            date = date
        ).mealServiceDietInfo
            ?.getOrNull(1)
            ?.row
            .orEmpty()
            .map { row ->
                MealInfo(
                    date = row.mealDate,
                    mealType = row.mealTypeName.orEmpty(),
                    menu = row.menu.orEmpty(),
                    calorieInfo = row.calorieInfo.orEmpty()
                )
            }
    }

    override suspend fun getSchedules(date: String?): Result<List<SchoolEvent>> = runCatching {
        apiService.getSchedules(
            officeCode = officeCode,
            schoolCode = schoolCode,
            date = date
        ).schoolSchedule
            ?.getOrNull(1)
            ?.row
            .orEmpty()
            .map { row ->
                SchoolEvent(
                    date = row.date,
                    title = row.title.orEmpty(),
                    description = row.description.orEmpty()
                )
            }
    }

    override suspend fun getTimetable(
        grade: String,
        classroom: String,
        date: String?
    ): Result<List<TimetableItem>> = runCatching {
        apiService.getTimetable(
            officeCode = officeCode,
            schoolCode = schoolCode,
            grade = grade,
            classroom = classroom,
            date = date
        ).misTimetable
            ?.getOrNull(1)
            ?.row
            .orEmpty()
            .map { row ->
                TimetableItem(
                    date = row.date,
                    period = row.period.orEmpty(),
                    subject = row.subject.orEmpty(),
                    grade = row.grade.orEmpty(),
                    classroom = row.classroom.orEmpty()
                )
            }
    }
}
