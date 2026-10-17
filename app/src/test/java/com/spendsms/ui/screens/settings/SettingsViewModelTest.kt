package com.spendsms.ui.screens.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.spendsms.data.datastore.ThemeMode
import com.spendsms.data.local.entity.TransactionEntity
import com.spendsms.domain.model.TransactionType
import com.spendsms.ui.FakeSettingsRepository
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        settingsRepository = FakeSettingsRepository()
        transactionRepository = FakeTransactionRepository()
        viewModel = SettingsViewModel(context, settingsRepository, transactionRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsUserPreferences() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.themeMode).isEqualTo(ThemeMode.SYSTEM)
        assertThat(state.isBiometricEnabled).isFalse()
        assertThat(state.monitoredSenders).contains("bKash")
        assertThat(state.monitoredSenders).contains("NAGAD")
    }

    @Test
    fun onThemeModeChange_updatesPreferences() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onThemeModeChange(ThemeMode.DARK)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.themeMode).isEqualTo(ThemeMode.DARK)
        assertThat(settingsRepository.preferencesFlow.value.themeMode).isEqualTo(ThemeMode.DARK)
    }

    @Test
    fun onBiometricToggle_updatesPreferences() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onBiometricToggle(true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.isBiometricEnabled).isTrue()
        assertThat(settingsRepository.preferencesFlow.value.isBiometricEnabled).isTrue()
    }

    @Test
    fun onAddSender_andRemoveSender_updatesMonitoredSenders() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onConfirmAddSender("EBL")
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.monitoredSenders).contains("EBL")
        assertThat(viewModel.uiState.value.isAddSenderDialogOpen).isFalse()

        viewModel.onRemoveSender("EBL")
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.monitoredSenders).doesNotContain("EBL")
    }

    @Test
    fun onConfirmDeleteAllData_clearsTransactionsAndSettings() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        transactionRepository.insertTransaction(
            TransactionEntity(
                smsId = 1,
                sender = "bKash",
                amount = 200.0,
                type = TransactionType.DEBIT,
                merchant = "Bata",
                category = "shopping",
                timestamp = System.currentTimeMillis(),
                rawBody = "Payment 200"
            )
        )

        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(transactionRepository.transactionsFlow.value).isNotEmpty()

        viewModel.onConfirmDeleteAllData()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(transactionRepository.transactionsFlow.value).isEmpty()
        assertThat(viewModel.uiState.value.isDeleteAllConfirmDialogOpen).isFalse()
    }

    @Test
    fun onExportCsv_exportsTransactionsSuccessfully() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        transactionRepository.insertTransaction(
            TransactionEntity(
                smsId = 1,
                sender = "bKash",
                amount = 150.0,
                type = TransactionType.DEBIT,
                merchant = "Meena Bazar",
                category = "food",
                timestamp = System.currentTimeMillis(),
                rawBody = "Payment 150"
            )
        )

        val outputStream = ByteArrayOutputStream()
        viewModel.onExportCsv(outputStream)
        testDispatcher.scheduler.advanceUntilIdle()

        val csvString = outputStream.toString("UTF-8")
        assertThat(csvString).contains("Meena Bazar")
        assertThat(csvString).contains("150.00")
        assertThat(viewModel.uiState.value.message).contains("Exported 1 transactions")
    }
}
