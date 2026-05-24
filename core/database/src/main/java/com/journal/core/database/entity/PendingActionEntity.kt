package com.journal.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Offline action queue.
 * Each row represents a user action (mark attendance, create/update/delete grade, etc.)
 * that was performed without internet connection and needs to be synced to the server.
 */
@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    /**
     * Action type constant — see [PendingActionType].
     */
    @ColumnInfo(name = "action_type")
    val actionType: String,

    /**
     * The server-side ID of the entity being affected.
     * For CREATE actions this may be a locally generated UUID (prefixed with "local_").
     */
    @ColumnInfo(name = "entity_id")
    val entityId: String,

    /** Serialized JSON of the request body */
    @ColumnInfo(name = "payload_json")
    val payloadJson: String,

    /** Extra context needed to build the API call */
    @ColumnInfo(name = "group_id")
    val groupId: String = "",

    @ColumnInfo(name = "discipline_id")
    val disciplineId: String = "",

    @ColumnInfo(name = "period_id")
    val periodId: String = "",

    @ColumnInfo(name = "lesson_type")
    val lessonType: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    /** How many times syncing this action has been attempted and failed */
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,

    /**
     * Non-null when the last sync attempt returned an unrecoverable error
     * (e.g. 404 / 409 conflict). The worker will skip such actions.
     */
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null
)

object PendingActionType {
    const val MARK_ATTENDANCE = "MARK_ATTENDANCE"
    const val CREATE_GRADE = "CREATE_GRADE"
    const val UPDATE_GRADE = "UPDATE_GRADE"
    const val DELETE_GRADE = "DELETE_GRADE"
    const val CREATE_ASSESSMENT_FORM = "CREATE_ASSESSMENT_FORM"
    const val UPDATE_ASSESSMENT_FORM = "UPDATE_ASSESSMENT_FORM"
    const val DELETE_ASSESSMENT_FORM = "DELETE_ASSESSMENT_FORM"
    const val UPDATE_LESSON_TOPIC = "UPDATE_LESSON_TOPIC"
}
