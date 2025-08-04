package com.example.projeto_ibg3.sync.core

import android.content.SharedPreferences
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncStateManager @Inject constructor(
    private val preferences: SharedPreferences
) {
    companion object {
        private const val KEY_LAST_SYNC = "last_sync_timestamp"
        private const val KEY_SYNC_IN_PROGRESS = "sync_in_progress"
        private const val KEY_DEVICE_ID = "device_id"
    }

    fun getLastSyncTimestamp(): Long {
        return preferences.getLong(KEY_LAST_SYNC, 0L)
    }

    fun updateLastSyncTimestamp() {
        preferences.edit()
            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
            .apply()
    }

    fun isSyncInProgress(): Boolean {
        return preferences.getBoolean(KEY_SYNC_IN_PROGRESS, false)
    }

    fun setSyncInProgress(inProgress: Boolean) {
        preferences.edit()
            .putBoolean(KEY_SYNC_IN_PROGRESS, inProgress)
            .apply()
    }

    fun getDeviceId(): String {
        return preferences.getString(KEY_DEVICE_ID, null) ?: run {
            val newId = UUID.randomUUID().toString()
            preferences.edit().putString(KEY_DEVICE_ID, newId).apply()
            newId
        }
    }
}