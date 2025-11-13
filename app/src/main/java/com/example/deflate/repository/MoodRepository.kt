package com.example.deflate.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.deflate.database.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

class MoodRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val moodDao = db.moodEntryDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    fun getAllMoodEntries(userId: String): Flow<List<MoodEntryEntity>> {
        return moodDao.getAllMoodEntries(userId)
    }
    
    // Get all mood entries synchronously (one-time fetch)
    suspend fun getAllMoodEntriesSync(userId: String): List<MoodEntryEntity> = withContext(Dispatchers.IO) {
        moodDao.getAllMoodEntriesSync(userId)
    }
    
    suspend fun saveMoodEntry(
        userId: String,
        username: String,
        mood: String,
        dateKey: String,
        timestamp: Long,
        source: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val entity = MoodEntryEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                username = username,
                mood = mood,
                dateKey = dateKey,
                timestamp = timestamp,
                source = source,
                firestoreId = null,
                isSynced = false,
                needsSync = true
            )
            
            moodDao.insertMoodEntry(entity)
            
            if (isOnline()) {
                syncMoodEntryToFirestore(entity)
            }
            
            Result.success(entity.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun syncMoodEntryToFirestore(entity: MoodEntryEntity) {
        try {
            val moodMap = hashMapOf(
                "userId" to entity.userId,
                "username" to entity.username,
                "mood" to entity.mood,
                "dateKey" to entity.dateKey,
                "timestamp" to entity.timestamp,
                "source" to entity.source
            )
            
            val firestoreId = if (entity.firestoreId != null) {
                firestore.collection("moodEntries")
                    .document(entity.firestoreId)
                    .set(moodMap)
                    .await()
                entity.firestoreId
            } else {
                val docRef = firestore.collection("moodEntries")
                    .add(moodMap)
                    .await()
                docRef.id
            }
            
            val updatedEntity = entity.copy(
                firestoreId = firestoreId,
                isSynced = true,
                needsSync = false
            )
            moodDao.updateMoodEntry(updatedEntity)
        } catch (e: Exception) {
            // Keep needsSync = true
        }
    }
    
    suspend fun syncFromFirestore(userId: String) = withContext(Dispatchers.IO) {
        if (!isOnline()) return@withContext
        
        try {
            val documents = firestore.collection("moodEntries")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            // Get all existing local entries to match against
            val existingEntries = moodDao.getAllMoodEntriesSync(userId)
            
            documents.forEach { doc ->
                val data = doc.data
                val firestoreId = doc.id
                
                // Check if entry with this firestoreId already exists
                val existingEntry = existingEntries.find { it.firestoreId == firestoreId }
                
                if (existingEntry != null) {
                    // Update existing entry with latest Firestore data
                    val updatedEntity = existingEntry.copy(
                        userId = data["userId"] as? String ?: existingEntry.userId,
                        username = data["username"] as? String ?: existingEntry.username,
                        mood = data["mood"] as? String ?: existingEntry.mood,
                        dateKey = data["dateKey"] as? String ?: existingEntry.dateKey,
                        timestamp = (data["timestamp"] as? Long) ?: existingEntry.timestamp,
                        source = data["source"] as? String ?: existingEntry.source,
                        isSynced = true,
                        needsSync = false
                    )
                    moodDao.updateMoodEntry(updatedEntity)
                } else {
                    // Check if there's a local unsynced entry with matching timestamp and mood
                    val matchingLocalEntry = existingEntries.find { entry ->
                        entry.timestamp == (data["timestamp"] as? Long) &&
                        entry.mood == (data["mood"] as? String) &&
                        entry.firestoreId == null
                    }
                    
                    if (matchingLocalEntry != null) {
                        // Update the local entry with Firestore ID
                        val updatedEntity = matchingLocalEntry.copy(
                            firestoreId = firestoreId,
                            isSynced = true,
                            needsSync = false
                        )
                        moodDao.updateMoodEntry(updatedEntity)
                    } else {
                        // Create new entry from Firestore
                        val newEntity = MoodEntryEntity(
                            id = UUID.randomUUID().toString(),
                            userId = data["userId"] as? String ?: "",
                            username = data["username"] as? String ?: "",
                            mood = data["mood"] as? String ?: "",
                            dateKey = data["dateKey"] as? String ?: "",
                            timestamp = (data["timestamp"] as? Long) ?: 0L,
                            source = data["source"] as? String ?: "",
                            firestoreId = firestoreId,
                            isSynced = true,
                            needsSync = false
                        )
                        moodDao.insertMoodEntry(newEntity)
                    }
                }
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    suspend fun syncUnsyncedMoodEntries(userId: String) = withContext(Dispatchers.IO) {
        if (!isOnline()) return@withContext
        
        try {
            val unsynced = moodDao.getUnsyncedMoodEntries(userId)
            unsynced.forEach { entity ->
                syncMoodEntryToFirestore(entity)
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
}

