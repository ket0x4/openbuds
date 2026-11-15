package com.openbuds.driver.huawei

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeBuds5iMatcherTest {

    @Test
    fun testMatchesFreeBudsFamily() {
        assertTrue(FreeBuds5iMatcher.matches("HUAWEI FreeBuds 5i"))
        assertTrue(FreeBuds5iMatcher.matches("FreeBuds Pro"))
        assertTrue(FreeBuds5iMatcher.matches("freebuds"))
    }

    @Test
    fun testRejectsOtherDevices() {
        assertFalse(FreeBuds5iMatcher.matches("WH-1000XM5"))
        assertFalse(FreeBuds5iMatcher.matches(""))
        assertFalse(FreeBuds5iMatcher.matches(null))
    }

    @Test
    fun testPrefersThe5iModel() {
        assertTrue(FreeBuds5iMatcher.preferred("HUAWEI FreeBuds 5i"))
        assertFalse(FreeBuds5iMatcher.preferred("HUAWEI FreeBuds Pro"))
    }

    @Test
    fun testDisplayNames() {
        assertTrue(FreeBuds5iMatcher.matches(FreeBuds5iMatcher.displayName))
        assertTrue(FreeBuds5iMatcher.matches(FreeBuds5iMatcher.modelName))
    }
}
