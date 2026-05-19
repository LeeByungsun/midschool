package com.bsbarron.midschoolapp.data.remote

import com.bsbarron.midschoolapp.BuildConfig
import com.bsbarron.midschoolapp.data.remote.dto.MealRowDto
import com.bsbarron.midschoolapp.data.remote.dto.NeisResponse
import com.bsbarron.midschoolapp.data.remote.dto.ScheduleRowDto
import com.bsbarron.midschoolapp.data.remote.dto.SchoolInfoRowDto
import com.bsbarron.midschoolapp.data.remote.dto.TimetableRowDto
import retrofit2.http.GET
import retrofit2.http.Query

interface NeisApiService {
    @GET("hub/mealServiceDietInfo")
    suspend fun getMeals(
        @Query("KEY") apiKey: String = BuildConfig.NEIS_API_KEY,
        @Query("Type") type: String = "json",
        @Query("pIndex") pageIndex: Int = 1,
        @Query("pSize") pageSize: Int = 100,
        @Query("ATPT_OFCDC_SC_CODE") officeCode: String,
        @Query("SD_SCHUL_CODE") schoolCode: String,
        @Query("MLSV_YMD") date: String? = null
    ): NeisResponse<MealRowDto>

    @GET("hub/SchoolSchedule")
    suspend fun getSchedules(
        @Query("KEY") apiKey: String = BuildConfig.NEIS_API_KEY,
        @Query("Type") type: String = "json",
        @Query("pIndex") pageIndex: Int = 1,
        @Query("pSize") pageSize: Int = 100,
        @Query("ATPT_OFCDC_SC_CODE") officeCode: String,
        @Query("SD_SCHUL_CODE") schoolCode: String,
        @Query("AA_YMD") date: String? = null
    ): NeisResponse<ScheduleRowDto>

    @GET("hub/elsTimetable")
    suspend fun getElementaryTimetable(
        @Query("KEY") apiKey: String = BuildConfig.NEIS_API_KEY,
        @Query("Type") type: String = "json",
        @Query("pIndex") pageIndex: Int = 1,
        @Query("pSize") pageSize: Int = 100,
        @Query("ATPT_OFCDC_SC_CODE") officeCode: String,
        @Query("SD_SCHUL_CODE") schoolCode: String,
        @Query("GRADE") grade: String,
        @Query("CLASS_NM") classroom: String,
        @Query("ALL_TI_YMD") date: String? = null
    ): NeisResponse<TimetableRowDto>

    @GET("hub/misTimetable")
    suspend fun getMiddleTimetable(
        @Query("KEY") apiKey: String = BuildConfig.NEIS_API_KEY,
        @Query("Type") type: String = "json",
        @Query("pIndex") pageIndex: Int = 1,
        @Query("pSize") pageSize: Int = 100,
        @Query("ATPT_OFCDC_SC_CODE") officeCode: String,
        @Query("SD_SCHUL_CODE") schoolCode: String,
        @Query("GRADE") grade: String,
        @Query("CLASS_NM") classroom: String,
        @Query("ALL_TI_YMD") date: String? = null
    ): NeisResponse<TimetableRowDto>

    @GET("hub/hisTimetable")
    suspend fun getHighTimetable(
        @Query("KEY") apiKey: String = BuildConfig.NEIS_API_KEY,
        @Query("Type") type: String = "json",
        @Query("pIndex") pageIndex: Int = 1,
        @Query("pSize") pageSize: Int = 100,
        @Query("ATPT_OFCDC_SC_CODE") officeCode: String,
        @Query("SD_SCHUL_CODE") schoolCode: String,
        @Query("GRADE") grade: String,
        @Query("CLASS_NM") classroom: String,
        @Query("ALL_TI_YMD") date: String? = null
    ): NeisResponse<TimetableRowDto>

    @GET("hub/schoolInfo")
    suspend fun getSchools(
        @Query("KEY") apiKey: String = BuildConfig.NEIS_API_KEY,
        @Query("Type") type: String = "json",
        @Query("pIndex") pageIndex: Int = 1,
        @Query("pSize") pageSize: Int = 100,
        @Query("SCHUL_NM") query: String
    ): NeisResponse<SchoolInfoRowDto>
}
