package com.example.deflate.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDataDao {
    @Query("SELECT * FROM activities WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllActivities(userId: String): Flow<List<ActivityDataEntity>>
    
    @Query("SELECT * FROM activities WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllActivitiesSync(userId: String): List<ActivityDataEntity>
    
    @Query("SELECT * FROM activities WHERE userId = :userId AND needsSync = 1")
    suspend fun getUnsyncedActivities(userId: String): List<ActivityDataEntity>
    
    @Query("SELECT * FROM activities WHERE userId = :userId AND weight IS NOT NULL ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestWeight(userId: String): ActivityDataEntity?
    
    @Query("SELECT * FROM activities WHERE userId = :userId AND steps IS NOT NULL ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSteps(userId: String): ActivityDataEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityDataEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<ActivityDataEntity>)
    
    @Update
    suspend fun updateActivity(activity: ActivityDataEntity)
    
    @Delete
    suspend fun deleteActivity(activity: ActivityDataEntity)
    
    @Query("DELETE FROM activities WHERE userId = :userId")
    suspend fun deleteAllActivities(userId: String)
}

