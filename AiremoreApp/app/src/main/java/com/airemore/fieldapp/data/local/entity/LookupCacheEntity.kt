package com.airemore.fieldapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row cache of everything /api/lookups.php returns, stored as raw
 * JSON blobs. Fetched once on login (and refreshed on pull-to-refresh
 * while online); read by every "New Form" screen so it works with zero
 * signal. See LookupRepository for the typed accessors.
 */
@Entity(tableName = "lookup_cache")
data class LookupCacheEntity(
    @PrimaryKey val id: Int = 1,
    val companiesJson: String = "[]",
    val acBrandsJson: String = "[]",
    val acTypesJson: String = "[]",
    val acCapacitiesJson: String = "[]",
    val pmChecklistItemsJson: String = "[]",
    val repairChecklistItemsJson: String = "[]",
    val findingOptionsJson: String = "{}", // {afi:[], recommendation:[], action_taken:[], ali:[]}
    val updatedAtMillis: Long = 0,
)
