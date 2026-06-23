package com.journal.core.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

@Singleton
class AndroidSystemNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationSettingsRepository: NotificationSettingsRepository
) {
    fun notifyGradeChange(event: StudentGradeNotificationEvent) {
        if (!notificationSettingsRepository.areGradeNotificationsEnabled()) return
        if (!canPostNotifications()) return

        ensureGradeChannel()

        val notification = NotificationCompat.Builder(context, GRADE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(event.title)
            .setContentText(event.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(event.message))
            .setContentIntent(launchAppIntent(event.notificationId))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context).notify(event.notificationId, notification)
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    private fun ensureGradeChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(GRADE_CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            GRADE_CHANNEL_ID,
            "Оценки",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Уведомления о новых и измененных оценках"
        }
        manager.createNotificationChannel(channel)
    }

    private fun launchAppIntent(requestCode: Int): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
            } ?: return null

        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private companion object {
        const val GRADE_CHANNEL_ID = "student_grade_updates"
    }
}

data class StudentGradeNotificationEvent(
    val type: StudentGradeNotificationType,
    val disciplineId: String,
    val disciplineName: String,
    val assessmentFormId: String,
    val assessmentTitle: String,
    val oldValue: String?,
    val newValue: String?
) {
    val title: String
        get() = when (type) {
            StudentGradeNotificationType.CREATED -> "Поставлена оценка"
            StudentGradeNotificationType.UPDATED -> "Изменена оценка"
            StudentGradeNotificationType.DELETED -> "Удалена оценка"
        }

    val message: String
        get() {
            val subject = "$disciplineName: $assessmentTitle"
            return when (type) {
                StudentGradeNotificationType.CREATED -> "$subject: ${newValue.orEmpty()}"
                StudentGradeNotificationType.UPDATED -> "$subject: ${oldValue.orEmpty()} -> ${newValue.orEmpty()}"
                StudentGradeNotificationType.DELETED -> "$subject: оценка ${oldValue.orEmpty()} удалена"
            }
        }

    val notificationId: Int
        get() = "$disciplineId|$assessmentFormId|$type|$oldValue|$newValue".hashCode().absoluteValue
}

enum class StudentGradeNotificationType {
    CREATED,
    UPDATED,
    DELETED
}
