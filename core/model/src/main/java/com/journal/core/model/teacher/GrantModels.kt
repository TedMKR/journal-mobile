package com.journal.core.model.teacher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JournalAccessGrant(
    @SerialName("id") val id: String,
    @SerialName("granter_id") val granterId: String,
    @SerialName("grantee_id") val granteeId: String,
    @SerialName("discipline_id") val disciplineId: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("period_id") val periodId: String? = null,
    @SerialName("access_level") val accessLevel: String,
    @SerialName("granted_at") val grantedAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("revoked_at") val revokedAt: String? = null
)

@Serializable
data class JournalAccessGrantsResponse(
    @SerialName("data") val data: List<JournalAccessGrant> = emptyList(),
    @SerialName("total") val total: Int? = null
)

@Serializable
data class JournalAccessGrantResponse(
    @SerialName("data") val data: JournalAccessGrant? = null
)

@Serializable
data class GrantJournalAccessRequest(
    @SerialName("granter_id") val granterId: String? = null,
    @SerialName("grantee_id") val granteeId: String,
    @SerialName("discipline_id") val disciplineId: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("period_id") val periodId: String? = null,
    @SerialName("access_level") val accessLevel: String,
    @SerialName("expires_at") val expiresAt: String? = null
)
