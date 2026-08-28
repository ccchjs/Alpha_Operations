package com.airemore.fieldapp.data.repository

import com.airemore.fieldapp.data.local.AppDatabase
import com.airemore.fieldapp.data.local.Company
import com.airemore.fieldapp.data.local.entity.LookupCacheEntity
import com.airemore.fieldapp.data.remote.ApiService
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class LookupData(
    val companies: List<Company>,
    val acBrands: List<String>,
    val acTypes: List<String>,
    val acCapacities: List<String>,
    val pmChecklistItems: List<String>,
    val repairChecklistItems: List<String>,
    val findingOptions: Map<String, List<String>>,
) {
    companion object {
        val EMPTY = LookupData(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyMap())
    }
}

class LookupRepository(private val api: ApiService, private val db: AppDatabase) {
    private val gson = Gson()

    /** Call on login and on manual "refresh" pull — requires network. */
    suspend fun refreshFromServer(): Boolean {
        return try {
            val response = api.getLookups()
            val body = response.body()
            if (!response.isSuccessful || body?.success != true) return false

            db.lookupDao().save(
                LookupCacheEntity(
                    companiesJson = gson.toJson(body.companies),
                    acBrandsJson = gson.toJson(body.ac_brands),
                    acTypesJson = gson.toJson(body.ac_types),
                    acCapacitiesJson = gson.toJson(body.ac_capacities),
                    pmChecklistItemsJson = gson.toJson(body.pm_checklist_items),
                    repairChecklistItemsJson = gson.toJson(body.repair_checklist_items),
                    findingOptionsJson = gson.toJson(body.repair_finding_options),
                    updatedAtMillis = System.currentTimeMillis(),
                )
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Works fully offline — reads whatever was cached last time there was signal. */
    suspend fun getCached(): LookupData {
        val cache = db.lookupDao().get() ?: return LookupData.EMPTY
        return cache.toLookupData()
    }

    fun observeCached(): Flow<LookupData> =
        db.lookupDao().observe().map { it?.toLookupData() ?: LookupData.EMPTY }

    private fun LookupCacheEntity.toLookupData(): LookupData = LookupData(
        companies = gson.fromJson(companiesJson, Array<CompanyJson>::class.java)
            .map { Company(it.id, it.name, it.address, it.type) },
        acBrands = gson.fromJson(acBrandsJson, Array<String>::class.java).toList(),
        acTypes = gson.fromJson(acTypesJson, Array<String>::class.java).toList(),
        acCapacities = gson.fromJson(acCapacitiesJson, Array<String>::class.java).toList(),
        pmChecklistItems = gson.fromJson(pmChecklistItemsJson, Array<String>::class.java).toList(),
        repairChecklistItems = gson.fromJson(repairChecklistItemsJson, Array<String>::class.java).toList(),
        findingOptions = gson.fromJson(findingOptionsJson, MapStringList::class.java) ?: emptyMap(),
    )

    private data class CompanyJson(val id: Int, val name: String, val address: String?, val type: String)
    private class MapStringList : LinkedHashMap<String, List<String>>()
}
