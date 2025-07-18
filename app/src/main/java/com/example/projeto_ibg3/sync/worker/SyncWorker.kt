package com.example.projeto_ibg3.sync.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.projeto_ibg3.data.local.database.AppDatabase
import com.example.projeto_ibg3.data.remote.api.ApiConfig
import com.example.projeto_ibg3.sync.utils.ExponentialBackoffRetry
import com.example.projeto_ibg3.sync.service.SyncService
import com.example.projeto_ibg3.sync.model.SyncResult

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val retryHelper = ExponentialBackoffRetry()

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val apiService = ApiConfig.getApiService()
            val syncService = SyncService(database, apiService, applicationContext)

            // Usa retry exponencial
            val result = retryHelper.execute {
                syncService.syncData()
            }

            when (result) {
                is SyncResult.SUCCESS -> Result.success()
                is SyncResult.NO_NETWORK -> Result.retry()
                is SyncResult.InProgress -> Result.retry()
                is SyncResult.ERROR -> Result.failure()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}