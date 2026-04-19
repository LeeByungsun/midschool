package com.bsbarron.midschoolapp.di

import com.bsbarron.midschoolapp.data.repository.PreferencesRepository
import com.bsbarron.midschoolapp.data.repository.PreferencesRepositoryImpl
import com.bsbarron.midschoolapp.data.repository.SchoolRepository
import com.bsbarron.midschoolapp.data.repository.SchoolRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindSchoolRepository(
        repositoryImpl: SchoolRepositoryImpl
    ): SchoolRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        repositoryImpl: PreferencesRepositoryImpl
    ): PreferencesRepository
}
