package com.journal.features.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journal.core.model.notification.AppNotification
import com.journal.core.model.notification.NotificationCategory
import com.journal.core.ui.AppBackground
import com.journal.core.ui.AppBadge
import com.journal.core.ui.AppDangerButton
import com.journal.core.ui.AppDangerLight
import com.journal.core.ui.AppHeaderBackground
import com.journal.core.ui.AppMutedText
import com.journal.core.ui.AppPrimary
import com.journal.core.ui.AppPrimaryButton
import com.journal.core.ui.AppSectionCard
import com.journal.core.ui.AppSecondaryButton
import com.journal.core.ui.AppSuccess
import com.journal.core.ui.AppSuccessLight
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NotificationsRoute(
    role: String?,
    userId: String?,
    onOpenRoute: (String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()

    LaunchedEffect(role, userId) {
        viewModel.activate(role, userId)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            NotificationsToolbar(
                unreadCount = unreadCount,
                hasReadNotifications = notifications.any { it.isRead },
                hasNotifications = notifications.isNotEmpty(),
                onMarkAllRead = viewModel::markAllRead,
                onDeleteRead = viewModel::deleteRead
            )
        }

        if (notifications.isEmpty()) {
            item {
                EmptyNotificationsCard()
            }
        } else {
            items(
                items = notifications,
                key = { it.id }
            ) { notification ->
                NotificationCard(
                    notification = notification,
                    onMarkRead = { viewModel.markRead(notification.id) },
                    onDelete = { viewModel.delete(notification.id) },
                    onOpenRoute = {
                        viewModel.markRead(notification.id)
                        notification.targetRoute?.let(onOpenRoute)
                    }
                )
            }
        }
    }
}

@Composable
private fun NotificationsToolbar(
    unreadCount: Int,
    hasReadNotifications: Boolean,
    hasNotifications: Boolean,
    onMarkAllRead: () -> Unit,
    onDeleteRead: () -> Unit
) {
    AppSectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Уведомления",
                        color = AppPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (unreadCount == 0) "Новых уведомлений нет" else "Непрочитанных: $unreadCount",
                        color = AppMutedText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                AppBadge(
                    text = unreadCount.toString(),
                    color = if (unreadCount > 0) Color.White else AppPrimary,
                    backgroundColor = if (unreadCount > 0) AppPrimary else AppHeaderBackground
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppPrimaryButton(
                    text = "Прочитать всё",
                    onClick = onMarkAllRead,
                    enabled = unreadCount > 0,
                    modifier = Modifier.weight(1f)
                )
                AppSecondaryButton(
                    text = "Удалить прочитанные",
                    onClick = onDeleteRead,
                    modifier = Modifier.weight(1f),
                    containerColor = if (hasReadNotifications) Color.White else AppBackground
                )
            }
            if (!hasNotifications) {
                Text(
                    text = "Новые события появятся здесь автоматически.",
                    color = AppMutedText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun EmptyNotificationsCard() {
    AppSectionCard {
        Text(
            text = "Список пуст",
            color = AppPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Когда появятся новые события, они будут показаны здесь.",
            color = AppMutedText,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun NotificationCard(
    notification: AppNotification,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit,
    onOpenRoute: () -> Unit
) {
    val accentBackground = if (notification.isRead) Color.White else Color(0xFFF4F6FF)
    AppSectionCard(backgroundColor = accentBackground) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryBadge(notification.category)
                        if (!notification.isRead) {
                            AppBadge(
                                text = "Новое",
                                color = AppSuccess,
                                backgroundColor = AppSuccessLight,
                                horizontalPadding = 8,
                                verticalPadding = 4
                            )
                        }
                    }
                    Text(
                        text = notification.title,
                        color = AppPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Text(
                    text = formatNotificationTime(notification.createdAt),
                    color = AppMutedText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
            Text(
                text = notification.message,
                color = AppMutedText,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (notification.targetRoute != null) {
                    AppPrimaryButton(
                        text = "Открыть",
                        onClick = onOpenRoute,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (!notification.isRead) {
                    AppSecondaryButton(
                        text = "Прочитано",
                        onClick = onMarkRead,
                        modifier = Modifier.weight(1f)
                    )
                }
                AppDangerButton(
                    text = "Удалить",
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    containerColor = Color(0xFFC44A4A)
                )
            }
        }
    }
}

@Composable
private fun CategoryBadge(category: String) {
    val label = when (category) {
        NotificationCategory.ADMIN -> "Администрирование"
        NotificationCategory.REMINDER -> "Напоминание"
        NotificationCategory.STUDY -> "Учёба"
        NotificationCategory.SYNC -> "Синхронизация"
        else -> "Система"
    }
    val background = when (category) {
        NotificationCategory.ADMIN -> Color(0xFFE9ECF8)
        NotificationCategory.STUDY -> Color(0xFFE7F1FF)
        NotificationCategory.REMINDER -> Color(0xFFFFF4D6)
        NotificationCategory.SYNC -> Color(0xFFE8F7EF)
        else -> AppHeaderBackground
    }
    AppBadge(
        text = label,
        color = AppPrimary,
        backgroundColor = background,
        horizontalPadding = 8,
        verticalPadding = 4
    )
}

private fun formatNotificationTime(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
