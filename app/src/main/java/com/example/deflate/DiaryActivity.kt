package com.example.deflate

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.example.deflate.repository.DiaryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

//-------------------------------------------------------------------------
// Diary screen activity
class DiaryActivity : BaseActivity() {

    //-------------------------------------------------------------------------
    // UI references
    private lateinit var btnBack: ImageView
    private lateinit var etEntry: EditText
    private lateinit var btnSave: Button
    private lateinit var btnHistory: Button

    private lateinit var btnMoodHappy: Button
    private lateinit var btnMoodSad: Button
    private lateinit var btnMoodAnxious: Button
    private lateinit var btnMoodTired: Button
    private lateinit var btnMoodExcited: Button
    private lateinit var btnMoodContent: Button
    private lateinit var bottomNav: BottomNavigationView

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var diaryRepository: DiaryRepository
    
    // Notification managers
    private lateinit var streakManager: StreakManager
    private lateinit var moodAnalyzer: MoodAnalyzer

    // State
    private var selectedMood: String? = null

    //-------------------------------------------------------------------------
    // Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        enableEdgeToEdge()
        setContentView(R.layout.activity_diary)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.diary_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        auth = FirebaseAuth.getInstance()
        diaryRepository = DiaryRepository(this)
        streakManager = StreakManager(this)
        moodAnalyzer = MoodAnalyzer(this)

        initializeViews()
        setupClickListeners()
        setupBottomNav()
    }

    // View binding
    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        etEntry = findViewById(R.id.etEntry)
        btnSave = findViewById(R.id.btnSave)
        btnHistory = findViewById(R.id.btnHistory)

        btnMoodHappy = findViewById(R.id.btnMoodHappy)
        btnMoodSad = findViewById(R.id.btnMoodSad)
        btnMoodAnxious = findViewById(R.id.btnMoodAnxious)
        btnMoodTired = findViewById(R.id.btnMoodTired)
        btnMoodExcited = findViewById(R.id.btnMoodExcited)
        btnMoodContent = findViewById(R.id.btnMoodContent)
        bottomNav = findViewById(R.id.bottomNavD)
    }

    //-------------------------------------------------------------------------
    // Event listeners
    private fun setupClickListeners() {
        btnBack.setOnClickListener { navigateToHome() }

        // mood selection
        btnMoodHappy.setOnClickListener { setMood("Happy") }
        btnMoodSad.setOnClickListener { setMood("Sad") }
        btnMoodAnxious.setOnClickListener { setMood("Anxious") }
        btnMoodTired.setOnClickListener { setMood("Tired") }
        btnMoodExcited.setOnClickListener { setMood("Excited") }
        btnMoodContent.setOnClickListener { setMood("Content") }

        // save entry
        btnSave.setOnClickListener { saveDiaryEntry() }

        // open history
        btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    //-------------------------------------------------------------------------
    // Select mood
    private fun setMood(mood: String) {
        selectedMood = mood
        val localizedMood = when (mood) {
            "Happy" -> getString(R.string.mood_happy_label)
            "Excited" -> getString(R.string.mood_excited_label)
            "Content" -> getString(R.string.mood_content_label)
            "Anxious" -> getString(R.string.mood_anxious_label)
            "Tired" -> getString(R.string.mood_tired_label)
            "Sad" -> getString(R.string.mood_sad_label)
            else -> mood
        }
        Toast.makeText(this, getString(R.string.toast_mood_selected, localizedMood), Toast.LENGTH_SHORT).show()

    }

    //-------------------------------------------------------------------------
    // Save diary entry (local first, then sync)
    private fun saveDiaryEntry() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, getString(R.string.toast_sign_in_to_save), Toast.LENGTH_SHORT).show()
            return
        }

        val text = etEntry.text.toString().trim()
        val mood = selectedMood

        if (mood == null) {
            Toast.makeText(this, getString(R.string.toast_select_mood), Toast.LENGTH_SHORT).show()
            return
        }
        if (text.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_write_feeling), Toast.LENGTH_SHORT).show()
            return
        }

        val now = Date()
        val datePretty = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(now)
        val username = user.email?.substringBefore("@") ?: "User"

        val entry = DiaryEntry(
            userId = user.uid,
            username = username,
            text = text,
            mood = mood,
            datePretty = datePretty,
            timestamp = System.currentTimeMillis()
        )

        btnSave.isEnabled = false
        
        CoroutineScope(Dispatchers.Main).launch {
            val result = diaryRepository.saveEntry(entry)
            result.onSuccess {
                Toast.makeText(this@DiaryActivity, getString(R.string.toast_entry_saved), Toast.LENGTH_SHORT).show()
                etEntry.text?.clear()
                selectedMood = null
                btnSave.isEnabled = true
                
               
                handlePostSaveNotifications(user.uid)
            }.onFailure { e ->
                Toast.makeText(this@DiaryActivity, getString(R.string.toast_entry_saved_offline), Toast.LENGTH_SHORT).show()
                etEntry.text?.clear()
                selectedMood = null
                btnSave.isEnabled = true
                
              
                handlePostSaveNotifications(user.uid)
            }
        }
    }

    //-------------------------------------------------------------------------
    // Handle notifications after saving entry
    private fun handlePostSaveNotifications(userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Update and check streak
                val newStreak = streakManager.updateStreak(userId)
                
                // Show streak notification if it's a milestone
                if (streakManager.shouldNotifyForStreak(newStreak)) {
                    NotificationHelper.showStreakNotification(applicationContext, newStreak)
                    streakManager.markStreakAsNotified(newStreak)
                }
                
                // Check for crisis situation
                val isInCrisis = moodAnalyzer.checkForCrisis(userId)
                if (isInCrisis) {
                    NotificationHelper.showCrisisNotification(applicationContext)
                    moodAnalyzer.markCrisisNotificationSent()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // Navigation helpers
    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_today -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }

                R.id.nav_diary -> true
                R.id.nav_calendar -> {
                    startActivity(Intent(this, CalendarActivity::class.java))
                    finish()
                    true
                }

                R.id.nav_insights -> {
                    startActivity(Intent(this, InsightsActivity::class.java))
                    finish()
                    true
                }

                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    true
                }

                else -> false
            }
        }
    }
}
// --------------------------------------------<<< End of File >>>------------------------------------------