package com.spendsms.parser

import com.google.common.truth.Truth.assertThat
import com.spendsms.domain.model.TransactionType
import com.spendsms.parser.model.ParseResult
import org.junit.Test

/**
 * Unit tests for SmsParser.
 * ALL sample SMS messages in this test suite are SYNTHETIC and generated
 * for testing and validation purposes.
 */
class SmsParserTest {

    private val parser = SmsParserEngine()

    // ----------------------------------------------------
    // bKash Synthetic Test Cases
    // ----------------------------------------------------

    @Test
    fun syntheticCase01_bkashMerchantPayment_standardEnglish() {
        val sms = "Payment Tk 1,250.00 to Star Kabab successful. Fee Tk 0.00. Balance Tk 5,400.00. TrxID BLA776X at 17/10/2026 10:15"
        val result = parser.parse("bKash", sms, 1729150000000L)

        assertThat(result).isInstanceOf(ParseResult.Parsed::class.java)
        val transaction = (result as ParseResult.Parsed).transaction
        assertThat(transaction.amount).isEqualTo(1250.0)
        assertThat(transaction.merchant).contains("Star Kabab")
        assertThat(transaction.type).isEqualTo(TransactionType.DEBIT)
        assertThat(transaction.balance).isEqualTo(5400.0)
        assertThat(transaction.referenceId).isEqualTo("BLA776X")
        assertThat(transaction.category).isEqualTo("food")
    }

    @Test
    fun syntheticCase02_bkashMerchantPayment_banglaDigits() {
        val sms = "Star Kabab-এ Tk ১,২৫০.০০ টাকা পেমেন্ট সফল হয়েছে। TrxID: BLA776X"
        val result = parser.parse("bKash", sms)

        assertThat(result).isInstanceOf(ParseResult.Parsed::class.java)
        val transaction = (result as ParseResult.Parsed).transaction
        assertThat(transaction.amount).isEqualTo(1250.0)
        assertThat(transaction.merchant).contains("Star Kabab")
        assertThat(transaction.referenceId).isEqualTo("BLA776X")
        assertThat(transaction.category).isEqualTo("food")
    }

    @Test
    fun syntheticCase03_bkashSendMoney() {
        val sms = "Send Money Tk 500.00 to 01700000000 successful. Fee Tk 5.00. Balance Tk 3,695.00. TrxID 9B12XY"
        val result = parser.parse("bKash", sms)

        assertThat(result).isInstanceOf(ParseResult.Parsed::class.java)
        val transaction = (result as ParseResult.Parsed).transaction
        assertThat(transaction.amount).isEqualTo(500.0)
        assertThat(transaction.type).isEqualTo(TransactionType.DEBIT)
        assertThat(transaction.balance).isEqualTo(3695.0)
        assertThat(transaction.referenceId).isEqualTo("9B12XY")
        assertThat(transaction.category).isEqualTo("transfer")
    }

    @Test
    fun syntheticCase04_bkashCashOut() {
        val sms = "Cash Out Tk 2,000.00 from 01900000000 successful. Fee Tk 37.00. Balance Tk 1,658.00. TrxID 8A33ZZ"
        val result = parser.parse("bKash", sms)

        assertThat(result).isInstanceOf(ParseResult.Parsed::class.java)
        val transaction = (result as ParseResult.Parsed).transaction
        assertThat(transaction.amount).isEqualTo(2000.0)
        assertThat(transaction.type).isEqualTo(TransactionType.DEBIT)
        assertThat(transaction.balance).isEqualTo(1658.0)
        assertThat(transaction.referenceId).isEqualTo("8A33ZZ")
        assertThat(transaction.category).isEqualTo("transfer")
    }

    @Test
    fun syntheticCase05_bkashMobileRecharge() {
        val sms = "Mobile Recharge Tk 100.00 to 01711000000 successful. Fee Tk 0.00. Balance Tk 1,558.00. TrxID 7C22YY"
        val result = parser.parse("bKash", sms)

        assertThat(result).isInstanceOf(ParseResult.Parsed::class.java)
        val transaction = (result as ParseResult.Parsed).transaction
        assertThat(transaction.amount).isEqualTo(100.0)
        assertThat(transaction.type).isEqualTo(TransactionType.DEBIT)
        assertThat(transaction.category).isEqualTo("recharge")
    }

    @Test
    fun syntheticCase06_bkashReceivedMoney_credit() {
        val sms = "You have received Tk 2,500.00 from 01811000000. Fee Tk 0.00. Balance Tk 7,900.00. TrxID 9A81KD"
        val result = parser.parse("bKash", sms)

        assertThat(result).isInstanceOf(ParseResult.Parsed::class.java)
        val transaction = (result as ParseResult.Parsed).transaction
        assertThat(transaction.amount).isEqualTo(2500.0)
        assertThat(transaction.type).isEqualTo(TransactionType.CREDIT)
        assertThat(transaction.balance).isEqualTo(7900.0)
        assertThat(transaction.referenceId).isEqualTo("9A81KD")
    }

