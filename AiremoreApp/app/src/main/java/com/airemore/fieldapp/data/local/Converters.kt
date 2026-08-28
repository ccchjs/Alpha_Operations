package com.airemore.fieldapp.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String = status.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)

    @TypeConverter
    fun fromStringList(list: List<String>): String = gson.toJson(list)

    @TypeConverter
    fun toStringList(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    @TypeConverter
    fun fromPmUnitList(list: List<PmUnit>): String = gson.toJson(list)

    @TypeConverter
    fun toPmUnitList(json: String): List<PmUnit> {
        if (json.isBlank()) return emptyList()
        val type = object : TypeToken<List<PmUnit>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    @TypeConverter
    fun fromRepairUnitList(list: List<RepairUnit>): String = gson.toJson(list)

    @TypeConverter
    fun toRepairUnitList(json: String): List<RepairUnit> {
        if (json.isBlank()) return emptyList()
        val type = object : TypeToken<List<RepairUnit>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    @TypeConverter
    fun fromInstallUnitList(list: List<InstallUnit>): String = gson.toJson(list)

    @TypeConverter
    fun toInstallUnitList(json: String): List<InstallUnit> {
        if (json.isBlank()) return emptyList()
        val type = object : TypeToken<List<InstallUnit>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    @TypeConverter
    fun fromPmParticularList(list: List<PmParticular>): String = gson.toJson(list)

    @TypeConverter
    fun toPmParticularList(json: String): List<PmParticular> {
        if (json.isBlank()) return emptyList()
        val type = object : TypeToken<List<PmParticular>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    @TypeConverter
    fun fromInstallStartupCertificateList(list: List<InstallStartupCertificate>): String = gson.toJson(list)

    @TypeConverter
    fun toInstallStartupCertificateList(json: String): List<InstallStartupCertificate> {
        if (json.isBlank()) return emptyList()
        val type = object : TypeToken<List<InstallStartupCertificate>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
}
