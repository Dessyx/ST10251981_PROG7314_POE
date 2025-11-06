package com.example.deflate.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class ActivityDataEntity(
    @PrimaryKey
    val id: String = "",
    val userId: String = "",
    val weight: Double? = null,
    val steps: Int? = null,
    val date: Long? = null, // Store as timestamp
    val timestamp: Long = 0L,
    val firestoreId: String? = null,
    val isSynced: Boolean = false,
    val needsSync: Boolean = false
)

