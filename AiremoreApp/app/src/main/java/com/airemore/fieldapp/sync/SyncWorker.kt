package com.airemore.fieldapp.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.airemore.fieldapp.AiremoreApp
import com.airemore.fieldapp.data.remote.NetworkModule
import com.airemore.fieldapp.data.repository.SyncOutcome

/**
 * Runs whenever WorkManager decides conditions are met (network available).
 * Walks every PENDING record across all three modules and tries to upload
 * it. This is the piece that makes the "queue" real: staff can keep
 * filling out forms with zero signal, tap Submit (which just flips a local
 * row to PENDING), and this worker quietly drains the queue the moment a
 * connection shows up — no action needed from the staff member.
 *
 * - Synced -> mark SYNCED, done.
 * - Rejected (4xx, e.g. missing required field) -> mark FAILED; the app
 *   will NOT auto-retry this one since resending the same bad data would
 *   just fail again. Staff needs to open it and fix something.
 * - Retryable (timeout/5xx) -> put back to PENDING; WorkManager's own
 *   retry/backoff plus the next periodic run will pick it up again.
 */
class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as AiremoreApp
        val session = app.session
        if (session.currentToken() == null) return Result.success() // not logged in, nothing to do

        val api = NetworkModule.provideApiService(session)
        var anyRetryable = false

        // --- PM ---
        for (form in app.pmRepository.getPending()) {
            app.pmRepository.markSyncing(form.localId)
            when (val outcome = app.pmRepository.trySyncOne(api, form)) {
                is SyncOutcome.Synced -> app.pmRepository.markSynced(form.localId, outcome.serverId, outcome.reportNo)
                is SyncOutcome.Rejected -> app.pmRepository.markFailed(form.localId, outcome.message)
                is SyncOutcome.Retryable -> { app.pmRepository.markPendingAgain(form.localId, outcome.message); anyRetryable = true }
            }
        }

        // --- Repair ---
        for (form in app.repairRepository.getPending()) {
            app.repairRepository.markSyncing(form.localId)
            when (val outcome = app.repairRepository.trySyncOne(api, form)) {
                is SyncOutcome.Synced -> app.repairRepository.markSynced(form.localId, outcome.serverId, outcome.reportNo)
                is SyncOutcome.Rejected -> app.repairRepository.markFailed(form.localId, outcome.message)
                is SyncOutcome.Retryable -> { app.repairRepository.markPendingAgain(form.localId, outcome.message); anyRetryable = true }
            }
        }

        // --- Installation ---
        for (form in app.installRepository.getPending()) {
            app.installRepository.markSyncing(form.localId)
            when (val outcome = app.installRepository.trySyncOne(api, form)) {
                is SyncOutcome.Synced -> app.installRepository.markSynced(form.localId, outcome.serverId, outcome.reportNo)
                is SyncOutcome.Rejected -> app.installRepository.markFailed(form.localId, outcome.message)
                is SyncOutcome.Retryable -> { app.installRepository.markPendingAgain(form.localId, outcome.message); anyRetryable = true }
            }
        }

        // If something failed due to network (not rejected by the server),
        // ask WorkManager to retry with its built-in exponential backoff.
        return if (anyRetryable) Result.retry() else Result.success()
    }
}
