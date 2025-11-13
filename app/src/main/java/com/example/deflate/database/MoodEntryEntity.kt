package com.example.deflate.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mood_entries")
data class MoodEntryEntity(
    @PrimaryKey
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val mood: String = "",
    val dateKey: String = "",
    val timestamp: Long = 0L,
    val source: String = "",
    val firestoreId: String? = null,
    val isSynced: Boolean = false,
    val needsSync: Boolean = false
)

