package com.example.deflate.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.deflate.DiaryEntry
import com.example.deflate.database.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

class DiaryRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val diaryDao = db.diaryEntryDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    // Convert Entity to Domain model
    private fun DiaryEntryEntity.toDiaryEntry(): DiaryEntry {
        return DiaryEntry(
            userId = userId,
            username = username,
            text = text,
            mood = mood,
            datePretty = datePretty,
            timestamp = timestamp
        )
    }
    
    // Convert Domain model to Entity
    private fun DiaryEntry.toEntity(firestoreId: String? = null, isSynced: Boolean = false): DiaryEntryEntity {
        return DiaryEntryEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            username = username,
            text = text,
            mood = mood,
            datePretty = datePretty,
            timestamp = timestamp,
            firestoreId = firestoreId,
            isSynced = isSynced,
            needsSync = !isSynced
        )
    }
    
    // Get all entries (from local database)
    fun getAllEntries(userId: String): Flow<List<DiaryEntry>> {
        return diaryDao.getAllEntries(userId).map { entities ->
            entities.map { it.toDiaryEntry() }
        }
    }
    
    // Get all entries synchronously (one-time fetch)
    suspend fun getAllEntriesSync(userId: String): List<DiaryEntry> = withContext(Dispatchers.IO) {
        diaryDao.getAllEntriesSync(userId).map { it.toDiaryEntry() }
    }
    
    // Save entry (local first, then sync if online)
    suspend fun saveEntry(entry: DiaryEntry): Result<String> = withContext(Dispatchers.IO) {
        try {
            val entity = entry.toEntity(isSynced = false)
            diaryDao.insertEntry(entity)
            
            // Try to sync if online
            if (isOnline()) {
                syncEntryToFirestore(entity)
            }
            
            Result.success(entity.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Sync entry to Firestore
    private suspend fun syncEntryToFirestore(entity: DiaryEntryEntity) {
        try {
            val entryMap = hashMapOf(
                "userId" to entity.userId,
                "username" to entity.username,
                "text" to entity.text,
                "mood" to entity.mood,
                "datePretty" to entity.datePretty,
                "timestamp" to entity.timestamp
            )
            
            val firestoreId = if (entity.firestoreId != null) {
                // Update existing
                firestore.collection("diaryEntries")
                    .document(entity.firestoreId)
                    .set(entryMap)
                    .await()
                entity.firestoreId
            } else {
                // Create new
                val docRef = firestore.collection("diaryEntries")
                    .add(entryMap)
                    .await()
                docRef.id
            }
            
            // Update entity as synced
            val updatedEntity = entity.copy(
                firestoreId = firestoreId,
                isSynced = true,
                needsSync = false
            )
            diaryDao.updateEntry(updatedEntity)
        } catch (e: Exception) {
            // Keep needsSync = true for retry later
        }
    }
    
    // Load entries from Firestore and sync to local
    suspend fun syncFromFirestore(userId: String) = withContext(Dispatchers.IO) {
        if (!isOnline()) return@withContext
        
        try {
            val documents = firestore.collection("diaryEntries")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            // Get all existing local entries to match against
            val existingEntries = diaryDao.getAllEntriesSync(userId)
            
            documents.forEach { doc ->
                val data = doc.data
                val firestoreId = doc.id
                
                // Check if entry with this firestoreId already exists
                val existingEntry = diaryDao.getEntryByFirestoreId(firestoreId)
                
                if (existingEntry != null) {
                    // Update existing entry with latest Firestore data
                    val updatedEntity = existingEntry.copy(
                        userId = data["userId"] as? String ?: existingEntry.userId,
                        username = data["username"] as? String ?: existingEntry.username,
                        text = data["text"] as? String ?: existingEntry.text,
                        mood = data["mood"] as? String ?: existingEntry.mood,
                        datePretty = data["datePretty"] as? String ?: existingEntry.datePretty,
                        timestamp = (data["timestamp"] as? Long) ?: existingEntry.timestamp,
                        isSynced = true,
                        needsSync = false
                    )
                    diaryDao.updateEntry(updatedEntity)
                } else {
                    // Check if there's a local unsynced entry with matching timestamp and content
                    val matchingLocalEntry = existingEntries.find { entry ->
                        entry.timestamp == (data["timestamp"] as? Long) &&
                        entry.text == (data["text"] as? String) &&
                        entry.firestoreId == null
                    }
                    
                    if (matchingLocalEntry != null) {
                        // Update the local entry with Firestore ID
                        val updatedEntity = matchingLocalEntry.copy(
                            firestoreId = firestoreId,
                            isSynced = true,
                            needsSync = false
                        )
                        diaryDao.updateEntry(updatedEntity)
                    } else {
                        // Create new entry from Firestore
                        val newEntity = DiaryEntryEntity(
                            id = UUID.randomUUID().toString(),
                            userId = data["userId"] as? String ?: "",
                            username = data["username"] as? String ?: "",
                            text = data["text"] as? String ?: "",
                            mood = data["mood"] as? String ?: "",
                            datePretty = data["datePretty"] as? String ?: "",
                            timestamp = (data["timestamp"] as? Long) ?: 0L,
                            firestoreId = firestoreId,
                            isSynced = true,
                            needsSync = false
                        )
                        diaryDao.insertEntry(newEntity)
                    }
                }
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    // Sync unsynced entries to Firestore
    suspend fun syncUnsyncedEntries(userId: String) = withContext(Dispatchers.IO) {
        if (!isOnline()) return@withContext
        
        try {
            val unsynced = diaryDao.getUnsyncedEntries(userId)
            unsynced.forEach { entity ->
                syncEntryToFirestore(entity)
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    // Remove duplicate entries from database
    suspend fun removeDuplicateEntries(userId: String) = withContext(Dispatchers.IO) {
        try {
            val allEntries = diaryDao.getAllEntriesSync(userId)
            val seen = mutableSetOf<String>()
            val toDelete = mutableListOf<DiaryEntryEntity>()
            
            // Sort by timestamp descending to keep the most recent entry
            val sortedEntries = allEntries.sortedByDescending { it.timestamp }
            
            sortedEntries.forEach { entry ->
                val key = "${entry.timestamp}_${entry.text}"
                if (seen.contains(key)) {
                    // This is a duplicate, mark for deletion
                    toDelete.add(entry)
                } else {
                    seen.add(key)
                }
            }
            
            // Delete duplicates
            toDelete.forEach { entry ->
                diaryDao.deleteEntry(entry)
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
}

