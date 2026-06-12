package com.journal.tests.data

import com.journal.core.data.repository.SessionRepository
import com.journal.core.database.JournalDatabase
import com.journal.core.database.dao.SessionDao
import com.journal.core.database.entity.SessionEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [SessionRepository].
 *
 * [SessionDao] and [JournalDatabase] are mocked with MockK.
 */
class SessionRepositoryTest {

    private lateinit var sessionDao: SessionDao
    private lateinit var db: JournalDatabase
    private lateinit var repository: SessionRepository

    @Before
    fun setUp() {
        sessionDao = mockk(relaxed = true)
        db = mockk(relaxed = true)
        coEvery { sessionDao.get() } returns null
        repository = SessionRepository(sessionDao, db)
    }

    // ─── observeSession ───────────────────────────────────────────────────────

    @Test
    fun `observeSession delegates to SessionDao observe`() = runTest {
        val entity = SessionEntity(role = "teacher", userId = "u1")
        every { sessionDao.observe() } returns flowOf(entity)

        val emitted = mutableListOf<SessionEntity?>()
        repository.observeSession().collect { emitted.add(it) }

        assertEquals(listOf(entity), emitted)
    }

    @Test
    fun `observeSession emits null when dao returns null`() = runTest {
        every { sessionDao.observe() } returns flowOf(null)

        val emitted = mutableListOf<SessionEntity?>()
        repository.observeSession().collect { emitted.add(it) }

        assertEquals(listOf<SessionEntity?>(null), emitted)
    }

    // ─── getSession ─���─────────────────────────────────────────────────────────

    @Test
    fun `getSession returns the entity from dao`() = runTest {
        val entity = SessionEntity(role = "admin", userId = "a1")
        coEvery { sessionDao.get() } returns entity

        assertEquals(entity, repository.getSession())
    }

    @Test
    fun `getSession returns null when dao returns null`() = runTest {
        coEvery { sessionDao.get() } returns null

        assertNull(repository.getSession())
    }

    // ─── saveSession ──────────────────────────────────────────────────────────

    @Test
    fun `saveSession upserts correct entity to dao`() = runTest {
        val upsertedSlot = slot<SessionEntity>()
        coEvery { sessionDao.upsert(capture(upsertedSlot)) } returns Unit

        repository.saveSession(role = "teacher", userId = "u42", fullName = "Иванов Иван")

        with(upsertedSlot.captured) {
            assertEquals("teacher", role)
            assertEquals("u42", userId)
            assertEquals("Иванов Иван", fullName)
        }
    }

    @Test
    fun `saveSession works with null optional fields`() = runTest {
        val upsertedSlot = slot<SessionEntity>()
        coEvery { sessionDao.upsert(capture(upsertedSlot)) } returns Unit

        repository.saveSession(role = "student")

        with(upsertedSlot.captured) {
            assertEquals("student", role)
            assertNull(userId)
            assertNull(fullName)
        }
    }

    // ─── clearAll ─────────────────────────────────────────────────────────────

    @Test
    fun `clearAll calls database clearAllTables`() = runTest {
        repository.clearAll()
        coVerify { db.clearAllTables() }
    }

    @Test
    fun `clearAll does not call sessionDao directly`() = runTest {
        repository.clearAll()
        coVerify(exactly = 0) { sessionDao.upsert(any()) }
    }
}
