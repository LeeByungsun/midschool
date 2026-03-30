package com.bsbarron.midschoolapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class NeisResponse<T>(
    @SerializedName("mealServiceDietInfo")
    val mealServiceDietInfo: List<NeisSection<T>>? = null,
    @SerializedName("SchoolSchedule")
    val schoolSchedule: List<NeisSection<T>>? = null,
    @SerializedName("misTimetable")
    val misTimetable: List<NeisSection<T>>? = null
)

data class NeisSection<T>(
    @SerializedName("head")
    val head: List<Map<String, Any>>? = null,
    @SerializedName("row")
    val row: List<T>? = null
)

data class MealRowDto(
    @SerializedName("MLSV_YMD")
    val mealDate: String,
    @SerializedName("MMEAL_SC_NM")
    val mealTypeName: String?,
    @SerializedName("DDISH_NM")
    val menu: String?,
    @SerializedName("CAL_INFO")
    val calorieInfo: String?
)

data class ScheduleRowDto(
    @SerializedName("AA_YMD")
    val date: String,
    @SerializedName("EVENT_NM")
    val title: String?,
    @SerializedName("EVENT_CNTNT")
    val description: String?
)

data class TimetableRowDto(
    @SerializedName("ALL_TI_YMD")
    val date: String,
    @SerializedName("PERIO")
    val period: String?,
    @SerializedName("ITRT_CNTNT")
    val subject: String?,
    @SerializedName("GRADE")
    val grade: String?,
    @SerializedName("CLASS_NM")
    val classroom: String?
)
