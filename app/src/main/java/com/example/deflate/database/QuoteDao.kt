package com.example.deflate.database

import androidx.room.*

@Dao
interface QuoteDao {
    @Query("SELECT * FROM quotes WHERE tag = :tag ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestQuote(tag: String): QuoteEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuote(quote: QuoteEntity)
    
    @Query("DELETE FROM quotes WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOldQuotes(cutoffTimestamp: Long)
}

