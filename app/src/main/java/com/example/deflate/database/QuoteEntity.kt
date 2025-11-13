package com.example.deflate.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey
    val tag: String = "",
    val body: String = "",
    val author: String = "",
    val timestamp: Long = 0L // When the quote was cached
)

