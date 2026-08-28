package com.airemore.fieldapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.airemore.fieldapp.data.local.dao.InstallDao
import com.airemore.fieldapp.data.local.dao.LookupDao
import com.airemore.fieldapp.data.local.dao.PmDao
import com.airemore.fieldapp.data.local.dao.RepairDao
import com.airemore.fieldapp.data.local.entity.InstallFormEntity
import com.airemore.fieldapp.data.local.entity.LookupCacheEntity
import com.airemore.fieldapp.data.local.entity.PmFormEntity
import com.airemore.fieldapp.data.local.entity.RepairFormEntity

@Database(
    entities = [
        PmFormEntity::class,
        RepairFormEntity::class,
        InstallFormEntity::class,
        LookupCacheEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pmDao(): PmDao
    abstract fun repairDao(): RepairDao
    abstract fun installDao(): InstallDao
    abstract fun lookupDao(): LookupDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /**
         * v1 -> v2: adds SM Store PM support (isSmStore/smChecklist/particulars/
         * pmStatement), Certification of Accomplishment on all three form types,
         * and Installation Start-Up Certificates. Written as ALTER TABLE ADD
         * COLUMN (not fallbackToDestructiveMigration) so a field tech with a
         * pending unsynced draft on their device doesn't lose it on app update.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pm_forms ADD COLUMN isSmStore INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pm_forms ADD COLUMN smChecklist TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE pm_forms ADD COLUMN particulars TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE pm_forms ADD COLUMN pmStatement TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE pm_forms ADD COLUMN coaType TEXT NOT NULL DEFAULT 'none'")
                db.execSQL("ALTER TABLE pm_forms ADD COLUMN coaMonthYear TEXT")
                db.execSQL("ALTER TABLE pm_forms ADD COLUMN coaDate TEXT")
                db.execSQL("ALTER TABLE pm_forms ADD COLUMN coaGenericText TEXT")

                db.execSQL("ALTER TABLE repair_forms ADD COLUMN coaType TEXT NOT NULL DEFAULT 'none'")
                db.execSQL("ALTER TABLE repair_forms ADD COLUMN coaDate TEXT")
                db.execSQL("ALTER TABLE repair_forms ADD COLUMN coaGenericText TEXT")

                db.execSQL("ALTER TABLE installation_forms ADD COLUMN startups TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE installation_forms ADD COLUMN coaType TEXT NOT NULL DEFAULT 'none'")
                db.execSQL("ALTER TABLE installation_forms ADD COLUMN coaDate TEXT")
                db.execSQL("ALTER TABLE installation_forms ADD COLUMN coaGenericText TEXT")
            }
        }

        fun build(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "airemore_field.db",
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
