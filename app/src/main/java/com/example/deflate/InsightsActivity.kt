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
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.deflate.repository.DiaryRepository
import com.example.deflate.repository.MoodRepository
import com.example.deflate.repository.ActivityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar
import android.util.Log

class InsightsActivity : BaseActivity() {
    
    private lateinit var btnBack: ImageView
    private lateinit var tvMoodEntriesCount: TextView
    private lateinit var tvStreakCount: TextView
    private lateinit var tvDiaryEntriesCount: TextView
    private lateinit var streakContainer: FrameLayout
    private lateinit var streakFragment: StreakContainerFragment
    private lateinit var filterCard: androidx.cardview.widget.CardView
    private lateinit var tvMoodCount: TextView
    private lateinit var tvWeightCount: TextView
    private lateinit var tvActivitiesCount: TextView
    private lateinit var tvEntryCategoriesTitle: TextView
    private lateinit var btnWeek: TextView
    private lateinit var btnMonth: TextView
    private lateinit var btnYear: TextView
    private lateinit var tvStepsTitle: TextView
    private lateinit var tvTotalSteps: TextView
    private lateinit var stepsGraphArea: StepsLineGraphView
    private lateinit var donutChart: DonutChartView
    private lateinit var moodBarChart: MoodBarChartView
    
    
    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var diaryRepository: DiaryRepository
    private lateinit var moodRepository: MoodRepository
    private lateinit var activityRepository: ActivityRepository
    
    // Data
    private var currentFilter = "day" // day, week, month, custom
    private var currentTimeFilter = "week" // week, month, year
    private val moodData = mutableMapOf<String, Int>()
    private val stepsData = mutableListOf<Int>()
    
    // Custom date range
    private var customStartDate: Long = 0L
    private var customEndDate: Long = 0L
    
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
        diaryRepository = DiaryRepository(this)
        moodRepository = MoodRepository(this)
        activityRepository = ActivityRepository(this)
        
        initializeViews()
        setupClickListeners()
        
