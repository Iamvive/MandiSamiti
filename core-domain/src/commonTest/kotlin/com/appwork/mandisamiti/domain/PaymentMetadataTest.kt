package com.appwork.mandisamiti.domain

import com.appwork.mandisamiti.domain.model.PaymentMetadata
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PaymentMetadataTest {

    @Test
    fun testFormatAndParseCheque() {
        val formatted = PaymentMetadata.format(
            bankName = "State Bank of India",
            checkNumber = "CHQ123456",
            accountHolder = "Ram Kumar",
            userRemarks = "Khad kharid"
        )
        val expected = "[BANK:State Bank of India][HOLDER:Ram Kumar][CHK:CHQ123456] Khad kharid"
        assertEquals(expected, formatted)

        val parsed = PaymentMetadata.parse(formatted)
        assertEquals("State Bank of India", parsed.bankName)
        assertEquals("CHQ123456", parsed.checkNumber)
        assertEquals("Ram Kumar", parsed.accountHolder)
        assertNull(parsed.accountNumber)
        assertNull(parsed.ifsc)
        assertNull(parsed.utrNumber)
        assertNull(parsed.upiId)
        assertEquals("Khad kharid", parsed.userRemarks)
        assertEquals("चेक #CHQ123456 · State Bank of India · (Ram Kumar) · Khad kharid", parsed.displaySummary(isEnglish = false))
    }

    @Test
    fun testFormatAndParseRtgs() {
        val formatted = PaymentMetadata.format(
            bankName = "Punjab National Bank",
            accountNumber = "987654321012",
            accountHolder = "Shyam Lal",
            ifsc = "PUNB0123400",
            utrNumber = "UTR998877",
            userRemarks = "Gehu fasal bhugtan"
        )
        val parsed = PaymentMetadata.parse(formatted)
        assertEquals("Punjab National Bank", parsed.bankName)
        assertEquals("987654321012", parsed.accountNumber)
        assertEquals("Shyam Lal", parsed.accountHolder)
        assertEquals("PUNB0123400", parsed.ifsc)
        assertEquals("UTR998877", parsed.utrNumber)
        assertEquals("Gehu fasal bhugtan", parsed.userRemarks)
        assertEquals("संदर्भ/UTR: UTR998877 · Punjab National Bank · खाता: 987654321012 (Shyam Lal) · IFSC: PUNB0123400 · Gehu fasal bhugtan", parsed.displaySummary(isEnglish = false))
    }

    @Test
    fun testFormatAndParseUpi() {
        val formatted = PaymentMetadata.format(
            upiId = "9876543210@paytm",
            utrNumber = "432109876543",
            userRemarks = "Mandi advance"
        )
        val parsed = PaymentMetadata.parse(formatted)
        assertEquals("9876543210@paytm", parsed.upiId)
        assertEquals("432109876543", parsed.utrNumber)
        assertEquals("Mandi advance", parsed.userRemarks)
        assertEquals("संदर्भ/UTR: 432109876543 · UPI: 9876543210@paytm · Mandi advance", parsed.displaySummary(isEnglish = false))
    }

    @Test
    fun testParseStripsContraTag() {
        val remarksWithContra = "[CONTRA:uuid-1234] [BANK:HDFC] [ACC:112233] [HOLDER:Mohan] Advance"
        val parsed = PaymentMetadata.parse(remarksWithContra)
        assertEquals("HDFC", parsed.bankName)
        assertEquals("112233", parsed.accountNumber)
        assertEquals("Mohan", parsed.accountHolder)
        assertEquals("Advance", parsed.userRemarks)
    }
}
