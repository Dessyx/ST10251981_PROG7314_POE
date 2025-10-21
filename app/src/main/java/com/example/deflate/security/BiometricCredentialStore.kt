package com.example.deflate.security



import android.content.Context
import android.util.Log


object BiometricCredentialStore {
    private const val TAG = "BiometricCredentialStore"

    // Keep these names consistent with SignInActivity / SettingsActivity constants
    // or centralize in a single constants file so they cannot drift.
    private const val CIPHERTEXT_PREFS_FILE = "biometric_creds_prefs"
    private const val CIPHERTEXT_PREF_KEY = "creds_ciphertext_key"
    private const val CREDS_SAVED_FLAG = "creds_saved_flag"
    private const val PREFS_MODE = Context.MODE_PRIVATE

    /**
     * Clears stored ciphertext wrapper JSON and flags from SharedPreferences,
     * and attempts to delete the AndroidKeyStore key with alias [keyAlias].
     *
     * @param context The context used to access SharedPreferences and KeyStore
     * @param keyAlias The alias of the AndroidKeyStore key that encrypted the credentials.
     *                 If this alias does not exist, the function simply removes prefs.
     *
     * @throws Exception rethrows any exception encountered when deleting the KeyStore entry
     *                   so the calling activity can handle or inform the user.
     */
    fun clearSavedCredentialsAndKey(context: Context, keyAlias: String) {
        // Remove stored wrapper JSON and flag
        try {
            val prefs = context.getSharedPreferences(CIPHERTEXT_PREFS_FILE, PREFS_MODE)
            prefs.edit()
                .remove(CIPHERTEXT_PREF_KEY)
                .putBoolean(CREDS_SAVED_FLAG, false)
                .apply()
        } catch (e: Exception) {
            // Log but continue to attempt keystore cleanup
            Log.e(TAG, "Failed to remove stored ciphertext from prefs: ${e.message}", e)
        }

        //  Attempt to delete the key from AndroidKeyStore
        try {
            val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            if (keyStore.containsAlias(keyAlias)) {
                keyStore.deleteEntry(keyAlias)
                Log.d(TAG, "Deleted keystore key alias: $keyAlias")
            } else {
                Log.d(TAG, "Keystore alias not found (nothing to delete): $keyAlias")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete keystore key alias '$keyAlias': ${e.message}", e)
            // Rethrow to inform callers (SettingsActivity handles/reports errors to user)
            throw e
        }
    }
}
