package com.journal.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.journal.core.database.entity.DashboardCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardCacheDao {

    @Query("SELECT * FROM dashboard_cache WHERE cache_key = :key")
    fun observe(key: String): Flow<DashboardCacheEntity?>

    @Query("SELECT * FROM dashboard_cache WHERE cache_key = :key")
    suspend fun get(key: String): DashboardCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DashboardCacheEntity)
}
