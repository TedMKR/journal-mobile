package com.journal.features.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.journal.core.data.repository.NotificationRepository
import com.journal.core.model.notification.AppNotification
import com.journal.core.model.notification.NotificationCategory
import com.journal.core.model.notification.NotificationDraft
import com.journal.shared.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    private var context: NotificationOwnerContext? = null
    private var notificationsJob: Job? = null
    private var unreadJob: Job? = null
    private var syncJob: Job? = null

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    fun activate(role: String?, userId: String?) {
        val normalizedRole = role?.takeIf(String::isNotBlank) ?: return
        val ownerKey = repository.ownerKey(normalizedRole, userId)
        if (context?.ownerKey == ownerKey) return

        val nextContext = NotificationOwnerContext(
            ownerKey = ownerKey,
            role = normalizedRole
        )
        context = nextContext
        notificationsJob?.cancel()
        unreadJob?.cancel()
        syncJob?.cancel()

        notificationsJob = repository.observeNotifications(ownerKey)
            .onEach { _notifications.value = it }
            .launchIn(viewModelScope)

        unreadJob = repository.observeUnreadCount(ownerKey)
            .onEach { _unreadCount.value = it }
            .launchIn(viewModelScope)

        syncJob = repository.observePendingSyncCount()
            .onEach { count ->
                if (count > 0) {
                    repository.add(
                        ownerKey = nextContext.ownerKey,
                        role = nextContext.role,
                        draft = NotificationDraft(
                            sourceKey = SYNC_PENDING_SOURCE,
                            title = "Есть несинхронизированные изменения",
                            message = "Ожидают отправки на сервер: $count. Проверьте подключение и дождитесь синхронизации.",
                            category = NotificationCategory.SYNC,
                            targetRoute = roleStartRoute(nextContext.role)
                        )
                    )
                } else {
                    repository.deleteBySource(nextContext.ownerKey, SYNC_PENDING_SOURCE)
                }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            repository.ensureDefaults(
                ownerKey = nextContext.ownerKey,
                role = nextContext.role,
                drafts = defaultDrafts(nextContext.role)
            )
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch {
            repository.markRead(id)
        }
    }

    fun markAllRead() {
        val ownerKey = context?.ownerKey ?: return
        viewModelScope.launch {
            repository.markAllRead(ownerKey)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    fun deleteRead() {
        val ownerKey = context?.ownerKey ?: return
        viewModelScope.launch {
            repository.deleteRead(ownerKey)
        }
    }

    private fun defaultDrafts(role: String): List<NotificationDraft> {
        val common = listOf(
            NotificationDraft(
                sourceKey = "system-center-enabled",
                title = "Центр уведомлений включён",
                message = "Здесь будут появляться важные события, напоминания и переходы к нужным разделам.",
                category = NotificationCategory.SYSTEM,
                targetRoute = Routes.NOTIFICATIONS
            )
        )
        val roleDrafts = when (role) {
            "admin" -> listOf(
                NotificationDraft(
                    sourceKey = "admin-audit-review",
                    title = "Проверьте аудит",
                    message = "Журнал административных событий доступен из уведомлений.",
                    category = NotificationCategory.ADMIN,
                    targetRoute = Routes.ADMIN_AUDIT
                ),
                NotificationDraft(
                    sourceKey = "admin-problem-students",
                    title = "Проблемные студенты",
                    message = "Откройте сводку студентов с высокой долей неудовлетворительных оценок.",
                    category = NotificationCategory.STUDY,
                    targetRoute = Routes.ADMIN_PROBLEM_STUDENTS
                )
            )
            "methodologist" -> listOf(
                NotificationDraft(
                    sourceKey = "methodist-journals",
                    title = "Журналы методиста",
                    message = "Можно быстро перейти к списку журналов и создать новый журнал.",
                    category = NotificationCategory.STUDY,
                    targetRoute = Routes.METHODIST_JOURNALS
                ),
                NotificationDraft(
                    sourceKey = "methodist-templates",
                    title = "Шаблоны КТП",
                    message = "Проверьте список КТП, импорт тем и назначение шаблонов.",
                    category = NotificationCategory.REMINDER,
                    targetRoute = Routes.METHODIST_TEMPLATES
                )
            )
            "student" -> listOf(
                NotificationDraft(
                    sourceKey = "student-schedule",
                    title = "Расписание занятий",
                    message = "Откройте расписание и перейдите к журналу по нужной дисциплине.",
                    category = NotificationCategory.STUDY,
                    targetRoute = Routes.STUDENT_SCHEDULE
                ),
                NotificationDraft(
                    sourceKey = "student-dashboard",
                    title = "Личный кабинет",
                    message = "Проверьте учебную сводку и карточки дисциплин.",
                    category = NotificationCategory.REMINDER,
                    targetRoute = Routes.STUDENT_DASHBOARD
                )
            )
            else -> listOf(
                NotificationDraft(
                    sourceKey = "teacher-schedule",
                    title = "Расписание преподавателя",
                    message = "Откройте расписание и перейдите к журналу занятия.",
                    category = NotificationCategory.STUDY,
                    targetRoute = Routes.TEACHER_HOME
                ),
                NotificationDraft(
                    sourceKey = "teacher-ved",
                    title = "Ведомости",
                    message = "Ведомости доступны из центра уведомлений и бокового меню.",
                    category = NotificationCategory.REMINDER,
                    targetRoute = Routes.TEACHER_VED
                )
            )
        }
        return common + roleDrafts
    }

    private fun roleStartRoute(role: String): String = when (role) {
        "admin" -> Routes.ADMIN_DASHBOARD
        "methodologist" -> Routes.METHODIST_DASHBOARD
        "student" -> Routes.STUDENT_SCHEDULE
        else -> Routes.TEACHER_HOME
    }

    private companion object {
        const val SYNC_PENDING_SOURCE = "sync-pending"
    }
}

private data class NotificationOwnerContext(
    val ownerKey: String,
    val role: String
)
