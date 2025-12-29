/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.moez.QKSMS.interactor

import com.moez.QKSMS.model.Contact
import com.moez.QKSMS.model.ForwardingFilter
import com.moez.QKSMS.repository.ContactRepository
import com.moez.QKSMS.repository.ForwardingRepository
import io.reactivex.Observable
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

/**
 * Unit tests for FilterEngine.
 */
class FilterEngineTest {

    @Mock
    private lateinit var forwardingRepo: ForwardingRepository

    @Mock
    private lateinit var contactRepo: ContactRepository

    private lateinit var filterEngine: FilterEngine

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Mock contact repo to return empty list
        whenever(contactRepo.getUnmanagedContacts())
            .thenReturn(Observable.just(emptyList()))

        filterEngine = FilterEngine(forwardingRepo, contactRepo)
    }

    // ==================== Sender Matching Tests ====================

    @Test
    fun `empty sender filter matches all senders`() {
        val filter = createFilter(senderFilter = "", senderMatchType = "CONTAINS")
        val filters = listOf(filter)
        whenever(forwardingRepo.getActiveFilters()).thenReturn(filters)

        val sms = createSms(sender = "+1234567890")
        val result = filterEngine.findMatchingFilters(sms)

        assertEquals(1, result.size)
    }

    @Test
    fun `sender contains match works with partial number`() {
        val filter = createFilter(senderFilter = "1234", senderMatchType = "CONTAINS")
        val filters = listOf(filter)
        whenever(forwardingRepo.getActiveFilters()).thenReturn(filters)

        val sms = createSms(sender = "+1234567890")
        val result = filterEngine.findMatchingFilters(sms)

        assertEquals(1, result.size)
    }

    @Test
    fun `sender contains match is case insensitive`() {
        val filter = createFilter(senderFilter = "ABC", senderMatchType = "CONTAINS")
        val filters = listOf(filter)
        whenever(forwardingRepo.getActiveFilters()).thenReturn(filters)

        val sms = createSms(sender = "abcdef")
        val result = filterEngine.findMatchingFilters(sms)

        assertEquals(1, result.size)
    }

    @Test
    fun `sender contains match with comma-separated keywords`() {
        val filter = createFilter(senderFilter = "bank,alert,notification", senderMatchType = "CONTAINS")
        val filters = listOf(filter)
        whenever(forwardingRepo.getActiveFilters()).thenReturn(filters)

        val sms = createSms(sender = "BANK-ALERTS")
        val result = filterEngine.findMatchingFilters(sms)

        assertEquals(1, result.size)
    }

    @Test
    fun `sender contains does not match when keyword not present`() {
        val filter = createFilter(senderFilter = "bank", senderMatchType = "CONTAINS")
        val filters = listOf(filter)
        whenever(forwardingRepo.getActiveFilters()).thenReturn(filters)

        val sms = createSms(sender = "+1234567890")
        val result = filterEngine.findMatchingFilters(sms)

        assertEquals(0, result.size)
    }

    @Test
    fun `sender regex match works with valid pattern`() {
        val filter = createFilter(senderFilter = "\\+1\\d{10}", senderMatchType = "REGEX")
        val filters = listOf(filter)
        whenever(forwardingRepo.getActiveFilters()).thenReturn(filters)

        val sms = createSms(sender = "+12345678901")
        val result = filterEngine.findMatchingFilters(sms)

        assertEquals(1, result.size)
    }

    @Test
    fun `sender regex returns empty on invalid pattern`() {
        val filter = createFilter(senderFilter = "[invalid(regex", senderMatchType = "REGEX")
        val filters = listOf(filter)
        whenever(forwardingRepo.getActiveFilters()).thenReturn(filters)

        val sms = createSms(sender = "+1234567890")
        val result = filterEngine.findMatchingFilters(sms)

        assertEquals(0, result.size)
    }

    // ==================== Content Matching Tests ====================

    @Test
    fun `empty content filter matches all content`() {
        val filter = createFilter(contentFilter = "", contentMatchType = "CONTAINS")
        val filters = listOf(filter)
        whenever(forwardingRepo.getActiveFilters()).thenReturn(filters)

        val sms = createSms(body = "Any message content")
        val result = filterEngine.findMatchingFilters(sms)

        assertEquals(1, result.size)
    }

    @Test
    fun `content contains match works with keyword`() {
        val filter = createFilter(contentFilter = "verification code", contentMatchType = "CONTAINS")
        val filters = listOf(filter)
        whenever(forwardingRepo.getActiveFilters()).thenReturn(filters)

        val sms = createSms(body = "Your verification code is 123456")
        val result = filterEngine.findMatchingFilters(sms)

        assertEquals(1, result.size)
    }

    @Test
    fun `content contains match is case insensitive`() {
        val filter = createFilter(contentFilter = "OTP", contentMatchType = "CONTAINS")
        val filters = listOf(filter)
        whenever(forwardingRepo.getActiveFilters()).thenReturn(filters)

        val sms = createSms(body = "Your otp is 123456")
        val result = filterEngine.findMatchingFilters(sms)

        assertEquals(1, result.size)
    }

    @Test
    fun `content contains with multiple keywords matches any`() {
        val filter = createFilter(contentFilter = "otp,code,password", contentMatchType = "CONTAINS")
        val filters = listOf(filter)
        whenever(forwardingRepo.getActiveFilters()).thenReturn(filters)

        val sms = createSms(body = "Your password reset link")
        val result = filterEngine.findMatchingFilters(sms)

        assertEquals(1, result.size)
    }

    @Test
    fun `content exact match requires exact content`() {
        val filter = createFilter(contentFilter = "Hello World", contentMatchType = "EXACT")
        val filters = listOf(filter)
        whenever(forwardingRepo.getActiveFilters()).thenReturn(filters)

        val sms1 = createSms(body = "Hello World")
        val sms2 = createSms(body = "Hello World!")

        val result1 = filterEngine.findMatchingFilters(sms1)
        val result2 = filterEngine.findMatchingFilters(sms2)

        assertEquals(1, result1.size)
        assertEquals(0, result2.size)
    }

    @Test
    fun `content regex match works`() {
        val filter = createFilter(contentFilter = "\\d{6}", contentMatchType = "REGEX")
        val filters = listOf(filter)
        whenever(forwardingRepo.getActiveFilters()).thenReturn(filters)

        val sms = createSms(body = "Your code is 123456")
        val result = filterEngine.findMatchingFilters(sms)

        assertEquals(1, result.size)
    }

    // ==================== SIM Slot Matching Tests ====================

    @Test
    fun `sim slot ALL matches any slot`() {
        val filter = createFilter(simSlotFilter = "ALL")
        val filters = listOf(filter)
        whenever(forwardingRepo.getActiveFilters()).thenReturn(filters)

        val sms0 = createSms(simSlot = 0)
        val sms1 = createSms(simSlot = 1)
        val sms2 = createSms(simSlot = 2)

        assertEquals(1, filterEngine.findMatchingFilters(sms0).size)
        assertEquals(1, filterEngine.findMatchingFilters(sms1).size)
        assertEquals(1, filterEngine.findMatchingFilters(sms2).size)
    }

    @Test
    fun `sim slot SIM1 matches slot 0 and 1`() {
        val filter = createFilter(simSlotFilter = "SIM1")
        val filters = listOf(filter)
        whenever(forwardingRepo.getActiveFilters()).thenReturn(filters)

        val sms0 = createSms(simSlot = 0)
        val sms1 = createSms(simSlot = 1)
        val sms2 = createSms(simSlot = 2)

        assertEquals(1, filterEngine.findMatchingFilters(sms0).size)
        assertEquals(1, filterEngine.findMatchingFilters(sms1).size)
        assertEquals(0, filterEngine.findMatchingFilters(sms2).size)
    }

    @Test
    fun `sim slot SIM2 matches slot 1 and 2`() {
        val filter = createFilter(simSlotFilter = "SIM2")
        val filters = listOf(filter)
        whenever(forwardingRepo.getActiveFilters()).thenReturn(filters)

        val sms0 = createSms(simSlot = 0)
        val sms1 = createSms(simSlot = 1)
        val sms2 = createSms(simSlot = 2)

        assertEquals(0, filterEngine.findMatchingFilters(sms0).size)
        assertEquals(1, filterEngine.findMatchingFilters(sms1).size)
        assertEquals(1, filterEngine.findMatchingFilters(sms2).size)
    }

    // ==================== Include/Exclude Filter Tests ====================

    @Test
    fun `include filter returns matching messages`() {
        val filter = createFilter(
            contentFilter = "important",
            contentMatchType = "CONTAINS",
            isIncludeFilter = true
        )
        val filters = listOf(filter)
        whenever(forwardingRepo.getActiveFilters()).thenReturn(filters)

        val sms = createSms(body = "This is important message")
        val result = filterEngine.findMatchingFilters(sms)

        assertEquals(1, result.size)
    }

    @Test
    fun `exclude filter returns non-matching messages`() {
        val filter = createFilter(
            contentFilter = "spam",
            contentMatchType = "CONTAINS",
            isIncludeFilter = false
        )
        val filters = listOf(filter)
        whenever(forwardingRepo.getActiveFilters()).thenReturn(filters)

        val sms1 = createSms(body = "Normal message")
        val sms2 = createSms(body = "This is spam message")

        assertEquals(1, filterEngine.findMatchingFilters(sms1).size)
        assertEquals(0, filterEngine.findMatchingFilters(sms2).size)
    }

    // ==================== Priority Tests ====================

    @Test
    fun `filters are sorted by priority`() {
        val filter1 = createFilter(name = "Low Priority", priority = 10)
        val filter2 = createFilter(name = "High Priority", priority = 1)
        val filter3 = createFilter(name = "Medium Priority", priority = 5)
        val filters = listOf(filter1, filter2, filter3)
        whenever(forwardingRepo.getActiveFilters()).thenReturn(filters)

        val sms = createSms()
        val result = filterEngine.findMatchingFilters(sms)

        assertEquals(3, result.size)
        assertEquals("High Priority", result[0].name)
        assertEquals("Medium Priority", result[1].name)
        assertEquals("Low Priority", result[2].name)
    }

    // ==================== No Active Filters Tests ====================

    @Test
    fun `returns empty list when no active filters`() {
        whenever(forwardingRepo.getActiveFilters()).thenReturn(emptyList())

        val sms = createSms()
        val result = filterEngine.findMatchingFilters(sms)

        assertTrue(result.isEmpty())
    }

    // ==================== Helper Functions ====================

    private fun createFilter(
        name: String = "Test Filter",
        senderFilter: String = "",
        senderMatchType: String = "CONTAINS",
        contentFilter: String = "",
        contentMatchType: String = "CONTAINS",
        isIncludeFilter: Boolean = true,
        simSlotFilter: String = "ALL",
        timeWindowEnabled: Boolean = false,
        priority: Int = 0
    ): ForwardingFilter {
        return ForwardingFilter(
            id = System.nanoTime(),
            name = name,
            enabled = true,
            priority = priority,
            senderFilter = senderFilter,
            senderMatchType = senderMatchType,
            contentFilter = contentFilter,
            contentMatchType = contentMatchType,
            isIncludeFilter = isIncludeFilter,
            simSlotFilter = simSlotFilter,
            timeWindowEnabled = timeWindowEnabled,
            destinationEmail = "test@example.com",
            emailAccountId = 1
        )
    }

    private fun createSms(
        sender: String = "+1234567890",
        body: String = "Test message",
        timestamp: Long = System.currentTimeMillis(),
        simSlot: Int = 0
    ): FilterEngine.SmsData {
        return FilterEngine.SmsData(
            sender = sender,
            body = body,
            timestamp = timestamp,
            simSlot = simSlot
        )
    }
}
