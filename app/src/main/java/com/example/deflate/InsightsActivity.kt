package com.example.deflate

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.ProgressBar
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar
import android.util.Log

class InsightsActivity : AppCompatActivity() {
    
    private lateinit var btnBack: ImageView
    private lateinit var tvMoodEntriesCount: TextView
    private lateinit var tvStreakCount: TextView
    private lateinit var tvDiaryEntriesCount: TextView
    private lateinit var tvDiaryStreakTitle: TextView
    private lateinit var tvDiaryStreakDays: TextView
    private lateinit var progressDiaryStreak: ProgressBar
    private lateinit var filterCard: androidx.cardview.widget.CardView
    private lateinit var tvMoodCount: TextView
    private lateinit var tvWeightCount: TextView
    private lateinit var tvActivitiesCount: TextView
    private lateinit var btnWeek: TextView
    private lateinit var btnMonth: TextView
    private lateinit var btnYear: TextView
    private lateinit var tvStepsTitle: TextView
    private lateinit var stepsGraphArea: StepsLineGraphView
    private lateinit var donutChart: DonutChartView
    
    
    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    // Data
    private var currentFilter = "day" // day, week, month
    private var currentTimeFilter = "week" // week, month, year
    private val moodData = mutableMapOf<String, Int>()
    private val stepsData = mutableListOf<Int>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_insights)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.insights_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        initializeViews()
        setupClickListeners()
        
        
        loadInsightsData()
    }
    
    override fun onResume() {
        super.onResume()

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            loadInsightsData()
        }, 500)
    }
    
    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        tvMoodEntriesCount = findViewById(R.id.tvMoodEntriesCount)
        tvStreakCount = findViewById(R.id.tvStreakCount)
        tvDiaryEntriesCount = findViewById(R.id.tvDiaryEntriesCount)
        tvDiaryStreakTitle = findViewById(R.id.tvDiaryStreakTitle)
        tvDiaryStreakDays = findViewById(R.id.tvDiaryStreakDays)
        progressDiaryStreak = findViewById(R.id.progressDiaryStreak)
        filterCard = findViewById(R.id.filterCard)
        
        
        // Initialize new UI elements
        tvMoodCount = findViewById(R.id.tvMoodCount)
        tvWeightCount = findViewById(R.id.tvWeightCount)
        tvActivitiesCount = findViewById(R.id.tvActivitiesCount)
        btnWeek = findViewById(R.id.btnWeek)
        btnMonth = findViewById(R.id.btnMonth)
        btnYear = findViewById(R.id.btnYear)
        tvStepsTitle = findViewById(R.id.tvStepsTitle)
        stepsGraphArea = findViewById(R.id.stepsGraphArea)
        donutChart = findViewById(R.id.donutChart)
    }
    
    
    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            navigateToHome()
        }
        
        filterCard.setOnClickListener {
            showFilterDialog()
        }
        
        // Time filter buttons
        btnWeek.setOnClickListener { setTimeFilter("week") }
        btnMonth.setOnClickListener { setTimeFilter("month") }
        btnYear.setOnClickListener { setTimeFilter("year") }
    }
    
    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun showFilterDialog() {
        val options = arrayOf("Day", "Week", "Month")
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Filter by")
            .setItems(options) { _, which ->
                val newFilter = when (which) {
                    0 -> "day"
                    1 -> "week"
                    2 -> "month"
                    else -> "day"
                }
                
                if (newFilter != currentFilter) {
                    currentFilter = newFilter
                    
                    
                    loadInsightsData()
                }
            }
        builder.show()
    }
    
    private fun loadInsightsData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            android.widget.Toast.makeText(this, "Please sign in to view insights", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        // Load mood entries count
        loadMoodEntriesCount(currentUser.uid)
        
        // Load diary entries count
        loadDiaryEntriesCount(currentUser.uid)
        
        // Load streak data
        loadStreakData(currentUser.uid)
        
        
        // Load diary streak
        loadDiaryStreak(currentUser.uid)
        
        // Load new data sections
        loadEntryCategories(currentUser.uid)
        updateStepsTitle()
        loadStepsData(currentUser.uid)
    }
    
    private fun loadMoodEntriesCount(userId: String) {
        db.collection("moodEntries")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { moodDocuments ->
                var totalMoodCount = moodDocuments.size()
                Log.d("InsightsActivity", "✅ Found ${moodDocuments.size()} mood entries from home screen")

                for ((index, doc) in moodDocuments.documents.withIndex()) {
                    val mood = doc.getString("mood")
                    val dateKey = doc.getString("dateKey")
                    val timestamp = doc.getLong("timestamp")
                }

                db.collection("diaryEntries")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener { diaryDocuments ->
                        totalMoodCount += diaryDocuments.size()
                        tvMoodEntriesCount.text = totalMoodCount.toString()
                    }
                    .addOnFailureListener { e ->
                        tvMoodEntriesCount.text = totalMoodCount.toString()
                    }
            }
            .addOnFailureListener { e ->
                tvMoodEntriesCount.text = "0"
                android.widget.Toast.makeText(this, "Error loading mood entries: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
    }
    
    private fun loadDiaryEntriesCount(userId: String) {

        Log.d("InsightsActivity", "Loading ALL diary entries for user: $userId")
        
        db.collection("diaryEntries")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("InsightsActivity", "Found ${documents.size()} diary entries")
                tvDiaryEntriesCount.text = documents.size().toString()
            }
            .addOnFailureListener { e ->
                Log.e("InsightsActivity", "Error loading diary entries", e)
                tvDiaryEntriesCount.text = "0"
            }
    }
    
    private fun loadStreakData(userId: String) {
        val allDates = mutableSetOf<String>()
        
        // Get diary entries
        db.collection("diaryEntries")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { diaryDocs ->
                for (document in diaryDocs) {
                    val timestamp = document.getLong("timestamp") ?: 0L
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
                    allDates.add(date)
                }
                Log.d("InsightsActivity", "📝 Found ${diaryDocs.size()} diary entries for streak calculation")
                
                // Also get mood entries
                db.collection("moodEntries")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener { moodDocs ->
                        for (document in moodDocs) {
                            val timestamp = document.getLong("timestamp") ?: 0L
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
                            allDates.add(date)
                        }
                        Log.d("InsightsActivity", "🎭 Found ${moodDocs.size()} mood entries for streak calculation")
                        
                        // Sort all dates
                        val sortedDates = allDates.sorted()
                        Log.d("InsightsActivity", "📅 Total unique active days: ${sortedDates.size}")
                        
                        // Count completed streaks (7+ consecutive days)
                        val completedStreaks = countCompletedStreaks(sortedDates)
                        tvStreakCount.text = completedStreaks.toString()
                        Log.d("InsightsActivity", "🔥 Completed 7-day streaks: $completedStreaks")
                    }
                    .addOnFailureListener { e ->
                        // If no mood entries, just use diary data
                        val sortedDates = allDates.sorted()
                        val completedStreaks = countCompletedStreaks(sortedDates)
                        tvStreakCount.text = completedStreaks.toString()
                        Log.d("InsightsActivity", "🔥 Completed streaks (diary only): $completedStreaks")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("InsightsActivity", "❌ Error loading streak data", e)
                tvStreakCount.text = "0"
            }
    }
    
    private fun countCompletedStreaks(dates: List<String>): Int {
        if (dates.isEmpty()) {
            Log.d("InsightsActivity", "⚠️ No dates to calculate streaks from")
            return 0
        }
        
        Log.d("InsightsActivity", "🔍 Calculating streaks from ${dates.size} unique days")
        Log.d("InsightsActivity", "📅 Date range: ${dates.first()} to ${dates.last()}")
        
        var completedStreaks = 0
        var currentStreak = 1
        var longestStreak = 1
        
        for (i in 1 until dates.size) {
            val prevDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dates[i - 1])
            val currDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dates[i])
            
            if (prevDate != null && currDate != null) {
                val diffInMillis = currDate.time - prevDate.time
                val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
                
                Log.d("InsightsActivity", "  Day ${i}: ${dates[i]} - Gap from previous: $diffInDays days")
                
                if (diffInDays == 1) {
                    // Consecutive day
                    currentStreak++
                    Log.d("InsightsActivity", "    ✅ Consecutive! Current streak: $currentStreak")
                } else {
                    // Streak broken
                    Log.d("InsightsActivity", "    ❌ Streak broken! Was at: $currentStreak days")
                    if (currentStreak >= 7) {
                        completedStreaks++
                        Log.d("InsightsActivity", "    🎉 Completed streak #$completedStreaks!")
                    }
                    longestStreak = Math.max(longestStreak, currentStreak)
                    currentStreak = 1
                }
            }
        }
        

        if (currentStreak >= 7) {
            completedStreaks++
            Log.d("InsightsActivity", "🎉 Final streak completed! Total: $completedStreaks")
        }
        
        longestStreak = Math.max(longestStreak, currentStreak)
        Log.d("InsightsActivity", "📊 Longest streak: $longestStreak days")
        Log.d("InsightsActivity", "🔥 Total completed 7-day streaks: $completedStreaks")
        
        return completedStreaks
    }
    
    
    private fun loadDiaryStreak(userId: String) {
        Log.d("InsightsActivity", "🔍 Loading diary streak for user: $userId")
        
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        // Get first day of current month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis
        
        // Get last day of current month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val monthEnd = calendar.timeInMillis
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        Log.d("InsightsActivity", "📅 Month range: $monthStart to $monthEnd (Days in month: $daysInMonth)")

        db.collection("diaryEntries")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("timestamp", monthStart)
            .whereLessThanOrEqualTo("timestamp", monthEnd)
            .get()
            .addOnSuccessListener { diaryDocs ->
                val entriesThisMonth = diaryDocs.size()
                
                Log.d("InsightsActivity", "📝 TOTAL diary entries this month: $entriesThisMonth")
                
                for ((index, document) in diaryDocs.documents.withIndex()) {
                    val timestamp = document.getLong("timestamp") ?: 0L
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
                    Log.d("InsightsActivity", "  Entry $index: date=$date, timestamp=$timestamp")
                }

                val maxEntries = 30
                progressDiaryStreak.max = maxEntries
                progressDiaryStreak.progress = Math.min(entriesThisMonth, maxEntries)
                tvDiaryStreakDays.text = "$entriesThisMonth entries this month"
                
                Log.d("InsightsActivity", "✅ Progress: $entriesThisMonth/$maxEntries")
            }
            .addOnFailureListener { e ->
                Log.e("InsightsActivity", "❌ Error loading diary streak: ${e.message}", e)
                android.widget.Toast.makeText(this, "Error loading diary streak: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                progressDiaryStreak.progress = 0
                tvDiaryStreakDays.text = "0 entries this month"
            }
    }
    
    
    
    
    
    private fun getDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        when (currentFilter) {
            "day" -> {
                // Start of today
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                
                // End of today
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endOfDay = calendar.timeInMillis
                
                Log.d("InsightsActivity", "📅 Day filter: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(startOfDay))} to ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(endOfDay))}")
                return Pair(startOfDay, endOfDay)
            }
            "week" -> {
                calendar.add(Calendar.DAY_OF_MONTH, -6)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val weekStart = calendar.timeInMillis
                
                Log.d("InsightsActivity", "📅 Week filter: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(weekStart))} to ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(now))}")
                return Pair(weekStart, now)
            }
            "month" -> {
                // Get last 30 days
                calendar.add(Calendar.DAY_OF_MONTH, -30)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val monthStart = calendar.timeInMillis
                Log.d("InsightsActivity", "📅 Month filter: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(monthStart))} to ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(now))}")
                return Pair(monthStart, now)
            }
            else -> {
                // Default to day
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endOfDay = calendar.timeInMillis
                
                return Pair(startOfDay, endOfDay)
            }
        }
    }
    
    //-------------------------------------------------------------------------
    // Time filter functionality
    private fun setTimeFilter(filter: String) {
        currentTimeFilter = filter
        
        // Update button backgrounds
        when (filter) {
            "week" -> {
                btnWeek.background = getDrawable(R.drawable.selected_filter_bg)
                btnMonth.background = getDrawable(R.drawable.unselected_filter_bg)
                btnYear.background = getDrawable(R.drawable.unselected_filter_bg)
            }
            "month" -> {
                btnWeek.background = getDrawable(R.drawable.unselected_filter_bg)
                btnMonth.background = getDrawable(R.drawable.selected_filter_bg)
                btnYear.background = getDrawable(R.drawable.unselected_filter_bg)
            }
            "year" -> {
                btnWeek.background = getDrawable(R.drawable.unselected_filter_bg)
                btnMonth.background = getDrawable(R.drawable.unselected_filter_bg)
                btnYear.background = getDrawable(R.drawable.selected_filter_bg)
            }
        }
        

        updateStepsTitle()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            loadStepsData(currentUser.uid)
        }
    }
    
    //-------------------------------------------------------------------------
    // Load entry categories data
    private fun loadEntryCategories(userId: String) {
        val dateRange = getDateRange()
        
        var finalMoodCount = 0
        var finalWeightCount = 0
        var finalActivitiesCount = 0

        db.collection("diaryEntries")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("timestamp", dateRange.first)
            .whereLessThanOrEqualTo("timestamp", dateRange.second)
            .get()
            .addOnSuccessListener { diaryDocuments ->
                var moodCount = diaryDocuments.size()
                

                db.collection("moodEntries")
                    .whereEqualTo("userId", userId)
                    .whereGreaterThanOrEqualTo("timestamp", dateRange.first)
                    .whereLessThanOrEqualTo("timestamp", dateRange.second)
                    .get()
                    .addOnSuccessListener { moodDocuments ->
                        moodCount += moodDocuments.size()
                        finalMoodCount = moodCount
                        tvMoodCount.text = moodCount.toString()
                        updateDonutChart(finalMoodCount, finalWeightCount, finalActivitiesCount)
                    }
                    .addOnFailureListener {
                        finalMoodCount = moodCount
                        tvMoodCount.text = moodCount.toString()
                        updateDonutChart(finalMoodCount, finalWeightCount, finalActivitiesCount)
                    }
            }
            .addOnFailureListener {
                // If diary entries fail, still try to count home screen mood entries
                db.collection("moodEntries")
                    .whereEqualTo("userId", userId)
                    .whereGreaterThanOrEqualTo("timestamp", dateRange.first)
                    .whereLessThanOrEqualTo("timestamp", dateRange.second)
                    .get()
                    .addOnSuccessListener { moodDocuments ->
                        finalMoodCount = moodDocuments.size()
                        tvMoodCount.text = moodDocuments.size().toString()
                        updateDonutChart(finalMoodCount, finalWeightCount, finalActivitiesCount)
                    }
                    .addOnFailureListener {
                        finalMoodCount = 0
                        tvMoodCount.text = "0"
                        updateDonutChart(finalMoodCount, finalWeightCount, finalActivitiesCount)
                    }
            }
        
        // Count weight entries from activities
        db.collection("activities")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("timestamp", dateRange.first)
            .whereLessThanOrEqualTo("timestamp", dateRange.second)
            .get()
            .addOnSuccessListener { documents ->
                var weightCount = 0
                for (document in documents) {
                    val weight = document.getDouble("weight")
                    if (weight != null && weight > 0) {
                        weightCount++
                    }
                }
                finalWeightCount = weightCount
                tvWeightCount.text = weightCount.toString()
                updateDonutChart(finalMoodCount, finalWeightCount, finalActivitiesCount)
            }
            .addOnFailureListener {
                tvWeightCount.text = "0"
            }
        
        // Count activities entries
        db.collection("activities")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("timestamp", dateRange.first)
            .whereLessThanOrEqualTo("timestamp", dateRange.second)
            .get()
            .addOnSuccessListener { documents ->
                var activitiesCount = 0
                for (document in documents) {
                    val steps = document.getLong("steps")
                    if (steps != null && steps > 0) {
                        activitiesCount++
                    }
                }
                finalActivitiesCount = activitiesCount
                tvActivitiesCount.text = activitiesCount.toString()
                updateDonutChart(finalMoodCount, finalWeightCount, finalActivitiesCount)
            }
            .addOnFailureListener {
                tvActivitiesCount.text = "0"
            }
    }
    
    private fun updateDonutChart(mood: Int, weight: Int, activities: Int) {
        donutChart.updateData(mood, weight, activities)
        Log.d("InsightsActivity", "📊 Updated donut chart: mood=$mood, weight=$weight, activities=$activities")
    }
    
    //-------------------------------------------------------------------------
    // Load steps data for the graph
    private fun loadStepsData(userId: String) {
        val dateRange = getTimeFilterRange()
        
        Log.d("InsightsActivity", "📊 Loading steps data for time filter: $currentTimeFilter")
        Log.d("InsightsActivity", "📅 Steps date range: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(dateRange.first))} to ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(dateRange.second))}")
        
        // Get steps data for the selected time period
        db.collection("activities")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("timestamp", dateRange.first)
            .whereLessThanOrEqualTo("timestamp", dateRange.second)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("InsightsActivity", "📊 Found ${documents.size()} activity documents")
                
                val stepsByDay = mutableMapOf<String, Int>()
                

                for ((index, document) in documents.documents.withIndex()) {
                    val steps = document.getLong("steps")?.toInt() ?: 0
                    val timestamp = document.getLong("timestamp") ?: 0L
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
                    
                    Log.d("InsightsActivity", "📊 Document $index: steps=$steps, date=$date, timestamp=$timestamp")
                    
                    if (steps > 0) {
                        stepsByDay[date] = (stepsByDay[date] ?: 0) + steps
                        Log.d("InsightsActivity", "📊 Added steps: $steps on $date (total for day: ${stepsByDay[date]})")
                    }
                }
                
                Log.d("InsightsActivity", "📊 Final steps data for graph: $stepsByDay")
                

                updateStepsGraph(stepsByDay)

                if (stepsByDay.isNotEmpty()) {
                    val totalSteps = stepsByDay.values.sum()
                    android.widget.Toast.makeText(this, "📊 Loaded ${stepsByDay.size} days of steps data (Total: $totalSteps steps)", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(this, "📊 No steps data found for this period", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("InsightsActivity", "Error loading steps data", e)
                android.widget.Toast.makeText(this, "Error loading steps data: ${e.message}", android.widget.Toast.LENGTH_LONG).show()

                updateStepsGraph(emptyMap())
            }
    }
    
    private fun updateStepsGraph(stepsByDay: Map<String, Int>) {
        Log.d("InsightsActivity", "Steps data: $stepsByDay")
        stepsGraphArea.updateStepsData(stepsByDay)
    }
    
    
    private fun updateStepsTitle() {
        val title = when (currentTimeFilter) {
            "week" -> "Steps for the week"
            "month" -> "Steps for the month"
            "year" -> "Steps for the year"
            else -> "Steps for the week"
        }
        tvStepsTitle.text = title
    }
    
    private fun getTimeFilterRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        when (currentTimeFilter) {
            "week" -> {

                calendar.add(Calendar.DAY_OF_MONTH, -6)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val weekStart = calendar.timeInMillis
                
                Log.d("InsightsActivity", "📅 Week range: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(weekStart))} to ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(now))}")
                return Pair(weekStart, now)
            }
            "month" -> {
                // Get last 30 days
                calendar.add(Calendar.DAY_OF_MONTH, -30)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val monthStart = calendar.timeInMillis
                return Pair(monthStart, now)
            }
            "year" -> {
                // Get last 365 days
                calendar.add(Calendar.DAY_OF_MONTH, -365)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val yearStart = calendar.timeInMillis
                return Pair(yearStart, now)
            }
            else -> {
                // Default to week
                calendar.add(Calendar.DAY_OF_MONTH, -6)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val weekStart = calendar.timeInMillis
                return Pair(weekStart, now)
            }
        }
    }
}