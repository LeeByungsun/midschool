package com.bsbarron.midschoolapp.di

import com.bsbarron.midschoolapp.data.repository.PreferencesRepository
import com.bsbarron.midschoolapp.data.repository.PreferencesRepositoryImpl
import com.bsbarron.midschoolapp.data.repository.SchoolRepository
import com.bsbarron.midschoolapp.data.repository.SchoolRepositoryImpl
import com.bsbarron.midschoolapp.data.repository.AndroidUserPreferencesStore
import com.bsbarron.midschoolapp.data.repository.UserPreferencesStore
import com.bsbarron.midschoolapp.ui.common.AndroidUiStringProvider
import com.bsbarron.midschoolapp.ui.common.UiStringProvider
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
