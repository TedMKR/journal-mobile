package com.journal.tests.common

import android.content.SharedPreferences
import com.journal.core.common.config.StoredTokens
import com.journal.core.common.config.TokenStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TokenStore].
 *
 * Instead of mocking [SharedPreferences] with MockK (which struggles with
 * nullable String? matchers), we use a lightweight hand-written fake that
 * stores key-value pairs in a plain [MutableMap].
 */
class TokenStoreTest {

    // ─── In-memory fake SharedPreferences ────────────────────────────────────

    private val storage = mutableMapOf<String, Any?>()
    private var applyCallCount = 0

    private val fakeEditor = object : SharedPreferences.Editor {
        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            storage[key] = value; return this
        }
        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            storage[key] = value; return this
        }
        override fun clear(): SharedPreferences.Editor {
            storage.clear(); return this
        }
        override fun apply() { applyCallCount++ }
        override fun commit(): Boolean { applyCallCount++; return true }
        // Unused overrides — required by the interface
        override fun putInt(key: String, value: Int) = this
        override fun putBoolean(key: String, value: Boolean) = this
        override fun putFloat(key: String, value: Float) = this
        override fun putStringSet(key: String, values: MutableSet<String>?) = this
        override fun remove(key: String) = this
    }

    private val fakePrefs = object : SharedPreferences {
        override fun edit(): SharedPreferences.Editor = fakeEditor
        override fun getString(key: String, defValue: String?): String? =
            (storage[key] as? String) ?: defValue
        override fun getLong(key: String, defValue: Long): Long =
            (storage[key] as? Long) ?: defValue
        override fun contains(key: String): Boolean = storage.containsKey(key)
        override fun getAll(): Map<String, *> = storage.toMap()
        override fun getInt(key: String, defValue: Int) = (storage[key] as? Int) ?: defValue
        override fun getFloat(key: String, defValue: Float) = (storage[key] as? Float) ?: defValue
        override fun getBoolean(key: String, defValue: Boolean) =
            (storage[key] as? Boolean) ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?) = defValues
        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener
        ) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener
        ) = Unit
    }

    private lateinit var store: TokenStore

    @Before
    fun setUp() {
        storage.clear()
        applyCallCount = 0
        store = TokenStore(fakePrefs)
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
        assertTrue("apply() was not called", applyCallCount > 0)
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

        assertEquals(
            StoredTokens(
                accessToken = "access",
                idToken = "id",
                refreshToken = "refresh",
                expiresAtMs = 12345L,
                role = "admin"
            ),
            store.load()
        )
    }

    @Test
    fun `load returns null when no access token is stored`() {
        assertNull(store.load())
    }

    @Test
    fun `load defaults role to teacher when role key is absent`() {
        // Put only access token directly in the backing storage
        storage["access_token"] = "access"
        assertEquals("teacher", store.load()?.role)
    }

    @Test
    fun `load returns zero for expires_at when key is absent`() {
        storage["access_token"] = "access"
        assertEquals(0L, store.load()?.expiresAtMs)
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
        assertTrue("apply() was not called after clear()", applyCallCount > 0)
    }
}
