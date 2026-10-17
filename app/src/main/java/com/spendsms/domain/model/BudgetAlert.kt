package com.spendsms.domain.model

data class BudgetAlert(
    val category: String,
    val percentage: Int, // 80 or 100
    val spent: Double,
    val limit: Double
)
