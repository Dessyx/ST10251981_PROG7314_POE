package com.example.deflate

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.ProgressBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import android.content.Context
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.deflate.RetrofitClient
import com.example.deflate.FavQsApi
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import com.example.deflate.repository.MoodRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


//-------------------------------------------------------------------------
// Home screen activity
class HomeActivity : BaseActivity() {
    //-------------------------------------------------------------------------
    // State and dependencies
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var prefs: android.content.SharedPreferences
    private var todayMood: String? = null
    private var currentTagIndex = 0
    private var currentMoodTags = listOf<String>()

    private val moodToTagMap = mapOf(
        "Happy" to listOf("happiness", "joy", "smile", "positive", "life"),
        "Sad" to listOf("motivation", "hope", "strength", "courage", "wisdom"),
        "Anxious" to listOf("wisdom", "peace", "calm", "strength", "courage"),
        "Tired" to listOf("motivation", "energy", "strength", "perseverance", "success"),
        "Excited" to listOf("inspiration", "enthusiasm", "passion", "adventure", "life"),
        "Content" to listOf("peace", "gratitude", "satisfaction", "harmony", "wisdom")
    )

    private val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private fun todayKey() = df.format(Date())


    //-------------------------------------------------------------------------
    // Render today's mood on the main mood button and persist if requested
    private fun applyTodayMood(moodKey: String?, save: Boolean = false) {
        val btn = findViewById<MaterialButton>(R.id.btnTodayMood)

        when (moodKey) {
            "Happy" -> {
                btn.text = ""
                btn.setIconResource(R.drawable.mood_happy)
                btn.iconTint = ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.black))
                btn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.yellow))
            }
            "Sad" -> {
                btn.text = ""
                btn.setIconResource(R.drawable.mood_sad)
                btn.iconTint = ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.black))
                btn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue))
            }
            "Anxious" -> {
                btn.text = ""
                btn.setIconResource(R.drawable.mood_anxious)
                btn.iconTint = ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.black))
                btn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.purple))
            }
            "Tired" -> {
                btn.text = ""
                btn.setIconResource(R.drawable.mood_tired)
                btn.iconTint = ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.black))
                btn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green))
            }
            "Excited" -> {
                btn.text = ""
                btn.setIconResource(R.drawable.mood_excited)
                btn.iconTint = ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.black))
                btn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.red))
            }
            "Content" -> {
                btn.text = ""
                btn.setIconResource(R.drawable.mood_content)
                btn.iconTint = ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.black))
                btn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.orange))
            }
            else -> {
                // Empty/default for a new day
                btn.icon = null
                btn.text = ""
                btn.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(this, android.R.color.white)
                )
            }
        }

        if (save) {
            prefs.edit()
                .putString("MOOD_KEY", moodKey)
                .putString("MOOD_DATE", todayKey())
                .apply()
        }
    }


    // Load persisted mood for the current day
    private fun loadMoodForToday(): String? {
        val savedDate = prefs.getString("MOOD_DATE", null)
        return if (savedDate == todayKey()) prefs.getString("MOOD_KEY", null) else null
    }

    //-------------------------------------------------------------------------
    // Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)

            insets
        }
        
   
        prefs = getSharedPreferences("home_prefs", Context.MODE_PRIVATE)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Get the logged in Firebase user
        val currentUser = auth.currentUser
        val updatedName = intent.getStringExtra("UPDATED_NAME")
        

        val appPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val pendingName = appPrefs.getString("pending_name_update", null)
         val username = updatedName ?: pendingName ?: currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "User"

        //  Welcome text
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        tvWelcome.text = getString(R.string.welcome_user, username)

        //  Mood buttons
        val btnMoodHappy = findViewById<Button>(R.id.btnMoodHappy)
        val btnMoodSad = findViewById<Button>(R.id.btnMoodSad)
        val btnMoodAnxious = findViewById<Button>(R.id.btnMoodAnxious)
        val btnMoodTired = findViewById<Button>(R.id.btnMoodTired)
        val btnMoodExcited = findViewById<Button>(R.id.btnMoodExcited)
        val btnMoodContent = findViewById<Button>(R.id.btnMoodContent)

        // Current Day format Date
        val tvDate = findViewById<TextView>(R.id.tvDate)
        val sdf = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
        val currentDate = sdf.format(Date())
        tvDate.text = currentDate


        applyTodayMood(loadMoodForToday(), save = false)

        // Mood selection actions
        btnMoodHappy.setOnClickListener { setMood("Happy") }
        btnMoodSad.setOnClickListener { setMood("Sad") }
        btnMoodAnxious.setOnClickListener { setMood("Anxious") }
        btnMoodTired.setOnClickListener { setMood("Tired") }
        btnMoodExcited.setOnClickListener { setMood("Excited") }
        btnMoodContent.setOnClickListener { setMood("Content") }

        // Diary button
        val btnDiary = findViewById<Button>(R.id.btnDiary)
        btnDiary.setOnClickListener {
            startActivity(Intent(this, DiaryActivity::class.java))
        }

        // Activities button
        val btnSteps = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSteps)
        btnSteps.setOnClickListener {
            startActivity(Intent(this, ActivitiesActivity::class.java))
        }

        loadStreakData()
        
        // Request notification permission 
        NotificationPermissionHelper.requestNotificationPermission(this)
        refreshUsername()

        // Bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_today -> true
                R.id.nav_diary -> { startActivity(Intent(this, DiaryActivity::class.java)); true }
                R.id.nav_calendar -> { startActivity(Intent(this, CalendarActivity::class.java)); true }
                R.id.nav_insights -> { startActivity(Intent(this, InsightsActivity::class.java)); true }
                R.id.nav_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
                else -> false
            }
        }
    }

    //-------------------------------------------------------------------------
    // Handle mood selection and fetch quotes
    private fun setMood(mood: String) {
        Log.d("HomeActivity", "🎭 User selected mood: $mood")
        todayMood = mood

        applyTodayMood(mood, save = true)

        saveMoodEntryToFirestore(mood)

        Toast.makeText(this, getString(R.string.mood_selected_toast, mood), Toast.LENGTH_SHORT).show()


        currentMoodTags = moodToTagMap[mood] ?: listOf(mood.lowercase())
        currentTagIndex = 0

        if (currentMoodTags.isNotEmpty()) {
            fetchQuote(currentMoodTags[0])
        }
    }

    //-------------------------------------------------------------------------
    // Fetch quotes by tag using Retrofit and update UI
    private fun fetchQuote(tag: String) {
        // First try to load from cache (offline support)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val db = com.example.deflate.database.AppDatabase.getDatabase(this@HomeActivity)
                val cachedQuote = withContext(Dispatchers.IO) {
                    db.quoteDao().getLatestQuote(tag)
                }

                if (cachedQuote != null) {
                    // If device locale is Afrikaans, translate the body only before display
                    val currentLang = Locale.getDefault().language
                    val displayedBody = if (currentLang == "af") {
                        // translate body to Afrikaans using TranslateUtils (suspend)
                        try {
                            TranslateUtils.translateIfNeeded(this@HomeActivity, cachedQuote.body, "af")
                        } catch (e: Exception) {
                            cachedQuote.body
                        }
                    } else {
                        cachedQuote.body
                    }

                    val quoteText = "\"$displayedBody\" \n- ${cachedQuote.author}"
                    findViewById<TextView>(R.id.tvQuote).text = quoteText
                    // we can still try to fetch fresh ones from API
                }


                val api = RetrofitClient.instance
                val call = api.getQuotes(mood = tag)
                call.enqueue(object : Callback<QuoteResponse> {
                    override fun onResponse(call: Call<QuoteResponse>, response: Response<QuoteResponse>) {
                        if (response.isSuccessful) {
                            val quotes = response.body()?.quotes
                            if (!quotes.isNullOrEmpty()) {
                                val firstQuote = quotes[0]

                                // Translate only the body if device locale is Afrikaans
                                CoroutineScope(Dispatchers.Main).launch {
                                    val currentLang = Locale.getDefault().language
                                    val displayedBody = if (currentLang == "af") {
                                        try {
                                            TranslateUtils.translateIfNeeded(this@HomeActivity, firstQuote.body, "af")
                                        } catch (e: Exception) {
                                            firstQuote.body
                                        }
                                    } else {
                                        firstQuote.body
                                    }

                                    val quoteText = "\"$displayedBody\" \n- ${firstQuote.author}"
                                    findViewById<TextView>(R.id.tvQuote).text = quoteText

                                    // Cache English body/author for offline use (keep raw body to allow retranslation on display)
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            val db2 = com.example.deflate.database.AppDatabase.getDatabase(this@HomeActivity)
                                            val quoteEntity = com.example.deflate.database.QuoteEntity(
                                                tag = tag,
                                                body = firstQuote.body, // store original English body
                                                author = firstQuote.author,
                                                timestamp = System.currentTimeMillis()
                                            )
                                            db2.quoteDao().insertQuote(quoteEntity)

                                            // Clean up old quotes (older than 30 days)
                                            val cutoffTime = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                                            db2.quoteDao().deleteOldQuotes(cutoffTime)
                                        } catch (e: Exception) {
                                            Log.e("HomeActivity", "Error caching quote", e)
                                        }
                                    }
                                }
                            } else {
                                findViewById<TextView>(R.id.tvQuote).text = getString(R.string.no_quotes_found)
                            }
                        } else {
                            // show nothing special — rely on cached quote if present
                        }
                    }

                    override fun onFailure(call: Call<QuoteResponse>, t: Throwable) {
                        // API failed — if cached quote existed it is already shown; try next tag fallback
                        tryNextTag()
                    }
                })

            } catch (e: Exception) {
                Log.e("HomeActivity", "Error fetching quote", e)
                // keep whatever quote is currently shown
            }
        }
    }

    // Iterate through tags and try fallback tags when needed
    private fun tryNextTag() {
        currentTagIndex++
        if (currentTagIndex < currentMoodTags.size) {
            fetchQuote(currentMoodTags[currentTagIndex])
        } else {
            val fallbackTags = listOf("wisdom", "life", "inspiration", "motivation")
            if (fallbackTags.isNotEmpty()) {
                fetchQuote(fallbackTags[0])
            } else {
                findViewById<TextView>(R.id.tvQuote).text = getString(R.string.no_quotes_found)

            }
        }
    }

    //-------------------------------------------------------------------------
    // Load and calculate streak data - Days logged THIS WEEK
    private fun loadStreakData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("HomeActivity", "No user logged in for streak calculation")
            return
        }

        Log.d("HomeActivity", "🔥 Loading streak data for user: ${currentUser.uid}")
        
        // Count days logged THIS WEEK (not consecutive, just total)
        val calendar = Calendar.getInstance()
        
        // Get start of this week (Monday)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val weekStart = calendar.timeInMillis
        
        // Get end of this week (Sunday)
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val weekEnd = calendar.timeInMillis
        
        Log.d("HomeActivity", "📅 Week range: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(weekStart))} to ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(weekEnd))}")
        

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val datesThisWeek = mutableSetOf<String>()
                
                // Get local database instance
                val localDb = com.example.deflate.database.AppDatabase.getDatabase(this@HomeActivity)
                val diaryEntries = withContext(Dispatchers.IO) {
                    localDb.diaryEntryDao().getAllEntriesSync(currentUser.uid)
                }

                val moodEntries = withContext(Dispatchers.IO) {
                    localDb.moodEntryDao().getAllMoodEntriesSync(currentUser.uid)
                }
                
                Log.d("HomeActivity", "📝 Found ${diaryEntries.size} total diary entries in local DB")
                Log.d("HomeActivity", "🎭 Found ${moodEntries.size} total mood entries in local DB")
                

                diaryEntries.forEach { entry ->
                    if (entry.timestamp >= weekStart && entry.timestamp <= weekEnd) {
                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(entry.timestamp))
                        datesThisWeek.add(date)
                        Log.d("HomeActivity", "📝 Diary entry on: $date")
                    }
                }

                moodEntries.forEach { entry ->
                    if (entry.timestamp >= weekStart && entry.timestamp <= weekEnd) {
                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(entry.timestamp))
                        datesThisWeek.add(date)
                        Log.d("HomeActivity", "🎭 Mood entry on: $date")
                    }
                }
                
                val daysLoggedThisWeek = datesThisWeek.size
                Log.d("HomeActivity", "📅 Total unique days logged this week: $daysLoggedThisWeek")
                Log.d("HomeActivity", "📅 Days: ${datesThisWeek.sorted()}")
                
                // Update UI
                updateStreakUI(daysLoggedThisWeek, 7)
            } catch (e: Exception) {
                Log.e("HomeActivity", "Error loading streak data", e)
                updateStreakUI(0, 7)
            }
        }
    }

    private fun updateStreakUI(currentStreak: Int, maxStreak: Int) {
        val tvStreak = findViewById<TextView>(R.id.tvStreak)
        val progressStreak = findViewById<ProgressBar>(R.id.progressStreak)
        val tvStreakMessage = findViewById<TextView>(R.id.tvStreakMessage)

        Log.d("HomeActivity", "🔥 Updating streak UI: $currentStreak/$maxStreak days")

        tvStreak?.text = getString(R.string.streak_format, currentStreak, maxStreak)


        // Update progress bar
        progressStreak?.max = maxStreak
        progressStreak?.progress = currentStreak

        val daysLeft = maxStreak - currentStreak
        when {
            currentStreak == 0 -> {
                tvStreakMessage?.text = getString(R.string.streak_start_prompt)
            }
            daysLeft > 0 -> {
                tvStreakMessage?.text = getString(R.string.streak_days_to_complete, daysLeft)
            }
            else -> {
                tvStreakMessage?.text = getString(R.string.streak_complete, maxStreak)
            }
        }
    }

    //-------------------------------------------------------------------------
    // Refresh username from various sources
    private fun refreshUsername() {
        val currentUser = auth.currentUser
        val appPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val pendingName = appPrefs.getString("pending_name_update", null)
        
        val username = pendingName ?: currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "User"
        
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        tvWelcome?.text = getString(R.string.welcome_user, username)

    }
    
    override fun onResume() {
        super.onResume()
        refreshUsername()
        loadStreakData()
    }
    
    //-------------------------------------------------------------------------
    // Save mood entry (local first, then sync)
    private fun saveMoodEntryToFirestore(mood: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("HomeActivity", "Cannot save mood: No user logged in")
            return
        }

        val now = Date()
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
        val username = currentUser.email?.substringBefore("@") ?: "User"
        
        Log.d("HomeActivity", "Saving mood: $mood for user: ${currentUser.uid} on date: $dateKey")

        val moodRepository = MoodRepository(this)
        CoroutineScope(Dispatchers.Main).launch {
            val result = moodRepository.saveMoodEntry(
                userId = currentUser.uid,
                username = username,
                mood = mood,
                dateKey = dateKey,
                timestamp = System.currentTimeMillis(),
                source = "home_screen"
            )
            
            result.onSuccess {
                Toast.makeText(this@HomeActivity, getString(R.string.mood_saved_success), Toast.LENGTH_SHORT).show()
                loadStreakData()
            }.onFailure { e ->
                Toast.makeText(this@HomeActivity, getString(R.string.mood_saved_offline), Toast.LENGTH_SHORT).show()
                loadStreakData()
            }
        }
    }
}
// --------------------------------------------<<< End of File >>>------------------------------------------
