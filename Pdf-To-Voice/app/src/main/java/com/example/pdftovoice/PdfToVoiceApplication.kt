package com.example.pdftovoice

import android.app.Application
import com.google.firebase.FirebaseApp

class PdfToVoiceApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
    }
}
