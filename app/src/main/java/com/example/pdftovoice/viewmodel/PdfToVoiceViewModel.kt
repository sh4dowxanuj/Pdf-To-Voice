package com.example.pdftovoice.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdftovoice.pdf.PdfProcessor
import com.example.pdftovoice.tts.TtsManager
import com.example.pdftovoice.utils.FileUtils
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
    val formattedSize: String
)

data class PdfToVoiceState(
    val isLoading: Boolean = false,
    val selectedPdfFile: PdfFileInfo? = null,
    val extractedText: String = "",
    val errorMessage: String? = null,
    val isTtsInitialized: Boolean = false,
    val currentlyReadingSegment: String = ""
)

class PdfToVoiceViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context = application
    private val pdfProcessor = PdfProcessor(application)
    private val ttsManager = TtsManager(application)
    
    private val _state = MutableStateFlow(PdfToVoiceState())
    val state: StateFlow<PdfToVoiceState> = _state.asStateFlow()
    
    // TTS State flows
    val isPlaying = ttsManager.isPlaying
    val isPaused = ttsManager.isPaused
    val currentPosition = ttsManager.currentPosition
    val speed = ttsManager.speed
    val pitch = ttsManager.pitch
    
    // Combined state for UI with currently reading segment
    val combinedState = combine(
        _state,
        isPlaying,
        isPaused
    ) { state, playing, paused ->
        state.copy(
            currentlyReadingSegment = if (playing || paused) {
                ttsManager.getCurrentSegment() ?: ""
            } else ""
        )
    }
    
    // Job management for cancellable operations
    private var currentPdfJob: Job? = null
    private val fileInfoCache = ConcurrentHashMap<Uri, PdfFileInfo>()
    
    companion object {
        private const val MAX_CACHE_SIZE = 20
        private const val ERROR_DISPLAY_DURATION = 5000L
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
        // First validate the file
        if (!FileUtils.isPdfFile(context, uri)) {
            _state.value = _state.value.copy(
                errorMessage = "Selected file is not a valid PDF document."
            )
            return
        }
        
        val fileName = FileUtils.getFileName(context, uri) ?: "Unknown PDF"
        val fileSize = FileUtils.getFileSize(context, uri)
        val formattedSize = fileSize?.let { FileUtils.formatFileSize(it) } ?: "Unknown size"
        
        val pdfFileInfo = PdfFileInfo(
            uri = uri,
            name = fileName,
            size = fileSize,
            formattedSize = formattedSize
        )
        
        // Check cache first
        fileInfoCache[uri]?.let {
            _state.value = _state.value.copy(
                selectedPdfFile = it,
                isLoading = true,
                errorMessage = null,
                extractedText = ""
            )
            extractTextFromPdf(uri, it)
        } ?: run {
            _state.value = _state.value.copy(
                selectedPdfFile = pdfFileInfo,
                isLoading = true,
                errorMessage = null,
                extractedText = ""
            )
            
            // Cancel any ongoing PDF processing job
            currentPdfJob?.cancel()
            
            // Launch a new job for PDF text extraction
            currentPdfJob = viewModelScope.launch {
                pdfProcessor.extractTextFromPdf(uri)
                    .onSuccess { text ->
                        val newState = if (text.length > 50000) {
                            // Show warning for very long texts
                            _state.value.copy(
                                extractedText = text,
                                isLoading = false,
                                errorMessage = "This is a large document (${text.length} characters). Reading may take some time."
                            )
                        } else {
                            _state.value.copy(
                                extractedText = text,
                                isLoading = false
                            )
                        }
                        
                        // Update cache
                        if (fileInfoCache.size < MAX_CACHE_SIZE) {
                            fileInfoCache[uri] = pdfFileInfo
                        }
                        
                        _state.value = newState
                    }
                    .onFailure { error ->
                        _state.value = _state.value.copy(
                            errorMessage = when {
                                error.message?.contains("password", ignoreCase = true) == true -> 
                                    "This PDF is password protected and cannot be read."
                                error.message?.contains("encrypt", ignoreCase = true) == true -> 
                                    "This PDF is encrypted and cannot be read."
                                error.message?.contains("corrupted", ignoreCase = true) == true -> 
                                    "This PDF file appears to be corrupted."
                                else -> "Failed to extract text from PDF: ${error.message}"
                            },
                            isLoading = false
                        )
                    }
            }
        }
    }
    
    private fun extractTextFromPdf(uri: Uri, pdfFileInfo: PdfFileInfo) {
        viewModelScope.launch {
            pdfProcessor.extractTextFromPdf(uri)
                .onSuccess { text ->
                    val newState = if (text.length > 50000) {
                        // Show warning for very long texts
                        _state.value.copy(
                            extractedText = text,
                            isLoading = false,
                            errorMessage = "This is a large document (${text.length} characters). Reading may take some time."
                        )
                    } else {
                        _state.value.copy(
                            extractedText = text,
                            isLoading = false
                        )
                    }
                    
                    // Update cache
                    if (fileInfoCache.size < MAX_CACHE_SIZE) {
                        fileInfoCache[uri] = pdfFileInfo
                    }
                    
                    _state.value = newState
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        errorMessage = when {
                            error.message?.contains("password", ignoreCase = true) == true -> 
                                "This PDF is password protected and cannot be read."
                            error.message?.contains("encrypt", ignoreCase = true) == true -> 
                                "This PDF is encrypted and cannot be read."
                            error.message?.contains("corrupted", ignoreCase = true) == true -> 
                                "This PDF file appears to be corrupted."
                            else -> "Failed to extract text from PDF: ${error.message}"
                        },
                        isLoading = false
                    )
                }
        }
    }
    
    fun playText() {
        val text = _state.value.extractedText
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
    
    fun seekTo(position: Int) {
        ttsManager.seekTo(position)
    }
    
    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
    
    fun clearPdf() {
        ttsManager.stop()
        _state.value = _state.value.copy(
            selectedPdfFile = null,
            extractedText = "",
            errorMessage = null
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        ttsManager.destroy()
    }
}
