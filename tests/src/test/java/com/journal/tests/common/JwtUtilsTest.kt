package com.journal.tests.common

import com.journal.core.common.config.JwtUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [JwtUtils].
 *
 * Uses Robolectric because [JwtUtils.decodePayload] relies on [android.util.Base64].
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class JwtUtilsTest {

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Builds a minimal (unsigned) JWT string from a raw JSON payload.
     * Uses standard JVM Base64 URL-encoding so the token is valid for decoding.
     */
    private fun buildJwt(payloadJson: String): String {
        val header = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"alg":"none"}""".toByteArray())
        val payload = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payloadJson.toByteArray())
        return "$header.$payload.fakeSig"
    }

    // ─── decodePayload ────────────────────────────────────────────────────────

    @Test
    fun `decodePayload returns JSONObject for well-formed token`() {
        val token = buildJwt("""{"sub":"user123","role":"teacher"}""")
        val payload = JwtUtils.decodePayload(token)
        assertNotNull(payload)
        assertEquals("user123", payload!!.getString("sub"))
    }

    @Test
    fun `decodePayload returns null for token without enough segments`() {
        assertNull(JwtUtils.decodePayload("onlyonepart"))
    }

    @Test
    fun `decodePayload returns null for empty string`() {
        assertNull(JwtUtils.decodePayload(""))
    }

    @Test
    fun `decodePayload returns null when payload is not valid JSON`() {
        val badPayload = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("NOT_JSON".toByteArray())
        assertNull(JwtUtils.decodePayload("header.$badPayload.sig"))
    }

    // ─── extractFullName ──────────────────────────────────────────────────────

    @Test
    fun `extractFullName returns name claim when present`() {
        val token = buildJwt("""{"name":"Иванов Иван Иванович"}""")
        assertEquals("Иванов Иван Иванович", JwtUtils.extractFullName(token))
    }

    @Test
    fun `extractFullName combines family and given when name is absent`() {
        val token = buildJwt("""{"family_name":"Петров","given_name":"Пётр"}""")
        assertEquals("Петров Пётр", JwtUtils.extractFullName(token))
    }

    @Test
    fun `extractFullName returns only family_name when given_name is absent`() {
        val token = buildJwt("""{"family_name":"Сидоров"}""")
        assertEquals("Сидоров", JwtUtils.extractFullName(token))
    }

    @Test
    fun `extractFullName falls back to preferred_username`() {
        val token = buildJwt("""{"preferred_username":"sidorov"}""")
        assertEquals("sidorov", JwtUtils.extractFullName(token))
    }

    @Test
    fun `extractFullName returns null when no name claims are present`() {
        val token = buildJwt("""{"sub":"abc"}""")
        assertNull(JwtUtils.extractFullName(token))
    }

    @Test
    fun `extractFullName returns null for invalid token`() {
        assertNull(JwtUtils.extractFullName("bad.token"))
    }

    // ─── extractFirstName ────────────────────────────────────────────────────

    @Test
    fun `extractFirstName returns given_name when present`() {
        val token = buildJwt("""{"given_name":"Иван","name":"Иванов Иван"}""")
        assertEquals("Иван", JwtUtils.extractFirstName(token))
    }

    @Test
    fun `extractFirstName derives first name from full name when given_name is absent`() {
        // Full name in Russian format: "Фамилия Имя Отчество"
        val token = buildJwt("""{"name":"Иванов Иван Иванович"}""")
        assertEquals("Иван", JwtUtils.extractFirstName(token))
    }

    @Test
    fun `extractFirstName returns single-word name as-is`() {
        val token = buildJwt("""{"name":"Иван"}""")
        assertEquals("Иван", JwtUtils.extractFirstName(token))
    }

    @Test
    fun `extractFirstName falls back to preferred_username`() {
        val token = buildJwt("""{"preferred_username":"ivan_ivanov"}""")
        assertEquals("ivan_ivanov", JwtUtils.extractFirstName(token))
    }

    @Test
    fun `extractFirstName returns null for invalid token`() {
        assertNull(JwtUtils.extractFirstName(""))
    }

    // ─── extractRoles ────────────────────────────────────────────────────────

    @Test
    fun `extractRoles returns roles list for matching clientId`() {
        val token = buildJwt(
            """{"resource_access":{"journal-backend":{"roles":["teacher","admin"]}}}"""
        )
        val roles = JwtUtils.extractRoles(token, "journal-backend")
        assertEquals(listOf("teacher", "admin"), roles)
    }

    @Test
    fun `extractRoles returns empty list when clientId is not in resource_access`() {
        val token = buildJwt(
            """{"resource_access":{"other-app":{"roles":["user"]}}}"""
        )
        assertTrue(JwtUtils.extractRoles(token, "journal-backend").isEmpty())
    }

    @Test
    fun `extractRoles returns empty list when resource_access is absent`() {
        val token = buildJwt("""{"sub":"user"}""")
        assertTrue(JwtUtils.extractRoles(token, "journal-backend").isEmpty())
    }

    @Test
    fun `extractRoles returns empty list for invalid token`() {
        assertTrue(JwtUtils.extractRoles("invalid", "journal-backend").isEmpty())
    }

    @Test
    fun `extractRoles returns empty list when roles array is empty`() {
        val token = buildJwt(
            """{"resource_access":{"journal-backend":{"roles":[]}}}"""
        )
        assertTrue(JwtUtils.extractRoles(token, "journal-backend").isEmpty())
    }
}
