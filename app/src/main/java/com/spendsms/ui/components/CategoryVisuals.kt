package com.spendsms.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.spendsms.R

data class CategoryMeta(
    val id: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val color: Color
)

object CategoryVisuals {

    val allCategories = listOf(
        "food",
        "transport",
        "bills",
        "shopping",
        "recharge",
        "transfer",
        "salary",
        "other"
    )

    fun getCategoryMeta(categoryId: String): CategoryMeta {
        return when (categoryId.lowercase()) {
            "food" -> CategoryMeta(
                id = "food",
                titleRes = R.string.cat_food,
                icon = Icons.Outlined.Restaurant,
                color = Color(0xFFF59E0B) // Amber
            )
            "transport" -> CategoryMeta(
                id = "transport",
                titleRes = R.string.cat_transport,
                icon = Icons.Outlined.DirectionsCar,
                color = Color(0xFF3B82F6) // Blue
            )
            "bills" -> CategoryMeta(
                id = "bills",
                titleRes = R.string.cat_bills,
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                color = Color(0xFFEF4444) // Red
            )
            "shopping" -> CategoryMeta(
                id = "shopping",
                titleRes = R.string.cat_shopping,
                icon = Icons.Outlined.ShoppingBag,
                color = Color(0xFFEC4899) // Pink
            )
            "recharge" -> CategoryMeta(
                id = "recharge",
                titleRes = R.string.cat_recharge,
                icon = Icons.Outlined.PhoneAndroid,
                color = Color(0xFF10B981) // Emerald
            )
            "transfer" -> CategoryMeta(
                id = "transfer",
                titleRes = R.string.cat_transfer,
                icon = Icons.Outlined.SwapHoriz,
                color = Color(0xFF8B5CF6) // Purple
            )
            "salary" -> CategoryMeta(
                id = "salary",
                titleRes = R.string.cat_salary,
                icon = Icons.Outlined.AccountBalanceWallet,
                color = Color(0xFF14B8A6) // Teal
            )
            else -> CategoryMeta(
                id = "other",
                titleRes = R.string.cat_other,
                icon = Icons.Outlined.MoreHoriz,
                color = Color(0xFF6B7280) // Gray
            )
        }
    }

    fun getProviderBrandColor(sender: String): Color {
        val s = sender.uppercase()
        return when {
            s.contains("BKASH") -> Color(0xFFE2136E) // bKash Pink
            s.contains("NAGAD") -> Color(0xFFF7941D) // Nagad Orange
            s.contains("ROCKET") || s.contains("16216") -> Color(0xFF8C3494) // Rocket Purple
            s.contains("UPAY") -> Color(0xFF005696)
            s.contains("CITY") -> Color(0xFFCC0000)
            s.contains("BRAC") -> Color(0xFF005C8A)
            s.contains("EBL") -> Color(0xFF006633)
            s.contains("SCB") || s.contains("STAN") -> Color(0xFF009900)
            s.contains("DBBL") -> Color(0xFF003366)
            else -> Color(0xFF4F46E5) // Indigo
        }
    }

    fun getProviderShortLabel(sender: String): String {
        val s = sender.uppercase()
        return when {
            s.contains("BKASH") -> "bKash"
            s.contains("NAGAD") -> "Nagad"
            s.contains("ROCKET") || s.contains("16216") -> "Rocket"
            s.contains("UPAY") -> "Upay"
            s.contains("CITY") -> "City Bank"
            s.contains("BRAC") -> "BRAC"
            s.contains("EBL") -> "EBL"
            s.contains("SCB") -> "SCB"
            s.contains("DBBL") -> "DBBL"
            else -> sender.take(10)
        }
    }
}
