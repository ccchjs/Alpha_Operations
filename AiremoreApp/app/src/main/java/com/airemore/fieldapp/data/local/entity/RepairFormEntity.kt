package com.airemore.fieldapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.airemore.fieldapp.data.local.RepairUnit
import com.airemore.fieldapp.data.local.SyncStatus
import java.util.UUID

@Entity(tableName = "repair_forms")
data class RepairFormEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val clientUuid: String = UUID.randomUUID().toString(),

    val companyId: Int? = null,
    val companyName: String = "",
    val address: String = "",
    val formDate: String = "",
    val workOrderNo: String = "",

    val personnel: List<String> = emptyList(),
    val units: List<RepairUnit> = emptyList(),

    val findings: String = "",
    val afi: List<String> = emptyList(),
    val afiOther: String = "",
    val recommendation: List<String> = emptyList(),
    val recommendationOther: String = "",
    val actionTaken: List<String> = emptyList(),
    val actionTakenOther: String = "",
    val ali: List<String> = emptyList(),
    val aliOther: String = "",

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
