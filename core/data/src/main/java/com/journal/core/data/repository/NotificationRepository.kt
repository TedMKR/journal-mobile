package com.journal.core.data.repository

import com.journal.core.database.dao.NotificationDao
import com.journal.core.database.dao.PendingActionDao
import com.journal.core.database.entity.NotificationEntity
import com.journal.core.model.notification.AppNotification
import com.journal.core.model.notification.NotificationDraft
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationDao: NotificationDao,
    private val pendingActionDao: PendingActionDao
) {
    fun ownerKey(role: String, userId: String?): String {
        val normalizedUserId = userId?.trim().orEmpty()
        return if (normalizedUserId.isNotBlank()) {
            "user:$normalizedUserId"
        } else {
            "role:$role"
        }
    }

    fun observeNotifications(ownerKey: String): Flow<List<AppNotification>> =
        notificationDao.observeActive(ownerKey).map { notifications ->
            notifications.map(NotificationEntity::toModel)
        }

    fun observeUnreadCount(ownerKey: String): Flow<Int> =
        notificationDao.observeUnreadCount(ownerKey)

    fun observePendingSyncCount(): Flow<Int> =
        pendingActionDao.observePendingCount()

    suspend fun ensureDefaults(ownerKey: String, role: String, drafts: List<NotificationDraft>) {
        val now = System.currentTimeMillis()
        drafts.forEach { draft ->
            notificationDao.insertIgnore(
                NotificationEntity(
                    id = stableId(ownerKey, draft.sourceKey),
                    ownerKey = ownerKey,
                    role = role,
                    sourceKey = draft.sourceKey,
                    title = draft.title,
                    message = draft.message,
                    category = draft.category,
                    targetRoute = draft.targetRoute,
                    createdAt = now
                )
            )
        }
    }

    suspend fun add(ownerKey: String, role: String, draft: NotificationDraft) {
        notificationDao.upsert(
            NotificationEntity(
                id = stableId(ownerKey, draft.sourceKey),
                ownerKey = ownerKey,
                role = role,
                sourceKey = draft.sourceKey,
                title = draft.title,
                message = draft.message,
                category = draft.category,
                targetRoute = draft.targetRoute,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun markRead(id: String) {
        notificationDao.markRead(id, System.currentTimeMillis())
    }

    suspend fun markAllRead(ownerKey: String) {
        notificationDao.markAllRead(ownerKey, System.currentTimeMillis())
    }

    suspend fun delete(id: String) {
        notificationDao.delete(id, System.currentTimeMillis())
    }

    suspend fun deleteBySource(ownerKey: String, sourceKey: String) {
        notificationDao.deleteBySource(ownerKey, sourceKey, System.currentTimeMillis())
    }

    suspend fun deleteRead(ownerKey: String) {
        notificationDao.deleteRead(ownerKey, System.currentTimeMillis())
    }

    private fun stableId(ownerKey: String, sourceKey: String): String {
        val bytes = "$ownerKey:$sourceKey".toByteArray(StandardCharsets.UTF_8)
        return "local_${UUID.nameUUIDFromBytes(bytes)}"
    }
}

private fun NotificationEntity.toModel(): AppNotification =
    AppNotification(
        id = id,
        ownerKey = ownerKey,
        role = role,
        title = title,
        message = message,
        category = category,
        targetRoute = targetRoute,
        createdAt = createdAt,
        readAt = readAt
    )
