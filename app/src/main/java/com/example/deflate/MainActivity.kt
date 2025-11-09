package com.example.deflate

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

//-------------------------------------------------------------------------
// Landing screen activity
class MainActivity : BaseActivity() {
    //-------------------------------------------------------------------------
    // Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        // Set the app locale before anything else
        LocaleHelper.setLocale(this, LocaleHelper.getLocale(this))

        super.onCreate(savedInstanceState)
        
        // ========================================================================
        // AUTHENTICATION CHECK: Check for cached session and biometrics
        // ========================================================================
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        
        if (currentUser != null) {
            // User has a cached session
            val biometricsEnabled = prefs.getBoolean("biometrics_enabled", false)
            val biometricsActive = prefs.getBoolean("biometrics_active", false)
            val needsRestart = prefs.getBoolean("biometrics_needs_restart", false)
            
            // If biometrics are active, require authentication at SignInActivity
            if (biometricsEnabled && biometricsActive) {
                // Redirect to SignInActivity for biometric authentication
                val intent = Intent(this, SignInActivity::class.java)
                intent.putExtra("REQUIRE_BIOMETRIC_AUTH", true)
                startActivity(intent)
                finish()
                return
            } 
            // If biometrics need restart, activate them now
            else if (needsRestart) {
                prefs.edit()
                    .putBoolean("biometrics_active", true)
                    .putBoolean("biometrics_needs_restart", false)
                    .apply()
                // Redirect to SignInActivity for biometric authentication
                val intent = Intent(this, SignInActivity::class.java)
                intent.putExtra("REQUIRE_BIOMETRIC_AUTH", true)
                startActivity(intent)
                finish()
                return
            }
            // No biometrics, allow offline access
            else {
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
                return
            }
        }
        
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }



    //-------------------------------------------------------------------------
        // Set up navigation buttons
        val letsBeginButton = findViewById<TextView>(R.id.lets_begin_button)
        letsBeginButton?.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        //Login link click listener
        val loginLink = findViewById<TextView>(R.id.login_link)
        loginLink?.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
    }
}


// --------------------------------------------<<< End of File >>>------------------------------------------