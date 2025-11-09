package com.example.deflate

import android.content.Context
import androidx.work.*
import com.example.deflate.workers.DiaryReminderWorker
import com.example.deflate.workers.MoodAnalysisWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit


 // Schedules notification workers using WorkManager

object NotificationScheduler {
    
    private const val DIARY_REMINDER_WORK_NAME = "diary_reminder_work"
    private const val MOOD_ANALYSIS_WORK_NAME = "mood_analysis_work"
    
    
    // Schedule all notification workers
  
    fun scheduleAllNotifications(context: Context) {
        scheduleDailyReminder(context)
        scheduleMoodAnalysis(context)
    }
    
    /**
     * Schedule daily diary reminder notification
     * Runs every day at 8 PM
     */
    fun scheduleDailyReminder(context: Context, hourOfDay: Int = 20) {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        
        // If target time has passed today, schedule for tomorrow
        if (targetTime.before(currentTime)) {
            targetTime.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        val initialDelay = targetTime.timeInMillis - currentTime.timeInMillis
        
        val reminderRequest = PeriodicWorkRequestBuilder<DiaryReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DIARY_REMINDER_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )
    }

    fun scheduleMoodAnalysis(context: Context) {
        val analysisRequest = PeriodicWorkRequestBuilder<MoodAnalysisWorker>(
            12, TimeUnit.HOURS
        )
            .setInitialDelay(1, TimeUnit.HOURS) // First check after 1 hour
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MOOD_ANALYSIS_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            analysisRequest
        )
    }
    

     // Cancel all scheduled notifications
 
    fun cancelAllNotifications(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(DIARY_REMINDER_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(MOOD_ANALYSIS_WORK_NAME)
    }
    

    fun updateReminderTime(context: Context, hourOfDay: Int) {
        WorkManager.getInstance(context).cancelUniqueWork(DIARY_REMINDER_WORK_NAME)
        scheduleDailyReminder(context, hourOfDay)
    }
}

