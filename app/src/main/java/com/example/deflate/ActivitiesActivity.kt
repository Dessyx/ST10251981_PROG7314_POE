package com.example.deflate

import android.content.Intent    // Imports
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.example.deflate.repository.ActivityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

//-------------------------------------------------------------------------
// Activity screen activity
class ActivitiesActivity : BaseActivity() {

    private lateinit var etWeight: EditText  // Declarations
    private lateinit var etSteps: EditText
    private lateinit var btnReset: Button
    private lateinit var btnSaveActivities: Button
    private lateinit var tvCurrentWeight: TextView
    private lateinit var tvCurrentSteps: TextView
    private lateinit var btnBack: ImageView
    private lateinit var bottomNav: BottomNavigationView

    // Firestore
    private lateinit var auth: FirebaseAuth
    private lateinit var activityRepository: ActivityRepository

    //-------------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_activities)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activities_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        activityRepository = ActivityRepository(this)

        initializeViews()
        setupClickListeners()
        loadCurrentActivities()
        
        // Sync activities
        syncActivities()
    }

    private fun initializeViews() {
        etWeight = findViewById(R.id.etWeight)
        etSteps = findViewById(R.id.etSteps)
        btnReset = findViewById(R.id.btnReset)
        btnSaveActivities = findViewById(R.id.btnSaveActivities)
        tvCurrentWeight = findViewById(R.id.tvCurrentWeight)
        tvCurrentSteps = findViewById(R.id.tvCurrentSteps)
        btnBack = findViewById(R.id.btnBack)
        bottomNav = findViewById(R.id.bottomNav)
        
        // Prevent EditText from losing focus when keyboard appears
        etWeight.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                // Scroll to make sure the EditText is visible
                view.post {
                    try {
                        val scrollView = findViewById<androidx.core.widget.NestedScrollView>(R.id.scrollView)
                        scrollView?.smoothScrollTo(0, view.top)
                    } catch (e: Exception) {
                        Log.e("ActivitiesActivity", "Error scrolling to view", e)
                    }
                }
            }
        }
        
        etSteps.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                // Scroll to make sure the EditText is visible
                view.post {
                    try {
                        val scrollView = findViewById<androidx.core.widget.NestedScrollView>(R.id.scrollView)
                        scrollView?.smoothScrollTo(0, view.top)
                    } catch (e: Exception) {
                        Log.e("ActivitiesActivity", "Error scrolling to view", e)
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        btnReset.setOnClickListener {
            resetInputs()
        }

        btnSaveActivities.setOnClickListener {
            saveActivities()
        }

        btnBack.setOnClickListener {
            navigateToHome()
        }

        // Bottom Navigation - only navigate if not currently on this screen
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_today -> {
                    if (this::class.java != HomeActivity::class.java) {
                        navigateToHome()
                    }
                    true
                }
                R.id.nav_diary -> {
                    if (this::class.java != DiaryActivity::class.java) {
                        navigateToDiary()
                    }
                    true
                }
                R.id.nav_calendar -> {
                    if (this::class.java != CalendarActivity::class.java) {
                        navigateToCalendar()
                    }
                    true
                }
                R.id.nav_insights -> {
                    if (this::class.java != InsightsActivity::class.java) {
                        navigateToInsights()
                    }
                    true
                }
                R.id.nav_settings -> {
                    if (this::class.java != SettingsActivity::class.java) {
                        navigateToSettings()
                    }
                    true
                }
                else -> false
            }
        }
        
        // Don't set a selected item since Activities is accessed from Home, not from bottom nav
    }

    private fun resetInputs() {
        etWeight.text.clear()
        etSteps.text.clear()
        Toast.makeText(this, getString(R.string.input_cleared), Toast.LENGTH_SHORT).show()
    }

    private fun saveActivities() {
        val weightText = etWeight.text.toString().trim()
        val stepsText = etSteps.text.toString().trim()

        if (weightText.isEmpty() && stepsText.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_weight_or_steps), Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.please_sign_in_to_save_activities), Toast.LENGTH_SHORT).show()
            return
        }

        var weight: Double? = null
        var steps: Int? = null

        if (weightText.isNotEmpty()) {
            try {
                weight = weightText.toDouble()
            } catch (e: NumberFormatException) {
                Toast.makeText(this, getString(R.string.please_enter_valid_weight), Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (stepsText.isNotEmpty()) {
            try {
                steps = stepsText.toInt()
            } catch (e: NumberFormatException) {
                Toast.makeText(this, getString(R.string.please_enter_valid_steps), Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Create activity data
        val activityData = ActivityData(
            userId = currentUser.uid,
            weight = weight,
            steps = steps,
            date = Date(),
            timestamp = System.currentTimeMillis()
        )

        // Save to local database (will sync to Firestore when online)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = activityRepository.saveActivity(activityData)
                result.onSuccess {
                    // Update UI
                    if (weight != null) {
                        tvCurrentWeight.text = getString(R.string.weight_format, weight)
                        etWeight.text.clear()
                    }
                    if (steps != null) {
                        tvCurrentSteps.text = getString(R.string.steps_format, steps)
                        etSteps.text.clear()
                    }
                    Toast.makeText(this@ActivitiesActivity, getString(R.string.activities_saved_successfully), Toast.LENGTH_SHORT).show()
                }.onFailure { e ->
                    Log.e("ActivitiesActivity", "Error saving activity", e)
                    Toast.makeText(this@ActivitiesActivity, "Saved locally. Will sync when online.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ActivitiesActivity", "Error in saveActivities", e)
                Toast.makeText(this@ActivitiesActivity, "Error saving. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun syncActivities() {
        val user = auth.currentUser ?: return
        CoroutineScope(Dispatchers.IO).launch {
            activityRepository.syncFromFirestore(user.uid)
            activityRepository.syncUnsyncedActivities(user.uid)
        }
    }

    private fun loadCurrentActivities() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Set default values when not signed in
            tvCurrentWeight.text = getString(R.string.weight_default)
            tvCurrentSteps.text = getString(R.string.steps_default)
            Toast.makeText(this, getString(R.string.please_sign_in_to_view_activities), Toast.LENGTH_SHORT).show()
            return
        }

        // Get the latest weight and steps from local database
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val latestWeight = activityRepository.getLatestWeight(currentUser.uid)
                val latestSteps = activityRepository.getLatestSteps(currentUser.uid)
                
                tvCurrentWeight.text = if (latestWeight?.weight != null) {
                    getString(R.string.weight_format, latestWeight.weight)
                } else {
                    getString(R.string.weight_default)
                }
                
                tvCurrentSteps.text = if (latestSteps?.steps != null) {
                    getString(R.string.steps_format, latestSteps.steps)
                } else {
                    getString(R.string.steps_default)
                }
            } catch (e: Exception) {
                Log.e("ActivitiesActivity", "Error loading activities", e)
                // Set default values on error
                tvCurrentWeight.text = getString(R.string.weight_default)
                tvCurrentSteps.text = getString(R.string.steps_default)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        syncActivities()
    }

    private fun navigateToHome() {
        if (this::class.java != HomeActivity::class.java) {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun navigateToDiary() {
        if (this::class.java != DiaryActivity::class.java) {
            val intent = Intent(this, DiaryActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun navigateToCalendar() {
        if (this::class.java != CalendarActivity::class.java) {
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun navigateToInsights() {
        if (this::class.java != InsightsActivity::class.java) {
            val intent = Intent(this, InsightsActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun navigateToSettings() {
        if (this::class.java != SettingsActivity::class.java) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
// --------------------------------------------<<< End of File >>>------------------------------------------