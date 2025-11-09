package com.example.deflate

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.biometric.BiometricManager
import android.content.Context
import android.widget.Switch
import com.example.deflate.LocaleHelper

import android.util.Log
import android.net.ConnectivityManager
import android.net.NetworkCapabilities


//-------------------------------------------------------------------------
// Settings screen activity
class SettingsActivity : BaseActivity() {

    companion object {
        private const val TAG = "SettingsActivity"
        private const val PREFS_FILE = "app_prefs"
        private const val KEY_BIOMETRICS_ENABLED = "biometrics_enabled"
        private const val KEY_BIOMETRICS_ACTIVE = "biometrics_active"
        private const val KEY_BIOMETRICS_NEEDS_RESTART = "biometrics_needs_restart"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_REMINDER_FREQUENCY = "reminder_frequency"
        private const val LANG_PREFS_FILE = "language_prefs"
        private const val KEY_LANGUAGE_ENGLISH_SELECTED = "language_english_selected"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var tvUserName: TextView
    private lateinit var languageToggle: LanguageToggleView
    private lateinit var btnReminderDaily: TextView
    private lateinit var btnReminderWeekly: TextView
    private lateinit var btnReminderMonthly: TextView
    private lateinit var notificationToggle: NotificationToggleView




    //-------------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {

        LocaleHelper.setLocale(this, LocaleHelper.getLocale(this))

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize the switch
        languageToggle = findViewById(R.id.languageToggle)
         val langPrefs = getSharedPreferences(LANG_PREFS_FILE, MODE_PRIVATE)


        val savedLangPref = langPrefs.getString(KEY_LANGUAGE_ENGLISH_SELECTED, null)
        val currentLocale = LocaleHelper.getLocale(this)
        val englishSelectedInitial = when {
            savedLangPref != null -> savedLangPref.toBoolean()
            else -> (currentLocale == "en")
        }


        languageToggle.setLeftSelected(englishSelectedInitial, animate = false)

        // When toggled, set locale and persist choice into language_prefs
        languageToggle.setOnSelectionChangedListener { leftSelected ->
            val newLang = if (leftSelected) "en" else "af"
            // Persist user choice into language_prefs (separate file)
            langPrefs.edit().putString(KEY_LANGUAGE_ENGLISH_SELECTED, leftSelected.toString()).apply()

            // Apply language immediately and recreate to update UI strings
            LocaleHelper.setLocale(this, newLang)
            recreate()
        }


        // Firebase Auth
        auth = FirebaseAuth.getInstance()

        // UI references
        val btnBack: ImageView = findViewById(R.id.btnBack)
        tvUserName = findViewById(R.id.tvUserName)
        val etName: EditText = findViewById(R.id.etName)
        val etPassword: EditText = findViewById(R.id.etPassword)
        val btnSave: Button = findViewById(R.id.btnSave)
        val btnLogout: Button = findViewById(R.id.btnLogout)
        val btnDeleteAccount: Button = findViewById(R.id.btnDeleteAccount)


        // Show logged-in user name or email
        val user = auth.currentUser
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        
        // Check for pending name update (offline support)
        val pendingName = prefs.getString("pending_name_update", null)
        val currentName = pendingName ?: user?.displayName ?: user?.email?.substringBefore("@") ?: "User"

        val biometricToggle = findViewById<BiometricToggleView>(R.id.biometricToggle)
        val biometricEnabled = prefs.getBoolean("biometrics_enabled", false)
        biometricToggle.setChecked(biometricEnabled, animate = false)
        
        // Notification toggle setup
        notificationToggle = findViewById(R.id.notificationToggle)
        val notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true) // Default to ON
        notificationToggle.setChecked(notificationsEnabled, animate = false)
        btnReminderDaily = findViewById(R.id.btnReminderDaily)
        btnReminderWeekly = findViewById(R.id.btnReminderWeekly)
        btnReminderMonthly = findViewById(R.id.btnReminderMonthly)
        val savedFrequency = prefs.getString(KEY_REMINDER_FREQUENCY, "daily") ?: "daily"
        setReminderFrequency(savedFrequency, animate = false)
        updateReminderButtonsState(notificationsEnabled)

        tvUserName.text = currentName
        etName.setText(currentName)

        // Go back to home
        btnBack.setOnClickListener {
            finish()
        }


        // Save new details (name/password)
        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            val newPass = etPassword.text.toString().trim()
            val user = auth.currentUser

