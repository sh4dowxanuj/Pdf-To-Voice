package com.example.pdftovoice.test

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseTestHelper {
    
    private val firestore = FirebaseFirestore.getInstance()
    
    suspend fun testConnection(): Boolean {
        return try {
            // Try to write a test document
            val testData = mapOf(
                "test" to "Firebase connection successful!",
                "timestamp" to System.currentTimeMillis()
            )
            
            firestore.collection("test")
                .add(testData)
                .await()
            
            Log.d("FirebaseTest", "✅ Firebase connection successful!")
            true
        } catch (e: Exception) {
            Log.e("FirebaseTest", "❌ Firebase connection failed: ${e.message}")
            false
        }
    }
    
    suspend fun testUserRegistration(): Boolean {
        return try {
            val testUser = mapOf(
                "email" to "test@example.com",
                "username" to "testuser",
                "password" to "hashed_password",
                "created_at" to System.currentTimeMillis(),
                "is_active" to true
            )
            
            firestore.collection("users")
                .add(testUser)
                .await()
            
            Log.d("FirebaseTest", "✅ User registration test successful!")
            true
        } catch (e: Exception) {
            Log.e("FirebaseTest", "❌ User registration test failed: ${e.message}")
            false
        }
    }
}
