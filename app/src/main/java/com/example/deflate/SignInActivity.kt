package com.example.deflate

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.facebook.CallbackManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.view.isVisible
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat




//-------------------------------------------------------------------------
// Sign In screen activity
class SignInActivity : BaseActivity() {


    companion object {
        private const val TAG = "SignInActivity"
        private const val RC_SIGN_IN = 9001
        private const val RC_GITHUB_AUTH = 9002
        private const val GITHUB_CLIENT_ID = "Ov23liwG3uaDjiDZJnR4"
        private const val GITHUB_REDIRECT_URI = "http://localhost:8080/github-callback"
        private const val PREFS_FILE = "app_prefs"
        private const val KEY_BIOMETRICS_ENABLED = "biometrics_enabled"
        private const val KEY_BIOMETRICS_ACTIVE = "biometrics_active"
        private const val KEY_BIOMETRICS_NEEDS_RESTART = "biometrics_needs_restart"



    }


    //  UI Elements
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signinButton: MaterialButton
    private lateinit var signupLink: TextView
    private lateinit var googleSignInButton: MaterialButton
    private lateinit var githubSignInButton: MaterialButton
    private lateinit var biometricSignInButton: MaterialButton
    private lateinit var btnBack: ImageView


    //  Firebase / SDKs
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager



    //-------------------------------------------------------------------------
    //  Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signin)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signin_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //-------------------------------------------------------------------------
        // Initiate Firebase and SDKs
        auth = FirebaseAuth.getInstance()
        configureGoogleSignIn()



        initViews()
        setupClickListeners()

        biometricSignInButton.isVisible = Biometrics.isAvailable(this)

