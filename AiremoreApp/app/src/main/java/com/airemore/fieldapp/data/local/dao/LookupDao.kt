package com.airemore.fieldapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.airemore.fieldapp.data.local.entity.LookupCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LookupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(cache: LookupCacheEntity)

    @Query("SELECT * FROM lookup_cache WHERE id = 1")
    suspend fun get(): LookupCacheEntity?

    @Query("SELECT * FROM lookup_cache WHERE id = 1")
    fun observe(): Flow<LookupCacheEntity?>
}
