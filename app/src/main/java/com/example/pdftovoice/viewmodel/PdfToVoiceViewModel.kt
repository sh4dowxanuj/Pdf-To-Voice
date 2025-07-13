package com.example.pdftovoice.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdftovoice.pdf.PdfProcessor
import com.example.pdftovoice.pdf.PdfTypeDetector
import com.example.pdftovoice.tts.TtsManager
import com.example.pdftovoice.tts.Language
import com.example.pdftovoice.utils.FileUtils
import com.example.pdftovoice.utils.PerformanceUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

data class PdfFileInfo(
    val uri: Uri,
    val name: String,
    val size: Long?,
    val formattedSize: String,
    val mimeType: String? = null,
    val extension: String? = null,
    val analysisInfo: PdfTypeDetector.PdfInfo? = null
)

data class PdfToVoiceState(
    val isLoading: Boolean = false,
    val selectedPdfFile: PdfFileInfo? = null,
    val extractedText: String = "",
    val errorMessage: String? = null,
    val isTtsInitialized: Boolean = false,
    val currentlyReadingSegment: String = "",
    val isAnalyzing: Boolean = false,
    val extractionMethod: PdfProcessor.ExtractionMethod? = null,
    val processingStatus: String? = null
)

class PdfToVoiceViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context = application
    private val pdfProcessor = PdfProcessor(application)
    private val pdfTypeDetector = PdfTypeDetector(application)
    private val ttsManager = TtsManager(application)
    
    private val _state = MutableStateFlow(PdfToVoiceState())
    val state: StateFlow<PdfToVoiceState> = _state.asStateFlow()
    
    // TTS State flows
    val isPlaying = ttsManager.isPlaying
    val isPaused = ttsManager.isPaused
    val currentPosition = ttsManager.currentPosition
    val speed = ttsManager.speed
    val pitch = ttsManager.pitch
    
    // Language state from TTS Manager
    val currentLanguage: StateFlow<Language> = ttsManager.currentLanguage
    val availableLanguages: StateFlow<List<Language>> = ttsManager.availableLanguages
    
    // Combined state for UI with currently reading segment
    val combinedState = combine(
        _state,
        ttsManager.currentSegment
    ) { state, currentSegment ->
        state.copy(
            currentlyReadingSegment = currentSegment
        )
    }
    
    // Job management for cancellable operations
    private var currentPdfJob: Job? = null
    private var currentAnalysisJob: Job? = null
    
    // Enhanced LRU cache with automatic cleanup
    private val fileInfoCache = object : LinkedHashMap<Uri, PdfFileInfo>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Uri, PdfFileInfo>?): Boolean {
            return size > MAX_CACHE_SIZE
        }
    }
    
    companion object {
        private const val MAX_CACHE_SIZE = 30 // Increased cache size
        private const val ERROR_DISPLAY_DURATION = 5000L
        private const val TAG = "PdfToVoiceViewModel"
        private const val LARGE_FILE_THRESHOLD = 50 * 1024 * 1024 // 50MB
    }
    
    init {
        initializeTts()
    }
    
    private fun initializeTts() {
        ttsManager.initialize { success ->
            _state.value = _state.value.copy(isTtsInitialized = success)
            if (!success) {
                _state.value = _state.value.copy(
                    errorMessage = "Failed to initialize Text-to-Speech engine. Please check if TTS is installed on your device."
                )
            }
        }
    }
    
    fun selectPdf(uri: Uri) {
        Log.d(TAG, "Selecting PDF: $uri")
        
        // Performance monitoring
        PerformanceUtils.logMemoryUsage(context, TAG)
        
        // Cancel any ongoing operations
        currentPdfJob?.cancel()
        currentAnalysisJob?.cancel()
        
        // First validate the file
        val validationResult = FileUtils.validatePdfFile(context, uri)
        if (!validationResult.isValid) {
            _state.value = _state.value.copy(
                errorMessage = validationResult.message,
                isLoading = false,
                isAnalyzing = false
            )
            return
        }
        
        // Check memory before processing large files
        if (PerformanceUtils.isMemoryUsageHigh(context, 75)) {
            Log.w(TAG, "High memory usage detected, performing garbage collection")
            PerformanceUtils.performGarbageCollection(context, TAG)
        }
        
        // Show validation message if it's a warning
        if (validationResult.message.contains("large", ignoreCase = true)) {
            _state.value = _state.value.copy(
                errorMessage = validationResult.message
            )
        }
        
        // Get basic file info
        val fileName = FileUtils.getFileName(context, uri) ?: "Unknown PDF"
        val fileSize = FileUtils.getFileSize(context, uri)
        val formattedSize = fileSize?.let { FileUtils.formatFileSize(it) } ?: "Unknown size"
        val mimeType = FileUtils.getMimeType(context, uri)
        val extension = FileUtils.getFileExtension(context, uri)
        
        val pdfFileInfo = PdfFileInfo(
            uri = uri,
            name = fileName,
            size = fileSize,
            formattedSize = formattedSize,
            mimeType = mimeType,
            extension = extension
        )
        
        _state.value = _state.value.copy(
            selectedPdfFile = pdfFileInfo,
            isAnalyzing = true,
            isLoading = false,
            errorMessage = null,
            extractedText = "",
            extractionMethod = null,
            processingStatus = "Analyzing PDF..."
        )
        
        // Start PDF analysis
        analyzePdfFile(uri, pdfFileInfo)
    }
    
    private fun analyzePdfFile(uri: Uri, pdfFileInfo: PdfFileInfo) {
        currentAnalysisJob = viewModelScope.launch {
            try {
                Log.d(TAG, "Starting PDF analysis")
                _state.value = _state.value.copy(
                    processingStatus = "Analyzing PDF structure and content..."
                )
                
                val analysisInfo = pdfTypeDetector.analyzePdf(uri)
                Log.d(TAG, "PDF analysis completed: $analysisInfo")
                
                val updatedFileInfo = pdfFileInfo.copy(analysisInfo = analysisInfo)
                
                // Update cache
                if (fileInfoCache.size < MAX_CACHE_SIZE) {
                    fileInfoCache[uri] = updatedFileInfo
                }
                
                _state.value = _state.value.copy(
                    selectedPdfFile = updatedFileInfo,
                    isAnalyzing = false,
                    processingStatus = null
                )
                
                // Show analysis results to user
                val statusMessage = buildString {
                    append("PDF Analysis Complete!\n")
                    append("Pages: ${analysisInfo.pageCount}\n")
                    append("Text Content: ${if (analysisInfo.hasSelectableText) "Yes" else "No"}\n")
                    append("Image Content: ${if (analysisInfo.isImagePdf) "Yes" else "No"}\n")
                    append("Supported Methods: ${analysisInfo.supportedMethods.size}")
                    
                    if (analysisInfo.isPasswordProtected) {
                        append("\n⚠️ Password protected")
                    }
                    if (analysisInfo.isEncrypted) {
                        append("\n⚠️ Encrypted")
                    }
                }
                
                _state.value = _state.value.copy(
                    errorMessage = statusMessage
                )
                
                // Auto-start extraction after analysis
                if (analysisInfo.isValid) {
                    extractTextFromPdf(uri, updatedFileInfo)
                } else {
                    _state.value = _state.value.copy(
                        errorMessage = analysisInfo.errorMessage ?: "Invalid PDF file"
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "PDF analysis failed", e)
                _state.value = _state.value.copy(
                    isAnalyzing = false,
                    errorMessage = "Failed to analyze PDF: ${e.message}",
                    processingStatus = null
                )
            }
        }
    }
    
    private fun extractTextFromPdf(uri: Uri, pdfFileInfo: PdfFileInfo) {
        currentPdfJob = viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    isLoading = true,
                    processingStatus = "Extracting text using best available method..."
                )
                
                Log.d(TAG, "Starting text extraction")
                
                val result = pdfProcessor.extractTextWithDetails(uri).getOrThrow()
                
                Log.d(TAG, "Text extraction completed using ${result.method}")
                
                val statusMessage = buildString {
                    if (result.method == PdfProcessor.ExtractionMethod.FALLBACK_SAMPLE) {
                        append("⚠️ Text extraction failed\n")
                        append("Unable to extract readable text from this PDF.\n")
                        append("Please try a different PDF file.")
                    } else {
                        append("✅ Extraction Complete!\n")
                        append("Method: ${result.method.name.replace("_", " ")}\n")
                        append("Text Length: ${result.text.length} characters\n")
                        append("Pages Processed: ${result.pageCount}")
                        
                        if (result.hasImages) {
                            append("\n📷 Contains images")
                        }
                    }
                }
                
                _state.value = _state.value.copy(
                    extractedText = result.text,
                    extractionMethod = result.method,
                    isLoading = false,
                    processingStatus = null,
                    errorMessage = if (result.method == PdfProcessor.ExtractionMethod.FALLBACK_SAMPLE) {
                        statusMessage
                    } else if (result.text.length > 50000) {
                        "Large document extracted (${result.text.length} characters). $statusMessage"
                    } else {
                        statusMessage
                    }
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Text extraction failed", e)
                _state.value = _state.value.copy(
                    errorMessage = when {
                        e.message?.contains("password", ignoreCase = true) == true -> 
                            "This PDF is password protected and cannot be read."
                        e.message?.contains("encrypt", ignoreCase = true) == true -> 
                            "This PDF is encrypted and cannot be read."
                        e.message?.contains("corrupted", ignoreCase = true) == true -> 
                            "This PDF file appears to be corrupted."
                        else -> "Failed to extract text from PDF: ${e.message}"
                    },
                    isLoading = false,
                    processingStatus = null
                )
            }
        }
    }
    
    fun playText() {
        val text = _state.value.extractedText
        Log.d(TAG, "Starting TTS playback for ${text.length} characters")
        
        if (text.isNotBlank() && _state.value.isTtsInitialized) {
            if (text.length > 10000) {
                // For very long texts, show a warning but still allow playing
                _state.value = _state.value.copy(
                    errorMessage = "Starting playback of long document. You can use pause and stop controls."
                )
            }
            ttsManager.speak(text)
        } else if (!_state.value.isTtsInitialized) {
            _state.value = _state.value.copy(
                errorMessage = "Text-to-Speech is not initialized. Please restart the app."
            )
        }
    }
    
    fun pauseReading() {
        ttsManager.pause()
    }
    
    fun resumeReading() {
        ttsManager.resume()
    }
    
    fun stopReading() {
        ttsManager.stop()
    }
    
    fun setSpeed(speed: Float) {
        ttsManager.setSpeed(speed)
    }
    
    fun setPitch(pitch: Float) {
        ttsManager.setPitch(pitch)
    }
    
    fun setLanguage(language: Language) {
        ttsManager.setLanguage(language)
    }
    
    fun clearPdf() {
        // Cancel any ongoing operations
        currentPdfJob?.cancel()
        currentAnalysisJob?.cancel()
        
        // Stop any ongoing TTS
        ttsManager.stop()
        
        _state.value = PdfToVoiceState(isTtsInitialized = _state.value.isTtsInitialized)
    }
    
    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
    
    fun retryExtraction() {
        val currentFile = _state.value.selectedPdfFile
        if (currentFile != null) {
            Log.d(TAG, "Retrying text extraction")
            extractTextFromPdf(currentFile.uri, currentFile)
        }
    }
    
    fun getExtractionMethodInfo(): String? {
        return _state.value.extractionMethod?.let { method ->
            when (method) {
                PdfProcessor.ExtractionMethod.PDFBOX_ANDROID -> 
                    "Text extracted using PDFBox - advanced parsing for all PDF documents"
                PdfProcessor.ExtractionMethod.ANDROID_RENDERER_OCR -> 
                    "Text extracted using OCR - machine learning recognition for scanned documents"
                PdfProcessor.ExtractionMethod.HYBRID -> 
                    "Text extracted using hybrid approach - PDFBox and OCR combined"
                PdfProcessor.ExtractionMethod.FALLBACK_SAMPLE -> 
                    "Using demonstration content - original PDF may require special handling"
            }
        }
    }
    
    fun showMethodInfo() {
        val methodInfo = getExtractionMethodInfo()
        if (methodInfo != null) {
            _state.value = _state.value.copy(
                errorMessage = "ℹ️ $methodInfo"
            )
        }
    }
    
    // Media player-style methods for FullScreenReaderScreen
    fun togglePlayPause() {
        if (isPlaying()) {
            pauseReading()
        } else if (isPaused()) {
            resumeReading()
        } else {
            playText()
        }
    }
    
    fun isPlaying(): Boolean {
        return ttsManager.isPlaying.value && !ttsManager.isPaused.value
    }
    
    fun isPaused(): Boolean {
        return ttsManager.isPaused.value
    }
    
    fun getSpeed(): Float {
        return ttsManager.speed.value
    }
    
    fun getPitch(): Float {
        return ttsManager.pitch.value
    }
    
    fun setExtractedText(text: String) {
        _state.value = _state.value.copy(
            extractedText = text,
            isLoading = false,
            errorMessage = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        // Cancel all ongoing operations
        currentPdfJob?.cancel()
        currentAnalysisJob?.cancel()
        
        // Clean up TTS resources
        ttsManager.destroy()
        
        Log.d(TAG, "ViewModel cleared and resources cleaned up")
    }
}
