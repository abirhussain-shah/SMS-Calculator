package com.spendsms.ui.screens.budgets

import com.google.common.truth.Truth.assertThat
import com.spendsms.data.local.entity.BudgetEntity
import com.spendsms.data.local.entity.TransactionEntity
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
class BudgetsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeTransactionRepository
    private lateinit var viewModel: BudgetsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeTransactionRepository()
        viewModel = BudgetsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsExpenseCategoriesWithSpending() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val currentMonth = YearMonth.now()
        val (startCurrent, _) = DateTimeFormatters.getStartAndEndOfMonth(currentMonth)

        repository.insertTransaction(
            TransactionEntity(
                smsId = 1,
                sender = "bKash",
                amount = 450.0,
                type = TransactionType.DEBIT,
                merchant = "KFC",
                category = "food",
                timestamp = startCurrent + 1000,
                rawBody = "Payment 450"
            )
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.selectedMonth).isEqualTo(currentMonth)
        assertThat(state.overallSpent).isEqualTo(450.0)
        assertThat(state.overallBudget).isNull()

        val foodCategory = state.categoryBudgets.find { it.categoryId == "food" }
        assertThat(foodCategory).isNotNull()
        assertThat(foodCategory!!.spent).isEqualTo(450.0)
        assertThat(foodCategory.monthlyLimit).isNull()
        assertThat(foodCategory.isOverBudget).isFalse()
        assertThat(foodCategory.isNearBudget).isFalse()
    }

    @Test
    fun saveBudget_updatesCategoryLimit() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.openEditDialog(category = "food", currentLimit = null)
        viewModel.onLimitInputChange("3000")
        viewModel.saveBudget()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        val foodCategory = state.categoryBudgets.find { it.categoryId == "food" }
        assertThat(foodCategory?.monthlyLimit).isEqualTo(3000.0)
        assertThat(state.userMessage).isEqualTo("budget_saved")
        assertThat(state.isEditing).isFalse()
    }

    @Test
    fun budgetThresholds_flagsNearAndOverBudget() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val currentMonth = YearMonth.now()
        val (startCurrent, _) = DateTimeFormatters.getStartAndEndOfMonth(currentMonth)

        // Set budget for transport: 1000
        repository.setBudget("transport", 1000.0)

        // Spend 850 (85% -> near budget)
        val txId = repository.insertTransaction(
            TransactionEntity(
                smsId = 1,
                sender = "bKash",
                amount = 850.0,
                type = TransactionType.DEBIT,
                merchant = "Uber",
                category = "transport",
                timestamp = startCurrent + 1000,
                rawBody = "Payment 850"
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        var transportCategory = viewModel.uiState.value.categoryBudgets.find { it.categoryId == "transport" }!!
        assertThat(transportCategory.spent).isEqualTo(850.0)
        assertThat(transportCategory.percentage).isEqualTo(85)
        assertThat(transportCategory.isNearBudget).isTrue()
        assertThat(transportCategory.isOverBudget).isFalse()

        // Spend another 200 (total 1050 -> over budget)
        repository.insertTransaction(
            TransactionEntity(
                smsId = 2,
                sender = "bKash",
                amount = 200.0,
                type = TransactionType.DEBIT,
                merchant = "Pathao",
                category = "transport",
                timestamp = startCurrent + 2000,
                rawBody = "Payment 200"
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        transportCategory = viewModel.uiState.value.categoryBudgets.find { it.categoryId == "transport" }!!
        assertThat(transportCategory.spent).isEqualTo(1050.0)
        assertThat(transportCategory.percentage).isEqualTo(105)
        assertThat(transportCategory.isNearBudget).isFalse()
        assertThat(transportCategory.isOverBudget).isTrue()
    }

    @Test
    fun deleteBudget_removesLimit() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        repository.setBudget("bills", 2500.0)
        testDispatcher.scheduler.advanceUntilIdle()

        var bills = viewModel.uiState.value.categoryBudgets.find { it.categoryId == "bills" }
        assertThat(bills?.monthlyLimit).isEqualTo(2500.0)

        viewModel.promptDeleteBudget("bills")
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.showDeleteConfirm).isTrue()

        viewModel.confirmDeleteBudget()
        testDispatcher.scheduler.advanceUntilIdle()

        bills = viewModel.uiState.value.categoryBudgets.find { it.categoryId == "bills" }
        assertThat(bills?.monthlyLimit).isNull()
        assertThat(viewModel.uiState.value.userMessage).isEqualTo("budget_deleted")
    }

    @Test
    fun saveOverallBudget_updatesOverallLimit() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.openEditDialog(category = BudgetEntity.OVERALL_CATEGORY, currentLimit = null)
        viewModel.onLimitInputChange("20000")
        viewModel.saveBudget()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.overallBudget).isNotNull()
        assertThat(state.overallBudget!!.monthlyLimit).isEqualTo(20000.0)
    }
}
