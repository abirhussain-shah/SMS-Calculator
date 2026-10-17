package com.spendsms.parser.category

import com.google.common.truth.Truth.assertThat
import com.spendsms.domain.model.Category
import com.spendsms.domain.model.TransactionType
import org.junit.Test

class CategoryClassifierTest {

    @Test
    fun testFoodClassification() {
        assertThat(CategoryClassifier.classify("Star Kabab")).isEqualTo(Category.FOOD)
        assertThat(CategoryClassifier.classify("Pizza Hut")).isEqualTo(Category.FOOD)
        assertThat(CategoryClassifier.classify("Sultans Dine Kacchi")).isEqualTo(Category.FOOD)
        assertThat(CategoryClassifier.classify("Foodpanda")).isEqualTo(Category.FOOD)
        assertThat(CategoryClassifier.classify("Cafe Rio")).isEqualTo(Category.FOOD)
    }

    @Test
    fun testTransportClassification() {
        assertThat(CategoryClassifier.classify("Uber BV")).isEqualTo(Category.TRANSPORT)
        assertThat(CategoryClassifier.classify("Pathao Ride")).isEqualTo(Category.TRANSPORT)
        assertThat(CategoryClassifier.classify("Shohoz Bus")).isEqualTo(Category.TRANSPORT)
        assertThat(CategoryClassifier.classify("Clean Fuel Filling Station")).isEqualTo(Category.TRANSPORT)
    }

    @Test
    fun testBillsClassification() {
        assertThat(CategoryClassifier.classify("DESCO Bill Payment")).isEqualTo(Category.BILLS)
        assertThat(CategoryClassifier.classify("DPDC Prepaid")).isEqualTo(Category.BILLS)
        assertThat(CategoryClassifier.classify("Dhaka WASA")).isEqualTo(Category.BILLS)
        assertThat(CategoryClassifier.classify("Carnival Internet")).isEqualTo(Category.BILLS)
    }

    @Test
    fun testShoppingClassification() {
        assertThat(CategoryClassifier.classify("Daraz Bangladesh")).isEqualTo(Category.SHOPPING)
        assertThat(CategoryClassifier.classify("Aarong Outlet")).isEqualTo(Category.SHOPPING)
        assertThat(CategoryClassifier.classify("Unimart Super Shop")).isEqualTo(Category.SHOPPING)
        assertThat(CategoryClassifier.classify("Shwapno")).isEqualTo(Category.SHOPPING)
        assertThat(CategoryClassifier.classify("Apex Footwear")).isEqualTo(Category.SHOPPING)
    }

    @Test
    fun testRechargeClassification() {
        assertThat(CategoryClassifier.classify("Mobile Recharge")).isEqualTo(Category.RECHARGE)
        assertThat(CategoryClassifier.classify("GP Flexiload")).isEqualTo(Category.RECHARGE)
        assertThat(CategoryClassifier.classify("Banglalink Top-up")).isEqualTo(Category.RECHARGE)
    }

    @Test
    fun testTransferClassification() {
        assertThat(CategoryClassifier.classify("Cash Out")).isEqualTo(Category.TRANSFER)
        assertThat(CategoryClassifier.classify("ATM WDL")).isEqualTo(Category.TRANSFER)
        assertThat(CategoryClassifier.classify("Fund Transfer NPSB")).isEqualTo(Category.TRANSFER)
    }

    @Test
    fun testSalaryClassification() {
        assertThat(CategoryClassifier.classify("Corporate Salary", "", TransactionType.CREDIT))
            .isEqualTo(Category.SALARY)
        assertThat(CategoryClassifier.classify("Inward Remittance", "", TransactionType.CREDIT))
            .isEqualTo(Category.SALARY)
    }

    @Test
    fun testOtherFallback() {
        assertThat(CategoryClassifier.classify("Unknown Person / General Merchant"))
            .isEqualTo(Category.OTHER)
    }
}
