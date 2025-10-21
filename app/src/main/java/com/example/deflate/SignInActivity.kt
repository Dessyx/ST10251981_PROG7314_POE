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
import com.example.deflate.security.CryptographyManager
import com.example.deflate.security.CiphertextWrapper



//-------------------------------------------------------------------------
// Sign In screen activity
class SignInActivity : AppCompatActivity() {


    companion object {
        private const val TAG = "SignInActivity"
        private const val RC_SIGN_IN = 9001
        private const val RC_GITHUB_AUTH = 9002
        private const val GITHUB_CLIENT_ID = "Ov23liwG3uaDjiDZJnR4"
        private const val GITHUB_REDIRECT_URI = "http://localhost:8080/github-callback"
        private const val CIPHERTEXT_PREFS_FILE = "biometric_creds_prefs"
        private const val CIPHERTEXT_PREF_KEY = "creds_ciphertext_key"
        private const val PREFS_MODE = Context.MODE_PRIVATE

        // Keystore key alias used by CryptographyManager
        private const val KEYSTORE_KEY_ALIAS = "biometric_aes_key_v1"

        // App-level flag key to know if credentials were saved
        private const val CREDS_SAVED_FLAG = "creds_saved_flag"
        private const val BIOMETRICS_ENABLED_FLAG = "biometrics_enabled_flag"

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

    private val cryptoManager: CryptographyManager = CryptographyManager()

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

        biometricSignInButton.isVisible = androidx.biometric.BiometricManager.from(this).canAuthenticate(
            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS

        val prefs = getSharedPreferences(CIPHERTEXT_PREFS_FILE, PREFS_MODE)
        if (!prefs.getBoolean("biometric_first_hint_shown", false) && biometricSignInButton.isVisible) {
            Toast.makeText(this, "Tip: enable biometric sign-in in Settings to use fingerprint sign-in.", Toast.LENGTH_LONG).show()
            prefs.edit().putBoolean("biometric_first_hint_shown", true).apply()
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
        signinButton.isEnabled = false
        signinButton.text = "Signing in..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                signinButton.isEnabled = true
                signinButton.text = "Sign In"

                if (task.isSuccessful) {
                    // Normal sign-in succeeded.
                    // If the user has enabled biometrics (consent) but we haven't saved creds yet,
                    // ask whether to save encrypted credentials for biometric sign-in.
                    val prefs = getSharedPreferences(CIPHERTEXT_PREFS_FILE, PREFS_MODE)
                    val biometricsEnabled = prefs.getBoolean(BIOMETRICS_ENABLED_FLAG, false)
                    val credsSaved = prefs.getBoolean(CREDS_SAVED_FLAG, false)

                    if (biometricsEnabled && !credsSaved) {
                        // Ask for permission to save credentials securely
                        AlertDialog.Builder(this)
                            .setTitle("Save login for biometric sign-in?")
                            .setMessage("Would you like to save your login securely so you can sign in with your fingerprint/face next time?")
                            .setPositiveButton("Yes") { _, _ ->
                                try {
                                    saveCredentialsForBiometric(email, password)
                                    prefs.edit().putBoolean(CREDS_SAVED_FLAG, true).apply()
                                    Toast.makeText(this, "Credentials saved for biometric sign-in", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Log.e(TAG, "saveCredentialsForBiometric failed: ${e.message}", e)
                                    Toast.makeText(this, "Failed to save credentials: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    handleSignInSuccess()
                                }
                            }
                            .setNegativeButton("No") { _, _ ->
                                // proceed without saving
                                handleSignInSuccess()
                            }
                            .setCancelable(false)
                            .show()
                    } else {
                        handleSignInSuccess()
                    }
                } else {
                    val error = (task.exception as? FirebaseAuthException)?.errorCode ?: task.exception?.localizedMessage ?: "UNKNOWN"
                    showError("Login failed: $error")
                }
            }
    }

    private fun onBiometricButtonClicked() {
        // Check whether device supports biometrics
        val bm = androidx.biometric.BiometricManager.from(this)
        val canAuth = bm.canAuthenticate(
            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        if (canAuth != androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Biometrics not available on this device", Toast.LENGTH_LONG).show()
            return
        }

        val prefs = getSharedPreferences(CIPHERTEXT_PREFS_FILE, PREFS_MODE)
        val biometricsEnabled = prefs.getBoolean(BIOMETRICS_ENABLED_FLAG, false)
        val credsSaved = prefs.getBoolean(CREDS_SAVED_FLAG, false)

        when {
            !biometricsEnabled -> {
                // Not enabled in Settings — open Settings to let the user enable
                AlertDialog.Builder(this)
                    .setTitle("Enable biometrics in Settings")
                    .setMessage("To use biometric sign in, first enable the feature in Settings. Open Settings now?")
                    .setPositiveButton("Open Settings") { _, _ ->
                        startActivity(Intent(this, SettingsActivity::class.java))
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            biometricsEnabled && !credsSaved -> {
                // Enabled but no saved credentials yet — instruct to sign in normally once
                AlertDialog.Builder(this)
                    .setTitle("First sign-in required")
                    .setMessage("You enabled biometric sign-in, but we need you to sign in normally once so credentials can be stored securely. After you sign in and permit saving credentials, biometric sign-in will work.")
                    .setPositiveButton("Okay", null)
                    .show()
            }

            biometricsEnabled && credsSaved -> {

                startBiometricSignIn()
            }
        }
    }
    private fun startBiometricSignIn() {

        val wrapper: CiphertextWrapper? = cryptoManager.getCiphertextWrapperFromSharedPrefs(
            context = this,
            filename = CIPHERTEXT_PREFS_FILE,
            mode = PREFS_MODE,
            prefKey = CIPHERTEXT_PREF_KEY
        )

        if (wrapper == null) {
            Toast.makeText(this, "No saved credentials. Please sign in normally once.", Toast.LENGTH_LONG).show()
            return
        }

        // Initialize cipher for decryption with IV (this will prepare a Cipher bound to the Keystore key)
        val cipher = try {
            cryptoManager.getInitializedCipherForDecryption(KEYSTORE_KEY_ALIAS, wrapper.initializationVector)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cipher for decryption: ${e.message}", e)
            Toast.makeText(this, "Unable to initialize crypto for biometric sign-in.", Toast.LENGTH_LONG).show()
            return
        }

        // Build BiometricPrompt with CryptoObject(cipher)
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // The cipher inside result is unlocked and can be used for decrypt
                val authCipher = result.cryptoObject?.cipher
                if (authCipher == null) {
                    runOnUiThread {
                        Toast.makeText(this@SignInActivity, "Biometric succeeded but crypto is unavailable", Toast.LENGTH_LONG).show()
                    }
                    return
                }

                try {
                    val plaintext = cryptoManager.decryptData(wrapper.cipherText, authCipher)
                    val parts = plaintext.split(":", limit = 2)
                    if (parts.size == 2) {
                        val email = parts[0]
                        val password = parts[1]
                        // Reuse the same sign-in flow
                        runOnUiThread {
                            signInWithEmail(email, password)
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@SignInActivity, "Decrypted data invalid", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Decryption failed after biometric: ${e.message}", e)
                    runOnUiThread {
                        Toast.makeText(this@SignInActivity, "Failed to decrypt credentials: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                runOnUiThread { Toast.makeText(this@SignInActivity, "Authentication error: $errString", Toast.LENGTH_SHORT).show() }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                runOnUiThread { Toast.makeText(this@SignInActivity, "Authentication failed", Toast.LENGTH_SHORT).show() }
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock with biometrics")
            .setSubtitle("Use your fingerprint or face to sign in")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()

        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    private fun saveCredentialsForBiometric(email: String, password: String) {
        // Prepare plaintext
        val plaintext = "$email:$password"

        // Get cipher initialized for encryption — this creates an IV
        val cipher = cryptoManager.getInitializedCipherForEncryption(KEYSTORE_KEY_ALIAS)

        // Encrypt plaintext -> CiphertextWrapper(ciphertext, iv)
        val wrapper = cryptoManager.encryptData(plaintext, cipher)

        // Persist wrapper using CryptographyManager helper (Gson JSON)
        cryptoManager.persistCiphertextWrapperToSharedPrefs(
            ciphertextWrapper = wrapper,
            context = this,
            filename = CIPHERTEXT_PREFS_FILE,
            mode = PREFS_MODE,
            prefKey = CIPHERTEXT_PREF_KEY
        )

        // Mark creds saved flag (so biometric button will use saved creds)
        val prefs = getSharedPreferences(CIPHERTEXT_PREFS_FILE, PREFS_MODE)
        prefs.edit().putBoolean(CREDS_SAVED_FLAG, true).apply()
    }
    private fun clearSavedCredentialsAndKey() {
        // Remove stored wrapper
        val prefs = getSharedPreferences(CIPHERTEXT_PREFS_FILE, PREFS_MODE)
        prefs.edit().remove(CIPHERTEXT_PREF_KEY).putBoolean(CREDS_SAVED_FLAG, false).apply()

        // Optionally delete the KeyStore key (so previous ciphertext cannot be decrypted)
        try {
            val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (keyStore.containsAlias(KEYSTORE_KEY_ALIAS)) {
                keyStore.deleteEntry(KEYSTORE_KEY_ALIAS)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete keystore key: ${e.message}", e)
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
                // Continue anyway - user is authenticated even if data save fails
            }

    }

}


// --------------------------------------------<<< End of File >>>------------------------------------------