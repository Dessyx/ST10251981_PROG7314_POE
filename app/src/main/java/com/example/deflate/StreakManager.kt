package com.example.deflate

import android.content.Context
import android.content.SharedPreferences
import com.example.deflate.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*


 // Manages streak tracking for diary entries
class StreakManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "streak_prefs",
        Context.MODE_PRIVATE
    )
    
    private val database = AppDatabase.getDatabase(context)
    private val diaryDao = database.diaryEntryDao()
    
    companion object {
        private const val KEY_CURRENT_STREAK = "current_streak"
        private const val KEY_LONGEST_STREAK = "longest_streak"
        private const val KEY_LAST_ENTRY_DATE = "last_entry_date"
        private const val KEY_NOTIFIED_STREAKS = "notified_streaks"
        
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }

     // Get current streak count
    fun getCurrentStreak(): Int {
        return prefs.getInt(KEY_CURRENT_STREAK, 0)
    }
    

     // Get longest streak count
    fun getLongestStreak(): Int {
        return prefs.getInt(KEY_LONGEST_STREAK, 0)
    }
    

     // Update streak based on new diary entry
    suspend fun updateStreak(userId: String): Int = withContext(Dispatchers.IO) {
        val today = DATE_FORMAT.format(Date())
        val lastEntryDate = prefs.getString(KEY_LAST_ENTRY_DATE, null)
        
        // Check if entry already exists for today 
        val entriesForToday = diaryDao.getEntriesForDate(userId, today)
        if (entriesForToday.isNotEmpty() && lastEntryDate == today) {
    
            return@withContext getCurrentStreak()
        }
        
        val currentStreak = getCurrentStreak()
        val newStreak: Int
        
        if (lastEntryDate == null) {
            // First entry ever
            newStreak = 1
        } else {
            val yesterday = getYesterdayDate()
            when (lastEntryDate) {
                today -> {
                    // Already logged today
                    newStreak = currentStreak
                }
                yesterday -> {
                    // Logged yesterday, continue streak
                    newStreak = currentStreak + 1
                }
                else -> {
                    // Streak broken, start new streak
                    newStreak = 1
                }
            }
        }
        
        // Update preferences
        prefs.edit().apply {
            putInt(KEY_CURRENT_STREAK, newStreak)
            putString(KEY_LAST_ENTRY_DATE, today)
            
            // Update longest streak if needed
            val longestStreak = getLongestStreak()
            if (newStreak > longestStreak) {
                putInt(KEY_LONGEST_STREAK, newStreak)
            }
            
            apply()
        }
        
        return@withContext newStreak
    }
    

     // Check if we should notify about this streak milestone

    fun shouldNotifyForStreak(streakDays: Int): Boolean {
        val notifiedStreaks = getNotifiedStreaks()
        
        // Notify for specific milestones
        val shouldNotify = when {
            streakDays in listOf(1, 3, 7, 14, 30, 60, 90, 365) -> true
            streakDays % 10 == 0 && streakDays > 0 -> true
            else -> false
        }
        
        return shouldNotify && !notifiedStreaks.contains(streakDays)
    }
    
 
     // Mark that we've notified for this streak
 
    fun markStreakAsNotified(streakDays: Int) {
        val notifiedStreaks = getNotifiedStreaks().toMutableSet()
        notifiedStreaks.add(streakDays)
        
        prefs.edit().putStringSet(KEY_NOTIFIED_STREAKS, notifiedStreaks.map { it.toString() }.toSet()).apply()
    }
    

    private fun getNotifiedStreaks(): Set<Int> {
        return prefs.getStringSet(KEY_NOTIFIED_STREAKS, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet() ?: emptySet()
    }
    

     // Get yesterday's date in format yyyy-MM-dd
     
    private fun getYesterdayDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return DATE_FORMAT.format(calendar.time)
    }
    

    suspend fun calculateStreakFromDatabase(userId: String): Int = withContext(Dispatchers.IO) {
        val allEntries = diaryDao.getAllEntriesSync(userId)
        
        if (allEntries.isEmpty()) return@withContext 0
        
        // Group entries by date
        val entriesByDate = allEntries
            .map { entry ->
                try {
                    val date = Date(entry.timestamp)
                    DATE_FORMAT.format(date)
                } catch (e: Exception) {
                    null
                }
            }
            .filterNotNull()
            .distinct()
            .sorted()
            .reversed() // Most recent first
        
        if (entriesByDate.isEmpty()) return@withContext 0
        
        val today = DATE_FORMAT.format(Date())
        val yesterday = getYesterdayDate()
        
        // Check if there's an entry today or yesterday
        val mostRecentDate = entriesByDate.first()
        if (mostRecentDate != today && mostRecentDate != yesterday) {
            return@withContext 0 // Streak is broken
        }
        
        // Count consecutive days
        var streak = 0
        val calendar = Calendar.getInstance()
        
        for (date in entriesByDate) {
            val expectedDate = DATE_FORMAT.format(calendar.time)
            
            if (date == expectedDate) {
                streak++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        
        return@withContext streak
    }
    
    fun resetStreak() {
        prefs.edit().apply {
            putInt(KEY_CURRENT_STREAK, 0)
            putString(KEY_LAST_ENTRY_DATE, null)
            remove(KEY_NOTIFIED_STREAKS)
            apply()
        }
    }
}

