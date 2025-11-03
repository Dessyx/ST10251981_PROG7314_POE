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
import com.google.firebase.firestore.FirebaseFirestore
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
    private lateinit var db: FirebaseFirestore

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
        db = FirebaseFirestore.getInstance()

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
        Toast.makeText(this, "Mood selected: $mood", Toast.LENGTH_SHORT).show()

    }

    //-------------------------------------------------------------------------
    // Save diary entry to Firestore for the current user
    private fun saveDiaryEntry() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Please sign in to save an entry.", Toast.LENGTH_SHORT).show()
            return
        }

        val text = etEntry.text.toString().trim()
        val mood = selectedMood

        if (mood == null) {
            Toast.makeText(this, "Please select a mood.", Toast.LENGTH_SHORT).show()
            return
        }
        if (text.isEmpty()) {
            Toast.makeText(this, "Please write about how you're feeling.", Toast.LENGTH_SHORT).show()
            return
        }

        val now = Date()
        val datePretty = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(now)
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
        val username = user.email?.substringBefore("@") ?: "User"

        val entry = hashMapOf(
            "userId" to user.uid,
            "username" to username,
            "text" to text,
            "mood" to mood,
            "datePretty" to datePretty,
            "timestamp" to System.currentTimeMillis()
        )

        btnSave.isEnabled = false
        db.collection("diaryEntries")
            .add(entry)
            .addOnSuccessListener {
                Toast.makeText(this, "Entry saved!", Toast.LENGTH_SHORT).show()
                etEntry.text?.clear()
                selectedMood = null
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener {
                btnSave.isEnabled = true
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