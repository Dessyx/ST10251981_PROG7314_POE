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
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var tvUserName: TextView

    private lateinit var switchLanguage: SwitchMaterial


    //-------------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved language before inflating layout
        LocaleHelper.setLocale(this, LocaleHelper.getLocale(this))

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize the switch
        switchLanguage = findViewById(R.id.switchLanguage)
        switchLanguage.isChecked = LocaleHelper.getLocale(this) == "af"

        // Handle language toggle
        switchLanguage.setOnCheckedChangeListener { _, isChecked ->
            val newLang = if (isChecked) "af" else "en"
            LocaleHelper.setLocale(this, newLang)  // Apply language
            recreate()                              // Refresh this activity
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
        val enabled = prefs.getBoolean("biometrics_enabled", false)

        biometricToggle.setChecked(enabled, animate = false)

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
            user?.delete()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Account deleted", Toast.LENGTH_LONG).show()
                    val intent = Intent(this, SignInActivity::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        biometricToggle.setOnCheckedChangeListener { checked ->
            if (checked) {
                // optional: verify device supports biometrics first
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
