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
import android.util.Log
import com.example.deflate.security.BiometricCredentialStore


//-------------------------------------------------------------------------
// Settings screen activity
class SettingsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SettingsActivity"

        // must match SignInActivity constants
        private const val CIPHERTEXT_PREFS_FILE = "biometric_creds_prefs"
        private const val PREFS_MODE = Context.MODE_PRIVATE
        private const val BIOMETRICS_ENABLED_FLAG = "biometrics_enabled_flag"
        private const val CREDS_SAVED_FLAG = "creds_saved_flag"

        // Keystore key alias used by SignInActivity / CryptographyManager
        // If you change the alias elsewhere, update this constant accordingly.
        private const val KEYSTORE_KEY_ALIAS = "biometric_aes_key_v1"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var tvUserName: TextView
    private var switchBiometrics: SwitchMaterial? = null

    //-------------------------------------------------------------------------
    // Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Firebase Auth
        auth = FirebaseAuth.getInstance()

        // UI references
        val btnBack: ImageView = findViewById(R.id.btnBack)
        tvUserName = findViewById(R.id.tvUserName)
        val etName: EditText = findViewById(R.id.etName)
        val etPassword: EditText = findViewById(R.id.etPassword)
        val btnSave: Button = findViewById(R.id.btnSave)
        val btnDeleteAccount: Button = findViewById(R.id.btnDeleteAccount)

        switchBiometrics = try {
            findViewById<SwitchMaterial>(R.id.switchBiometrics)
        } catch (e: Exception) {
            null
        }


        // Show logged-in user name or email
        val user = auth.currentUser

        val currentName = user?.displayName ?: user?.email?.substringBefore("@") ?: "User"

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
                val db = FirebaseFirestore.getInstance()
                val userId = user.uid
                val userMap = hashMapOf("username" to newName)
                db.collection("users").document(userId)
                    .set(userMap, SetOptions.merge())


                // Update profile display name (and mirror to Firestore)
                if (newName.isNotEmpty() && newName != user.displayName) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(newName)
                        .build()

                    user.updateProfile(profileUpdates).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            tvUserName.text = newName
                            Toast.makeText(this, "Name updated!", Toast.LENGTH_SHORT).show()

                            // Store updated name in Firestore
                            val userMap = hashMapOf("name" to newName)
                            db.collection("users").document(userId)
                                .set(userMap, SetOptions.merge())
                                .addOnSuccessListener {
                                    // Successfully stored in Firestore
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this,
                                        "Failed to store name: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                        } else {
                            Toast.makeText(
                                this,
                                "Failed to update name: ${task.exception?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }


                // Update password
                if (newPass.isNotEmpty()) {
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
                }
            }
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
        switchBiometrics?.let { sw ->
            // Reflect stored preference
            sw.isChecked = getPrefs().getBoolean(BIOMETRICS_ENABLED_FLAG, false)

            sw.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    // Enabling biometrics: verify device supports biometrics
                    val bm = BiometricManager.from(this)
                    val canAuth = bm.canAuthenticate(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG
                                or BiometricManager.Authenticators.BIOMETRIC_WEAK
                    )
                    if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
                        Toast.makeText(
                            this,
                            "Biometrics not available on this device",
                            Toast.LENGTH_SHORT
                        ).show()
                        // revert switch visually
                        sw.isChecked = false
                        return@setOnCheckedChangeListener
                    }

                    // Persist user's consent; actual credentials are only saved after a normal login
                    getPrefs().edit().putBoolean(BIOMETRICS_ENABLED_FLAG, true).apply()
                    Toast.makeText(
                        this,
                        "Biometric sign-in enabled. Sign in once normally to store credentials.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // Disabling biometric sign-in: clear stored creds and optionally the keystore key
                    getPrefs().edit().putBoolean(BIOMETRICS_ENABLED_FLAG, false).apply()

                    try {
                        BiometricCredentialStore.clearSavedCredentialsAndKey(
                            this,
                            KEYSTORE_KEY_ALIAS
                        )
                        Toast.makeText(
                            this,
                            "Biometric sign-in disabled and saved credentials cleared",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error while clearing biometric creds/key: ${e.message}", e)
                        Toast.makeText(
                            this,
                            "Biometric disabled, but failed to clear all stored data.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
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
        }
      private fun getPrefs() = getSharedPreferences(CIPHERTEXT_PREFS_FILE, PREFS_MODE)
    }







// --------------------------------------------<<< End of File >>>------------------------------------------
