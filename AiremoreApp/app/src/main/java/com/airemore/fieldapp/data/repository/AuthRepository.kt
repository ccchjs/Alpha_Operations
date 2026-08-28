package com.airemore.fieldapp.data.repository

import android.os.Build
import com.airemore.fieldapp.data.local.AppDatabase
import com.airemore.fieldapp.data.prefs.SessionManager
import com.airemore.fieldapp.data.remote.ApiService
import com.airemore.fieldapp.data.remote.dto.LoginRequest

sealed class AuthResult {
    data object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(
    private val api: ApiService,
    private val session: SessionManager,
    private val db: AppDatabase,
) {
    suspend fun login(username: String, password: String): AuthResult {
        return try {
            val deviceLabel = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifBlank { "Android device" }
            val response = api.login(LoginRequest(username.trim(), password, deviceLabel))
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.token != null && body.user != null) {
                session.saveSession(
                    token = body.token,
                    userId = body.user.id,
                    username = body.user.username,
                    fullName = body.user.full_name,
                    role = body.user.role,
                )
                AuthResult.Success
            } else {
                AuthResult.Error(body?.message ?: "Hindi na-verify ang username/password.")
            }
        } catch (e: Exception) {
            AuthResult.Error("Walang connection. Suriin ang internet at subukan ulit. (${e.message ?: "network error"})")
        }
    }

    suspend fun logout() {
        try { api.logout() } catch (_: Exception) { /* best-effort; clear local session regardless */ }
        session.clear()
    }
}
