package com.example.pdftovoice.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pdftovoice.ui.screens.FullScreenReaderScreen
import com.example.pdftovoice.ui.theme.PdfToVoiceTheme
import com.example.pdftovoice.viewmodel.PdfToVoiceViewModel

/**
 * Full-screen immersive reading activity that combines:
 * - Media player functionality
 * - Text highlighting and synchronization
 * - Immersive reading experience
 */
class FullScreenReaderActivity : ComponentActivity() {
    
    companion object {
        private const val EXTRA_PDF_URI = "pdf_uri"
        private const val EXTRA_PDF_NAME = "pdf_name"
        private const val EXTRA_EXTRACTED_TEXT = "extracted_text"
        
        fun createIntent(
            context: Context,
            pdfUri: String? = null,
            pdfName: String? = null,
            extractedText: String? = null
        ): Intent {
            return Intent(context, FullScreenReaderActivity::class.java).apply {
                pdfUri?.let { putExtra(EXTRA_PDF_URI, it) }
                pdfName?.let { putExtra(EXTRA_PDF_NAME, it) }
                extractedText?.let { putExtra(EXTRA_EXTRACTED_TEXT, it) }
            }
        }
    }
    
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable immersive full-screen mode
        setupImmersiveMode()
        
        // Get intent extras
        val pdfUri = intent.getStringExtra(EXTRA_PDF_URI)
        val pdfName = intent.getStringExtra(EXTRA_PDF_NAME)
        val extractedText = intent.getStringExtra(EXTRA_EXTRACTED_TEXT)
        
        setContent {
            PdfToVoiceTheme {
                val windowSizeClass = calculateWindowSizeClass(this)
                val viewModel: PdfToVoiceViewModel = viewModel()
                
                // Handle back navigation
                BackHandler {
                    finish()
                }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FullScreenReaderScreen(
                        windowSizeClass = windowSizeClass,
                        viewModel = viewModel,
                        initialPdfUri = pdfUri,
                        initialPdfName = pdfName,
                        initialExtractedText = extractedText,
                        onClose = { finish() }
                    )
                }
            }
        }
    }
    
    private fun setupImmersiveMode() {
        // Keep screen on during reading
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Setup immersive mode with edge-to-edge content
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            // Hide system bars
            hide(WindowInsetsCompat.Type.systemBars())
            // Set behavior for when system bars are shown/hidden
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Re-enable immersive mode when returning to activity
        setupImmersiveMode()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clear the keep screen on flag
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
