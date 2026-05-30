package com.journal.tests.common

import android.content.SharedPreferences
import com.journal.core.common.config.StoredTokens
import com.journal.core.common.config.TokenStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TokenStore].
 *
 * [SharedPreferences] and its [Editor][SharedPreferences.Editor] are mocked
 * via MockK with an in-memory map as backing storage.
 *
 * Note: MockK's [capture] requires `T : Any` (non-nullable), so nullable
 * String? captures are handled via [io.mockk.anyNullable] + [answers] blocks.
 */
class TokenStoreTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var store: TokenStore

    /** Simulated in-memory backing storage for the mock [SharedPreferences]. */
    private val storage = mutableMapOf<String, Any?>()

    @Before
    fun setUp() {
        editor = mockk(relaxed = true)
        prefs = mockk()

        // ── Editor write stubs ────────────────────────────────────────────────
        // Use anyNullable() for the String? parameter – MockK capture() does not
        // support nullable type variables, so we use firstArg/secondArg instead.
        every { editor.putString(any(), anyNullable()) } answers {
            storage[firstArg()] = secondArg<String?>()
            editor
        }
        every { editor.putLong(any(), any()) } answers {
            storage[firstArg<String>()] = secondArg<Long>()
            editor
        }
        every { editor.clear() } answers {
            storage.clear()
            editor
        }
        // apply() is a void call – relaxed mock already handles it, but we keep
        // it explicit so verify { editor.apply() } works correctly.
        every { editor.apply() } returns Unit

        // ── SharedPreferences read stubs ──────────────────────────────────────
        every { prefs.edit() } returns editor
        every { prefs.getString(any(), anyNullable()) } answers {
            (storage[firstArg()] as? String) ?: secondArg<String?>()
        }
        every { prefs.getLong(any(), any()) } answers {
            (storage[firstArg<String>()] as? Long) ?: secondArg<Long>()
        }

        store = TokenStore(prefs)
    }

    // ─── save ─────────────────────────────────────────────────────────────────

    @Test
    fun `save persists all token fields`() {
        store.save(
            accessToken = "access",
            idToken = "id",
            refreshToken = "refresh",
            expiresAtMs = 999L,
            role = "teacher"
        )

        assertEquals("access", storage["access_token"])
        assertEquals("id", storage["id_token"])
        assertEquals("refresh", storage["refresh_token"])
        assertEquals(999L, storage["expires_at"])
        assertEquals("teacher", storage["role"])
    }

    @Test
    fun `save calls editor apply`() {
        store.save("a", "b", "c", 1L, "teacher")
        verify { editor.apply() }
    }

    @Test
    fun `save accepts null idToken and refreshToken`() {
        store.save(
            accessToken = "access",
            idToken = null,
            refreshToken = null,
            expiresAtMs = 0L,
            role = "student"
        )
        val loaded = store.load()
        assertNull(loaded?.idToken)
        assertNull(loaded?.refreshToken)
    }

    // ─── load ─────────────────────────────────────────────────────────────────

    @Test
    fun `load returns StoredTokens when access token is saved`() {
        store.save("access", "id", "refresh", 12345L, "admin")

        val result = store.load()

        assertEquals(
            StoredTokens(
                accessToken = "access",
                idToken = "id",
                refreshToken = "refresh",
                expiresAtMs = 12345L,
                role = "admin"
            ),
            result
        )
    }

    @Test
    fun `load returns null when no access token is stored`() {
        assertNull(store.load())
    }

    @Test
    fun `load defaults role to teacher when role key is absent`() {
        // Put only access token directly into the backing storage
        storage["access_token"] = "access"

        val result = store.load()
        assertEquals("teacher", result?.role)
    }

    @Test
    fun `load returns zero for expires_at when key is absent`() {
        storage["access_token"] = "access"
        val result = store.load()
        assertEquals(0L, result?.expiresAtMs)
    }

    // ─── clear ────────────────────────────────────────────────────────────────

    @Test
    fun `clear removes all stored data`() {
        store.save("access", "id", "refresh", 999L, "teacher")
        store.clear()

        assertTrue(storage.isEmpty())
    }

    @Test
    fun `clear makes load return null`() {
        store.save("access", "id", "refresh", 999L, "teacher")
        store.clear()

        assertNull(store.load())
    }

    @Test
    fun `clear calls editor apply`() {
        store.clear()
        verify { editor.apply() }
    }
}
