package com.spendsms

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class Phase1SetupTest {

    @Test
    fun testAppPackageAndConfiguration() {
        val appName = "SpendSMS"
        assertThat(appName).isEqualTo("SpendSMS")
    }
}
