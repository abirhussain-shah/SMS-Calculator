package com.spendsms.ui.screens.home

import com.google.common.truth.Truth.assertThat
import com.spendsms.data.local.entity.TransactionEntity
import com.spendsms.data.local.entity.UnparsedSmsEntity
import com.spendsms.domain.model.TransactionType
import com.spendsms.ui.FakeTransactionRepository
import com.spendsms.ui.util.DateTimeFormatters
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
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeTransactionRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeTransactionRepository()
        viewModel = HomeViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_calculatesAggregatesForCurrentMonth() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val currentMonth = YearMonth.now()
        val (startCurrent, _) = DateTimeFormatters.getStartAndEndOfMonth(currentMonth)
        val prevMonth = currentMonth.minusMonths(1)
        val (startPrev, _) = DateTimeFormatters.getStartAndEndOfMonth(prevMonth)

        // Add transaction in current month
        repository.insertTransaction(
            TransactionEntity(
                smsId = 1,
                sender = "bKash",
                amount = 1200.0,
                type = TransactionType.DEBIT,
                merchant = "Shwapno",
                category = "shopping",
                timestamp = startCurrent + 10000,
                rawBody = "Payment 1200"
            )
        )
        // Add credit in current month
        repository.insertTransaction(
            TransactionEntity(
                smsId = 2,
                sender = "bKash",
                amount = 5000.0,
                type = TransactionType.CREDIT,
                merchant = "Salary",
                category = "salary",
                timestamp = startCurrent + 20000,
                rawBody = "Received 5000"
            )
        )
        // Add transaction in previous month (should not affect current month)
        repository.insertTransaction(
            TransactionEntity(
                smsId = 3,
                sender = "bKash",
                amount = 800.0,
                type = TransactionType.DEBIT,
                merchant = "Pathao",
                category = "transport",
                timestamp = startPrev + 10000,
                rawBody = "Payment 800"
            )
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.selectedMonth).isEqualTo(currentMonth)
        assertThat(state.totalSpent).isEqualTo(1200.0)
        assertThat(state.totalReceived).isEqualTo(5000.0)
        assertThat(state.netBalance).isEqualTo(3800.0)
        assertThat(state.categorySpendings).hasSize(1)
        assertThat(state.categorySpendings[0].category).isEqualTo("shopping")
        assertThat(state.categorySpendings[0].totalAmount).isEqualTo(1200.0)
        assertThat(state.recentTransactions).hasSize(2)
    }

    @Test
    fun monthNavigation_updatesMonthAndAggregates() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val currentMonth = YearMonth.now()
        val prevMonth = currentMonth.minusMonths(1)
        val (startPrev, _) = DateTimeFormatters.getStartAndEndOfMonth(prevMonth)

        repository.insertTransaction(
            TransactionEntity(
                smsId = 1,
                sender = "NAGAD",
                amount = 650.0,
                type = TransactionType.DEBIT,
                merchant = "Chaldal",
                category = "food",
                timestamp = startPrev + 5000,
                rawBody = "Payment 650"
            )
        )

        viewModel.onPreviousMonth()
        testDispatcher.scheduler.advanceUntilIdle()

        val prevState = viewModel.uiState.value
        assertThat(prevState.selectedMonth).isEqualTo(prevMonth)
        assertThat(prevState.totalSpent).isEqualTo(650.0)
        assertThat(prevState.recentTransactions).hasSize(1)

        viewModel.onResetToCurrentMonth()
        testDispatcher.scheduler.advanceUntilIdle()

        val curState = viewModel.uiState.value
        assertThat(curState.selectedMonth).isEqualTo(currentMonth)
        assertThat(curState.totalSpent).isEqualTo(0.0)
    }

    @Test
    fun pendingReviewCount_isReportedCorrectly() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        repository.insertUnparsedSms(
            UnparsedSmsEntity(
                smsId = 10,
                sender = "GP",
                body = "Unknown format promo",
                timestamp = System.currentTimeMillis(),
                reason = "Pattern not recognized"
            )
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.pendingReviewCount).isEqualTo(1)
    }
}
