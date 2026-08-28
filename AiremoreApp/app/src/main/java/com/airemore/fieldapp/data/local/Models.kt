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
)
