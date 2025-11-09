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
        NotificationScheduler.scheduleAllNotifications(this)
    }
}

