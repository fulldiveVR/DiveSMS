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

import com.moez.QKSMS.model.ForwardingFilter
import com.moez.QKSMS.repository.ContactRepository
import io.reactivex.Observable
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

/**
 * Unit tests for TemplateEngine.
 */
class TemplateEngineTest {

    @Mock
    private lateinit var contactRepo: ContactRepository

    private lateinit var templateEngine: TemplateEngine

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Mock contact repo to return empty list
        whenever(contactRepo.getUnmanagedContacts())
            .thenReturn(Observable.just(emptyList()))

        templateEngine = TemplateEngine(contactRepo)
    }

    // ==================== Variable Replacement Tests ====================

    @Test
    fun `replaces sender variable`() {
        val filter = createFilter(
            subjectTemplate = "Message from {{sender}}",
            bodyTemplate = "Sender: {{sender}}"
        )
        val sms = createSms(sender = "+1234567890")

        val result = templateEngine.render(filter, sms)

        assertTrue(result.subject.contains("+1234567890"))
        assertTrue(result.body.contains("+1234567890"))
    }

    @Test
    fun `replaces message variable`() {
        val filter = createFilter(
            subjectTemplate = "New SMS",
            bodyTemplate = "Content: {{message}}"
        )
        val sms = createSms(body = "Hello World")

        val result = templateEngine.render(filter, sms)

        assertTrue(result.body.contains("Hello World"))
    }

    @Test
    fun `replaces date variable`() {
        val filter = createFilter(
            bodyTemplate = "Received on: {{date}}"
        )
        val sms = createSms()

        val result = templateEngine.render(filter, sms)

        // Date should be formatted like "Dec 28, 2025"
        assertTrue(result.body.contains("Received on:"))
        assertFalse(result.body.contains("{{date}}"))
    }

    @Test
    fun `replaces time variable`() {
        val filter = createFilter(
            bodyTemplate = "Received at: {{time}}"
        )
        val sms = createSms()

        val result = templateEngine.render(filter, sms)

        // Time should be formatted like "2:30 PM"
        assertTrue(result.body.contains("Received at:"))
        assertFalse(result.body.contains("{{time}}"))
    }

    @Test
    fun `replaces sim_slot variable`() {
        val filter = createFilter(
            bodyTemplate = "Received on: {{sim_slot}}"
        )
        val sms = createSms(simSlot = 0)

        val result = templateEngine.render(filter, sms)

        assertTrue(result.body.contains("SIM 1"))
    }

    @Test
    fun `sim slot 1 formats as SIM 2`() {
        val filter = createFilter(
            bodyTemplate = "{{sim_slot}}"
        )
        val sms = createSms(simSlot = 1)

        val result = templateEngine.render(filter, sms)

        assertTrue(result.body.contains("SIM 2"))
    }

    @Test
    fun `negative sim slot formats as Unknown`() {
        val filter = createFilter(
            bodyTemplate = "{{sim_slot}}"
        )
        val sms = createSms(simSlot = -1)

        val result = templateEngine.render(filter, sms)

        assertTrue(result.body.contains("Unknown"))
    }

    @Test
    fun `replaces contact_name with sender when no contact found`() {
        val filter = createFilter(
            bodyTemplate = "From: {{contact_name}}"
        )
        val sms = createSms(sender = "+1234567890")

        val result = templateEngine.render(filter, sms)

        // When no contact is found, should use the phone number
        assertTrue(result.body.contains("+1234567890"))
    }

    @Test
    fun `replaces multiple variables in same template`() {
        val filter = createFilter(
            subjectTemplate = "SMS from {{sender}}",
            bodyTemplate = "From: {{sender}}\nMessage: {{message}}\nTime: {{time}}"
        )
        val sms = createSms(sender = "+1234567890", body = "Hello")

        val result = templateEngine.render(filter, sms)

        assertTrue(result.subject.contains("+1234567890"))
        assertTrue(result.body.contains("+1234567890"))
        assertTrue(result.body.contains("Hello"))
        assertFalse(result.body.contains("{{"))
    }

    // ==================== Default Template Tests ====================

    @Test
    fun `uses default subject when template is blank`() {
        val filter = createFilter(subjectTemplate = "")
        val sms = createSms(sender = "+1234567890")

        val result = templateEngine.render(filter, sms)

        // Default subject contains sender
        assertTrue(result.subject.contains("+1234567890"))
    }

    @Test
    fun `uses default body when template is blank`() {
        val filter = createFilter(bodyTemplate = "")
        val sms = createSms(body = "Test message")

        val result = templateEngine.render(filter, sms)

        // Default body contains message
        assertTrue(result.body.contains("Test message"))
        assertTrue(result.body.contains("Wize SMS"))
    }

    // ==================== HTML Template Tests ====================

    @Test
    fun `respects useHtmlTemplate flag when true`() {
        val filter = createFilter(useHtmlTemplate = true)
        val sms = createSms()

        val result = templateEngine.render(filter, sms)

        assertTrue(result.isHtml)
    }

    @Test
    fun `respects useHtmlTemplate flag when false`() {
        val filter = createFilter(useHtmlTemplate = false)
        val sms = createSms()

        val result = templateEngine.render(filter, sms)

        assertFalse(result.isHtml)
    }

    // ==================== Preview Tests ====================

    @Test
    fun `renderPreview generates sample content`() {
        val result = templateEngine.renderPreview(
            subjectTemplate = "SMS from {{contact_name}}",
            bodyTemplate = "Message: {{message}}",
            isHtml = false
        )

        // Preview uses sample data
        assertTrue(result.subject.contains("John Doe"))
        assertTrue(result.body.contains("verification code"))
    }

    @Test
    fun `renderPreview uses default templates when blank`() {
        val result = templateEngine.renderPreview(
            subjectTemplate = "",
            bodyTemplate = "",
            isHtml = false
        )

        // Should use default templates
        assertFalse(result.subject.contains("{{"))
        assertFalse(result.body.contains("{{"))
        assertTrue(result.body.contains("Wize SMS"))
    }

    // ==================== Available Variables Tests ====================

    @Test
    fun `getAvailableVariables returns all variables`() {
        val variables = templateEngine.getAvailableVariables()

        assertEquals(6, variables.size)
        assertTrue(variables.any { it.first == "{{sender}}" })
        assertTrue(variables.any { it.first == "{{message}}" })
        assertTrue(variables.any { it.first == "{{time}}" })
        assertTrue(variables.any { it.first == "{{date}}" })
        assertTrue(variables.any { it.first == "{{contact_name}}" })
        assertTrue(variables.any { it.first == "{{sim_slot}}" })
    }

    @Test
    fun `available variables have descriptions`() {
        val variables = templateEngine.getAvailableVariables()

        variables.forEach { (variable, description) ->
            assertTrue("Variable $variable should have description", description.isNotBlank())
        }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `handles empty message body`() {
        val filter = createFilter(bodyTemplate = "Message: {{message}}")
        val sms = createSms(body = "")

        val result = templateEngine.render(filter, sms)

        assertTrue(result.body.contains("Message:"))
    }

    @Test
    fun `handles special characters in message`() {
        val filter = createFilter(bodyTemplate = "{{message}}")
        val sms = createSms(body = "<script>alert('xss')</script>")

        val result = templateEngine.render(filter, sms)

        // Should preserve special characters (no escaping in basic template engine)
        assertTrue(result.body.contains("<script>"))
    }

    @Test
    fun `preserves template text around variables`() {
        val filter = createFilter(
            bodyTemplate = "Hello! You received a message from {{sender}}. The message says: {{message}}. Thank you!"
        )
        val sms = createSms(sender = "John", body = "Hi there")

        val result = templateEngine.render(filter, sms)

        assertTrue(result.body.startsWith("Hello!"))
        assertTrue(result.body.contains("You received a message from John"))
        assertTrue(result.body.contains("The message says: Hi there"))
        assertTrue(result.body.endsWith("Thank you!"))
    }

    // ==================== Helper Functions ====================

    private fun createFilter(
        subjectTemplate: String = "Test Subject",
        bodyTemplate: String = "Test Body",
        useHtmlTemplate: Boolean = false
    ): ForwardingFilter {
        return ForwardingFilter(
            id = System.nanoTime(),
            name = "Test Filter",
            enabled = true,
            emailSubjectTemplate = subjectTemplate,
            emailBodyTemplate = bodyTemplate,
            useHtmlTemplate = useHtmlTemplate,
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
