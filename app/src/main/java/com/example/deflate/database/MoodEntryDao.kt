package com.example.deflate.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodEntryDao {
    @Query("SELECT * FROM mood_entries WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllMoodEntries(userId: String): Flow<List<MoodEntryEntity>>
    
    @Query("SELECT * FROM mood_entries WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllMoodEntriesSync(userId: String): List<MoodEntryEntity>
    
    @Query("SELECT * FROM mood_entries WHERE userId = :userId AND needsSync = 1")
    suspend fun getUnsyncedMoodEntries(userId: String): List<MoodEntryEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodEntry(entry: MoodEntryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodEntries(entries: List<MoodEntryEntity>)
    
    @Update
    suspend fun updateMoodEntry(entry: MoodEntryEntity)
    
    @Delete
    suspend fun deleteMoodEntry(entry: MoodEntryEntity)
    
    @Query("DELETE FROM mood_entries WHERE userId = :userId")
    suspend fun deleteAllMoodEntries(userId: String)
}

