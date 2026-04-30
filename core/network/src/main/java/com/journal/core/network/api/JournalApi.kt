package com.journal.core.network.api

import com.journal.core.model.teacher.LessonsResponse
import retrofit2.http.GET
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
}
