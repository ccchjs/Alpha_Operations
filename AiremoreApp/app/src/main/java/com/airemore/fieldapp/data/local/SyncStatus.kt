package com.airemore.fieldapp.data.local

/**
 * Every record the staff creates goes through this lifecycle:
 *
 *   DRAFT   -> staff is still filling it out (not queued for upload yet)
 *   PENDING -> "Submit" was tapped; queued, waiting for a network connection
 *   SYNCING -> upload currently in flight
 *   SYNCED  -> server confirmed and returned a server_id + service_report_no
 *   FAILED  -> server rejected it (validation error) — needs the user to
 *              fix something and resubmit; WorkManager will NOT auto-retry
 *              a FAILED record (only a network-timeout leaves it PENDING
 *              for auto-retry, see SyncWorker)
 */
enum class SyncStatus {
    DRAFT, PENDING, SYNCING, SYNCED, FAILED
}
