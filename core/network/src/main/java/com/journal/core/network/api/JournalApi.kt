package com.journal.core.network.api

import com.journal.core.model.teacher.JournalGridResponse
import com.journal.core.model.teacher.LessonsResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface JournalApi {

    @GET("lessons")
    suspend fun getLessons(
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null,
        @Query("period_id") periodId: String? = null,
        @Query("group_id") groupId: String? = null,
        @Query("discipline_id") disciplineId: String? = null
    ): LessonsResponse

    @GET("groups/{group_id}/journal")
    suspend fun getGroupJournalGrid(
        @Path("group_id") groupId: String,
        @Query("discipline_id") disciplineId: String,
        @Query("academic_period_id") academicPeriodId: String,
        @Query("subgroup_id") subgroupId: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): JournalGridResponse
}