            if (user != null) {
                // Save name locally first (offline support)
                if (newName.isNotEmpty() && newName != user.displayName) {
                    val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    prefs.edit().putString("pending_name_update", newName).apply()
                    
                    // Update UI immediately
                    tvUserName.text = newName
                    Toast.makeText(this, "Name saved locally. Will sync when online.", Toast.LENGTH_SHORT).show()
                    
                    // Try to sync to Firebase (if online)
                    syncNameToFirebase(newName, user.uid)
                }

                // Update password (requires internet)
                if (newPass.isNotEmpty()) {
                    // Check if online
                    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                    val network = connectivityManager.activeNetwork
                    val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
                    val isOnline = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                                   capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    
                    if (isOnline) {
                        user.updatePassword(newPass).addOnCompleteListener { passTask ->
                            if (passTask.isSuccessful) {
                                Toast.makeText(this, "Password updated!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(
                                    this,
                                    "Failed to update password: ${passTask.exception?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Password update requires internet connection.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // Logout
        btnLogout.setOnClickListener {
            // Show confirmation dialog
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.logout))
                .setMessage(getString(R.string.logout_confirmation))
                .setPositiveButton(getString(R.string.logout)) { _, _ ->
                    performLogout()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        // Delete account
        btnDeleteAccount.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("⚠️ WARNING: This will permanently delete your account and ALL your data (diary entries, moods, activities, etc.). This action CANNOT be undone!\n\nAre you absolutely sure?")
                .setPositiveButton("Yes, Delete Everything") { _, _ ->
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Final Confirmation")
                        .setMessage("This is your last chance! Deleting your account is PERMANENT. Continue?")
                        .setPositiveButton("DELETE ACCOUNT") { _, _ ->
                            performAccountDeletion()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Reminder frequency button listeners
        btnReminderDaily.setOnClickListener {
            if (notificationToggle.isChecked) {
                setReminderFrequency("daily")
                updateNotificationSchedule("daily")
            }
        }
        
        btnReminderWeekly.setOnClickListener {
            if (notificationToggle.isChecked) {
                setReminderFrequency("weekly")
                updateNotificationSchedule("weekly")
            }
        }
        
        btnReminderMonthly.setOnClickListener {
            if (notificationToggle.isChecked) {
                setReminderFrequency("monthly")
                updateNotificationSchedule("monthly")
            }
        }
        
        // Notification toggle listener
        notificationToggle.setOnCheckedChangeListener { checked ->
            prefs.edit()
                .putBoolean(KEY_NOTIFICATIONS_ENABLED, checked)
                .apply()

            updateReminderButtonsState(checked)
            
            if (checked) {

                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()

                val frequency = prefs.getString(KEY_REMINDER_FREQUENCY, "daily") ?: "daily"
                updateNotificationSchedule(frequency)
            } else {

                Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show()

                NotificationScheduler.cancelAllNotifications(this)
            }
        }
        
        biometricToggle.setOnCheckedChangeListener { checked ->
            if (checked) {

                val bm = androidx.biometric.BiometricManager.from(this)
                val can =
                    bm.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK)
                if (can != androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
                    // show error and revert
                    Toast.makeText(
                        this,
                        "Biometrics not available on this device",
                        Toast.LENGTH_SHORT
                    ).show()
                    biometricToggle.setChecked(false, animate = true)
                    return@setOnCheckedChangeListener
                }

                prefs.edit()
                    .putBoolean("biometrics_enabled", true)
                    .putBoolean("biometrics_active", false)
                    .putBoolean("biometrics_needs_restart", true)
                    .apply()
                Toast.makeText(
                    this,
                    "Biometric enabled. Close and re-open the app for activation.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                prefs.edit()
                    .putBoolean("biometrics_enabled", false)
                    .putBoolean("biometrics_active", false)
                    .putBoolean("biometrics_needs_restart", false)
                    .apply()
                Toast.makeText(this, "Biometric disabled.", Toast.LENGTH_SHORT).show()
            }
        }

        //  Bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_settings

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_today -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }

                R.id.nav_diary -> {
                    startActivity(Intent(this, DiaryActivity::class.java))
                    true
                }

                R.id.nav_calendar -> {
                    startActivity(Intent(this, CalendarActivity::class.java))
                    true
                }

                R.id.nav_insights -> {
                    startActivity(Intent(this, InsightsActivity::class.java))
                    true
                }

                R.id.nav_settings -> true
                else -> false
            }
        }
        
        // Sync pending name updates on resume
        syncPendingNameUpdate()
    }
    
    private fun syncNameToFirebase(newName: String, userId: String) {
        val user = auth.currentUser ?: return
        
        // Check if online
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
        val isOnline = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                       capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        
        if (!isOnline) {
            return // Will sync later when online
        }
        
        val db = FirebaseFirestore.getInstance()
        
        // Update profile display name
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newName)
            .build()

        user.updateProfile(profileUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Store updated name in Firestore
                val userMap = hashMapOf("name" to newName, "username" to newName)
                db.collection("users").document(userId)
                    .set(userMap, SetOptions.merge())
                    .addOnSuccessListener {
                        // Clear pending update
                        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                        prefs.edit().remove("pending_name_update").apply()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to store name in Firestore", e)
                    }
            } else {
                Log.e(TAG, "Failed to update profile", task.exception)
            }
        }
    }
    
    private fun syncPendingNameUpdate() {
        val user = auth.currentUser ?: return
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val pendingName = prefs.getString("pending_name_update", null)
        
        if (pendingName != null && pendingName.isNotEmpty()) {
            syncNameToFirebase(pendingName, user.uid)
        }
    }
    
    override fun onResume() {
        super.onResume()
        syncPendingNameUpdate()
    }
    
    private fun performAccountDeletion() {
        val user = auth.currentUser
        if (user != null) {
            user.delete().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val prefs = getSharedPreferences(PREFS_FILE, MODE_PRIVATE)
                    prefs.edit().clear().apply()
                    val homePrefs = getSharedPreferences("home_prefs", MODE_PRIVATE)
                    homePrefs.edit().clear().apply()
                    
                    Toast.makeText(this, "Account permanently deleted", Toast.LENGTH_LONG).show()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    val errorMessage = task.exception?.message ?: "Unknown error"
                    Toast.makeText(
                        this,
                        "Failed to delete account: $errorMessage\n\nYou may need to re-authenticate first.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun setReminderFrequency(frequency: String, animate: Boolean = true) {
        val prefs = getSharedPreferences(PREFS_FILE, MODE_PRIVATE)
        prefs.edit().putString(KEY_REMINDER_FREQUENCY, frequency).apply()
        
        // Update button appearances
        when (frequency) {
            "daily" -> {
                btnReminderDaily.background = getDrawable(R.drawable.selected_filter_bg)
                btnReminderDaily.setTypeface(null, android.graphics.Typeface.BOLD)
                btnReminderWeekly.background = getDrawable(R.drawable.unselected_filter_bg)
                btnReminderWeekly.setTypeface(null, android.graphics.Typeface.NORMAL)
                btnReminderMonthly.background = getDrawable(R.drawable.unselected_filter_bg)
                btnReminderMonthly.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
            "weekly" -> {
                btnReminderDaily.background = getDrawable(R.drawable.unselected_filter_bg)
                btnReminderDaily.setTypeface(null, android.graphics.Typeface.NORMAL)
                btnReminderWeekly.background = getDrawable(R.drawable.selected_filter_bg)
                btnReminderWeekly.setTypeface(null, android.graphics.Typeface.BOLD)
                btnReminderMonthly.background = getDrawable(R.drawable.unselected_filter_bg)
                btnReminderMonthly.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
            "monthly" -> {
                btnReminderDaily.background = getDrawable(R.drawable.unselected_filter_bg)
                btnReminderDaily.setTypeface(null, android.graphics.Typeface.NORMAL)
                btnReminderWeekly.background = getDrawable(R.drawable.unselected_filter_bg)
                btnReminderWeekly.setTypeface(null, android.graphics.Typeface.NORMAL)
                btnReminderMonthly.background = getDrawable(R.drawable.selected_filter_bg)
                btnReminderMonthly.setTypeface(null, android.graphics.Typeface.BOLD)
            }
        }
    }
    
    private fun updateReminderButtonsState(enabled: Boolean) {
        if (enabled) {
            btnReminderDaily.isEnabled = true
            btnReminderWeekly.isEnabled = true
            btnReminderMonthly.isEnabled = true

            val prefs = getSharedPreferences(PREFS_FILE, MODE_PRIVATE)
            val frequency = prefs.getString(KEY_REMINDER_FREQUENCY, "daily") ?: "daily"
            setReminderFrequency(frequency, animate = false)
        } else {

            btnReminderDaily.isEnabled = false
            btnReminderWeekly.isEnabled = false
            btnReminderMonthly.isEnabled = false
            
            btnReminderDaily.background = getDrawable(R.drawable.disabled_filter_bg)
            btnReminderDaily.setTextColor(getColor(android.R.color.darker_gray))
            btnReminderWeekly.background = getDrawable(R.drawable.disabled_filter_bg)
            btnReminderWeekly.setTextColor(getColor(android.R.color.darker_gray))
            btnReminderMonthly.background = getDrawable(R.drawable.disabled_filter_bg)
            btnReminderMonthly.setTextColor(getColor(android.R.color.darker_gray))
        }
    }
    
    private fun updateNotificationSchedule(frequency: String) {
        // Cancel existing notifications
        NotificationScheduler.cancelAllNotifications(this)
        
        // Schedule based on frequency
        when (frequency) {
            "daily" -> {
                NotificationScheduler.scheduleAllNotifications(this)
                Toast.makeText(this, "Daily reminders enabled", Toast.LENGTH_SHORT).show()
            }
            "weekly" -> {

                NotificationScheduler.scheduleWeeklyReminder(this)
                Toast.makeText(this, "Weekly reminders enabled (Sunday 8 PM)", Toast.LENGTH_SHORT).show()
            }
            "monthly" -> {

                NotificationScheduler.scheduleMonthlyReminder(this)
                Toast.makeText(this, "Monthly reminders enabled (1st of month 8 PM)", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun performLogout() {
        // Sign out from Firebase
        auth.signOut()
        
        // Clear biometric preferences
        val prefs = getSharedPreferences(PREFS_FILE, MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_BIOMETRICS_ENABLED, false)
            .putBoolean(KEY_BIOMETRICS_ACTIVE, false)
            .putBoolean(KEY_BIOMETRICS_NEEDS_RESTART, false)
            .remove("pending_name_update")
            .apply()
        
        // Show success message
        Toast.makeText(this, getString(R.string.logged_out), Toast.LENGTH_SHORT).show()
        
        // Navigate to SignInActivity and clear activity stack
        val intent = Intent(this, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }


}




// --------------------------------------------<<< End of File >>>------------------------------------------