    // ----------------------------------------------------
    // Nagad Synthetic Test Cases
    // ----------------------------------------------------

    @Test
    fun syntheticCase07_nagadMerchantPayment() {
        val sms = "Payment of Tk 850.00 to Unimart is successful. Balance: Tk 3,450.00. TxnID: 71AF6G0"
        val result = parser.parse("NAGAD", sms)

        assertThat(result).isInstanceOf(ParseResult.Parsed::class.java)
        val transaction = (result as ParseResult.Parsed).transaction
        assertThat(transaction.amount).isEqualTo(850.0)
        assertThat(transaction.merchant).contains("Unimart")
        assertThat(transaction.type).isEqualTo(TransactionType.DEBIT)
        assertThat(transaction.balance).isEqualTo(3450.0)
        assertThat(transaction.referenceId).isEqualTo("71AF6G0")
        assertThat(transaction.category).isEqualTo("shopping")
    }

    @Test
    fun syntheticCase08_nagadCashOut() {
        val sms = "Cash Out Tk 1,000.00 to 01700000000 is successful. Balance: Tk 2,450.00. TxnID: 71AF7H1"
        val result = parser.parse("NAGAD", sms)

        assertThat(result).isInstanceOf(ParseResult.Parsed::class.java)
        val transaction = (result as ParseResult.Parsed).transaction
        assertThat(transaction.amount).isEqualTo(1000.0)
        assertThat(transaction.type).isEqualTo(TransactionType.DEBIT)
        assertThat(transaction.category).isEqualTo("transfer")
    }

    @Test
    fun syntheticCase09_nagadMobileRecharge() {
        val sms = "Mobile Recharge of Tk 200.00 for 01711000000 is successful. Balance: Tk 2,250.00. TxnID: 71AF8I2"
        val result = parser.parse("NAGAD", sms)

        assertThat(result).isInstanceOf(ParseResult.Parsed::class.java)
        val transaction = (result as ParseResult.Parsed).transaction
        assertThat(transaction.amount).isEqualTo(200.0)
        assertThat(transaction.type).isEqualTo(TransactionType.DEBIT)
        assertThat(transaction.category).isEqualTo("recharge")
    }

    @Test
    fun syntheticCase10_nagadReceivedMoney_credit() {
        val sms = "You have received Tk 5,000.00 from 01800000000. Balance: Tk 7,250.00. TxnID: 71AF9J3"
        val result = parser.parse("NAGAD", sms)

        assertThat(result).isInstanceOf(ParseResult.Parsed::class.java)
        val transaction = (result as ParseResult.Parsed).transaction
        assertThat(transaction.amount).isEqualTo(5000.0)
        assertThat(transaction.type).isEqualTo(TransactionType.CREDIT)
        assertThat(transaction.balance).isEqualTo(7250.0)
    }

    // ----------------------------------------------------
    // Rocket (DBBL) Synthetic Test Cases
    // ----------------------------------------------------

    @Test
    fun syntheticCase11_rocketPayment() {
        val sms = "Payment Tk1,500.00 to 017110000001 successful. TxnId:123456789. Bal:Tk5,230.50"
        val result = parser.parse("Rocket", sms)

        assertThat(result).isInstanceOf(ParseResult.Parsed::class.java)
        val transaction = (result as ParseResult.Parsed).transaction
        assertThat(transaction.amount).isEqualTo(1500.0)
        assertThat(transaction.type).isEqualTo(TransactionType.DEBIT)
        assertThat(transaction.referenceId).isEqualTo("123456789")
        assertThat(transaction.balance).isEqualTo(5230.50)
    }

    @Test
    fun syntheticCase12_rocketCashOut() {
        val sms = "Cash Out Tk2,000.00 from 019110000001 successful. TxnId:987654321. Bal:Tk3,230.50"
        val result = parser.parse("Rocket", sms)

        assertThat(result).isInstanceOf(ParseResult.Parsed::class.java)
        val transaction = (result as ParseResult.Parsed).transaction
        assertThat(transaction.amount).isEqualTo(2000.0)
        assertThat(transaction.type).isEqualTo(TransactionType.DEBIT)
        assertThat(transaction.category).isEqualTo("transfer")
    }

    // ----------------------------------------------------
    // Bank Card & Account Synthetic Test Cases
    // ----------------------------------------------------

