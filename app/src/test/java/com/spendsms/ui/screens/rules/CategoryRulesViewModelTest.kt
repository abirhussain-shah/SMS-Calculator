package com.spendsms.ui.screens.rules

import com.google.common.truth.Truth.assertThat
import com.spendsms.data.local.entity.CategoryRuleEntity
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
class CategoryRulesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeTransactionRepository
    private lateinit var viewModel: CategoryRulesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeTransactionRepository()
        viewModel = CategoryRulesViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsAllCategoryRules() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        repository.saveCategoryRule("Daraz", "shopping")
        repository.saveCategoryRule("Star Kabab", "food")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.rules).hasSize(2)
        assertThat(state.filteredRules).hasSize(2)
        assertThat(state.rules.any { it.merchant == "Daraz" && it.category == "shopping" }).isTrue()
    }

    @Test
    fun searchQuery_filtersRulesByMerchantOrCategory() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        repository.saveCategoryRule("Daraz", "shopping")
        repository.saveCategoryRule("Star Kabab", "food")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChange("kabab")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.filteredRules).hasSize(1)
        assertThat(state.filteredRules[0].merchant).isEqualTo("Star Kabab")

        viewModel.onSearchQueryChange("shopping")
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.filteredRules).hasSize(1)
        assertThat(viewModel.uiState.value.filteredRules[0].merchant).isEqualTo("Daraz")
    }

    @Test
    fun saveRule_savesRuleAndSetsSuccessMessage() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.openAddDialog()
        viewModel.onAddMerchantChange("Pathao Food")
        viewModel.onAddCategoryChange("food")
        viewModel.saveRule()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isAddDialogOpen).isFalse()
        assertThat(state.userMessage).isEqualTo("rule_saved")
        assertThat(repository.getCategoryRuleForMerchant("Pathao Food")?.category).isEqualTo("food")
    }

    @Test
    fun saveRule_withBlankMerchant_showsError() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.openAddDialog()
        viewModel.onAddMerchantChange("   ")
        viewModel.saveRule()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isAddDialogOpen).isTrue()
        assertThat(state.userMessage).isEqualTo("invalid_input")
    }

    @Test
    fun deleteRule_removesRuleFromRepository() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        repository.saveCategoryRule("Old Shop", "shopping")
        testDispatcher.scheduler.advanceUntilIdle()

        val rule = CategoryRuleEntity(merchant = "Old Shop", category = "shopping")
        viewModel.promptDelete(rule)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.ruleToDelete).isEqualTo(rule)

        viewModel.confirmDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.ruleToDelete).isNull()
        assertThat(viewModel.uiState.value.userMessage).isEqualTo("rule_deleted")
        assertThat(repository.getCategoryRuleForMerchant("Old Shop")).isNull()
    }
}
