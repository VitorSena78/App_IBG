package com.example.projeto_ibg3.sync.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.projeto_ibg3.data.local.database.AppDatabase
import com.example.projeto_ibg3.data.remote.api.ApiService
import com.example.projeto_ibg3.sync.utils.ExponentialBackoffRetry
import com.example.projeto_ibg3.sync.service.SyncService
import com.example.projeto_ibg3.sync.model.SyncResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncService: SyncService,
) : CoroutineWorker(context, params) {

    private val retryHelper = ExponentialBackoffRetry()

    override suspend fun doWork(): Result {
        return try {

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