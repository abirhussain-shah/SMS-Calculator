package com.spendsms.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.spendsms.R

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Transactions : Screen("transactions")
    data object Review : Screen("review")
    data object Settings : Screen("settings")
    data object Budgets : Screen("budgets")
    data object Recurring : Screen("recurring")
    data object CategoryRules : Screen("category_rules")
}

sealed class BottomNavDestination(
    val route: String,
    @StringRes val titleRes: Int,
    @StringRes val contentDescriptionRes: Int,
    val icon: ImageVector
) {
    data object Home : BottomNavDestination(
        route = Screen.Home.route,
        titleRes = R.string.nav_home,
        contentDescriptionRes = R.string.cd_nav_home,
        icon = Icons.Default.Home
    )

    data object Transactions : BottomNavDestination(
        route = Screen.Transactions.route,
        titleRes = R.string.nav_transactions,
        contentDescriptionRes = R.string.cd_nav_transactions,
        icon = Icons.AutoMirrored.Filled.ReceiptLong
    )

    data object Review : BottomNavDestination(
        route = Screen.Review.route,
        titleRes = R.string.nav_review,
        contentDescriptionRes = R.string.cd_nav_review,
        icon = Icons.Default.RateReview
    )

    data object Settings : BottomNavDestination(
        route = Screen.Settings.route,
        titleRes = R.string.nav_settings,
        contentDescriptionRes = R.string.cd_nav_settings,
        icon = Icons.Default.Settings
    )

    companion object {
        val items = listOf(Home, Transactions, Review, Settings)
    }
}
