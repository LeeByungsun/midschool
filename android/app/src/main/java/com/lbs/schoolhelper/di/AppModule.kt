package com.lbs.schoolhelper.di

import com.lbs.schoolhelper.data.repository.PreferencesRepository
import com.lbs.schoolhelper.data.repository.PreferencesRepositoryImpl
import com.lbs.schoolhelper.data.repository.SchoolRepository
import com.lbs.schoolhelper.data.repository.SchoolRepositoryImpl
import com.lbs.schoolhelper.data.repository.AndroidUserPreferencesStore
import com.lbs.schoolhelper.data.repository.UserPreferencesStore
import com.lbs.schoolhelper.ui.common.AndroidUiStringProvider
import com.lbs.schoolhelper.ui.common.UiStringProvider
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

    @Binds
    @Singleton
    abstract fun bindUserPreferencesStore(
        store: AndroidUserPreferencesStore
    ): UserPreferencesStore

    @Binds
    @Singleton
    abstract fun bindUiStringProvider(
        provider: AndroidUiStringProvider
    ): UiStringProvider
}