    @Test
    fun syntheticCase13_cityBankCardDebit_posPurchase() {
        val sms = "Your City Bank Card **1234 has been debited for BDT 3,250.00 at Shwapno on 17-OCT-26. Avail Bal BDT 45,200.00"
        val result = parser.parse("CityBank", sms)

        assertThat(result).isInstanceOf(ParseResult.Parsed::class.java)
        val transaction = (result as ParseResult.Parsed).transaction
        assertThat(transaction.amount).isEqualTo(3250.0)
        assertThat(transaction.merchant).contains("Shwapno")
        assertThat(transaction.category).isEqualTo("shopping")
        assertThat(transaction.balance).isEqualTo(45200.0)
    }

    @Test
    fun syntheticCase14_cityBankAcctCredit_salary() {
        val sms = "Your Acct *1234 has been credited with BDT 55,000.00 on 17-OCT-26 towards Salary. Avail Bal BDT 205,000.00"
        val result = parser.parse("CityBank", sms)

        assertThat(result).isInstanceOf(ParseResult.Parsed::class.java)
        val transaction = (result as ParseResult.Parsed).transaction
        assertThat(transaction.amount).isEqualTo(55000.0)
        assertThat(transaction.type).isEqualTo(TransactionType.CREDIT)
        assertThat(transaction.category).isEqualTo("salary")
        assertThat(transaction.balance).isEqualTo(205000.0)
    }

    @Test
    fun syntheticCase15_cityBankAcctDebit_atm() {
        val sms = "Acct *1234 is debited for BDT 10,000.00 on 17-OCT-26 via ATM WDL. Avail Bal BDT 150,000.00"
        val result = parser.parse("CityBank", sms)

        assertThat(result).isInstanceOf(ParseResult.Parsed::class.java)
        val transaction = (result as ParseResult.Parsed).transaction
        assertThat(transaction.amount).isEqualTo(10000.0)
        assertThat(transaction.category).isEqualTo("transfer")
    }

    @Test
    fun syntheticCase16_bracBankCardDebit() {
        val sms = "Your BRAC Bank Card ending 5678 was used for BDT 2,150.00 at Daraz on 17/10/2026. Avail Bal: BDT 35,400.00"
        val result = parser.parse("BRACBank", sms)

        assertThat(result).isInstanceOf(ParseResult.Parsed::class.java)
        val transaction = (result as ParseResult.Parsed).transaction
        assertThat(transaction.amount).isEqualTo(2150.0)
        assertThat(transaction.merchant).contains("Daraz")
        assertThat(transaction.category).isEqualTo("shopping")
    }

    @Test
    fun syntheticCase17_bracBankAcctDebit() {
        val sms = "A/C *5678 debited by BDT 15,000.00 on 17-Oct-26. Trans: Fund Transfer. Bal: BDT 80,000.00"
        val result = parser.parse("BRACBank", sms)

        assertThat(result).isInstanceOf(ParseResult.Parsed::class.java)
        val transaction = (result as ParseResult.Parsed).transaction
        assertThat(transaction.amount).isEqualTo(15000.0)
        assertThat(transaction.category).isEqualTo("transfer")
    }

    @Test
    fun syntheticCase18_eblCardDebit() {
        val sms = "Dear Cardholder, your Card 4567 has been debited for BDT 1,890.00 at Aarong on 17-10-2026. Avail Limit BDT 88,000.00"
        val result = parser.parse("EBL", sms)

        assertThat(result).isInstanceOf(ParseResult.Parsed::class.java)
        val transaction = (result as ParseResult.Parsed).transaction
        assertThat(transaction.amount).isEqualTo(1890.0)
        assertThat(transaction.merchant).contains("Aarong")
        assertThat(transaction.category).isEqualTo("shopping")
    }

    @Test
    fun syntheticCase19_eblAcctDebit() {
        val sms = "A/C *4567 is debited for BDT 5,000.00 on 17-Oct-26. Available Bal: BDT 42,100.00"
        val result = parser.parse("EBL", sms)

        assertThat(result).isInstanceOf(ParseResult.Parsed::class.java)
        val transaction = (result as ParseResult.Parsed).transaction
        assertThat(transaction.amount).isEqualTo(5000.0)
    }

    @Test
    fun syntheticCase20_scbCardDebit() {
        val sms = "SCB Card ending 9012 spent BDT 4,500.00 at Pizza Hut on 17/10/2026. Available limit BDT 120,000.00"
        val result = parser.parse("SCB", sms)

        assertThat(result).isInstanceOf(ParseResult.Parsed::class.java)
        val transaction = (result as ParseResult.Parsed).transaction
        assertThat(transaction.amount).isEqualTo(4500.0)
        assertThat(transaction.merchant).contains("Pizza Hut")
        assertThat(transaction.category).isEqualTo("food")
    }

