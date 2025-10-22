package com.example.deflate
import androidx.appcompat.app.AppCompatActivity

object Biometrics {
    fun isAvailable(activity: AppCompatActivity): Boolean {
        val bm = androidx.biometric.BiometricManager.from(activity)
        val res = bm.canAuthenticate(
            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        return res == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
    }
}