package com.airemore.fieldapp

import android.app.Application
import com.airemore.fieldapp.data.local.AppDatabase
import com.airemore.fieldapp.data.prefs.SessionManager
import com.airemore.fieldapp.data.remote.NetworkModule
import com.airemore.fieldapp.data.repository.LookupRepository
import com.airemore.fieldapp.data.repository.PmRepository
import com.airemore.fieldapp.data.repository.RepairRepository
import com.airemore.fieldapp.data.repository.InstallRepository
import com.airemore.fieldapp.data.repository.AuthRepository
import com.airemore.fieldapp.sync.SyncScheduler

/**
 * A hand-rolled "service locator" instead of Hilt/Dagger — keeps the
 * project buildable with zero extra annotation-processor setup. For a
 * bigger app, swap this for Hilt; the repository interfaces below don't
 * need to change.
 */
class AiremoreApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var session: SessionManager
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var lookupRepository: LookupRepository
        private set
    lateinit var pmRepository: PmRepository
        private set
    lateinit var repairRepository: RepairRepository
        private set
    lateinit var installRepository: InstallRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.build(this)
        session = SessionManager(this)
        val api = NetworkModule.provideApiService(session)

        authRepository = AuthRepository(api, session, database)
        lookupRepository = LookupRepository(api, database)
        pmRepository = PmRepository(database, this)
        repairRepository = RepairRepository(database, this)
        installRepository = InstallRepository(database, this)

        // Kick a sync attempt immediately on process start (covers "app was
        // killed with pending records, signal is back now") and schedule a
        // periodic safety-net sync every 15 minutes while the OS allows it.
        SyncScheduler.scheduleImmediate(this)
        SyncScheduler.schedulePeriodic(this)
        SyncScheduler.observeConnectivity(this)
    }
}
