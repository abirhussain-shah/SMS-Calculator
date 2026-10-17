package com.spendsms.domain.model

import androidx.annotation.StringRes
import com.spendsms.R

enum class Category(
    val id: String,
    @StringRes val titleRes: Int
) {
    FOOD("food", R.string.cat_food),
    TRANSPORT("transport", R.string.cat_transport),
    BILLS("bills", R.string.cat_bills),
    SHOPPING("shopping", R.string.cat_shopping),
    RECHARGE("recharge", R.string.cat_recharge),
    TRANSFER("transfer", R.string.cat_transfer),
    SALARY("salary", R.string.cat_salary),
    OTHER("other", R.string.cat_other);

    companion object {
        fun fromId(id: String?): Category {
            if (id == null) return OTHER
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OTHER
        }
    }
}
