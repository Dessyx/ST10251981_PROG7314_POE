package com.example.deflate

import android.content.Context
import android.content.SharedPreferences
import com.example.deflate.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit


 // Analyzes mood patterns to detect crisis situations
class MoodAnalyzer(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "mood_analyzer_prefs",
        Context.MODE_PRIVATE
    )
    
    private val database = AppDatabase.getDatabase(context)
    private val diaryDao = database.diaryEntryDao()
    private val moodDao = database.moodEntryDao()
    
    companion object {
        private const val KEY_LAST_CRISIS_NOTIFICATION = "last_crisis_notification"
        private const val CRISIS_NOTIFICATION_COOLDOWN_DAYS = 3 // Don't spam crisis notifications
        
        // Moods our group considered negative
        private val NEGATIVE_MOODS = setOf("Sad", "Anxious", "Tired")
        
        // Crisis detection thresholds
        private const val CRISIS_DAYS_THRESHOLD = 5 // Check last 5 days
        private const val CRISIS_NEGATIVE_PERCENTAGE = 0.8 // 80% negative moods
    }
    

     // Check if user is in a potential crisis situation
    suspend fun checkForCrisis(userId: String): Boolean = withContext(Dispatchers.IO) {
        // Check cooldown period to avoid spamming
        val lastNotification = prefs.getLong(KEY_LAST_CRISIS_NOTIFICATION, 0)
        val daysSinceLastNotification = TimeUnit.MILLISECONDS.toDays(
            System.currentTimeMillis() - lastNotification
        )
        
        if (daysSinceLastNotification < CRISIS_NOTIFICATION_COOLDOWN_DAYS) {
            return@withContext false // Still in cooldown
        }
        
        // Get recent diary entries
        val recentEntries = getRecentDiaryMoods(userId)
        
        if (recentEntries.size < 3) {
            return@withContext false // Not enough data
        }

        val negativeCount = recentEntries.count { it in NEGATIVE_MOODS }
        val negativePercentage = negativeCount.toDouble() / recentEntries.size

        val isConsistentlyNegative = negativePercentage >= CRISIS_NEGATIVE_PERCENTAGE

        val lastThreeNegative = recentEntries.take(3).all { it in NEGATIVE_MOODS }
        
        return@withContext isConsistentlyNegative || lastThreeNegative
    }
    

     // Mark that a crisis notif has been sent 
    fun markCrisisNotificationSent() {
        prefs.edit().putLong(KEY_LAST_CRISIS_NOTIFICATION, System.currentTimeMillis()).apply()
    }
    

    // Get recent diary moods 
    private suspend fun getRecentDiaryMoods(userId: String): List<String> {
        val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(CRISIS_DAYS_THRESHOLD.toLong())
        
        val entries = diaryDao.getAllEntriesSync(userId)
            .filter { it.timestamp >= cutoffTime }
            .sortedByDescending { it.timestamp }
            .map { it.mood }
        
        return entries
    }
    

     // Get mood statistics for insights

    suspend fun getMoodStatistics(userId: String, days: Int = 7): MoodStatistics = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        
        val entries = diaryDao.getAllEntriesSync(userId)
            .filter { it.timestamp >= cutoffTime }
        
        if (entries.isEmpty()) {
            return@withContext MoodStatistics(0, emptyMap(), 0.0)
        }
        
        // Count moods
        val moodCounts = entries.groupingBy { it.mood }.eachCount()
        
        // Calculate negative percentage
        val negativeCount = entries.count { it.mood in NEGATIVE_MOODS }
        val negativePercentage = negativeCount.toDouble() / entries.size
        
        return@withContext MoodStatistics(
            totalEntries = entries.size,
            moodCounts = moodCounts,
            negativePercentage = negativePercentage
        )
    }
    
   
    // Get the mood trend (improving, stable, declining)
 
    suspend fun getMoodTrend(userId: String): MoodTrend = withContext(Dispatchers.IO) {
        val allEntries = diaryDao.getAllEntriesSync(userId)
            .sortedByDescending { it.timestamp }
        
        if (allEntries.size < 6) {
            return@withContext MoodTrend.STABLE
        }
        
        // Split into recent (last 3) and previous (next 3)
        val recentMoods = allEntries.take(3).map { it.mood }
        val previousMoods = allEntries.drop(3).take(3).map { it.mood }
        
        val recentNegativeCount = recentMoods.count { it in NEGATIVE_MOODS }
        val previousNegativeCount = previousMoods.count { it in NEGATIVE_MOODS }
        
        return@withContext when {
            recentNegativeCount < previousNegativeCount -> MoodTrend.IMPROVING
            recentNegativeCount > previousNegativeCount -> MoodTrend.DECLINING
            else -> MoodTrend.STABLE
        }
    }
    

    fun reset() {
        prefs.edit().clear().apply()
    }
}


 // Mood statistics data class
data class MoodStatistics(
    val totalEntries: Int,
    val moodCounts: Map<String, Int>,
    val negativePercentage: Double
)


enum class MoodTrend {
    IMPROVING,
    STABLE,
    DECLINING
}

