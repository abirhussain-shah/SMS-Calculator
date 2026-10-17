package com.spendsms.di

import android.content.Context
import androidx.room.Room
import com.spendsms.data.local.SpendSMSDatabase
import com.spendsms.data.local.dao.BudgetDao
import com.spendsms.data.local.dao.CategoryRuleDao
import com.spendsms.data.local.dao.RecurringPaymentDao
import com.spendsms.data.local.dao.TransactionDao
import com.spendsms.data.local.dao.UnparsedSmsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): SpendSMSDatabase {
        return Room.databaseBuilder(
            context,
            SpendSMSDatabase::class.java,
            SpendSMSDatabase.DATABASE_NAME
        )
            .addMigrations(
                SpendSMSDatabase.MIGRATION_1_2,
                SpendSMSDatabase.MIGRATION_2_3
            )
            .build()
    }

    @Provides
    fun provideTransactionDao(database: SpendSMSDatabase): TransactionDao =
        database.transactionDao()

    @Provides
    fun provideCategoryRuleDao(database: SpendSMSDatabase): CategoryRuleDao =
        database.categoryRuleDao()

    @Provides
    fun provideUnparsedSmsDao(database: SpendSMSDatabase): UnparsedSmsDao =
        database.unparsedSmsDao()

    @Provides
    fun provideBudgetDao(database: SpendSMSDatabase): BudgetDao =
        database.budgetDao()

    @Provides
    fun provideRecurringPaymentDao(database: SpendSMSDatabase): RecurringPaymentDao =
        database.recurringPaymentDao()
}
