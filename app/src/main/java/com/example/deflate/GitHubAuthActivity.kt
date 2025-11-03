package com.example.deflate

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class GitHubAuthActivity : BaseActivity() {

    companion object {
        private const val TAG = "GitHubAuthActivity"
        private const val GITHUB_CLIENT_ID = "Ov23liwG3uaDjiDZJnR4"
        private const val GITHUB_REDIRECT_URI = "http://localhost:8080/github-callback"
        const val EXTRA_AUTH_CODE = "auth_code"
    }

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_github_auth)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.github_auth_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupWebView()
        setupCloseButton()
        loadGitHubAuth()
    }

    private fun setupWebView() {
        webView = findViewById(R.id.github_webview)
        
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            setGeolocationEnabled(true)
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            databaseEnabled = true
            setSupportMultipleWindows(false)
            userAgentString = userAgentString + " DeflateApp/1.0"
        }

        // Enable proper focus and keyboard handling
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.requestFocus()

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                Log.d(TAG, "Loading URL: $url")
                
                if (url?.startsWith(GITHUB_REDIRECT_URI) == true) {
                    val uri = Uri.parse(url)
                    val code = uri.getQueryParameter("code")
                    
                    if (code != null) {
                        Log.d(TAG, "GitHub auth code received: $code")
                        handleAuthSuccess(code)
                        return true
                    }
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page finished loading: $url")
                
                // Ensure keyboard support is enabled
                view?.evaluateJavascript("""
                    document.body.style.zoom='1.0';
                    // Force focus on input fields when tapped
                    document.addEventListener('DOMContentLoaded', function() {
                        var inputs = document.querySelectorAll('input[type="text"], input[type="email"], input[type="password"]');
                        inputs.forEach(function(input) {
                            input.addEventListener('click', function() {
                                this.focus();
                                this.click();
                            });
                            input.addEventListener('focus', function() {
                                this.click();
                            });
                        });
                    });
                """.trimIndent(), null)
            }

            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                Log.e(TAG, "WebView error: $description for URL: $failingUrl")
                Toast.makeText(this@GitHubAuthActivity, "Network error: $description", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupCloseButton() {
        val closeButton = findViewById<ImageView>(R.id.btn_close)
        closeButton.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private fun loadGitHubAuth() {
        val githubAuthUrl = "https://github.com/login/oauth/authorize" +
                "?client_id=$GITHUB_CLIENT_ID&redirect_uri=$GITHUB_REDIRECT_URI&scope=user:email"

        Log.d(TAG, "Loading GitHub auth URL: $githubAuthUrl")
        webView.loadUrl(githubAuthUrl)
    }

    private fun handleAuthSuccess(code: String) {
        Log.d(TAG, "Authentication successful with code: $code")
        
        // Show success message
        Toast.makeText(this, "GitHub authentication successful!", Toast.LENGTH_SHORT).show()
        
        val resultIntent = Intent().apply {
            putExtra(EXTRA_AUTH_CODE, code)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            setResult(Activity.RESULT_CANCELED)
            super.onBackPressed()
        }
    }
}
