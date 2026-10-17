package com.spendsms.ui.screens.review

import com.google.common.truth.Truth.assertThat
import com.spendsms.data.local.entity.UnparsedSmsEntity
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
class ReviewViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeTransactionRepository
    private lateinit var viewModel: ReviewViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeTransactionRepository()
        viewModel = ReviewViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsPendingUnparsedMessages() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        repository.insertUnparsedSms(
            UnparsedSmsEntity(
                smsId = 1,
                sender = "City Bank",
                body = "Unrecognized bank format alert",
                timestamp = System.currentTimeMillis(),
                reason = "Pattern not recognized"
            )
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.pendingList).hasSize(1)
        assertThat(state.pendingList[0].sender).isEqualTo("City Bank")
    }

    @Test
    fun ignoreSms_removesFromPendingList() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val id = repository.insertUnparsedSms(
            UnparsedSmsEntity(
                smsId = 1,
                sender = "GP",
                body = "Some promo text",
                timestamp = System.currentTimeMillis(),
                reason = "Pattern not recognized"
            )
        )

        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.pendingList).hasSize(1)

        viewModel.onIgnore(id)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.pendingList).isEmpty()
    }

    @Test
    fun deleteSms_permanentlyRemovesFromRepository() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val id = repository.insertUnparsedSms(
            UnparsedSmsEntity(
                smsId = 1,
                sender = "bKash",
                body = "Corrupted text",
                timestamp = System.currentTimeMillis(),
                reason = "Corrupted"
            )
        )

        viewModel.onDelete(id)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(repository.unparsedSmsFlow.value).isEmpty()
    }

    @Test
    fun confirmConvert_createsTransactionAndRemovesUnparsedSms() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val unparsed = UnparsedSmsEntity(
            id = 1,
            smsId = 100,
            sender = "EBL",
            body = "EBL Card POS Purchase of BDT 1,200 at Agora",
            timestamp = 1729155000000L,
            reason = "Pattern not recognized"
        )
        repository.unparsedSmsFlow.value = listOf(unparsed)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onStartConvert(unparsed)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.isConvertDialogOpen).isTrue()
        assertThat(viewModel.uiState.value.selectedForConversion).isEqualTo(unparsed)

        viewModel.onConfirmConvert(
            amount = 1200.0,
            type = TransactionType.DEBIT,
            merchant = "Agora",
            category = "shopping"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify transaction was inserted into repository
        val txList = repository.transactionsFlow.value
        assertThat(txList).hasSize(1)
        val created = txList[0]
        assertThat(created.smsId).isEqualTo(100)
        assertThat(created.sender).isEqualTo("EBL")
        assertThat(created.amount).isEqualTo(1200.0)
        assertThat(created.type).isEqualTo(TransactionType.DEBIT)
        assertThat(created.merchant).isEqualTo("Agora")
        assertThat(created.category).isEqualTo("shopping")
        assertThat(created.isUserEdited).isTrue()

        // Verify unparsed SMS was deleted
        assertThat(repository.unparsedSmsFlow.value).isEmpty()

        // Verify dialog is closed
        assertThat(viewModel.uiState.value.isConvertDialogOpen).isFalse()

        // Verify custom rule was remembered for Agora
        assertThat(repository.getCategoryRuleForMerchant("Agora")?.category).isEqualTo("shopping")
    }

    @Test
    fun startConvert_extractsDataAndSuggestsCategory() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val unparsed = UnparsedSmsEntity(
            id = 2,
            smsId = 200,
            sender = "bKash",
            body = "Payment of Tk 650.00 to Sultan Dines was successful.",
            timestamp = 1729155000000L,
            reason = "Pattern not recognized"
        )

        viewModel.onStartConvert(unparsed)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isConvertDialogOpen).isTrue()
        assertThat(state.extractedData).isNotNull()
        assertThat(state.extractedData?.detectedAmount).isEqualTo(650.0)
        assertThat(state.extractedData?.suggestedCategory?.categoryId).isEqualTo("food")
    }

    @Test
    fun toggleSelectAll_selectsAndDeselectsAllItems() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val id1 = repository.insertUnparsedSms(
            UnparsedSmsEntity(smsId = 1, sender = "bKash", body = "Msg 1", timestamp = 1000L, reason = "Unparsed")
        )
        val id2 = repository.insertUnparsedSms(
            UnparsedSmsEntity(smsId = 2, sender = "Nagad", body = "Msg 2", timestamp = 2000L, reason = "Unparsed")
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.pendingList).hasSize(2)
        assertThat(viewModel.uiState.value.selectedIds).isEmpty()
        assertThat(viewModel.uiState.value.isAllSelected).isFalse()

        // Select All
        viewModel.toggleSelectAll()
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedIds).containsExactly(id1, id2)
        assertThat(viewModel.uiState.value.isAllSelected).isTrue()
        assertThat(viewModel.uiState.value.selectedCount).isEqualTo(2)

        // Deselect All
        viewModel.toggleSelectAll()
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedIds).isEmpty()
        assertThat(viewModel.uiState.value.isAllSelected).isFalse()
    }

    @Test
    fun toggleSelection_selectsIndividualItem() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val id1 = repository.insertUnparsedSms(
            UnparsedSmsEntity(smsId = 1, sender = "bKash", body = "Msg 1", timestamp = 1000L, reason = "Unparsed")
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleSelection(id1)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedIds).containsExactly(id1)

        viewModel.toggleSelection(id1)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedIds).isEmpty()
    }

    @Test
    fun onIgnoreSelected_dismissesAllSelectedItems() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val id1 = repository.insertUnparsedSms(
            UnparsedSmsEntity(smsId = 1, sender = "bKash", body = "Msg 1", timestamp = 1000L, reason = "Unparsed")
        )
        val id2 = repository.insertUnparsedSms(
            UnparsedSmsEntity(smsId = 2, sender = "Nagad", body = "Msg 2", timestamp = 2000L, reason = "Unparsed")
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleSelection(id1)
        viewModel.toggleSelection(id2)

        viewModel.onIgnoreSelected()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.pendingList).isEmpty()
        assertThat(viewModel.uiState.value.selectedIds).isEmpty()
    }

    @Test
    fun onAutoConvertSelected_convertsSelectedToTransactions() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val id1 = repository.insertUnparsedSms(
            UnparsedSmsEntity(
                smsId = 1,
                sender = "bKash",
                body = "Payment of Tk 750 to Pizza Guy successful",
                timestamp = 1000L,
                reason = "Unparsed"
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleSelection(id1)
        viewModel.onAutoConvertSelected()
        testDispatcher.scheduler.advanceUntilIdle()

        // Unparsed should be removed
        assertThat(viewModel.uiState.value.pendingList).isEmpty()
        assertThat(viewModel.uiState.value.selectedIds).isEmpty()

        // Transaction should be inserted
        val txs = repository.transactionsFlow.value
        assertThat(txs).hasSize(1)
        assertThat(txs[0].amount).isEqualTo(750.0)
        assertThat(txs[0].category).isEqualTo("food")
    }
}
