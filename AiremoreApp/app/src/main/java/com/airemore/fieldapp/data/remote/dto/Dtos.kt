package com.airemore.fieldapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequest(val username: String, val password: String, val device_label: String)

data class LoginResponse(
    val success: Boolean,
    val message: String? = null,
    val token: String? = null,
    val user: UserDto? = null,
)

data class UserDto(
    val id: Int,
    val username: String,
    val full_name: String,
    val role: String,
)

data class CompanyDto(
    val id: Int,
    val name: String,
    val address: String?,
    val type: String,
    val branch_name: String? = null,
)

data class SmChecklistItemDto(val key: String, val text: String)
data class SmChecklistGroupDto(val letter: String, val title: String, val items: List<SmChecklistItemDto> = emptyList())

data class LookupsResponse(
    val success: Boolean,
    val message: String? = null,
    val companies: List<CompanyDto> = emptyList(),
    val ac_brands: List<String> = emptyList(),
    val ac_types: List<String> = emptyList(),
    val ac_capacities: List<String> = emptyList(),
    val pm_checklist_items: List<String> = emptyList(),
    val repair_checklist_items: List<String> = emptyList(),
    val repair_finding_options: Map<String, List<String>> = emptyMap(),
    val sm_store_checklist_groups: List<SmChecklistGroupDto> = emptyList(),
)

/** Response of GET /api/company_particulars.php?company_id=… (SM Store PM form). */
data class CompanyParticularDto(val particular_name: String, val sort_order: Int = 0)
data class CompanyParticularsResponse(
    val success: Boolean,
    val message: String? = null,
    val particulars: List<CompanyParticularDto> = emptyList(),
)

data class AddCompanyRequest(val name: String, val address: String, val type: String = "regular")
data class AddCompanyResponse(val success: Boolean, val message: String? = null, val company: CompanyDto? = null)

/** Common response shape for pm_save.php / repair_save.php / installation_save.php. */
data class SaveFormResponse(
    val success: Boolean,
    val message: String? = null,
    val already_existed: Boolean = false,
    val server_id: Int? = null,
    val service_report_no: String? = null,
    val work_order_no: String? = null,
)

data class RecordSummaryDto(
    val id: Int,
    val service_report_no: String,
    val work_order_no: String?,
    val company_name: String,
    val form_date: String,
    val status: String,
    val created_at: String,
)

data class ListRecordsResponse(
    val success: Boolean,
    val message: String? = null,
    val records: List<RecordSummaryDto> = emptyList(),
)
