package com.airemore.fieldapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
    version = 1,
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

        fun build(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "airemore_field.db",
                ).build().also { instance = it }
            }
    }
}