        // Activation after restart:
        // If the user previously enabled biometrics and the "needs_restart" flag is set,
        // we only mark the biometrics as active now (this code runs at app start).
        // This enforces the "close and reopen app for biometrics to work" rule.
        val prefs = getSharedPreferences(PREFS_FILE, MODE_PRIVATE)
        val needsRestart = prefs.getBoolean(KEY_BIOMETRICS_NEEDS_RESTART, false)
        if (needsRestart) {
            // App has been restarted (we're now running): activate biometrics and clear the flag.
            prefs.edit().putBoolean(KEY_BIOMETRICS_ACTIVE, true).putBoolean(KEY_BIOMETRICS_NEEDS_RESTART, false).apply()
            Log.d(TAG, "Biometrics activation: app restart detected - biometrics now active")
        }
    }


    //-------------------------------------------------------------------------
    // Setup
    private fun initViews() {
        usernameEditText = findViewById(R.id.username_edittext)
        passwordEditText = findViewById(R.id.password_edittext)
        signinButton = findViewById(R.id.login_button)
        googleSignInButton = findViewById(R.id.google_signin_button)
        githubSignInButton = findViewById(R.id.github_signin_button)
        biometricSignInButton = findViewById(R.id.biometric_signin_button)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupClickListeners() {
        signinButton.setOnClickListener { handleSignIn() }
        googleSignInButton.setOnClickListener { signInWithGoogle() }
        githubSignInButton.setOnClickListener { signInWithGitHub() }
        biometricSignInButton.setOnClickListener { onBiometricButtonClicked() }
        btnBack.setOnClickListener { navigateToSignUp() }
    }


    //-------------------------------------------------------------------------
    //  Email/Password Authentication
    private fun handleSignIn() {
        val input = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (!validateInputs(input, password)) return

        signinButton.isEnabled = false
        signinButton.text = "Signing in..."

        if (input.contains("@")) {
            // User entered an email
            signInWithEmail(input, password)
        } else {
            // User entered a username, look up email in Firestore
            val db = FirebaseFirestore.getInstance()
            db.collection("users")
                .whereEqualTo("username", input)
                .get()
                .addOnSuccessListener { docs ->
                    if (!docs.isEmpty) {
                        val email = docs.documents[0].getString("email")
                        if (!email.isNullOrEmpty()) {
                            signInWithEmail(email, password)
                        } else {
                            showError("No email linked to this username")
                        }
                    } else {
                        showError("Username not found")
                    }
                }
                .addOnFailureListener { e ->
                    showError("Error: ${e.message}")

                 }
        }
    }

    private fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                signinButton.isEnabled = true
                signinButton.text = "Sign In"

                if (task.isSuccessful) {
                    handleSignInSuccess()
                } else {
                    val error = (task.exception as? FirebaseAuthException)?.errorCode ?: "UNKNOWN"
                    showError("Login failed: $error")
                }
            }
    }

    private fun showError(msg: String) {
        signinButton.isEnabled = true
        signinButton.text = "Sign In"
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun validateInputs(username: String, password: String): Boolean {
        var isValid = true
        if (TextUtils.isEmpty(username)) {
            usernameEditText.error = "Username is required"
            usernameEditText.requestFocus()
            isValid = false
        }
        if (TextUtils.isEmpty(password)) {
            passwordEditText.error = "Password is required"
            if (isValid) passwordEditText.requestFocus()
            isValid = false
        }
        return isValid
    }


    //  Authenticate Result Handlers
    private fun handleSignInSuccess() {
        Toast.makeText(this, "Sign in successful!", Toast.LENGTH_LONG).show()
        signinButton.isEnabled = true
        signinButton.text = "Sign In"
        navigateToHome()
    }

    private fun handleSignInError(errorMessage: String) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        signinButton.isEnabled = true
        signinButton.text = "Sign In"
    }

    private fun navigateToSignUp() {
        startActivity(Intent(this, SignUpActivity::class.java))
        finish()
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }


    //-------------------------------------------------------------------------
    //  Google Authentication
    private fun configureGoogleSignIn() {
        val webClientId = getString(R.string.default_web_client_id)
        Log.d(TAG, "Using web client ID: $webClientId")
        Log.d(TAG, "Package name: ${packageName}")

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        Log.d(TAG, "Google Sign-In client configured successfully")
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.e(TAG, "Google Sign-In failed: ${e.statusCode} - ${e.message}")
                val errorMessage = when (e.statusCode) {
                    10 -> "DEVELOPER_ERROR: Check SHA-1 fingerprint and OAuth configuration"
                    7 -> "NETWORK_ERROR: Check internet connection"
                    12501 -> "USER_CANCELLED: Sign-in was cancelled"
                    else -> "Google Sign-In failed: ${e.statusCode} - ${e.message}"
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == RC_GITHUB_AUTH) {
            if (resultCode == Activity.RESULT_OK) {
                val authCode = data?.getStringExtra(GitHubAuthActivity.EXTRA_AUTH_CODE)
                if (authCode != null) {
                    handleGitHubCallback(authCode)
                } else {
                    Toast.makeText(this, "GitHub authentication failed", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "GitHub authentication cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    handleGoogleSignInSuccess(user)
                } else {
                    Toast.makeText(this, "Authentication failed. Please try again.", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun handleGoogleSignInSuccess(user: FirebaseUser?) {
        user?.let {
            Toast.makeText(this, "Welcome ${it.displayName ?: "User"}!", Toast.LENGTH_LONG).show()
            navigateToHome()
        }
    }

    //-------------------------------------------------------------------------
    // GitHub Authentication
    private fun signInWithGitHub() {
        if (GITHUB_CLIENT_ID == "YOUR_GITHUB_CLIENT_ID") {
            Toast.makeText(this, "GitHub Client ID not configured.", Toast.LENGTH_LONG).show()
            return
        }
        
        val intent = Intent(this, GitHubAuthActivity::class.java)
        startActivityForResult(intent, RC_GITHUB_AUTH)
    }


    private fun handleGitHubCallback(code: String) {
        Log.d(TAG, "GitHub authentication code received: $code")
        
        // Show loading state
        Toast.makeText(this, "Authenticating with GitHub...", Toast.LENGTH_SHORT).show()
        
        // For now, we'll create a custom user session for GitHub users
        // In a real implementation, you'd exchange the code for an access token
        // and then create a custom Firebase token
        
        val githubEmail = "github_user_${System.currentTimeMillis()}@github.com"
        val githubPassword = "github_temp_password_${System.currentTimeMillis()}"
        
        // Create a temporary user account for GitHub authentication
        auth.createUserWithEmailAndPassword(githubEmail, githubPassword)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "GitHub user session created successfully")
                    val user = auth.currentUser
                    handleGitHubSignInSuccess(user, code)
                } else {
                    // If user already exists, try to sign in
                    auth.signInWithEmailAndPassword(githubEmail, githubPassword)
                        .addOnCompleteListener(this) { signInTask ->
                            if (signInTask.isSuccessful) {
                                Log.d(TAG, "GitHub user signed in successfully")
                                val user = auth.currentUser
                                handleGitHubSignInSuccess(user, code)
                            } else {
                                Log.e(TAG, "GitHub authentication failed", signInTask.exception)
                                Toast.makeText(this, "GitHub authentication failed: ${signInTask.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                }
            }
    }
    
    private fun handleGitHubSignInSuccess(user: FirebaseUser?, githubCode: String) {
        user?.let {
            val displayName = "GitHub User"
            val email = it.email ?: ""
            
            // Save user data to Firestore
            saveGitHubUserDataToFirestore(it.uid, displayName, email, githubCode)
            
            Toast.makeText(this, "Welcome! GitHub authentication successful", Toast.LENGTH_LONG).show()
            navigateToHome()
        }
    }
    
    private fun saveGitHubUserDataToFirestore(uid: String, displayName: String, email: String, githubCode: String) {
        val db = FirebaseFirestore.getInstance()
        val userData = hashMapOf(
            "uid" to uid,
            "name" to "GitHub",
            "surname" to "User",
            "username" to "github_user_${System.currentTimeMillis()}",
            "email" to email,
            "createdAt" to java.util.Date(),
            "isActive" to true,
            "signUpMethod" to "github",
            "githubCode" to githubCode
        )

        Log.d(TAG, "Saving GitHub user data to Firestore for UID: $uid")

        db.collection("users")
            .document(uid)
            .set(userData)
            .addOnSuccessListener {
                Log.d(TAG, "GitHub user data saved successfully to Firestore")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to save GitHub user data to Firestore", exception)

            }

    }
    //-------------------------------------------------------------------------
    // Biometric Authentication
    private fun onBiometricButtonClicked() {
        val prefs = getSharedPreferences(PREFS_FILE, MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_BIOMETRICS_ENABLED, false)
        val active = prefs.getBoolean(KEY_BIOMETRICS_ACTIVE, false)

        when {
            !enabled -> {

                AlertDialog.Builder(this)
                    .setTitle("Enable biometrics in Settings")
                    .setMessage("To use biometric sign in, first sign in with your account, then open Settings and enable Biometrics (toggle). After enabling, close and re-open the app for the feature to activate.")
                    .setPositiveButton("OK", null)
                    .show()
                return
            }

            enabled && !active -> {
                // Biometrics enabled in Settings but app hasn't been restarted yet
                AlertDialog.Builder(this)
                    .setTitle("Restart required")
                    .setMessage("Biometric sign-in will be available after you close and re-open the app. Please close and re-open the app, then try again.")
                    .setPositiveButton("OK", null)
                    .show()
                return
            }

            enabled && active -> {

                authenticateAndProceed()
            }
        }
    }

    private fun authenticateAndProceed() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                runOnUiThread {
                    handleBiometricSuccess()
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                runOnUiThread {
                    Toast.makeText(this@SignInActivity, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                runOnUiThread {
                    Toast.makeText(this@SignInActivity, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
        })

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock with biometrics")
            .setSubtitle("Use your fingerprint or face to access the app")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()

        // Launch prompt
        prompt.authenticate(info)
    }
    private fun handleBiometricSuccess() {
        Toast.makeText(this, "Biometric authentication successful ", Toast.LENGTH_SHORT).show()


        handleSignInSuccess()
    }

}


// --------------------------------------------<<< End of File >>>------------------------------------------