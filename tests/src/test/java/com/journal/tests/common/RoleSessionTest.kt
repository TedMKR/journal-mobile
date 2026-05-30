package com.journal.tests.common

import app.cash.turbine.test
import com.journal.core.common.config.RoleSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [RoleSession].
 *
 * Pure Kotlin — no Android APIs needed.
 */
class RoleSessionTest {

    // ─── Initial state ────────────────────────────────────────────────────────

    @Test
    fun `initial role matches the constructor argument`() {
        val session = RoleSession("teacher")
        assertEquals("teacher", session.role.value)
    }

    @Test
    fun `initial role can be any supported value`() {
        listOf("teacher", "student", "methodologist", "dean", "admin").forEach { role ->
            assertEquals(role, RoleSession(role).role.value)
        }
    }

    // ─── setRole ──────────────────────────────────────────────────────────────

    @Test
    fun `setRole updates the role flow`() {
        val session = RoleSession("teacher")
        session.setRole("admin")
        assertEquals("admin", session.role.value)
    }

    @Test
    fun `setRole emits the new value to collectors`() = runTest {
        val session = RoleSession("student")

        session.role.test {
            assertEquals("student", awaitItem()) // initial value

            session.setRole("methodologist")
            assertEquals("methodologist", awaitItem())
        }
    }

    @Test
    fun `setRole can be called multiple times`() {
        val session = RoleSession("teacher")
        session.setRole("admin")
        session.setRole("student")
        assertEquals("student", session.role.value)
    }

    @Test
    fun `setRole with the same value does not emit duplicate`() = runTest {
        val session = RoleSession("teacher")

        session.role.test {
            assertEquals("teacher", awaitItem())

            // StateFlow de-duplicates equal values — no second emission
            session.setRole("teacher")
            expectNoEvents()
        }
    }
}
