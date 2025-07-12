package com.example.pdftovoice.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.pdftovoice.data.LanguagePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class Language(
    val code: String,
    val name: String,
    val locale: Locale
)

class TtsManager(private val context: Context) {
    
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private val languagePreferences = LanguagePreferences(context)
    
    // Use thread-safe state management
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentText = MutableStateFlow("")
    val currentText: StateFlow<String> = _currentText.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()
    
    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()
    
    private val _currentSegment = MutableStateFlow("")
    val currentSegment: StateFlow<String> = _currentSegment.asStateFlow()
    
    private val _speed = MutableStateFlow(1.0f)
    val speed: StateFlow<Float> = _speed.asStateFlow()
    
    private val _pitch = MutableStateFlow(1.0f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()
    
    private val _currentLanguage = MutableStateFlow(getDefaultLanguage())
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()
    
    private val _availableLanguages = MutableStateFlow<List<Language>>(emptyList())
    val availableLanguages: StateFlow<List<Language>> = _availableLanguages.asStateFlow()
    
    // Optimized for memory efficiency
    private var textSegments: List<String> = emptyList()
    private var currentSegmentIndex = 0
    
    // Cache for processed segments to avoid reprocessing
    private val segmentCache = ConcurrentHashMap<String, List<String>>()
    
    companion object {
        private const val MAX_CACHE_SIZE = 10
        private const val MAX_SEGMENT_LENGTH = 200 // Optimal for TTS
        
        private val SUPPORTED_LANGUAGES = listOf(
            Language("en", "English", Locale.ENGLISH),
            Language("es", "Español", Locale.forLanguageTag("es")),
            Language("fr", "Français", Locale.FRENCH),
            Language("de", "Deutsch", Locale.GERMAN),
            Language("it", "Italiano", Locale.ITALIAN),
            Language("pt", "Português", Locale.forLanguageTag("pt")),
            Language("ru", "Русский", Locale.forLanguageTag("ru")),
            Language("zh", "中文", Locale.CHINESE),
            Language("ja", "日本語", Locale.JAPANESE),
            Language("ko", "한국어", Locale.KOREAN),
            Language("ar", "العربية", Locale.forLanguageTag("ar")),
            Language("hi", "हिन्दी", Locale.forLanguageTag("hi")),
            Language("mr", "मराठी", Locale.forLanguageTag("mr")),
            Language("ta", "தமிழ்", Locale.forLanguageTag("ta")),
            Language("te", "తెలుగు", Locale.forLanguageTag("te")),
            Language("bn", "বাংলা", Locale.forLanguageTag("bn")),
            Language("gu", "ગુજરાતી", Locale.forLanguageTag("gu")),
            Language("kn", "ಕನ್ನಡ", Locale.forLanguageTag("kn")),
            Language("ml", "മലയാളം", Locale.forLanguageTag("ml")),
            Language("pa", "ਪੰਜਾਬੀ", Locale.forLanguageTag("pa")),
            Language("nl", "Nederlands", Locale.forLanguageTag("nl")),
            Language("sv", "Svenska", Locale.forLanguageTag("sv")),
            Language("da", "Dansk", Locale.forLanguageTag("da")),
            Language("no", "Norsk", Locale.forLanguageTag("no")),
            Language("fi", "Suomi", Locale.forLanguageTag("fi")),
            Language("pl", "Polski", Locale.forLanguageTag("pl")),
            Language("tr", "Türkçe", Locale.forLanguageTag("tr")),
            Language("th", "ไทย", Locale.forLanguageTag("th"))
        )
    }
    
    fun initialize(onInitialized: (Boolean) -> Unit) {
        textToSpeech = TextToSpeech(context) { status ->
            isInitialized = status == TextToSpeech.SUCCESS
            if (isInitialized) {
                textToSpeech?.language = _currentLanguage.value.locale
                setupUtteranceListener()
                checkAvailableLanguages()
            }
            onInitialized(isInitialized)
        }
    }
    
    private fun setupUtteranceListener() {
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            @Deprecated("Deprecated in Java")
            override fun onStart(utteranceId: String?) {
                _isPlaying.value = true
                _isPaused.value = false
            }
            
            @Deprecated("Deprecated in Java")
            override fun onDone(utteranceId: String?) {
                if (currentSegmentIndex < textSegments.size - 1) {
                    currentSegmentIndex++
                    _currentPosition.value = currentSegmentIndex
                    speakCurrentSegment()
                } else {
                    _isPlaying.value = false
                    _currentPosition.value = 0
                    _currentSegment.value = ""
                    currentSegmentIndex = 0
                }
            }
            
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isPlaying.value = false
                _currentSegment.value = ""
            }
        })
    }
    
    private fun loadAvailableLanguages() {
        val locales = Locale.getAvailableLocales()
        val languages = locales.mapNotNull { locale ->
            try {
                val displayName = locale.getDisplayLanguage(locale)
                if (displayName.isNotEmpty() && !locale.language.equals("und", ignoreCase = true)) {
                    Language(locale.language, displayName, locale)
                } else null
            } catch (e: Exception) {
                null
            }
        }.distinctBy { it.code }
        
        _availableLanguages.value = languages
    }
    
    private fun checkAvailableLanguages() {
        val availableLanguages = mutableListOf<Language>()
        
        SUPPORTED_LANGUAGES.forEach { language ->
            val result = textToSpeech?.isLanguageAvailable(language.locale)
            if (result == TextToSpeech.LANG_AVAILABLE || 
                result == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE) {
                availableLanguages.add(language)
            }
        }
        
        _availableLanguages.value = availableLanguages.ifEmpty { 
            listOf(getDefaultLanguage()) 
        }
    }
    
    fun setLanguage(language: Language): Boolean {
        if (!isInitialized) return false
        
        val result = textToSpeech?.setLanguage(language.locale)
        return if (result == TextToSpeech.LANG_AVAILABLE || 
                   result == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                   result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE) {
            _currentLanguage.value = language
            languagePreferences.saveSelectedLanguage(language)
            true
        } else {
            false
        }
    }
    
    fun isLanguageAvailable(language: Language): Boolean {
        val result = textToSpeech?.isLanguageAvailable(language.locale)
        return result == TextToSpeech.LANG_AVAILABLE || 
               result == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
               result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
    }
    
    private fun getDefaultLanguage(): Language {
        // First check saved language preference
        languagePreferences.getSelectedLanguage()?.let { return it }
        
        // Fall back to system locale
        val systemLocale = Locale.getDefault()
        return SUPPORTED_LANGUAGES.find { 
            it.locale.language == systemLocale.language 
        } ?: SUPPORTED_LANGUAGES.first() // Default to English
    }
    
    fun speak(text: String) {
        if (!isInitialized) return
        
        _currentText.value = text
        textSegments = splitTextIntoSegments(text)
        currentSegmentIndex = 0
        _currentPosition.value = 0
        _currentSegment.value = ""
        
        textToSpeech?.setSpeechRate(_speed.value)
        textToSpeech?.setPitch(_pitch.value)
        
        speakCurrentSegment()
    }
    
    private fun speakCurrentSegment() {
        if (currentSegmentIndex < textSegments.size) {
            val segment = textSegments[currentSegmentIndex]
            _currentSegment.value = segment
            android.util.Log.d("TtsManager", "Speaking segment $currentSegmentIndex: '$segment'")
            textToSpeech?.speak(segment, TextToSpeech.QUEUE_FLUSH, null, "utterance_$currentSegmentIndex")
        }
    }
    
    private fun splitTextIntoSegments(text: String): List<String> {
        // Check cache first for performance
        segmentCache[text]?.let { return it }
        
        // Optimized text splitting for better TTS performance
        val segments = text
            .split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .flatMap { segment ->
                // Break down very long segments for better TTS control
                if (segment.length > MAX_SEGMENT_LENGTH) {
                    segment.chunked(MAX_SEGMENT_LENGTH) { chunk ->
                        val chunkStr = chunk.toString().trim()
                        if (!chunkStr.matches(Regex(".*[.!?]$"))) "$chunkStr." else chunkStr
                    }
                } else {
                    listOf(if (!segment.matches(Regex(".*[.!?]$"))) "$segment." else segment)
                }
            }
        
        // Cache the result but limit cache size
        if (segmentCache.size >= MAX_CACHE_SIZE) {
            segmentCache.clear() // Simple cache eviction
        }
        segmentCache[text] = segments
        
        return segments
    }
    
    fun pause() {
        if (!isInitialized) return
        
        if (_isPlaying.value) {
            textToSpeech?.stop()
            _isPaused.value = true
            _isPlaying.value = false
            // Keep current segment when pausing
        }
    }
    
    fun resume() {
        if (!isInitialized) return
        
        if (_isPaused.value) {
            speakCurrentSegment()
            _isPaused.value = false
        }
    }
    
    fun stop() {
        if (!isInitialized) return
        
        textToSpeech?.stop()
        _isPlaying.value = false
        _isPaused.value = false
        _currentPosition.value = 0
        _currentSegment.value = ""
        currentSegmentIndex = 0
    }
    
    fun setSpeed(speed: Float) {
        _speed.value = speed.coerceIn(0.1f, 3.0f)
        if (isInitialized) {
            textToSpeech?.setSpeechRate(_speed.value)
        }
    }
    
    fun setPitch(pitch: Float) {
        _pitch.value = pitch.coerceIn(0.1f, 2.0f)
        if (isInitialized) {
            textToSpeech?.setPitch(_pitch.value)
        }
    }
    
    fun getCurrentSegment(): String? {
        return if (currentSegmentIndex < textSegments.size) {
            textSegments[currentSegmentIndex]
        } else null
    }
    
    fun seekTo(position: Int) {
        if (position in 0 until textSegments.size) {
            val wasPlaying = _isPlaying.value
            stop()
            currentSegmentIndex = position
            _currentPosition.value = position
            
            if (wasPlaying) {
                speakCurrentSegment()
            }
        }
    }
    
    fun destroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
    }
}
