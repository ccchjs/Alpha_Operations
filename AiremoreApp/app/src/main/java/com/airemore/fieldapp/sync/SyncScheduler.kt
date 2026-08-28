package com.airemore.fieldapp.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Three ways SyncWorker gets triggered, so "signal comes back" always
 * eventually results in an upload without the staff having to do anything:
 *   1. scheduleImmediate() — called right after app start and right after
 *      "Submit" is tapped. Runs as soon as WorkManager sees a connection
 *      (could be instantly if already online).
 *   2. schedulePeriodic() — a 15-minute safety net in case #1 and #3 both
 *      somehow got missed (e.g. app process was killed).
 *   3. observeConnectivity() — a live OS callback for "network just became
 *      available", which triggers an immediate sync attempt the moment
 *      wifi/data reconnects, instead of waiting for the next periodic tick.
 */
object SyncScheduler {

    private const val IMMEDIATE_WORK = "airemore_sync_immediate"
    private const val PERIODIC_WORK = "airemore_sync_periodic"

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun scheduleImmediate(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()
        // REPLACE (not KEEP): every new trigger — a fresh Submit, or the
        // network coming back via observeConnectivity() — must be able to
        // cancel a previous attempt that's sitting in a growing exponential
        // backoff wait from an earlier failure. With KEEP, that stale
        // backoff timer wins and new/pending records just sit "queued"
        // even though signal is back, because WorkManager silently drops
        // the new request instead of giving the worker a fresh attempt.
        WorkManager.getInstance(context)
            .enqueueUniqueWork(IMMEDIATE_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(networkConstraints)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun observeConnectivity(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scheduleImmediate(context)
            }
        })
    }
}
