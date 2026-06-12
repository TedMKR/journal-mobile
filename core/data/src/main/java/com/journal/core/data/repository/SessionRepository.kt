package com.journal.core.data.repository

import com.journal.core.database.JournalDatabase
import com.journal.core.database.dao.SessionDao
import com.journal.core.database.entity.SessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the current user session in Room.
 * Session is cleared entirely on logout (together with all cached data).
 */
@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val db: JournalDatabase
) {

    fun observeSession(): Flow<SessionEntity?> = sessionDao.observe()

    suspend fun getSession(): SessionEntity? = sessionDao.get()

    suspend fun saveSession(
        role: String,
        userId: String? = null,
        fullName: String? = null,
        lastOnlineAt: Long = System.currentTimeMillis()
    ) {
        val existing = sessionDao.get()
        sessionDao.upsert(
            SessionEntity(
                role = role,
                userId = userId ?: existing?.userId,
                fullName = fullName ?: existing?.fullName,
                lastOnlineAt = lastOnlineAt,
                offlineAllowedUntil = lastOnlineAt + SessionEntity.DEFAULT_OFFLINE_TTL_MS
            )
        )
    }

    /**
     * Clears ALL local data — session + every cached table.
     * Must be called on logout so no user data leaks to the next account.
     */
    suspend fun clearAll() {
        db.clearAllTables()
    }
}
