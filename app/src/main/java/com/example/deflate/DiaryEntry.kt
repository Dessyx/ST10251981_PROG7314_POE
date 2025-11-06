package com.example.deflate

import com.google.firebase.firestore.PropertyName

data class DiaryEntry(
    @PropertyName("userId")
    val userId: String = "",
    
    @PropertyName("username")
    val username: String = "",
    
    @PropertyName("text")
    val text: String = "",
    
    @PropertyName("mood")
    val mood: String = "",
    
    @PropertyName("datePretty")
    val datePretty: String = "",
    
    @PropertyName("timestamp")
    val timestamp: Long = 0L
)

