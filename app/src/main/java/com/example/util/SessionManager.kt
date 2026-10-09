package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.HomeHelpDatabase
import com.example.data.local.SessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("hh_partner_secure_prefs", Context.MODE_PRIVATE)
    private val db = HomeHelpDatabase.getDatabase(context)
    private val sessionDao = db.sessionDao()

    suspend fun saveSession(partnerId: String, mobileNumber: String): String = withContext(Dispatchers.IO) {
        val token = "hh_partner_jwt_${UUID.randomUUID().toString().replace("-", "")}"
        prefs.edit()
            .putString(KEY_SESSION_TOKEN, token)
            .putString(KEY_PARTNER_ID, partnerId)
            .putString(KEY_MOBILE, mobileNumber)
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .apply()

        sessionDao.setSession(
            SessionEntity(
                id = 1,
                partnerId = partnerId,
                sessionToken = token,
                mobileNumber = mobileNumber,
                createdAt = System.currentTimeMillis()
            )
        )
        token
    }

    suspend fun getActiveSessionToken(): String? = withContext(Dispatchers.IO) {
        val dbSession = sessionDao.getActiveSession()
        val prefToken = prefs.getString(KEY_SESSION_TOKEN, null)
        if (dbSession != null && dbSession.sessionToken == prefToken) {
            prefToken
        } else {
            dbSession?.sessionToken ?: prefToken
        }
    }

    suspend fun getActivePartnerId(): String? = withContext(Dispatchers.IO) {
        val dbSession = sessionDao.getActiveSession()
        dbSession?.partnerId ?: prefs.getString(KEY_PARTNER_ID, null)
    }

    suspend fun clearSession() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
        sessionDao.clearSession()
    }

    companion object {
        private const val KEY_SESSION_TOKEN = "session_token"
        private const val KEY_PARTNER_ID = "partner_id"
        private const val KEY_MOBILE = "mobile_number"
        private const val KEY_TIMESTAMP = "timestamp"
    }
}
