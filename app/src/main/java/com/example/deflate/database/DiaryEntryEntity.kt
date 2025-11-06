package com.example.deflate.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntryEntity(
    @PrimaryKey
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val text: String = "",
    val mood: String = "",
    val datePretty: String = "",
    val timestamp: Long = 0L,
    val firestoreId: String? = null, // Firestore document ID
    val isSynced: Boolean = false, // Whether synced to Firestore
    val needsSync: Boolean = false // Whether needs to be synced
)

