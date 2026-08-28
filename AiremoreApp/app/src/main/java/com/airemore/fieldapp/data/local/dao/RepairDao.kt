package com.airemore.fieldapp.data.local.dao

import androidx.room.*
import com.airemore.fieldapp.data.local.SyncStatus
import com.airemore.fieldapp.data.local.entity.RepairFormEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RepairDao {
    @Insert
    suspend fun insert(form: RepairFormEntity): Long

    @Update
    suspend fun update(form: RepairFormEntity)

    @Query("SELECT * FROM repair_forms WHERE localId = :id")
    suspend fun getById(id: Long): RepairFormEntity?

    @Query("SELECT * FROM repair_forms ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<RepairFormEntity>>

    @Query("SELECT * FROM repair_forms WHERE syncStatus = :status")
    suspend fun getByStatus(status: SyncStatus): List<RepairFormEntity>

    @Query("SELECT COUNT(*) FROM repair_forms WHERE syncStatus IN ('PENDING','SYNCING')")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM repair_forms WHERE syncStatus = 'FAILED'")
    fun observeFailedCount(): Flow<Int>

    @Query("UPDATE repair_forms SET syncStatus = :status, lastError = :error, syncAttempts = syncAttempts + 1 WHERE localId = :id")
    suspend fun markAttempt(id: Long, status: SyncStatus, error: String?)

    @Query("UPDATE repair_forms SET syncStatus = 'SYNCED', serverId = :serverId, serviceReportNo = :reportNo, lastError = NULL WHERE localId = :id")
    suspend fun markSynced(id: Long, serverId: Int, reportNo: String)

    @Delete
    suspend fun delete(form: RepairFormEntity)
}
