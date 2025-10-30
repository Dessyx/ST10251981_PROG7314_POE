package com.example.deflate

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

//-------------------------------------------------------------------------
// Landing screen activity
class MainActivity : AppCompatActivity() {
    //-------------------------------------------------------------------------
    // Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        LocaleHelper.setLocale(this, LocaleHelper.getLocale(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        enableEdgeToEdge()
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
        
        // Login link click listener
        val loginLink = findViewById<TextView>(R.id.login_link)
        loginLink?.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
    }
}
// --------------------------------------------<<< End of File >>>------------------------------------------