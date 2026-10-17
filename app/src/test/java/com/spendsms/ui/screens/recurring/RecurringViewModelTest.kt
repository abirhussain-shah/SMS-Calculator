package com.spendsms.ui.screens.recurring

import com.google.common.truth.Truth.assertThat
import com.spendsms.data.local.entity.RecurringPaymentEntity
import com.spendsms.domain.repository.RecurringRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
class RecurringViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeRecurringRepository
    private lateinit var viewModel: RecurringViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeRecurringRepository()
        viewModel = RecurringViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsActivePaymentsAndSuggestions() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        fakeRepository.addActivePayment(
            RecurringPaymentEntity(
                id = 1L,
                merchant = "Carnival Internet",
                amount = 1150.0,
                category = "utilities",
                frequency = RecurringPaymentEntity.FREQUENCY_MONTHLY,
                approximateDay = 5,
                lastPaymentDate = 1729000000000L,
                nextExpectedDate = 1731600000000L,
                isDismissed = false,
                isConfirmed = true,
                reminderDaysBefore = 2
            )
        )
        fakeRepository.addSuggestion(
            RecurringPaymentEntity(
                id = 2L,
                merchant = "Netflix",
                amount = 1200.0,
                category = "entertainment",
                frequency = RecurringPaymentEntity.FREQUENCY_MONTHLY,
                approximateDay = 15,
                lastPaymentDate = 1729000000000L,
                nextExpectedDate = 1731600000000L,
                isDismissed = false,
                isConfirmed = false,
                reminderDaysBefore = 2
            )
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.activePayments).hasSize(1)
        assertThat(state.activePayments[0].merchant).isEqualTo("Carnival Internet")
        assertThat(state.pendingSuggestions).hasSize(1)
        assertThat(state.pendingSuggestions[0].merchant).isEqualTo("Netflix")
    }

    @Test
    fun confirmSuggestion_callsRepositoryAndSetsMessage() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        fakeRepository.addSuggestion(
            RecurringPaymentEntity(
                id = 5L,
                merchant = "Gym",
                amount = 1500.0,
                category = "health",
                frequency = RecurringPaymentEntity.FREQUENCY_MONTHLY,
                approximateDay = 1,
                lastPaymentDate = 1729000000000L,
                nextExpectedDate = 1731600000000L,
                isDismissed = false,
                isConfirmed = false,
                reminderDaysBefore = 2
            )
        )

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.confirmSuggestion(5L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(fakeRepository.confirmedIds).contains(5L)
        assertThat(viewModel.uiState.value.userMessage).isEqualTo("recurring_confirmed")
    }

    @Test
    fun dismissSuggestion_callsRepositoryAndSetsMessage() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        fakeRepository.addSuggestion(
            RecurringPaymentEntity(
                id = 8L,
                merchant = "One-off",
                amount = 200.0,
                category = "misc",
                frequency = RecurringPaymentEntity.FREQUENCY_MONTHLY,
                approximateDay = 1,
                lastPaymentDate = 1729000000000L,
                nextExpectedDate = 1731600000000L,
                isDismissed = false,
                isConfirmed = false,
                reminderDaysBefore = 2
            )
        )

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.dismissSuggestion(8L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(fakeRepository.dismissedIds).contains(8L)
        assertThat(viewModel.uiState.value.userMessage).isEqualTo("recurring_dismissed")
    }

    @Test
    fun saveRecurringPayment_validInputs_savesAndDismissesDialog() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.openAddDialog()
        viewModel.onMerchantChange("Water Bill")
        viewModel.onAmountChange("450")
        viewModel.onCategoryChange("utilities")
        viewModel.onDayChange("10")
        viewModel.saveRecurringPayment()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isAddDialogOpen).isFalse()
        assertThat(state.userMessage).isEqualTo("recurring_saved")
        assertThat(fakeRepository.savedPayments.any { it.merchant == "Water Bill" && it.amount == 450.0 }).isTrue()
    }

    @Test
    fun saveRecurringPayment_invalidInputs_showsErrorMessage() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.openAddDialog()
        viewModel.onMerchantChange("")
        viewModel.onAmountChange("0")
        viewModel.saveRecurringPayment()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isAddDialogOpen).isTrue()
        assertThat(state.userMessage).isEqualTo("invalid_input")
    }

    @Test
    fun confirmDelete_deletesPayment() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val payment = RecurringPaymentEntity(
            id = 42L,
            merchant = "Old Service",
            amount = 300.0,
            category = "bills",
            frequency = RecurringPaymentEntity.FREQUENCY_MONTHLY,
            approximateDay = 1,
            lastPaymentDate = 1729000000000L,
            nextExpectedDate = 1731600000000L,
            isDismissed = false,
            isConfirmed = true,
            reminderDaysBefore = 2
        )

        viewModel.promptDelete(payment)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.paymentToDelete).isEqualTo(payment)

        viewModel.confirmDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(fakeRepository.deletedIds).contains(42L)
        assertThat(viewModel.uiState.value.paymentToDelete).isNull()
        assertThat(viewModel.uiState.value.userMessage).isEqualTo("recurring_deleted")
    }

    private class FakeRecurringRepository : RecurringRepository {
        private val _active = MutableStateFlow<List<RecurringPaymentEntity>>(emptyList())
        private val _suggestions = MutableStateFlow<List<RecurringPaymentEntity>>(emptyList())

        val confirmedIds = mutableListOf<Long>()
        val dismissedIds = mutableListOf<Long>()
        val deletedIds = mutableListOf<Long>()
        val savedPayments = mutableListOf<RecurringPaymentEntity>()

        fun addActivePayment(payment: RecurringPaymentEntity) {
            _active.value = _active.value + payment
        }

        fun addSuggestion(payment: RecurringPaymentEntity) {
            _suggestions.value = _suggestions.value + payment
        }

        override fun getActiveRecurringPayments(): Flow<List<RecurringPaymentEntity>> = _active.asStateFlow()
        override fun getPendingSuggestions(): Flow<List<RecurringPaymentEntity>> = _suggestions.asStateFlow()
        override fun getAllRecurringPayments(): Flow<List<RecurringPaymentEntity>> = _active.asStateFlow()

        override suspend fun upsertRecurringPayment(recurringPayment: RecurringPaymentEntity): Long {
            savedPayments.add(recurringPayment)
            return 1L
        }

        override suspend fun confirmRecurring(id: Long) {
            confirmedIds.add(id)
        }

        override suspend fun dismissRecurring(id: Long) {
            dismissedIds.add(id)
        }

        override suspend fun deleteRecurring(id: Long) {
            deletedIds.add(id)
        }

        override suspend fun runAutoDetection(): List<RecurringPaymentEntity> = emptyList()

        override suspend fun checkUpcomingReminders(nowMillis: Long): List<RecurringPaymentEntity> = emptyList()

        override suspend fun markReminded(id: Long, timestamp: Long) {}
    }
}
