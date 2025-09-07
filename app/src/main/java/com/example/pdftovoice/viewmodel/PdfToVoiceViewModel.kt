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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
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
    val processingStatus: String? = null,
    // Translation related state
    val originalExtractedText: String = "",
    val translatedText: String? = null,
    val activeTextSource: TextSource = TextSource.ORIGINAL,
    val isTranslating: Boolean = false,
    val translationProgress: Int = 0, // percentage 0..100
    val translationLanguage: String? = null,
    val translationError: String? = null
)

enum class TextSource { ORIGINAL, TRANSLATED }

class PdfToVoiceViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context = application
    private val pdfProcessor = PdfProcessor(application)
    private val pdfTypeDetector = PdfTypeDetector(application)
    private val ttsManager = TtsManager(application)
    
    private val _state = MutableStateFlow(PdfToVoiceState())
    val state: StateFlow<PdfToVoiceState> = _state.asStateFlow()
    
    // TTS State flows
    val isPlaying: StateFlow<Boolean> = ttsManager.isPlaying
    val isPaused: StateFlow<Boolean> = ttsManager.isPaused
    val currentPosition: StateFlow<Int> = ttsManager.currentPosition
    val speed: StateFlow<Float> = ttsManager.speed
    val pitch: StateFlow<Float> = ttsManager.pitch
    
    // Word-by-word highlighting states
    val currentWord: StateFlow<String> = ttsManager.currentWord
    val wordIndex: StateFlow<Int> = ttsManager.wordIndex
    
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
                    extractTextFromPdf(uri)
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
    
    private fun extractTextFromPdf(uri: Uri) {
        currentPdfJob = viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    isLoading = true,
                    processingStatus = "Extracting text using best available method..."
                )

                Log.d(TAG, "Starting text extraction")

                val languageCode = currentLanguage.value.code
                val result = pdfProcessor.extractTextWithDetails(uri, languageCode).getOrThrow()

                Log.d(TAG, "Text extraction completed using ${result.method}")

                val statusMessage = buildString {
                    append("✅ Extraction Complete!\n")
                    append("Method: ${result.method.name.replace("_", " ")}\n")
                    append("Text Length: ${result.text.length} characters\n")
                    append("Pages Processed: ${result.pageCount}")

                    if (result.hasImages) {
                        append("\n📷 Contains images")
                    }
                }

                _state.value = _state.value.copy(
                    extractedText = result.text,
                    originalExtractedText = result.text,
                    extractionMethod = result.method,
                    isLoading = false,
                    processingStatus = null,
                    errorMessage = if (result.text.length > 50000) {
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
        updateLocale(language)
    // Start translation asynchronously if needed without overwriting original prematurely
    startTranslationIfNeeded(language)
    }

    private fun updateLocale(language: Language) {
        val locale = java.util.Locale(language.code)
        java.util.Locale.setDefault(locale)
        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private var lastTranslatedLanguage: String? = null
    private var translationJob: Job? = null

    private fun startTranslationIfNeeded(language: Language) {
        val st = _state.value
        val original = st.originalExtractedText.ifBlank { st.extractedText }
        if (original.isBlank()) return
        if (language.code == lastTranslatedLanguage && st.translatedText != null) return
        // Avoid translating if text already appears to be in that language (very naive heuristic)
        if (language.code.equals("en", true) && original.matches(Regex("[A-Za-z0-9\\s,.;:'\"!?()-]+"))) return
        startTranslation(original, language.code)
    }

    fun toggleTextSource() {
        val st = _state.value
        val newSource = if (st.activeTextSource == TextSource.ORIGINAL && st.translatedText != null) TextSource.TRANSLATED else TextSource.ORIGINAL
        _state.value = st.copy(
            activeTextSource = newSource,
            extractedText = if (newSource == TextSource.TRANSLATED) st.translatedText ?: st.originalExtractedText else st.originalExtractedText
        )
        // Restart TTS if playing
        if (isPlaying.value) {
            stopReading()
            playText()
        }
    }

    fun cancelTranslation() {
        translationJob?.cancel()
        _state.value = _state.value.copy(isTranslating = false, processingStatus = null)
    }

    private fun startTranslation(original: String, targetLang: String) {
        translationJob?.cancel()
        translationJob = viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    isTranslating = true,
                    translationProgress = 0,
                    translationLanguage = targetLang,
                    translationError = null,
                    processingStatus = "Translating..."
                )
                val translated = translateFullTextChunked(original, targetLang) { progress ->
                    _state.value = _state.value.copy(
                        translationProgress = progress,
                        processingStatus = "Translating... $progress%"
                    )
                }
                lastTranslatedLanguage = targetLang
                _state.value = _state.value.copy(
                    translatedText = translated,
                    isTranslating = false,
                    processingStatus = null,
                    activeTextSource = if (_state.value.activeTextSource == TextSource.TRANSLATED || _state.value.translatedText == null) TextSource.TRANSLATED else _state.value.activeTextSource,
                    extractedText = if (_state.value.activeTextSource == TextSource.TRANSLATED) translated else _state.value.extractedText
                )
            } catch (e: CancellationException) {
                Log.w(TAG, "Translation cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Translation failed", e)
                _state.value = _state.value.copy(
                    translationError = e.message,
                    isTranslating = false,
                    processingStatus = null
                )
            }
        }
    }

    private suspend fun translateFullTextChunked(text: String, targetLang: String, progressCallback: (Int) -> Unit): String = withContext(Dispatchers.IO) {
        val chunkSize = 4500 // characters before URL encoding
        val chunks = mutableListOf<String>()
        var idx = 0
        while (idx < text.length) {
            var end = (idx + chunkSize).coerceAtMost(text.length)
            // try not to cut in middle of word
            if (end < text.length) {
                val nextSpace = text.lastIndexOf(' ', end)
                if (nextSpace > idx + 1000) { // ensure we don't shrink too much
                    end = nextSpace
                }
            }
            chunks.add(text.substring(idx, end))
            idx = end
        }
        if (chunks.isEmpty()) return@withContext ""
        val sb = StringBuilder(text.length + 64)
        for ((i, chunk) in chunks.withIndex()) {
            val translatedChunk = translateChunk(chunk, targetLang)
            sb.append(translatedChunk)
            if (i < chunks.lastIndex) sb.append('\n')
            val progress = ((i + 1) * 100f / chunks.size).toInt()
            progressCallback(progress)
            if (!isActive) throw CancellationException()
            delay(150) // small pacing delay
        }
        sb.toString()
    }

    private fun buildFormBody(params: Map<String, String>): ByteArray = params.entries.joinToString("&") { (k,v) ->
        "${java.net.URLEncoder.encode(k, "UTF-8")}=${java.net.URLEncoder.encode(v, "UTF-8")}"
    }.toByteArray()

    private fun translateChunk(chunk: String, targetLang: String): String {
        return try {
            val url = java.net.URL("https://libretranslate.de/translate")
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connectTimeout = 8000
                readTimeout = 20000
                doOutput = true
            }
            val body = buildFormBody(mapOf(
                "q" to chunk,
                "source" to "auto",
                "target" to targetLang,
                "format" to "text"
            ))
            conn.outputStream.use { it.write(body) }
            val code = conn.responseCode
            val response = try { conn.inputStream.bufferedReader().readText() } catch (e: Exception) { conn.errorStream?.bufferedReader()?.readText() ?: "" }
            if (code != 200) return chunk // fallback to original
            val json = org.json.JSONObject(response)
            json.optString("translatedText", chunk)
        } catch (e: Exception) {
            Log.e(TAG, "Chunk translation error: ${e.message}")
            chunk
        }
    }
    
    fun clearPdf() {
        // Cancel any ongoing operations
        currentPdfJob?.cancel()
        currentAnalysisJob?.cancel()
    translationJob?.cancel()
        
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
            extractTextFromPdf(currentFile.uri)
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
            originalExtractedText = text,
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
