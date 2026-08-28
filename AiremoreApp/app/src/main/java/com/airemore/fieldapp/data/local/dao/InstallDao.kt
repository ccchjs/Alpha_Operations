package com.airemore.fieldapp.data.local.dao

import androidx.room.*
import com.airemore.fieldapp.data.local.SyncStatus
import com.airemore.fieldapp.data.local.entity.InstallFormEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstallDao {
    @Insert
    suspend fun insert(form: InstallFormEntity): Long

    @Update
    suspend fun update(form: InstallFormEntity)

    @Query("SELECT * FROM installation_forms WHERE localId = :id")
    suspend fun getById(id: Long): InstallFormEntity?

    @Query("SELECT * FROM installation_forms ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<InstallFormEntity>>

    @Query("SELECT * FROM installation_forms WHERE syncStatus = :status")
    suspend fun getByStatus(status: SyncStatus): List<InstallFormEntity>

    @Query("SELECT COUNT(*) FROM installation_forms WHERE syncStatus IN ('PENDING','SYNCING')")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM installation_forms WHERE syncStatus = 'FAILED'")
    fun observeFailedCount(): Flow<Int>

    @Query("UPDATE installation_forms SET syncStatus = :status, lastError = :error, syncAttempts = syncAttempts + 1 WHERE localId = :id")
    suspend fun markAttempt(id: Long, status: SyncStatus, error: String?)

    @Query("UPDATE installation_forms SET syncStatus = 'SYNCED', serverId = :serverId, serviceReportNo = :reportNo, lastError = NULL WHERE localId = :id")
    suspend fun markSynced(id: Long, serverId: Int, reportNo: String)

    @Delete
    suspend fun delete(form: InstallFormEntity)
}
