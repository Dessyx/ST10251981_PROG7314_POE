package com.example.deflate.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.deflate.MoodAnalyzer
import com.example.deflate.NotificationHelper
import com.google.firebase.auth.FirebaseAuth


 // Worker to analyze mood patterns and send crisis support notifications if needed
class MoodAnalysisWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            
            if (currentUser != null) {
                val moodAnalyzer = MoodAnalyzer(applicationContext)
                
                // Checks if user is in crisis
                val isInCrisis = moodAnalyzer.checkForCrisis(currentUser.uid)
                
                if (isInCrisis) {
                    // Send crisis support notification
                    NotificationHelper.showCrisisNotification(applicationContext)
                    moodAnalyzer.markCrisisNotificationSent()
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

