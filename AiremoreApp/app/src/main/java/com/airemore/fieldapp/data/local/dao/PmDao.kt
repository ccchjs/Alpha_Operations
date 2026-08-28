package com.airemore.fieldapp.data.local.dao

import androidx.room.*
import com.airemore.fieldapp.data.local.SyncStatus
import com.airemore.fieldapp.data.local.entity.PmFormEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PmDao {
    @Insert
    suspend fun insert(form: PmFormEntity): Long

    @Update
    suspend fun update(form: PmFormEntity)

    @Query("SELECT * FROM pm_forms WHERE localId = :id")
    suspend fun getById(id: Long): PmFormEntity?

    @Query("SELECT * FROM pm_forms ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<PmFormEntity>>

    @Query("SELECT * FROM pm_forms WHERE syncStatus = :status")
    suspend fun getByStatus(status: SyncStatus): List<PmFormEntity>

    @Query("SELECT COUNT(*) FROM pm_forms WHERE syncStatus IN ('PENDING','SYNCING')")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pm_forms WHERE syncStatus = 'FAILED'")
    fun observeFailedCount(): Flow<Int>

    @Query("UPDATE pm_forms SET syncStatus = :status, lastError = :error, syncAttempts = syncAttempts + 1 WHERE localId = :id")
    suspend fun markAttempt(id: Long, status: SyncStatus, error: String?)

    @Query("UPDATE pm_forms SET syncStatus = 'SYNCED', serverId = :serverId, serviceReportNo = :reportNo, lastError = NULL WHERE localId = :id")
    suspend fun markSynced(id: Long, serverId: Int, reportNo: String)

    @Delete
    suspend fun delete(form: PmFormEntity)
}
