package com.example.deflate

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class DeflateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Enable Firestore offline persistence
        val db = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        db.firestoreSettings = settings
        
        // Initialize SyncManager
        SyncManager.initialize(this)
        
        // Initialize notification system
        NotificationHelper.createNotificationChannels(this)

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true) 
        if (notificationsEnabled) {

            val frequency = prefs.getString("reminder_frequency", "daily") ?: "daily"
            when (frequency) {
                "daily" -> NotificationScheduler.scheduleAllNotifications(this)
                "weekly" -> NotificationScheduler.scheduleWeeklyReminder(this)
                "monthly" -> NotificationScheduler.scheduleMonthlyReminder(this)
                else -> NotificationScheduler.scheduleAllNotifications(this)
            }
        }
    }
}