        // Sync data when activity starts
        syncAllData()
        loadInsightsData()
    }
    
    override fun onResume() {
        super.onResume()
        syncAllData()
        loadInsightsData()
    }
    
    private fun syncAllData() {
        val user = auth.currentUser ?: return
        CoroutineScope(Dispatchers.IO).launch {
            diaryRepository.syncFromFirestore(user.uid)
            diaryRepository.syncUnsyncedEntries(user.uid)
            moodRepository.syncFromFirestore(user.uid)
            moodRepository.syncUnsyncedMoodEntries(user.uid)
            activityRepository.syncFromFirestore(user.uid)
            activityRepository.syncUnsyncedActivities(user.uid)
        }
    }
    
    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        tvMoodEntriesCount = findViewById(R.id.tvMoodEntriesCount)
        tvStreakCount = findViewById(R.id.tvStreakCount)
        tvDiaryEntriesCount = findViewById(R.id.tvDiaryEntriesCount)
        streakContainer = findViewById(R.id.streakContainer)
        streakFragment = StreakContainerFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.streakContainer, streakFragment)
            .commit()
        filterCard = findViewById(R.id.filterCard)

        tvMoodCount = findViewById(R.id.tvMoodCount)
        tvWeightCount = findViewById(R.id.tvWeightCount)
        tvActivitiesCount = findViewById(R.id.tvActivitiesCount)
        tvEntryCategoriesTitle = findViewById(R.id.tvEntryCategoriesTitle)
        btnWeek = findViewById(R.id.btnWeek)
        btnMonth = findViewById(R.id.btnMonth)
        btnYear = findViewById(R.id.btnYear)
        tvStepsTitle = findViewById(R.id.tvStepsTitle)
        tvTotalSteps = findViewById(R.id.tvTotalSteps)
        stepsGraphArea = findViewById(R.id.stepsGraphArea)
        donutChart = findViewById(R.id.donutChart)
        moodBarChart = findViewById(R.id.moodBarChart)
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
        val bottomFilterFragment = BottomFilterFragment.newInstance()
        
        bottomFilterFragment.setOnFilterSelectedListener { filter ->
            if (filter != currentFilter) {
                currentFilter = filter
                updateEntryCategoriesTitle()
                loadInsightsData()
            }
        }
        
        bottomFilterFragment.setOnCustomDateRangeSelectedListener { startDate, endDate ->
            customStartDate = startDate
            customEndDate = endDate
            currentFilter = "custom"
            updateEntryCategoriesTitle()
            loadInsightsData()
        }
        
        supportFragmentManager.beginTransaction()
            .add(android.R.id.content, bottomFilterFragment)
            .commit()
    }
    
    private fun loadInsightsData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            android.widget.Toast.makeText(this, "Please sign in to view insights", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        tvMoodEntriesCount.text = "0"
        tvDiaryEntriesCount.text = "0"
        tvStreakCount.text = "0"
        tvMoodCount.text = "0"
        tvWeightCount.text = "0"
        tvActivitiesCount.text = "0"
        tvTotalSteps.text = "0"

        loadMoodEntriesCount(currentUser.uid)
        loadDiaryEntriesCount(currentUser.uid)
        loadStreakData(currentUser.uid)
        loadDiaryStreak(currentUser.uid)
        loadEntryCategories(currentUser.uid)
        updateStepsTitle()
        updateEntryCategoriesTitle()
        loadStepsData(currentUser.uid)
        loadMoodDataForChart(currentUser.uid)
    }
    
    private fun loadMoodEntriesCount(userId: String) {
        val dateRange = getDateRange()
        
        // Load from local database (offline support)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val moodEntries = moodRepository.getAllMoodEntriesSync(userId)
                val diaryEntries = diaryRepository.getAllEntriesSync(userId)
                
                var totalMoodCount = 0
                
                // Filter mood entries by date range
                moodEntries.forEach { entry ->
                    if (entry.timestamp >= dateRange.first && entry.timestamp <= dateRange.second) {
                        totalMoodCount++
                    }
                }
                
                // Filter diary entries by date range
                diaryEntries.forEach { entry ->
                    if (entry.timestamp >= dateRange.first && entry.timestamp <= dateRange.second) {
                        totalMoodCount++
                    }
                }
                
                tvMoodEntriesCount.text = totalMoodCount.toString()
            } catch (e: Exception) {
                Log.e("InsightsActivity", "Error loading mood entries count", e)
                tvMoodEntriesCount.text = "0"
            }
        }
    }
    
    private fun loadDiaryEntriesCount(userId: String) {
        val dateRange = getDateRange()
        
        // Load from local database (offline support)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val entries = diaryRepository.getAllEntriesSync(userId)
                val diaryCount = entries.count { entry ->
                    entry.timestamp >= dateRange.first && entry.timestamp <= dateRange.second
                }
                tvDiaryEntriesCount.text = diaryCount.toString()
            } catch (e: Exception) {
                Log.e("InsightsActivity", "Error loading diary entries count", e)
                tvDiaryEntriesCount.text = "0"
            }
        }
    }
    
    private fun loadStreakData(userId: String) {
        val dateRange = getDateRange()
        val allDates = mutableSetOf<String>()
        
        // Load from local database (offline support)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val diaryEntries = diaryRepository.getAllEntriesSync(userId)
                val moodEntries = moodRepository.getAllMoodEntriesSync(userId)
                
                // Filter diary entries by date range
                diaryEntries.forEach { entry ->
                    if (entry.timestamp >= dateRange.first && entry.timestamp <= dateRange.second) {
                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(entry.timestamp))
                        allDates.add(date)
                    }
                }
                
                // Filter mood entries by date range
                moodEntries.forEach { entry ->
                    if (entry.timestamp >= dateRange.first && entry.timestamp <= dateRange.second) {
                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(entry.timestamp))
                        allDates.add(date)
                    }
                }
                
                // Sort all dates
                val sortedDates = allDates.sorted()
                
                // Count completed streaks (7+ consecutive days)
                val completedStreaks = countCompletedStreaks(sortedDates)
                tvStreakCount.text = completedStreaks.toString()
            } catch (e: Exception) {
                Log.e("InsightsActivity", "Error loading streak data", e)
                tvStreakCount.text = "0"
            }
        }
    }
    
    private fun countCompletedStreaks(dates: List<String>): Int {
        if (dates.isEmpty()) {
            return 0
        }
        
        
        var completedStreaks = 0
        var currentStreak = 1
        var longestStreak = 1
        
        for (i in 1 until dates.size) {
            val prevDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dates[i - 1])
            val currDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dates[i])
            
            if (prevDate != null && currDate != null) {
                val diffInMillis = currDate.time - prevDate.time
                val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
                
                
                if (diffInDays == 1) {
                    // Consecutive day
                    currentStreak++
                } else {
                    // Streak broken
                    if (currentStreak >= 7) {
                        completedStreaks++
                    }
                    longestStreak = Math.max(longestStreak, currentStreak)
                    currentStreak = 1
                }
            }
        }
        

        if (currentStreak >= 7) {
            completedStreaks++
        }
        
        longestStreak = Math.max(longestStreak, currentStreak)
        
        return completedStreaks
    }
    
    
    private fun loadDiaryStreak(userId: String) {
        // Load from local database (offline support)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val entries = diaryRepository.getAllEntriesSync(userId)
                val entryDates = mutableSetOf<String>()

                entries.forEach { entry ->
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(entry.timestamp))
                    entryDates.add(date)
                }

                val consecutiveDays = calculateConsecutiveDays(entryDates)
                
                val maxEntries = 30
                streakFragment.updateStreakData(consecutiveDays, maxEntries)
                streakFragment.updateTitle("Diary entry streak")
            } catch (e: Exception) {
                Log.e("InsightsActivity", "Error loading diary streak", e)
                streakFragment.updateStreakData(0, 30)
                streakFragment.updateTitle("Diary entry streak")
            }
        }
    }
    
    private fun calculateConsecutiveDays(entryDates: Set<String>): Int {
        if (entryDates.isEmpty()) return 0
        
        val calendar = Calendar.getInstance()
        var consecutiveDays = 0

        for (i in 0 until 365) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_MONTH, -i)
            val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            
            if (entryDates.contains(dateString)) {
                consecutiveDays++
            } else {
                break
            }
        }
        
        return consecutiveDays
    }
    
    
    
    
    
    private fun getDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        when (currentFilter) {
            "custom" -> {
                val calendar = Calendar.getInstance()
                
                // Start of custom start date
                calendar.timeInMillis = customStartDate
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                
                // End of custom end date
                calendar.timeInMillis = customEndDate
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endOfDay = calendar.timeInMillis
                
                return Pair(startOfDay, endOfDay)
            }
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
                
                return Pair(startOfDay, endOfDay)
            }
            "week" -> {
                calendar.add(Calendar.DAY_OF_MONTH, -6)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val weekStart = calendar.timeInMillis
                
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
    }
    
    //-------------------------------------------------------------------------
    // Load steps data for the graph
    private fun loadStepsData(userId: String) {
        val dateRange = getTimeFilterRange()
        
        // Load from local database (offline support)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val activities = activityRepository.getAllActivitiesSync(userId)
                
                // Filter activities by date range and convert to QuerySnapshot-like structure
                val filteredActivities = activities.filter { activity ->
                    activity.timestamp >= dateRange.first && activity.timestamp <= dateRange.second &&
                    activity.steps != null && activity.steps!! > 0
                }.sortedBy { it.timestamp }
                
                // Convert to a format that processStepsDataByFilter can use
                val processedStepsData = processStepsDataByFilter(filteredActivities)
                
                updateStepsGraph(processedStepsData)
                updateTotalSteps(processedStepsData)
            } catch (e: Exception) {
                Log.e("InsightsActivity", "Error loading steps data", e)
                updateStepsGraph(emptyMap())
                updateTotalSteps(emptyMap())
            }
        }
    }
    
    private fun updateStepsGraph(stepsByDay: Map<String, Int>) {
        stepsGraphArea.updateStepsData(stepsByDay, currentTimeFilter)
    }
    
    private fun processStepsDataByFilter(activities: List<ActivityData>): Map<String, Int> {
        val processedData = mutableMapOf<String, Int>()
        val calendar = Calendar.getInstance()
        
        when (currentTimeFilter) {
            "week" -> {
                // Process by day for the last 7 days
                for (activity in activities) {
                    val steps = activity.steps ?: 0
                    val timestamp = activity.timestamp
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
                    
                    if (steps > 0) {
                        processedData[date] = (processedData[date] ?: 0) + steps
                    }
                }
            }
            "month" -> {
                // Process by week for the last 4 weeks
                val currentWeek = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)
                for (activity in activities) {
                    val steps = activity.steps ?: 0
                    val timestamp = activity.timestamp
                    val date = Date(timestamp)
                    calendar.time = date
                    
                    val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
                    val weekNumber = currentWeek - weekOfYear + 1
                    val weekKey = "Week ${weekNumber}"
                    
                    if (steps > 0) {
                        processedData[weekKey] = (processedData[weekKey] ?: 0) + steps
                    }
                }
            }
            "year" -> {
                // Process by month for the last 12 months
                for (activity in activities) {
                    val steps = activity.steps ?: 0
                    val timestamp = activity.timestamp
                    val date = Date(timestamp)
                    val monthName = SimpleDateFormat("MMM", Locale.getDefault()).format(date)
                    
                    if (steps > 0) {
                        processedData[monthName] = (processedData[monthName] ?: 0) + steps
                    }
                }
            }
        }
        
        return processedData
    }
    
    private fun updateTotalSteps(stepsByDay: Map<String, Int>) {
        val totalSteps = if (stepsByDay.isNotEmpty()) {
            stepsByDay.values.sum()
        } else {
            0
        }
        tvTotalSteps.text = totalSteps.toString()
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
    
    private fun updateEntryCategoriesTitle() {
        val title = when (currentFilter) {
            "day" -> "Today's Entry Categories"
            "week" -> "This Week's Entry Categories"
            "month" -> "This Month's Entry Categories"
            "custom" -> "Custom Date Range Entry Categories"
            else -> "Today's Entry Categories"
        }
        tvEntryCategoriesTitle.text = title
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
    
    //-------------------------------------------------------------------------
    // Load mood data for bar chart
    private fun loadMoodDataForChart(userId: String) {
        val dateRange = getDateRange()
        val moodCounts = mutableMapOf<String, Int>()

        if (currentFilter == "day") {
        } else {
            val moodTypes = listOf("Happy", "Excited", "Content", "Anxious", "Tired", "Sad")
            moodTypes.forEach { mood ->
                moodCounts[mood] = 0
            }
        }
        
        // Load from local database (offline support)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val moodEntries = moodRepository.getAllMoodEntriesSync(userId)
                val diaryEntries = diaryRepository.getAllEntriesSync(userId)
                
                // Process mood entries
                moodEntries.forEach { entry ->
                    val shouldInclude = entry.timestamp >= dateRange.first && entry.timestamp <= dateRange.second
                    
                    if (shouldInclude && entry.mood.isNotEmpty()) {
                        if (currentFilter == "day") {
                            moodCounts[entry.mood] = (moodCounts[entry.mood] ?: 0) + 1
                        } else {
                            if (moodCounts.containsKey(entry.mood)) {
                                moodCounts[entry.mood] = (moodCounts[entry.mood] ?: 0) + 1
                            }
                        }
                    }
                }
                
                // Process diary entries
                diaryEntries.forEach { entry ->
                    val shouldInclude = entry.timestamp >= dateRange.first && entry.timestamp <= dateRange.second
                    
                    if (shouldInclude && entry.mood.isNotEmpty()) {
                        if (currentFilter == "day") {
                            moodCounts[entry.mood] = (moodCounts[entry.mood] ?: 0) + 1
                        } else {
                            if (moodCounts.containsKey(entry.mood)) {
                                moodCounts[entry.mood] = (moodCounts[entry.mood] ?: 0) + 1
                            }
                        }
                    }
                }
                
                moodBarChart.updateMoodData(moodCounts)
            } catch (e: Exception) {
                Log.e("InsightsActivity", "Error loading mood data for chart", e)
                moodBarChart.updateMoodData(moodCounts)
            }
        }
    }
    
}