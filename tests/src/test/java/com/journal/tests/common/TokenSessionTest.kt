package com.journal.tests.common

import app.cash.turbine.test
import com.journal.core.common.config.TokenSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [TokenSession].
 *
 * Pure Kotlin — no Android APIs needed.
 */
class TokenSessionTest {

    private val session = TokenSession()

    // ─── Initial state ────────────────────────────────────────────────────────

    @Test
    fun `accessToken initial value is null`() = runTest {
        assertNull(session.accessToken.value)
    }

    @Test
    fun `idToken initial value is null`() = runTest {
        assertNull(session.idToken.value)
    }

    @Test
    fun `refreshToken initial value is null`() = runTest {
        assertNull(session.refreshToken.value)
    }

    // ─── setTokens ────────────────────────────────────────────────────────────

    @Test
    fun `setTokens updates all three flows`() = runTest {
        session.setTokens(
            accessToken = "access123",
            idToken = "id123",
            refreshToken = "refresh123"
        )
        assertEquals("access123", session.accessToken.value)
        assertEquals("id123", session.idToken.value)
        assertEquals("refresh123", session.refreshToken.value)
    }

    @Test
    fun `setTokens accepts null idToken and refreshToken`() = runTest {
        session.setTokens(accessToken = "access_only", idToken = null, refreshToken = null)
        assertEquals("access_only", session.accessToken.value)
        assertNull(session.idToken.value)
        assertNull(session.refreshToken.value)
    }

    @Test
    fun `setTokens emits new value to collectors`() = runTest {
        session.accessToken.test {
            assertNull(awaitItem()) // initial null

            session.setTokens("newAccess", null, null)
            assertEquals("newAccess", awaitItem())
        }
    }

    // ─── clear ────────────────────────────────────────────────────────────────

    @Test
    fun `clear resets all flows to null`() = runTest {
        session.setTokens("a", "b", "c")
        session.clear()
        assertNull(session.accessToken.value)
        assertNull(session.idToken.value)
        assertNull(session.refreshToken.value)
    }

    @Test
    fun `clear emits null to accessToken collectors`() = runTest {
        session.setTokens("access", "id", "refresh")

        session.accessToken.test {
            assertEquals("access", awaitItem()) // current value

            session.clear()
            assertNull(awaitItem())
        }
    }
}
