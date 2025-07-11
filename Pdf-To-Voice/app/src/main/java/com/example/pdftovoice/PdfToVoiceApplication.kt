package com.example.pdftovoice

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class PdfToVoiceApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this)
            Log.d("PdfToVoiceApplication", "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e("PdfToVoiceApplication", "Error initializing Firebase: ${e.message}", e)
        }
    }
}
