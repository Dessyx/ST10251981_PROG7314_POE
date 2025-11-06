package com.example.deflate

import android.content.Context
import com.example.deflate.repository.ActivityRepository
import com.example.deflate.repository.DiaryRepository
import com.example.deflate.repository.MoodRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SyncManager {
    private var diaryRepository: DiaryRepository? = null
    private var activityRepository: ActivityRepository? = null
    private var moodRepository: MoodRepository? = null
    
    fun initialize(context: Context) {
        diaryRepository = DiaryRepository(context)
        activityRepository = ActivityRepository(context)
        moodRepository = MoodRepository(context)
    }
    
    fun syncAll(userId: String) {
        val scope = CoroutineScope(Dispatchers.IO)
        
        scope.launch {
            // Sync from Firestore to local
            diaryRepository?.syncFromFirestore(userId)
            activityRepository?.syncFromFirestore(userId)
            moodRepository?.syncFromFirestore(userId)
            
            // Sync unsynced local entries to Firestore
            diaryRepository?.syncUnsyncedEntries(userId)
            activityRepository?.syncUnsyncedActivities(userId)
            moodRepository?.syncUnsyncedMoodEntries(userId)
        }
    }
    
    fun syncUnsynced(userId: String) {
        val scope = CoroutineScope(Dispatchers.IO)
        
        scope.launch {
            diaryRepository?.syncUnsyncedEntries(userId)
            activityRepository?.syncUnsyncedActivities(userId)
            moodRepository?.syncUnsyncedMoodEntries(userId)
        }
    }
}

