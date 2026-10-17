package com.spendsms.di

import com.spendsms.data.repository.SettingsRepositoryImpl
import com.spendsms.data.repository.TransactionRepositoryImpl
import com.spendsms.domain.repository.SettingsRepository
import com.spendsms.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        transactionRepositoryImpl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindRecurringRepository(
        recurringRepositoryImpl: com.spendsms.data.repository.RecurringRepositoryImpl
    ): com.spendsms.domain.repository.RecurringRepository
}
