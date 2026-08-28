package com.airemore.fieldapp.data.repository

import android.content.Context
import com.airemore.fieldapp.data.local.AppDatabase
import com.airemore.fieldapp.data.local.InstallStartupCertificate
import com.airemore.fieldapp.data.local.InstallUnit
import com.airemore.fieldapp.data.local.SyncStatus
import com.airemore.fieldapp.data.local.entity.InstallFormEntity
import com.airemore.fieldapp.data.remote.ApiService
import com.airemore.fieldapp.sync.SyncScheduler
import com.airemore.fieldapp.sync.buildSitePhotoParts
import com.airemore.fieldapp.sync.toRequestBody
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import java.io.File

class InstallRepository(private val db: AppDatabase, private val context: Context) {
    private val dao get() = db.installDao()
    private val gson = Gson()

    fun observeAll(): Flow<List<InstallFormEntity>> = dao.observeAll()
    fun observePendingCount(): Flow<Int> = dao.observePendingCount()
    fun observeFailedCount(): Flow<Int> = dao.observeFailedCount()
    suspend fun getById(id: Long): InstallFormEntity? = dao.getById(id)

    suspend fun createDraft(createdByName: String): Long =
        dao.insert(InstallFormEntity(createdByName = createdByName))

    suspend fun saveDraft(form: InstallFormEntity) = dao.update(form)

    suspend fun submit(localId: Long) {
        val form = dao.getById(localId) ?: return
        dao.update(form.copy(syncStatus = SyncStatus.PENDING))
        SyncScheduler.scheduleImmediate(context)
    }

    suspend fun retry(localId: Long) {
        val form = dao.getById(localId) ?: return
        dao.update(form.copy(syncStatus = SyncStatus.PENDING))
        SyncScheduler.scheduleImmediate(context)
    }

    suspend fun delete(form: InstallFormEntity) {
        form.customerSignaturePath?.let { File(it).delete() }
        (form.photosBefore + form.photosAfter).forEach { File(it).delete() }
        dao.delete(form)
    }

    suspend fun trySyncOne(api: ApiService, form: InstallFormEntity): SyncOutcome {
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
                "pm_activity" to form.pmActivity,
                "startups" to form.startups.map { it.toApiMap() },
                "customer_name" to form.customerName,
                "customer_position" to form.customerPosition,
                "customer_signature_date" to form.customerSignatureDate,
                "customer_signature" to form.customerSignaturePath?.let { pngToDataUrl(it) },
                "coa_type" to form.coaType,
                "coa_date" to form.coaDate,
                "coa_generic_text" to form.coaGenericText,
            )
            val dataJson = gson.toJson(payload).toRequestBody()
            val photoParts = buildSitePhotoParts(form.photosBefore, form.photosAfter)

            val response = api.saveInstallation(dataJson, photoParts)
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
    suspend fun getPending(): List<InstallFormEntity> = dao.getByStatus(SyncStatus.PENDING)

    private fun pngToDataUrl(path: String): String? {
        val bytes = File(path).takeIf { it.exists() }?.readBytes() ?: return null
        val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        return "data:image/png;base64,$b64"
    }
}

private fun InstallUnit.toApiMap(): Map<String, Any?> = mapOf(
    "quantity" to quantity, "ac_type" to acType, "brand" to brand, "model" to model, "capacity" to capacity,
)

private fun InstallStartupCertificate.toApiMap(): Map<String, Any?> = mapOf(
    "serial_ahu_fcu" to serialAhuFcu, "serial_accu" to serialAccu,
    "pressure_suction_supply" to pressureSuctionSupply, "pressure_discharge_return" to pressureDischargeReturn,
    "temp_return_air" to tempReturnAir, "temp_supply_air" to tempSupplyAir,
    "temp_accu_in" to tempAccuIn, "temp_accu_out" to tempAccuOut,
    "voltage_l1_l2" to voltageL1L2, "voltage_l2_l3" to voltageL2L3, "voltage_l1_l3" to voltageL1L3,
    "ampere_t1" to ampereT1, "ampere_t2" to ampereT2, "ampere_t3" to ampereT3,
    "ambient_temp" to ambientTemp, "room_temp" to roomTemp,
    "pipe_dia_suction_supply" to pipeDiaSuctionSupply, "pipe_dia_discharge_return" to pipeDiaDischargeReturn,
    "pipe_dia_drain" to pipeDiaDrain,
    "pipe_len_biref_line" to pipeLenBirefLine, "pipe_len_drain_line" to pipeLenDrainLine,
    "wire_feeder_line" to wireFeederLine, "wire_control_wires" to wireControlWires,
    "breaker_size" to breakerSize,
    "pipe_insul_biref_line" to pipeInsulBirefLine, "pipe_insul_drain_line" to pipeInsulDrainLine,
    "remarks" to remarks, "witnessed_by" to witnessedBy,
)
