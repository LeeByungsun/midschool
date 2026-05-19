package com.bsbarron.midschoolapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class NeisResponse<T>(
    @SerializedName("mealServiceDietInfo")
    val mealServiceDietInfo: List<NeisSection<T>>? = null,
    @SerializedName("SchoolSchedule")
    val schoolSchedule: List<NeisSection<T>>? = null,
    @SerializedName("elsTimetable")
    val elsTimetable: List<NeisSection<T>>? = null,
    @SerializedName("misTimetable")
    val misTimetable: List<NeisSection<T>>? = null,
    @SerializedName("schoolInfo")
    val schoolInfo: List<NeisSection<T>>? = null,
    @SerializedName("hisTimetable")
    val hisTimetable: List<NeisSection<T>>? = null
)

data class NeisSection<T>(
    @SerializedName("head")
    val head: List<NeisHeadDto>? = null,
    @SerializedName("row")
    val row: List<T>? = null
)

data class NeisHeadDto(
    @SerializedName("RESULT")
    val result: NeisResultDto? = null,
    @SerializedName("list_total_count")
    val totalCount: Int? = null
)

data class NeisResultDto(
    @SerializedName("CODE")
    val code: String? = null,
    @SerializedName("MESSAGE")
    val message: String? = null
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

data class SchoolInfoRowDto(
    @SerializedName("ATPT_OFCDC_SC_CODE")
    val officeCode: String,
    @SerializedName("ATPT_OFCDC_SC_NM")
    val officeName: String?,
    @SerializedName("SD_SCHUL_CODE")
    val schoolCode: String,
    @SerializedName("SCHUL_NM")
    val schoolName: String?,
    @SerializedName("SCHUL_KND_SC_NM")
    val schoolKind: String?,
    @SerializedName("LCTN_SC_NM")
    val location: String?,
    @SerializedName("JU_ORG_NM")
    val jurisdiction: String?,
    @SerializedName("FOND_SC_NM")
    val foundation: String?,
    @SerializedName("ORG_RDNMA")
    val roadAddress: String?,
    @SerializedName("ORG_TELNO")
    val telephone: String?,
    @SerializedName("HMPG_ADRES")
    val homepage: String?
)
