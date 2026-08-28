package com.airemore.fieldapp.data.repository

import com.airemore.fieldapp.data.local.Company
import com.airemore.fieldapp.data.local.AppDatabase
import com.airemore.fieldapp.data.local.PmParticular
import com.airemore.fieldapp.data.local.SmChecklistGroup
import com.airemore.fieldapp.data.local.SmChecklistItem
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
    val smStoreChecklistGroups: List<SmChecklistGroup> = emptyList(),
) {
    companion object {
        val EMPTY = LookupData(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyMap(), emptyList())
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
                    smChecklistGroupsJson = gson.toJson(body.sm_store_checklist_groups),
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

    /** SM Store PM form: fetch the particulars assigned to a company. Requires network. */
    suspend fun fetchCompanyParticulars(companyId: Int): List<PmParticular>? {
        return try {
            val response = api.getCompanyParticulars(companyId)
            val body = response.body()
            if (!response.isSuccessful || body?.success != true) return null
            body.particulars
                .sortedBy { it.sort_order }
                .map { PmParticular(item = it.particular_name) }
        } catch (e: Exception) {
            null
        }
    }

    private fun LookupCacheEntity.toLookupData(): LookupData = LookupData(
        companies = gson.fromJson(companiesJson, Array<CompanyJson>::class.java)
            .map { Company(it.id, it.name, it.address, it.type, it.branch_name) },
        acBrands = gson.fromJson(acBrandsJson, Array<String>::class.java).toList(),
        acTypes = gson.fromJson(acTypesJson, Array<String>::class.java).toList(),
        acCapacities = gson.fromJson(acCapacitiesJson, Array<String>::class.java).toList(),
        pmChecklistItems = gson.fromJson(pmChecklistItemsJson, Array<String>::class.java).toList(),
        repairChecklistItems = gson.fromJson(repairChecklistItemsJson, Array<String>::class.java).toList(),
        findingOptions = gson.fromJson(findingOptionsJson, MapStringList::class.java) ?: emptyMap(),
        smStoreChecklistGroups = gson.fromJson(smChecklistGroupsJson, Array<SmGroupJson>::class.java)
            ?.map { g -> SmChecklistGroup(g.letter, g.title, g.items.map { SmChecklistItem(it.key, it.text) }) }
            ?: emptyList(),
    )

    private data class CompanyJson(val id: Int, val name: String, val address: String?, val type: String, val branch_name: String? = null)
    private data class SmItemJson(val key: String, val text: String)
    private data class SmGroupJson(val letter: String, val title: String, val items: List<SmItemJson> = emptyList())
    private class MapStringList : LinkedHashMap<String, List<String>>()
}
