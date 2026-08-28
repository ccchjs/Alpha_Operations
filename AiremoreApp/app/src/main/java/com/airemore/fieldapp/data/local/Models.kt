package com.airemore.fieldapp.data.local

import java.util.UUID

/**
 * These are plain Kotlin data classes (not Room entities themselves) that
 * get stored as JSON text inside a single row via Room TypeConverters
 * (see Converters.kt). This keeps one PM/Repair/Installation record as
 * ONE row you can read/write atomically while the multi-step wizard is
 * open, instead of juggling five separate tables and foreign keys for a
 * record that only exists locally until it syncs. The server-side PHP API
 * is the one that explodes this back into normalized tables.
 */

data class ChecklistEntry(
    val itemName: String,
    var status: String = "",   // "check" | "x" | ""
    var remarks: String = "",
)

/** One AC unit inside a PM form: brand/model/type/capacity + 12 before/after readings + its own 17-item checklist + up to 2 before/after photos. */
data class PmUnit(
    val clientUnitUuid: String = UUID.randomUUID().toString(),
    var brand: String = "",
    var model: String = "",
    var acType: String = "",
    var capacity: String = "",
    var voltageBefore: String = "",
    var voltageAfter: String = "",
    var currentBefore: String = "",
    var currentAfter: String = "",
    var suctionPressureBefore: String = "",
    var suctionPressureAfter: String = "",
    var dischargePressureBefore: String = "",
    var dischargePressureAfter: String = "",
    var tempSupplyBefore: String = "",
    var tempSupplyAfter: String = "",
    var tempReturnBefore: String = "",
    var tempReturnAfter: String = "",
    var checklist: MutableList<ChecklistEntry> = mutableListOf(),
    var photosBefore: MutableList<String> = mutableListOf(), // local file:// paths, max 2
    var photosAfter: MutableList<String> = mutableListOf(),
)

/** One AC unit inside a Repair form: adds location/serial, no checklist (matches Repair_units). */
data class RepairUnit(
    val clientUnitUuid: String = UUID.randomUUID().toString(),
    var location: String = "",
    var brand: String = "",
    var serialNumber: String = "",
    var model: String = "",
    var acType: String = "",
    var capacity: String = "",
    var voltageBefore: String = "",
    var voltageAfter: String = "",
    var currentBefore: String = "",
    var currentAfter: String = "",
    var suctionPressureBefore: String = "",
    var suctionPressureAfter: String = "",
    var dischargePressureBefore: String = "",
    var dischargePressureAfter: String = "",
    var tempSupplyBefore: String = "",
    var tempSupplyAfter: String = "",
    var tempReturnBefore: String = "",
    var tempReturnAfter: String = "",
    var photosBefore: MutableList<String> = mutableListOf(),
    var photosAfter: MutableList<String> = mutableListOf(),
)

/** One AC unit inside an Installation record: quantity/type/brand/model/capacity, no readings. */
data class InstallUnit(
    val clientUnitUuid: String = UUID.randomUUID().toString(),
    var quantity: String = "1",
    var acType: String = "",
    var brand: String = "",
    var model: String = "",
    var capacity: String = "",
)

data class Company(
    val id: Int,
    val name: String,
    val address: String?,
    val type: String,
    val branchName: String? = null,
) {
    val isSmStore: Boolean get() = type == "sm_store"
}

/** One row of the SM Store PM form's "Particulars" section (company_particulars → pm_form_particulars). */
data class PmParticular(
    val item: String = "",
    var tempBefore: String = "",
    var tempAfter: String = "",
    var status: String = "",
)

/** One block of the Installation form's Start-Up Certificate sub-form (28 technical fields, admin/engineer-filled). */
data class InstallStartupCertificate(
    val clientBlockUuid: String = UUID.randomUUID().toString(),
    var serialAhuFcu: String = "",
    var serialAccu: String = "",
    var pressureSuctionSupply: String = "",
    var pressureDischargeReturn: String = "",
    var tempReturnAir: String = "",
    var tempSupplyAir: String = "",
    var tempAccuIn: String = "",
    var tempAccuOut: String = "",
    var voltageL1L2: String = "",
    var voltageL2L3: String = "",
    var voltageL1L3: String = "",
    var ampereT1: String = "",
    var ampereT2: String = "",
    var ampereT3: String = "",
    var ambientTemp: String = "",
    var roomTemp: String = "",
    var pipeDiaSuctionSupply: String = "",
    var pipeDiaDischargeReturn: String = "",
    var pipeDiaDrain: String = "",
    var pipeLenBirefLine: String = "",
    var pipeLenDrainLine: String = "",
    var wireFeederLine: String = "",
    var wireControlWires: String = "",
    var breakerSize: String = "",
    var pipeInsulBirefLine: String = "",
    var pipeInsulDrainLine: String = "",
    var remarks: String = "",
    var witnessedBy: String = "",
)

/** One checked item in the SM Store PM Checklist (grouped A/B/C — see lookups.php sm_store_checklist_groups). */
data class SmChecklistItem(val key: String, val text: String)
data class SmChecklistGroup(val letter: String, val title: String, val items: List<SmChecklistItem>)
