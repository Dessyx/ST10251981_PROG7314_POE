package com.example.deflate.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryEntryDao {
    @Query("SELECT * FROM diary_entries WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllEntries(userId: String): Flow<List<DiaryEntryEntity>>
    
    @Query("SELECT * FROM diary_entries WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllEntriesSync(userId: String): List<DiaryEntryEntity>
    
    @Query("SELECT * FROM diary_entries WHERE userId = :userId AND datePretty LIKE :datePattern ORDER BY timestamp DESC")
    suspend fun getEntriesForDate(userId: String, datePattern: String): List<DiaryEntryEntity>
    
    @Query("SELECT * FROM diary_entries WHERE userId = :userId AND needsSync = 1")
    suspend fun getUnsyncedEntries(userId: String): List<DiaryEntryEntity>
    
    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getEntryById(id: String): DiaryEntryEntity?
    
    @Query("SELECT * FROM diary_entries WHERE firestoreId = :firestoreId")
    suspend fun getEntryByFirestoreId(firestoreId: String): DiaryEntryEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: DiaryEntryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<DiaryEntryEntity>)
    
    @Update
    suspend fun updateEntry(entry: DiaryEntryEntity)
    
    @Delete
    suspend fun deleteEntry(entry: DiaryEntryEntity)
    
    @Query("DELETE FROM diary_entries WHERE userId = :userId")
    suspend fun deleteAllEntries(userId: String)
}

