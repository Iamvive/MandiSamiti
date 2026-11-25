package com.appwork.mandisamiti

import com.appwork.mandisamiti.platform.PiiScrubber
import kotlin.test.Test
import kotlin.test.assertEquals

class PiiScrubberTest {
    @Test fun masksPlainAndPrefixedNumbers() {
        assertEquals("call [phone] now", PiiScrubber.scrub("call 9876543210 now"))
        assertEquals("[phone] and [phone]", PiiScrubber.scrub("+91 98765 43210 and 91-9876543210"))
    }

    @Test fun leavesOtherTextAlone() {
        assertEquals("Bill 12345 failed: HTTP 500", PiiScrubber.scrub("Bill 12345 failed: HTTP 500"))
    }
}
