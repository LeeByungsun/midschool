package com.bsbarron.midschoolapp.data

import com.bsbarron.midschoolapp.BuildConfig
import com.bsbarron.midschoolapp.data.remote.NeisApiService
import com.bsbarron.midschoolapp.data.repository.SchoolRepository
import com.bsbarron.midschoolapp.data.repository.SchoolRepositoryImpl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object AppContainer {
    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.NEIS_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val neisApiService: NeisApiService by lazy {
        retrofit.create(NeisApiService::class.java)
    }

    val schoolRepository: SchoolRepository by lazy {
        SchoolRepositoryImpl(
            apiService = neisApiService,
            officeCode = "J10",
            schoolCode = "7679399"
        )
    }
}