    @Test
    fun syntheticCase21_genericBankDebit() {
        val sms = "Your account has been debited for BDT 800.00 at Uber BV on 17-10-2026."
        val result = parser.parse("UNKNOWN_BANK", sms)

        assertThat(result).isInstanceOf(ParseResult.Parsed::class.java)
        val transaction = (result as ParseResult.Parsed).transaction
        assertThat(transaction.amount).isEqualTo(800.0)
        assertThat(transaction.merchant).contains("Uber")
        assertThat(transaction.category).isEqualTo("transport")
    }

    @Test
    fun syntheticCase22_genericBankCredit() {
        val sms = "Your account has been credited for BDT 12,000.00 from Inward Remittance."
        val result = parser.parse("UNKNOWN_BANK", sms)

        assertThat(result).isInstanceOf(ParseResult.Parsed::class.java)
        val transaction = (result as ParseResult.Parsed).transaction
        assertThat(transaction.amount).isEqualTo(12000.0)
        assertThat(transaction.type).isEqualTo(TransactionType.CREDIT)
    }

    // ----------------------------------------------------
    // Ignored & Bad Input Synthetic Test Cases
    // ----------------------------------------------------

    @Test
    fun syntheticCase23_otpVerificationSms_ignored() {
        val sms = "Your OTP verification code is 492810. Do not share this code with anyone."
        val result = parser.parse("bKash", sms)

        assertThat(result).isInstanceOf(ParseResult.Ignored::class.java)
    }

    @Test
    fun syntheticCase24_securityPinSms_ignored() {
        val sms = "Never share your PIN with anyone. bKash will never ask for your PIN."
        val result = parser.parse("bKash", sms)

        assertThat(result).isInstanceOf(ParseResult.Ignored::class.java)
    }

    @Test
    fun syntheticCase25_promotionalMarketingSms_ignored() {
        val sms = "Congratulations! Get special discount and cashback offer up to 20% on your next payment."
        val result = parser.parse("bKash", sms)

        assertThat(result).isInstanceOf(ParseResult.Ignored::class.java)
    }

    @Test
    fun syntheticCase26_emptyOrNullSms_failedWithoutCrash() {
        val resultEmpty = parser.parse("bKash", "")
        val resultNull = parser.parse("bKash", null)

        assertThat(resultEmpty).isInstanceOf(ParseResult.Failed::class.java)
        assertThat(resultNull).isInstanceOf(ParseResult.Failed::class.java)
    }

    @Test
    fun syntheticCase27_nonFinancialConversation_failedWithoutCrash() {
        val sms = "Hello, are we meeting at Dhanmondi Lake today at 5 PM?"
        val result = parser.parse("Friend", sms)

        assertThat(result).isInstanceOf(ParseResult.Failed::class.java)
    }

    @Test
    fun syntheticCase28_malformedGarbageInput_failedWithoutCrash() {
        val sms = "!@#$$%^&*()_+=-[]{};:'\",.<>?/\\|~` 00000000000"
        val result = parser.parse("bKash", sms)

        assertThat(result).isInstanceOf(ParseResult.Failed::class.java)
    }

    @Test
    fun syntheticCase29_duplicateMessageConsistency() {
        val sms = "Payment Tk 350.00 to Shawarma House successful. Fee Tk 0.00. Balance Tk 4,200.00. TrxID 9K27XJ"
        val res1 = parser.parse("bKash", sms, 1729155000000L)
        val res2 = parser.parse("bKash", sms, 1729155000000L)

        assertThat(res1).isInstanceOf(ParseResult.Parsed::class.java)
        assertThat(res2).isInstanceOf(ParseResult.Parsed::class.java)
        val t1 = (res1 as ParseResult.Parsed).transaction
        val t2 = (res2 as ParseResult.Parsed).transaction
        assertThat(t1).isEqualTo(t2)
    }

    @Test
    fun syntheticCase30_lowBalanceAlert_ignored() {
        // Real-world sample alert: "You have only 9.81Tk left."
        val sms = "You have only 9.81Tk left."
        val result = parser.parse("GP", sms)

        assertThat(result).isInstanceOf(ParseResult.Ignored::class.java)
    }

    @Test
    fun syntheticCase31_telecomDeduction_parsed() {
        val sms = "Tk 10.00 has been deducted for auto-renewal of your internet pack."
        val result = parser.parse("GP", sms)

        assertThat(result).isInstanceOf(ParseResult.Parsed::class.java)
        val transaction = (result as ParseResult.Parsed).transaction
        assertThat(transaction.amount).isEqualTo(10.0)
        assertThat(transaction.type).isEqualTo(TransactionType.DEBIT)
        assertThat(transaction.category).isEqualTo("bills")
    }
}
