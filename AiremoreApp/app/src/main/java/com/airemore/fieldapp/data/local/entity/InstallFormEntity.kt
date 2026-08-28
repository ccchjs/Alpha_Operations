package com.airemore.fieldapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.airemore.fieldapp.data.local.InstallUnit
import com.airemore.fieldapp.data.local.SyncStatus
import java.util.UUID

@Entity(tableName = "installation_forms")
data class InstallFormEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val clientUuid: String = UUID.randomUUID().toString(),

    val companyId: Int? = null,
    val companyName: String = "",
    val address: String = "",
    val formDate: String = "",
    val workOrderNo: String = "",

    val personnel: List<String> = emptyList(),
    val units: List<InstallUnit> = emptyList(),

    val pmActivity: String = "", // free-text "details" of the install job
    val photosBefore: List<String> = emptyList(),
    val photosAfter: List<String> = emptyList(),

    val customerName: String = "",
    val customerPosition: String = "",
    val customerSignatureDate: String? = null,
    val customerSignaturePath: String? = null,

    val syncStatus: SyncStatus = SyncStatus.DRAFT,
    val syncAttempts: Int = 0,
    val lastError: String? = null,
    val serverId: Int? = null,
    val serviceReportNo: String? = null,

    val createdAtMillis: Long = System.currentTimeMillis(),
    val createdByName: String = "",
)
