package com.airemore.fieldapp.data.repository

import android.content.Context
import com.airemore.fieldapp.data.local.AppDatabase
import com.airemore.fieldapp.data.local.PmUnit
import com.airemore.fieldapp.data.local.SyncStatus
import com.airemore.fieldapp.data.local.entity.PmFormEntity
import com.airemore.fieldapp.data.remote.ApiService
import com.airemore.fieldapp.sync.SyncScheduler
import com.airemore.fieldapp.sync.buildPhotoParts
import com.airemore.fieldapp.sync.toRequestBody
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import java.io.File

sealed class SyncOutcome {
    data class Synced(val serverId: Int, val reportNo: String) : SyncOutcome()
    /** Server rejected it (bad data) — don't keep retrying automatically. */
    data class Rejected(val message: String) : SyncOutcome()
    /** Network/timeout — safe to leave PENDING and retry later. */
    data class Retryable(val message: String) : SyncOutcome()
}

class PmRepository(private val db: AppDatabase, private val context: Context) {
    private val dao get() = db.pmDao()
    private val gson = Gson()

    fun observeAll(): Flow<List<PmFormEntity>> = dao.observeAll()
    fun observePendingCount(): Flow<Int> = dao.observePendingCount()
    fun observeFailedCount(): Flow<Int> = dao.observeFailedCount()
    suspend fun getById(id: Long): PmFormEntity? = dao.getById(id)

    suspend fun createDraft(createdByName: String): Long =
        dao.insert(PmFormEntity(createdByName = createdByName))

    suspend fun saveDraft(form: PmFormEntity) = dao.update(form)

    /** Staff tapped "Submit" — queue it and try to sync right away if online. */
    suspend fun submit(localId: Long) {
        val form = dao.getById(localId) ?: return
        dao.update(form.copy(syncStatus = SyncStatus.PENDING))
        SyncScheduler.scheduleImmediate(context)
    }

    /** Staff fixed something on a FAILED record and wants to try again. */
    suspend fun retry(localId: Long) {
        val form = dao.getById(localId) ?: return
        dao.update(form.copy(syncStatus = SyncStatus.PENDING))
        SyncScheduler.scheduleImmediate(context)
    }

    suspend fun delete(form: PmFormEntity) {
        form.customerSignaturePath?.let { File(it).delete() }
        form.units.forEach { u -> (u.photosBefore + u.photosAfter).forEach { File(it).delete() } }
        dao.delete(form)
    }

    /** Called by SyncWorker for every PENDING record. Never throws. */
    suspend fun trySyncOne(api: ApiService, form: PmFormEntity): SyncOutcome {
        return try {
            val payload = mapOf(
                "client_uuid" to form.clientUuid,
                "company_id" to form.companyId,
                "company_name" to form.companyName,
                "address" to form.address,
                "form_date" to form.formDate,
                "work_order_no" to form.workOrderNo,
                "personnel" to form.personnel,
                "units" to form.units.map { it.toApiMap() },
                "findings" to form.findings,
                "afi" to form.afi, "afi_other" to form.afiOther,
                "recommendation" to form.recommendation, "recommendation_other" to form.recommendationOther,
                "action_taken" to form.actionTaken, "action_taken_other" to form.actionTakenOther,
                "ali" to form.ali, "ali_other" to form.aliOther,
                "next_pm_date" to form.nextPmDate,
                "customer_name" to form.customerName,
                "customer_position" to form.customerPosition,
                "customer_signature_date" to form.customerSignatureDate,
                "customer_signature" to form.customerSignaturePath?.let { pngToDataUrl(it) },
            )
            val dataJson = gson.toJson(payload).toRequestBody()
            val photoParts = buildPhotoParts(
                form.units.mapIndexed { index, u -> index to (u.photosBefore to u.photosAfter) }
            )

            val response = api.savePm(dataJson, photoParts)
            val body = response.body()

            when {
                response.isSuccessful && body?.success == true ->
                    SyncOutcome.Synced(body.server_id ?: -1, body.service_report_no ?: "")
                response.code() in 400..499 ->
                    SyncOutcome.Rejected(body?.message ?: "Tinanggihan ng server (${response.code()}).")
                else ->
                    SyncOutcome.Retryable(body?.message ?: "Server error (${response.code()}).")
            }
        } catch (e: Exception) {
            SyncOutcome.Retryable(e.message ?: "Walang connection.")
        }
    }

    suspend fun markSyncing(localId: Long) = dao.markAttempt(localId, SyncStatus.SYNCING, null)
    suspend fun markSynced(localId: Long, serverId: Int, reportNo: String) = dao.markSynced(localId, serverId, reportNo)
    suspend fun markFailed(localId: Long, error: String) = dao.markAttempt(localId, SyncStatus.FAILED, error)
    suspend fun markPendingAgain(localId: Long, error: String) = dao.markAttempt(localId, SyncStatus.PENDING, error)
    suspend fun getPending(): List<PmFormEntity> = dao.getByStatus(SyncStatus.PENDING)

    private fun pngToDataUrl(path: String): String? {
        val bytes = File(path).takeIf { it.exists() }?.readBytes() ?: return null
        val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        return "data:image/png;base64,$b64"
    }
}

private fun PmUnit.toApiMap(): Map<String, Any?> = mapOf(
    "client_unit_uuid" to clientUnitUuid,
    "brand" to brand, "model" to model, "ac_type" to acType, "capacity" to capacity,
    "voltage_before" to voltageBefore.ifBlank { null },
    "voltage_after" to voltageAfter.ifBlank { null },
    "current_before" to currentBefore.ifBlank { null },
    "current_after" to currentAfter.ifBlank { null },
    "suction_pressure_before" to suctionPressureBefore.ifBlank { null },
    "suction_pressure_after" to suctionPressureAfter.ifBlank { null },
    "discharge_pressure_before" to dischargePressureBefore.ifBlank { null },
    "discharge_pressure_after" to dischargePressureAfter.ifBlank { null },
    "temp_supply_before" to tempSupplyBefore.ifBlank { null },
    "temp_supply_after" to tempSupplyAfter.ifBlank { null },
    "temp_return_before" to tempReturnBefore.ifBlank { null },
    "temp_return_after" to tempReturnAfter.ifBlank { null },
    "checklist" to checklist.map { mapOf("item_name" to it.itemName, "status" to it.status, "remarks" to it.remarks) },
)
