package com.example.deflate

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

//-------------------------------------------------------------------------
// Sign Up screen activity
class SignUpActivity : BaseActivity() {

    companion object {
        private const val TAG = "SignUpActivity"
    }

    private lateinit var nameEditText: EditText
    private lateinit var surnameEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var registerButton: MaterialButton
    private lateinit var loginLink: TextView

    //-------------------------------------------------------------------------
    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    //-------------------------------------------------------------------------
    // Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signup_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        initializeViews()

        // Set up click listeners
        setupClickListeners()
    }

    //-------------------------------------------------------------------------
    // View binding
    private fun initializeViews() {
        nameEditText = findViewById(R.id.name_edittext)
        surnameEditText = findViewById(R.id.surname_edittext)
        usernameEditText = findViewById(R.id.username_edittext)
        passwordEditText = findViewById(R.id.password_edittext)
        registerButton = findViewById(R.id.register_button)
        loginLink = findViewById(R.id.login_link)
    }

    //-------------------------------------------------------------------------
    // Event listeners
    private fun setupClickListeners() {
        registerButton.setOnClickListener {
            handleRegistration()
        }

        loginLink.setOnClickListener {
            navigateToLogin()
        }
    }

    //-------------------------------------------------------------------------
    // Registration flow
    private fun handleRegistration() {
        val name = nameEditText.text.toString().trim()
        val surname = surnameEditText.text.toString().trim()
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (!validateInputs(name, surname, username, password)) return

        // Check network connectivity
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Account creation requires internet connection. Please connect to the internet and try again.", Toast.LENGTH_LONG).show()
            return
        }

        registerButton.isEnabled = false
        registerButton.text = "Registering..."

        registerUserWithFirebase(name, surname, username, password)
    }
    
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // Input validation
    private fun validateInputs(name: String, surname: String, username: String, password: String): Boolean {
        var isValid = true
        clearErrors()

        if (TextUtils.isEmpty(name)) {
            nameEditText.error = "Name is required"
            nameEditText.requestFocus()
            isValid = false
        } else if (name.length < 2) {
            nameEditText.error = "Name must be at least 2 characters"
            nameEditText.requestFocus()
            isValid = false
        }

        if (TextUtils.isEmpty(surname)) {
            surnameEditText.error = "Surname is required"
            if (isValid) surnameEditText.requestFocus()
            isValid = false
        } else if (surname.length < 2) {
            surnameEditText.error = "Surname must be at least 2 characters"
            if (isValid) surnameEditText.requestFocus()
            isValid = false
        }

        if (TextUtils.isEmpty(username)) {
            usernameEditText.error = "Username is required"
            if (isValid) usernameEditText.requestFocus()
            isValid = false
        } else if (username.length < 3) {
            usernameEditText.error = "Username must be at least 3 characters"
            if (isValid) usernameEditText.requestFocus()
            isValid = false
        } else if (!username.matches("^[a-zA-Z0-9_]+$".toRegex())) {
            usernameEditText.error = "Username can only contain letters, numbers, and underscores"
            if (isValid) usernameEditText.requestFocus()
            isValid = false
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.error = "Password is required"
            if (isValid) passwordEditText.requestFocus()
            isValid = false
        } else if (password.length < 8) {
            passwordEditText.error = "Password must be at least 8 characters"
            if (isValid) passwordEditText.requestFocus()
            isValid = false
        } else if (!password.any { it.isUpperCase() } || !password.any { !it.isLetterOrDigit() }) {
            passwordEditText.error = "Password must contain 1 capital letter, 1 symbol, and 8+ characters"
            if (isValid) passwordEditText.requestFocus()
            isValid = false
        }

        return isValid
    }

    // Clear validation errors
    private fun clearErrors() {
        nameEditText.error = null
        surnameEditText.error = null
        usernameEditText.error = null
        passwordEditText.error = null
    }

    //-------------------------------------------------------------------------
    // Firebase create user and store profile
    private fun registerUserWithFirebase(name: String, surname: String, username: String, password: String) {
        val email = "$username@deflate.com"

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let { saveUserDataToFirestore(it.uid, name, surname, username, email) }
                } else {
                    val exception = task.exception
                    val errorMessage = if (exception is FirebaseAuthException) {
                        when (exception.errorCode) {
                            "ERROR_EMAIL_ALREADY_IN_USE" -> "This email is already registered"
                            "ERROR_INVALID_EMAIL" -> "Invalid email format"
                            "ERROR_WEAK_PASSWORD" -> "Password is too weak"
                            "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Please check your connection"
                            else -> exception.message ?: "Registration failed"
                        }
                    } else {
                        exception?.message ?: "Unknown error"
                    }
                    handleRegistrationError(errorMessage)
                }
            }
    }

    // Save user document to Firestore
    private fun saveUserDataToFirestore(uid: String, name: String, surname: String, username: String, email: String) {
        val userData = hashMapOf(
            "uid" to uid,
            "name" to name,
            "surname" to surname,
            "username" to username,
            "email" to email,
            "createdAt" to Date(),
            "isActive" to true
        )


        val user = auth.currentUser
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()
        
        user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->

            firestore.collection("users")
                .document(uid)
                .set(userData)
                .addOnSuccessListener {
                    // Save name to SharedPreferences for offline access
                    val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    prefs.edit().putString("pending_name_update", name).apply()
                    handleRegistrationSuccess(name, surname)
                }
                .addOnFailureListener { exception ->
                    handleRegistrationError("User created but failed to save data: ${exception.message}")
                }
        }
    }

    // Success handler
    private fun handleRegistrationSuccess(name: String, surname: String) {
        Toast.makeText(this, "Registration successful! Welcome $name $surname", Toast.LENGTH_LONG).show()
        registerButton.isEnabled = true
        registerButton.text = "Register"
        clearForm()
        navigateToLogin()
    }

    // Error handler
    private fun handleRegistrationError(errorMessage: String) {
        Toast.makeText(this, "Registration failed: $errorMessage", Toast.LENGTH_LONG).show()
        registerButton.isEnabled = true
        registerButton.text = "Register"
    }

    // Clear form inputs
    private fun clearForm() {
        nameEditText.text.clear()
        surnameEditText.text.clear()
        usernameEditText.text.clear()
        passwordEditText.text.clear()
        clearErrors()
    }

    // Navigation to Sign In
    private fun navigateToLogin() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }
}
// --------------------------------------------<<< End of File >>>------------------------------------------