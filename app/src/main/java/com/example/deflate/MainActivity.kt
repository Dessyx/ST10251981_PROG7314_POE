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
        // OFFLINE ACCESS: Check for cached authentication session
        // ========================================================================
        // Firebase caches authentication tokens locally after first login.
        // This allows users to access the app OFFLINE after they've logged in once.
        // 
        // How it works:
        // 1. User signs in while online → Firebase validates → Token cached
        // 2. User closes app and goes offline
        // 3. User reopens app → This check finds cached token → User is logged in!
        // 4. App works fully offline (diary, activities, mood, etc.)
        // ========================================================================
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is already logged in (cached session) - works offline!
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
            return
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