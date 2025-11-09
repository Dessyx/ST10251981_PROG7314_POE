package com.example.deflate.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.deflate.NotificationHelper

/**
 * Worker to send daily diary reminder notifications
 */
class DiaryReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            // Send diary reminder notification
            NotificationHelper.showDiaryReminderNotification(applicationContext)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

