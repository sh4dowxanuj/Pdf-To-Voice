package com.example.pdftovoice.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class TtsManager(private val context: Context) {
    
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    
    // Use thread-safe state management
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentText = MutableStateFlow("")
    val currentText: StateFlow<String> = _currentText.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()
    
    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()
    
    private val _speed = MutableStateFlow(1.0f)
    val speed: StateFlow<Float> = _speed.asStateFlow()
    
    private val _pitch = MutableStateFlow(1.0f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()
    
    // Optimized for memory efficiency
    private var textSegments: List<String> = emptyList()
    private var currentSegmentIndex = 0
    
    // Cache for processed segments to avoid reprocessing
    private val segmentCache = ConcurrentHashMap<String, List<String>>()
    
    companion object {
        private const val MAX_CACHE_SIZE = 10
        private const val MAX_SEGMENT_LENGTH = 200 // Optimal for TTS
    }
    
    fun initialize(onInitialized: (Boolean) -> Unit) {
        textToSpeech = TextToSpeech(context) { status ->
            isInitialized = status == TextToSpeech.SUCCESS
            if (isInitialized) {
                textToSpeech?.language = Locale.getDefault()
                setupUtteranceListener()
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
                    currentSegmentIndex = 0
                }
            }
            
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isPlaying.value = false
            }
        })
    }
    
    fun speak(text: String) {
        if (!isInitialized) return
        
        _currentText.value = text
        textSegments = splitTextIntoSegments(text)
        currentSegmentIndex = 0
        _currentPosition.value = 0
        
        textToSpeech?.setSpeechRate(_speed.value)
        textToSpeech?.setPitch(_pitch.value)
        
        speakCurrentSegment()
    }
    
    private fun speakCurrentSegment() {
        if (currentSegmentIndex < textSegments.size) {
            val segment = textSegments[currentSegmentIndex]
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
