package com.example.deflate.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.deflate.ActivityData
import com.example.deflate.database.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

class ActivityRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val activityDao = db.activityDataDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    private fun ActivityDataEntity.toActivityData(): ActivityData {
        return ActivityData(
            userId = userId,
            weight = weight,
            steps = steps,
            date = date?.let { java.util.Date(it) },
            timestamp = timestamp
        )
    }
    
    private fun ActivityData.toEntity(firestoreId: String? = null, isSynced: Boolean = false): ActivityDataEntity {
        return ActivityDataEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            weight = weight,
            steps = steps,
            date = date?.time,
            timestamp = timestamp,
            firestoreId = firestoreId,
            isSynced = isSynced,
            needsSync = !isSynced
        )
    }
    
    fun getAllActivities(userId: String): Flow<List<ActivityData>> {
        return activityDao.getAllActivities(userId).map { entities ->
            entities.map { it.toActivityData() }
        }
    }
    
    // Get all activities synchronously (one-time fetch)
    suspend fun getAllActivitiesSync(userId: String): List<ActivityData> = withContext(Dispatchers.IO) {
        activityDao.getAllActivitiesSync(userId).map { it.toActivityData() }
    }
    
    suspend fun getLatestWeight(userId: String): ActivityData? = withContext(Dispatchers.IO) {
        activityDao.getLatestWeight(userId)?.toActivityData()
    }
    
    suspend fun getLatestSteps(userId: String): ActivityData? = withContext(Dispatchers.IO) {
        activityDao.getLatestSteps(userId)?.toActivityData()
    }
    
    suspend fun saveActivity(activity: ActivityData): Result<String> = withContext(Dispatchers.IO) {
        try {
            val entity = activity.toEntity(isSynced = false)
            activityDao.insertActivity(entity)
            
            if (isOnline()) {
                syncActivityToFirestore(entity)
            }
            
            Result.success(entity.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun syncActivityToFirestore(entity: ActivityDataEntity) {
        try {
            val activityMap = hashMapOf<String, Any>(
                "userId" to entity.userId,
                "timestamp" to entity.timestamp
            ).apply {
                entity.weight?.let { put("weight", it) }
                entity.steps?.let { put("steps", it) }
                entity.date?.let { put("date", java.util.Date(it)) }
            }
            
            val firestoreId = if (entity.firestoreId != null) {
                firestore.collection("activities")
                    .document(entity.firestoreId)
                    .set(activityMap)
                    .await()
                entity.firestoreId
            } else {
                val docRef = firestore.collection("activities")
                    .add(activityMap)
                    .await()
                docRef.id
            }
            
            val updatedEntity = entity.copy(
                firestoreId = firestoreId,
                isSynced = true,
                needsSync = false
            )
            activityDao.updateActivity(updatedEntity)
        } catch (e: Exception) {
            // Keep needsSync = true
        }
    }
    
    suspend fun syncFromFirestore(userId: String) = withContext(Dispatchers.IO) {
        if (!isOnline()) return@withContext
        
        try {
            val documents = firestore.collection("activities")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val entities = documents.map { doc ->
                val data = doc.data
                ActivityDataEntity(
                    id = UUID.randomUUID().toString(),
                    userId = data["userId"] as? String ?: "",
                    weight = (data["weight"] as? Number)?.toDouble(),
                    steps = (data["steps"] as? Number)?.toInt(),
                    date = (data["date"] as? com.google.firebase.Timestamp)?.toDate()?.time,
                    timestamp = (data["timestamp"] as? Long) ?: 0L,
                    firestoreId = doc.id,
                    isSynced = true,
                    needsSync = false
                )
            }
            
            activityDao.insertActivities(entities)
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    suspend fun syncUnsyncedActivities(userId: String) = withContext(Dispatchers.IO) {
        if (!isOnline()) return@withContext
        
        try {
            val unsynced = activityDao.getUnsyncedActivities(userId)
            unsynced.forEach { entity ->
                syncActivityToFirestore(entity)
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
}

