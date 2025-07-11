package com.example.pdftovoice

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class PdfToVoiceApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Initialize Firebase
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d("PdfToVoiceApplication", "Firebase initialized successfully")
            } else {
                Log.d("PdfToVoiceApplication", "Firebase already initialized")
            }
        } catch (e: Exception) {
            Log.e("PdfToVoiceApplication", "Error initializing Firebase: ${e.message}", e)
            // Don't crash the app, just log the error
        }
    }
}
