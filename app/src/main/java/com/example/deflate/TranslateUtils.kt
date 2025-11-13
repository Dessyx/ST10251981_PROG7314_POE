package com.example.deflate

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object TranslateUtils {

    /**
     * Translate text from English to the target language code (e.g. "af").
     * Returns original text on any failure (safe fallback).
     */
    suspend fun translateIfNeeded(context: Context, text: String, targetLang: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val target = when (targetLang.lowercase()) {
                    "af", "afrikaans" -> TranslateLanguage.AFRIKAANS
                    "en", "english" -> TranslateLanguage.ENGLISH
                    else -> TranslateLanguage.AFRIKAANS
                }

                // We assume source is English; adjust if required
                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.ENGLISH)
                    .setTargetLanguage(target)
                    .build()

                val translator: Translator = Translation.getClient(options)

                // Download conditions - require wifi (change if desired)
                val conditions = DownloadConditions.Builder()
                    .requireWifi()
                    .build()

                val downloaded = try {
                    translator.downloadModelIfNeeded(conditions).await()
                    true
                } catch (e: Exception) {
                    false
                }

                if (!downloaded) {
                    translator.close()
                    return@withContext text
                }

                // Translate and return result (or fallback)
                try {
                    val result = translator.translate(text).await()
                    result
                } catch (e: Exception) {
                    text
                } finally {
                    translator.close()
                }
            } catch (ex: Exception) {
                text
            }
        }
    }
}

/**
 * Simple await extension for Tasks so we can use coroutines.
 */
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { e -> cont.resumeWithException(e) }
    addOnCanceledListener { cont.cancel() }
}

