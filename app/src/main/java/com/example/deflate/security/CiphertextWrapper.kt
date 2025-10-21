package com.example.deflate.security

data class CiphertextWrapper(
    val cipherText: ByteArray,
    val initializationVector: ByteArray
)