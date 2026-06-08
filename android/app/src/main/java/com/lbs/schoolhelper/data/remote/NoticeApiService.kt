package com.lbs.schoolhelper.data.remote

import com.lbs.schoolhelper.data.remote.dto.NoticeListResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface NoticeApiService {
    @GET("api/notices")
    suspend fun getNotices(
        @Query("officeCode") officeCode: String,
        @Query("schoolCode") schoolCode: String,
        @Query("limit") limit: Int = 3
    ): NoticeListResponseDto
}
