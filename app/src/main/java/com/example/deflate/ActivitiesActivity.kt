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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
    private lateinit var db: FirebaseFirestore

    //-------------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        enableEdgeToEdge()
        setContentView(R.layout.activity_activities)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activities_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initializeViews()
        setupClickListeners()
        loadCurrentActivities()
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

        // Bottom Navigation
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_today -> {
                    navigateToHome()
                    true
                }
                R.id.nav_diary -> {
                    navigateToDiary()
                    true
                }
                R.id.nav_calendar -> {
                    navigateToCalendar()
                    true
                }
                R.id.nav_insights -> {
                    navigateToInsights()
                    true
                }
                R.id.nav_settings -> {
                    navigateToSettings()
                    true
                }
                else -> false
            }
        }
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

        // Save to Firestore
        db.collection("activities")
            .add(activityData)
            .addOnSuccessListener { documentReference ->
                Log.d("ActivitiesActivity", "Document added with ID: ${documentReference.id}")

                // Update UI
                if (weight != null) {
                    tvCurrentWeight.text = getString(R.string.weight_format, weight.toInt())
                    etWeight.text.clear()
                }
                if (steps != null) {
                    tvCurrentSteps.text = getString(R.string.steps_format, steps)
                    etSteps.text.clear()
                }

                Toast.makeText(this, getString(R.string.activities_saved_successfully), Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("ActivitiesActivity", "Error adding document", e)
                Toast.makeText(this, getString(R.string.failed_to_save_activities, e.message), Toast.LENGTH_SHORT).show()
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

        // Get the latest weight entry - simplified query
        db.collection("activities")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10) // Get more documents to filter client-side
            .get()
            .addOnSuccessListener { documents ->
                var latestWeight: Double? = null
                for (document in documents) {
                    val activityData = document.toObject(ActivityData::class.java)
                    if (activityData.weight != null) {
                        latestWeight = activityData.weight
                        break // Found the latest weight entry
                    }
                }
                tvCurrentWeight.text = if (latestWeight != null) getString(R.string.weight_format, latestWeight.toInt()) else getString(R.string.weight_default)
            }
            .addOnFailureListener { e ->
                Log.e("ActivitiesActivity", getString(R.string.error_loading_weight_data), e)
                tvCurrentWeight.text = getString(R.string.weight_default)
            }

        // Get the latest steps entry - simplified query
        db.collection("activities")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10) // Get more documents to filter client-side
            .get()
            .addOnSuccessListener { documents ->
                var latestSteps: Int? = null
                for (document in documents) {
                    val activityData = document.toObject(ActivityData::class.java)
                    if (activityData.steps != null) {
                        latestSteps = activityData.steps
                        break // Found the latest steps entry
                    }
                }
                tvCurrentSteps.text = if (latestSteps != null) getString(R.string.steps_format, latestSteps) else getString(R.string.steps_default)
            }
            .addOnFailureListener { e ->
                Log.e("ActivitiesActivity", getString(R.string.error_loading_steps_data), e)
                tvCurrentSteps.text = getString(R.string.steps_default)
            }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToDiary() {
        val intent = Intent(this, DiaryActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToCalendar() {
        val intent = Intent(this, CalendarActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToInsights() {
        val intent = Intent(this, InsightsActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
        finish()
    }
}
// --------------------------------------------<<< End of File >>>------------------------------------------