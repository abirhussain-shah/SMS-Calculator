package com.spendsms.ui.screens.transactions

import com.google.common.truth.Truth.assertThat
import com.spendsms.data.local.entity.TransactionEntity
import com.spendsms.domain.model.TransactionType
import com.spendsms.ui.FakeTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeTransactionRepository
    private lateinit var viewModel: TransactionsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeTransactionRepository()
        viewModel = TransactionsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsAllTransactionsGroupedByDate() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val now = System.currentTimeMillis()
        repository.insertTransaction(
            TransactionEntity(
                smsId = 1,
                sender = "bKash",
                amount = 250.0,
                type = TransactionType.DEBIT,
                merchant = "Bata",
                category = "shopping",
                timestamp = now,
                rawBody = "Payment 250"
            )
        )
        repository.insertTransaction(
            TransactionEntity(
                smsId = 2,
                sender = "bKash",
                amount = 500.0,
                type = TransactionType.DEBIT,
                merchant = "Apex",
                category = "shopping",
                timestamp = now - 5000,
                rawBody = "Payment 500"
            )
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.totalCount).isEqualTo(2)
        assertThat(state.groupedTransactions).isNotEmpty()
    }

    @Test
    fun searchQuery_filtersByMerchant() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val now = System.currentTimeMillis()
        repository.insertTransaction(
            TransactionEntity(
                smsId = 1,
                sender = "bKash",
                amount = 350.0,
                type = TransactionType.DEBIT,
                merchant = "Pizza Hut",
                category = "food",
                timestamp = now,
                rawBody = "Payment 350 to Pizza Hut"
            )
        )
        repository.insertTransaction(
            TransactionEntity(
                smsId = 2,
                sender = "NAGAD",
                amount = 120.0,
                type = TransactionType.DEBIT,
                merchant = "Uber",
                category = "transport",
                timestamp = now,
                rawBody = "Payment 120 to Uber"
            )
        )

        viewModel.onSearchQueryChange("Pizza")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.totalCount).isEqualTo(1)
        val transactions = state.groupedTransactions.values.flatten()
        assertThat(transactions[0].merchant).isEqualTo("Pizza Hut")
    }

    @Test
    fun filterType_filtersByDebitAndCredit() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val now = System.currentTimeMillis()
        repository.insertTransaction(
            TransactionEntity(
                smsId = 1,
                sender = "bKash",
                amount = 350.0,
                type = TransactionType.DEBIT,
                merchant = "Burger King",
                category = "food",
                timestamp = now,
                rawBody = "Debit 350"
            )
        )
        repository.insertTransaction(
            TransactionEntity(
                smsId = 2,
                sender = "bKash",
                amount = 10000.0,
                type = TransactionType.CREDIT,
                merchant = "Salary",
                category = "salary",
                timestamp = now,
                rawBody = "Credit 10000"
            )
        )

        // Filter Credit
        viewModel.onFilterTypeChange(TransactionFilterType.CREDIT)
        testDispatcher.scheduler.advanceUntilIdle()

        val creditState = viewModel.uiState.value
        assertThat(creditState.totalCount).isEqualTo(1)
        val creditTransactions = creditState.groupedTransactions.values.flatten()
        assertThat(creditTransactions[0].type).isEqualTo(TransactionType.CREDIT)

        // Filter Debit
        viewModel.onFilterTypeChange(TransactionFilterType.DEBIT)
        testDispatcher.scheduler.advanceUntilIdle()

        val debitState = viewModel.uiState.value
        assertThat(debitState.totalCount).isEqualTo(1)
        val debitTransactions = debitState.groupedTransactions.values.flatten()
        assertThat(debitTransactions[0].type).isEqualTo(TransactionType.DEBIT)
    }

    @Test
    fun updateCategory_andRememberMerchant_updatesBoth() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val id = repository.insertTransaction(
            TransactionEntity(
                smsId = 1,
                sender = "bKash",
                amount = 500.0,
                type = TransactionType.DEBIT,
                merchant = "Aarong",
                category = "other",
                timestamp = System.currentTimeMillis(),
                rawBody = "Payment 500"
            )
        )

        viewModel.onUpdateCategory(
            transactionId = id,
            newCategory = "shopping",
            rememberForMerchant = true
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val updated = repository.transactionsFlow.value.find { it.id == id }
        assertThat(updated?.category).isEqualTo("shopping")
        assertThat(updated?.isUserEdited).isTrue()

        val rule = repository.getCategoryRuleForMerchant("Aarong")
        assertThat(rule?.category).isEqualTo("shopping")
    }

    @Test
    fun deleteTransaction_removesFromRepository() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val id = repository.insertTransaction(
            TransactionEntity(
                smsId = 1,
                sender = "bKash",
                amount = 100.0,
                type = TransactionType.DEBIT,
                merchant = "Test",
                category = "other",
                timestamp = System.currentTimeMillis(),
                rawBody = "Payment 100"
            )
        )

        viewModel.onDeleteTransaction(id)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(repository.transactionsFlow.value).isEmpty()
    }
}
