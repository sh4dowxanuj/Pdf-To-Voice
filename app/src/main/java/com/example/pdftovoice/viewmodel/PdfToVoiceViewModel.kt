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
import com.example.pdftovoice.BuildConfig
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
    val translationError: String? = null,
    // Additional translation diagnostics
    val isGeminiAvailable: Boolean = false,
    val translationProvider: String? = null, // Gemini | Libre | Mixed
    val translationPartial: Boolean = false
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
    val keyPresent = !BuildConfig.GEMINI_API_KEY.isNullOrBlank()
    Log.d(TAG, "Gemini API key present: $keyPresent length=${BuildConfig.GEMINI_API_KEY.length}")
    _state.value = _state.value.copy(isGeminiAvailable = keyPresent)
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
    // Translation LRU cache
    private val translationCache = object : LinkedHashMap<String, String>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > 20
    }
    private fun cacheKey(text: String, lang: String): String = text.length.toString() + ":" + text.hashCode() + ":" + lang.lowercase()

    private fun startTranslationIfNeeded(language: Language) {
        val st = _state.value
        val original = st.originalExtractedText.ifBlank { st.extractedText }
        if (original.isBlank()) return
        if (language.code == lastTranslatedLanguage && st.translatedText != null) return
        // Avoid translating if text already appears to be in that language (very naive heuristic)
        if (language.code.equals("en", true) && original.matches(Regex("[A-Za-z0-9\\s,.;:'\"!?()-]+"))) return
        val key = cacheKey(original, language.code)
        translationCache[key]?.let { cached ->
            Log.d(TAG, "Using cached translation for ${language.code}")
            _state.value = _state.value.copy(
                translatedText = cached,
                translationLanguage = language.code,
                activeTextSource = TextSource.TRANSLATED,
                extractedText = if (_state.value.activeTextSource == TextSource.TRANSLATED) cached else _state.value.extractedText,
                translationProvider = "Cache",
                translationPartial = false,
                isTranslating = false
            )
            lastTranslatedLanguage = language.code
            return
        }
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

    fun clearTranslation() {
        val st = _state.value
        _state.value = st.copy(
            translatedText = null,
            activeTextSource = TextSource.ORIGINAL,
            extractedText = st.originalExtractedText,
            translationLanguage = null,
            translationProvider = null,
            translationPartial = false,
            translationProgress = 0,
            translationError = null
        )
    }

    private fun startTranslation(original: String, targetLang: String) {
        translationJob?.cancel()
        translationJob = viewModelScope.launch {
            try {
                // Detect probable existing language; if matches target skip
                val detected = detectLanguage(original)
                if (detected != null && detected.equals(targetLang, true)) {
                    Log.d(TAG, "Skipping translation: detected language $detected already matches target $targetLang")
                    lastTranslatedLanguage = targetLang
                    _state.value = _state.value.copy(
                        translationLanguage = targetLang,
                        translationProvider = "Skip",
                        translatedText = original,
                        activeTextSource = if (_state.value.activeTextSource == TextSource.TRANSLATED) TextSource.TRANSLATED else _state.value.activeTextSource
                    )
                    return@launch
                }
                _state.value = _state.value.copy(
                    isTranslating = true,
                    translationProgress = 0,
                    translationLanguage = targetLang,
                    translationError = null,
                    processingStatus = "Translating...",
                    translationProvider = null,
                    translationPartial = false
                )
                // Inline chunked translation to allow partial preservation
                val chunkSize = 4500
                val chunks = mutableListOf<String>()
                var idx = 0
                while (idx < original.length) {
                    var end = (idx + chunkSize).coerceAtMost(original.length)
                    if (end < original.length) {
                        val nextSpace = original.lastIndexOf(' ', end)
                        if (nextSpace > idx + 1000) end = nextSpace
                    }
                    chunks.add(original.substring(idx, end))
                    idx = end
                }
                val sb = StringBuilder(original.length + 64)
                val providersUsed = mutableSetOf<String>()
                for ((i, chunk) in chunks.withIndex()) {
                    if (!isActive) break
                    val tr = translateChunk(chunk, targetLang)
                    sb.append(tr.text)
                    if (i < chunks.lastIndex) sb.append('\n')
                    providersUsed += tr.provider
                    val progress = ((i + 1) * 100f / chunks.size).toInt()
                    // Publish partial progress & text
                    val partial = sb.toString()
                    _state.value = _state.value.copy(
                        translationProgress = progress,
                        processingStatus = "Translating... $progress%",
                        translatedText = partial,
                        translationProvider = providersUsed.firstOrNull(),
                        translationPartial = progress < 100
                    )
                    delay(150)
                }
                var translated = sb.toString()
                // Whitespace normalization (collapse 3+ blank lines to 2)
                translated = translated.replace(Regex("\n{3,}"), "\n\n").trim()
                val providerLabel = when {
                    providersUsed.isEmpty() -> null
                    providersUsed.size == 1 -> providersUsed.first()
                    else -> "Mixed"
                }
                lastTranslatedLanguage = targetLang
                _state.value = _state.value.copy(
                    translatedText = translated,
                    isTranslating = false,
                    processingStatus = null,
                    activeTextSource = if (_state.value.activeTextSource == TextSource.TRANSLATED || _state.value.translatedText == null) TextSource.TRANSLATED else _state.value.activeTextSource,
                    extractedText = if (_state.value.activeTextSource == TextSource.TRANSLATED) translated else _state.value.extractedText,
                    translationProvider = providerLabel,
                    translationPartial = false
                )
                // Cache result
                val key = cacheKey(original, targetLang)
                translationCache[key] = translated
            } catch (e: CancellationException) {
                Log.w(TAG, "Translation cancelled")
                // Preserve partial text if any accumulated in previous state updates not available; can't access builder here
                // (Future improvement: refactor for finer-grained partial capture)
                _state.value = _state.value.copy(
                    isTranslating = false,
                    processingStatus = null,
                    translationPartial = true
                )
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

    // Manual translation request (decoupled from TTS language change)
    fun requestTranslation(targetLang: String) {
        val st = _state.value
        val original = st.originalExtractedText.ifBlank { st.extractedText }
        if (original.isBlank()) return
        val lang = targetLang.lowercase()
        val key = cacheKey(original, lang)
        translationCache[key]?.let { cached ->
            _state.value = _state.value.copy(
                translatedText = cached,
                translationLanguage = lang,
                activeTextSource = TextSource.TRANSLATED,
                extractedText = if (_state.value.activeTextSource == TextSource.TRANSLATED) cached else _state.value.extractedText,
                translationProvider = "Cache",
                translationPartial = false,
                isTranslating = false
            )
            lastTranslatedLanguage = lang
            return
        }
        startTranslation(original, lang)
    }

    private fun buildFormBody(params: Map<String, String>): ByteArray = params.entries.joinToString("&") { (k,v) ->
        "${java.net.URLEncoder.encode(k, "UTF-8")}=${java.net.URLEncoder.encode(v, "UTF-8")}"
    }.toByteArray()

    private data class TranslationResult(val text: String, val provider: String)

    private fun translateChunk(chunk: String, targetLang: String): TranslationResult {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!apiKey.isNullOrBlank()) {
            val gem = translateChunkGemini(chunk, targetLang, apiKey)
            if (gem != null) return TranslationResult(gem, "Gemini")
        }
        val libre = translateChunkLibre(chunk, targetLang)
        val provider = if (!apiKey.isNullOrBlank()) "Libre" else "Libre" // explicit for clarity
        return TranslationResult(libre, provider)
    }

    private fun translateChunkGemini(chunk: String, targetLang: String, apiKey: String): String? {
        return try {
            // Simple prompt-based translation. For large scale move to official client.
            val prompt = "Translate the following text into language code '$targetLang'. Return only translated text without extra commentary.\n\n$chunk"
            val url = java.net.URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=$apiKey")
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 10000
                readTimeout = 25000
                doOutput = true
            }
            val escapedPrompt = prompt.replace("\"", "\\\"")
            val payload = """{"contents":[{"parts":[{"text":"$escapedPrompt"}]}]}"""
            conn.outputStream.use { it.write(payload.toByteArray()) }
            val code = conn.responseCode
            val response = try { conn.inputStream.bufferedReader().readText() } catch (e: Exception) { conn.errorStream?.bufferedReader()?.readText() ?: "" }
            if (code != 200) {
                Log.w(TAG, "Gemini translation HTTP $code: ${response.take(120)}")
                return null
            }
            // Minimal JSON extraction
            val root = org.json.JSONObject(response)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val content = candidates.getJSONObject(0).optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null
            var text = parts.getJSONObject(0).optString("text")
            text = text.trim()
            // Remove leading labels
            text = text.removePrefix("Translation:").removePrefix("translation:").trim()
            // Strip enclosing quotes if present
            if ((text.startsWith('"') && text.endsWith('"') && text.length > 1) ||
                (text.startsWith('“') && text.endsWith('”') && text.length > 1)) {
                text = text.substring(1, text.length - 1).trim()
            }
            text.ifBlank { null }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini chunk translation error: ${e.message}")
            null
        }
    }

    // Simple heuristic language detection for a few common languages
    private fun detectLanguage(text: String): String? {
        if (text.length < 20) return null
        val sample = text.take(4000)
        var latinLetters = 0
        var nonLatin = 0
        var spanishHits = 0
        var frenchHits = 0
        for (ch in sample) {
            when {
                ch.isLetter() && ch.code < 128 -> latinLetters++
                ch.isLetter() -> nonLatin++
            }
            when (ch.lowercaseChar()) {
                'ñ','¿','¡' -> spanishHits += 2
                'á','é','í','ó','ú' -> spanishHits++
                'à','ç','è','é','ê','ô','ù' -> frenchHits++
            }
        }
        val totalLetters = latinLetters + nonLatin
        if (totalLetters == 0) return null
        val asciiRatio = latinLetters.toDouble() / totalLetters
        return when {
            spanishHits >= 5 && spanishHits > frenchHits -> "es"
            frenchHits >= 4 && frenchHits >= spanishHits -> "fr"
            asciiRatio > 0.9 && nonLatin < 5 -> "en"
            else -> null
        }
    }

    private fun translateChunkLibre(chunk: String, targetLang: String): String {
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
            if (code != 200) return chunk
            val json = org.json.JSONObject(response)
            json.optString("translatedText", chunk)
        } catch (e: Exception) {
            Log.e(TAG, "Libre chunk translation error: ${e.message}")
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
