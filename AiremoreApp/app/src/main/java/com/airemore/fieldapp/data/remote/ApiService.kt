package com.airemore.fieldapp.data.remote

import com.airemore.fieldapp.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("auth_login.php")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("auth_logout.php")
    suspend fun logout(): Response<LoginResponse>

    @GET("lookups.php")
    suspend fun getLookups(): Response<LookupsResponse>

    /** SM Store PM form: particulars assigned to the selected company. */
    @GET("company_particulars.php")
    suspend fun getCompanyParticulars(@Query("company_id") companyId: Int): Response<CompanyParticularsResponse>

    @POST("companies_add.php")
    suspend fun addCompany(@Body body: AddCompanyRequest): Response<AddCompanyResponse>

    @Multipart
    @POST("pm_save.php")
    suspend fun savePm(
        @Part("data") data: RequestBody,
        @Part photos: List<MultipartBody.Part>,
    ): Response<SaveFormResponse>

    @GET("pm_list.php")
    suspend fun listPm(@Query("limit") limit: Int = 50): Response<ListRecordsResponse>

    @Multipart
    @POST("repair_save.php")
    suspend fun saveRepair(
        @Part("data") data: RequestBody,
        @Part photos: List<MultipartBody.Part>,
    ): Response<SaveFormResponse>

    @GET("repair_list.php")
    suspend fun listRepair(@Query("limit") limit: Int = 50): Response<ListRecordsResponse>

    @Multipart
    @POST("installation_save.php")
    suspend fun saveInstallation(
        @Part("data") data: RequestBody,
        @Part photos: List<MultipartBody.Part>,
    ): Response<SaveFormResponse>

    @GET("installation_list.php")
    suspend fun listInstallation(@Query("limit") limit: Int = 50): Response<ListRecordsResponse>
}
