package com.airemore.fieldapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.airemore.fieldapp.data.local.PmParticular
import com.airemore.fieldapp.data.local.PmUnit
import com.airemore.fieldapp.data.local.SyncStatus
import java.util.UUID

@Entity(tableName = "pm_forms")
data class PmFormEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val clientUuid: String = UUID.randomUUID().toString(),

    val companyId: Int? = null,
    val companyName: String = "",
    val address: String = "",
    val formDate: String = "",          // yyyy-MM-dd
    val workOrderNo: String = "",

    // True when the selected company's type is "sm_store" — switches the
    // form from per-unit AC readings/checklist to the Particulars-based
    // SM Store variant (see pm/sm_store_form.php on the web).
    val isSmStore: Boolean = false,

    val personnel: List<String> = emptyList(), // names 2..10 (name 1 = logged-in user, added at submit time)

    // Regular companies only:
    val units: List<PmUnit> = emptyList(),

    // SM Store companies only:
    val smChecklist: List<String> = emptyList(), // checked item keys, e.g. ["A1","A3","B2"]
    val particulars: List<PmParticular> = emptyList(),
    val pmStatement: String = "",

    // Certification of Accomplishment — optional, all companies.
    val coaType: String = "none", // "none" | "generic" | "coa"
    val coaMonthYear: String? = null, // "coa" type only, e.g. "APRIL 2026"
    val coaDate: String? = null,
    val coaGenericText: String? = null,

    val findings: String = "",
    val afi: List<String> = emptyList(),
    val afiOther: String = "",
    val recommendation: List<String> = emptyList(),
    val recommendationOther: String = "",
    val actionTaken: List<String> = emptyList(),
    val actionTakenOther: String = "",
    val ali: List<String> = emptyList(),
    val aliOther: String = "",

    val nextPmDate: String? = null,

    val customerName: String = "",
    val customerPosition: String = "",
    val customerSignatureDate: String? = null,
    val customerSignaturePath: String? = null, // local PNG file path

    val syncStatus: SyncStatus = SyncStatus.DRAFT,
    val syncAttempts: Int = 0,
    val lastError: String? = null,
    val serverId: Int? = null,
    val serviceReportNo: String? = null,

    val createdAtMillis: Long = System.currentTimeMillis(),
    val createdByName: String = "",
)
