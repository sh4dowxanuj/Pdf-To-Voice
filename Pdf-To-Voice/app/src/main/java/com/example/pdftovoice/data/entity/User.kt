package com.example.pdftovoice.data.entity

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class User(
    @DocumentId
    val id: String = "",
    @PropertyName("email")
    val email: String = "",
    @PropertyName("username")
    val username: String = "",
    @PropertyName("password")
    val password: String = "",
    @PropertyName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @PropertyName("is_active")
    val isActive: Boolean = true
) {
    // No-argument constructor for Firebase
    constructor() : this("", "", "", "", 0L, true)
}
