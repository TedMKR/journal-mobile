package com.journal.core.model.notification

data class AppNotification(
    val id: String,
    val ownerKey: String,
    val role: String,
    val title: String,
    val message: String,
    val category: String,
    val targetRoute: String?,
    val createdAt: Long,
    val readAt: Long?
) {
    val isRead: Boolean
        get() = readAt != null
}

data class NotificationDraft(
    val sourceKey: String,
    val title: String,
    val message: String,
    val category: String = NotificationCategory.SYSTEM,
    val targetRoute: String? = null
)

object NotificationCategory {
    const val SYSTEM = "system"
    const val REMINDER = "reminder"
    const val ADMIN = "admin"
    const val STUDY = "study"
    const val SYNC = "sync"
}
