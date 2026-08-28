package com.airemore.fieldapp.data.remote

import com.airemore.fieldapp.BuildConfig
import com.airemore.fieldapp.data.prefs.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the single Retrofit instance the whole app shares. The auth
 * interceptor reads the saved token from DataStore and attaches it as
 * "Authorization: Bearer <token>" to every request automatically — no
 * screen or repository has to remember to do it.
 */
object NetworkModule {

    fun provideApiService(session: SessionManager): ApiService {
        val authInterceptor = Interceptor { chain ->
            val token = runBlocking { session.currentToken() }
            val request = chain.request().newBuilder().apply {
                if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token")
            }.build()
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

        // Field connections (worksite wifi/mobile data) can be slow — long
        // timeouts so a big multi-photo PM submission doesn't get killed
        // mid-upload right when signal is weak.
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
