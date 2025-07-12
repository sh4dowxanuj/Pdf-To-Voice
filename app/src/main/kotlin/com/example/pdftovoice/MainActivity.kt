package com.example.pdftovoice

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.pdftovoice.databinding.ActivityMainBinding
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var textToSpeech: TextToSpeech
    private var pdfText: String = ""
    private var currentWordIndex = 0
    private var wordPositions = mutableListOf<Pair<Int, Int>>()
    private var isPlaying = false
    private var isPaused = false
    
    private val pdfPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { extractTextFromPdf(it) }
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openPdfPicker()
        } else {
            Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViews()
        initializeTextToSpeech()
    }
    
    private fun setupViews() {
        // Set up PDF selection button
        binding.selectPdfButton.setOnClickListener {
            checkPermissionAndOpenPdf()
        }
        
        // Set up media controls
        binding.playPauseButton.setOnClickListener {
            if (isPlaying) {
                pauseSpeech()
            } else {
                if (isPaused) {
                    resumeSpeech()
                } else {
                    startSpeech()
                }
            }
        }
        
        binding.stopButton.setOnClickListener {
            stopSpeech()
        }
        
        binding.previousButton.setOnClickListener {
            previousWord()
        }
        
        binding.nextButton.setOnClickListener {
            nextWord()
        }
        
        binding.speedSlider.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val speed = 0.5f + (progress / 100f) * 1.5f // Range: 0.5x to 2.0x
                textToSpeech.setSpeechRate(speed)
                binding.speedText.text = String.format("%.1fx", speed)
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
        
        // Initialize speed
        binding.speedSlider.progress = 50 // 1.0x speed
        binding.speedText.text = "1.0x"
    }
    
    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(this, this)
    }
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.getDefault())
            
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, getString(R.string.language_not_supported), Toast.LENGTH_SHORT).show()
            } else {
                setupUtteranceProgressListener()
            }
        } else {
            Toast.makeText(this, getString(R.string.tts_init_failed), Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupUtteranceProgressListener() {
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                runOnUiThread {
                    isPlaying = true
                    isPaused = false
                    updatePlayPauseButton()
                }
            }
            
            override fun onDone(utteranceId: String?) {
                runOnUiThread {
                    isPlaying = false
                    isPaused = false
                    currentWordIndex = 0
                    updatePlayPauseButton()
                    removeHighlight()
                }
            }
            
            override fun onError(utteranceId: String?) {
                runOnUiThread {
                    isPlaying = false
                    isPaused = false
                    updatePlayPauseButton()
                    Toast.makeText(this@MainActivity, getString(R.string.speech_error), Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                runOnUiThread {
                    highlightCurrentWord(start, end)
                    scrollToCurrentWord(start)
                }
            }
        })
    }
    
    private fun checkPermissionAndOpenPdf() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ doesn't need READ_EXTERNAL_STORAGE for document picker
            openPdfPicker()
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    openPdfPicker()
                }
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) -> {
                    Toast.makeText(
                        this,
                        getString(R.string.permission_required),
                        Toast.LENGTH_LONG
                    ).show()
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }
    
    private fun openPdfPicker() {
        pdfPickerLauncher.launch("application/pdf")
    }
    
    private fun extractTextFromPdf(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val pdfReader = PdfReader(inputStream)
                val stringBuilder = StringBuilder()
                
                for (pageNum in 1..pdfReader.numberOfPages) {
                    val pageText = PdfTextExtractor.getTextFromPage(pdfReader, pageNum)
                    stringBuilder.append(pageText).append("\n")
                }
                
                pdfReader.close()
                pdfText = stringBuilder.toString().trim()
                
                if (pdfText.isNotEmpty()) {
                    processText()
                    binding.textDisplay.text = pdfText
                    Toast.makeText(this, getString(R.string.pdf_loaded), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.no_text_found), Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "${getString(R.string.error_reading_pdf)}: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun processText() {
        wordPositions.clear()
        val words = pdfText.split("\\s+".toRegex())
        var currentPos = 0
        
        for (word in words) {
            val startPos = pdfText.indexOf(word, currentPos)
            if (startPos != -1) {
                wordPositions.add(Pair(startPos, startPos + word.length))
                currentPos = startPos + word.length
            }
        }
    }
    
    private fun startSpeech() {
        if (pdfText.isNotEmpty()) {
            currentWordIndex = 0
            textToSpeech.speak(pdfText, TextToSpeech.QUEUE_FLUSH, null, "PDF_SPEECH")
        } else {
            Toast.makeText(this, getString(R.string.no_pdf_selected), Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun pauseSpeech() {
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
            isPlaying = false
            isPaused = true
            updatePlayPauseButton()
        }
    }
    
    private fun resumeSpeech() {
        if (isPaused && pdfText.isNotEmpty()) {
            // Get remaining text from current word position
            val remainingText = if (currentWordIndex < wordPositions.size) {
                pdfText.substring(wordPositions[currentWordIndex].first)
            } else {
                pdfText
            }
            textToSpeech.speak(remainingText, TextToSpeech.QUEUE_FLUSH, null, "PDF_SPEECH")
        }
    }
    
    private fun stopSpeech() {
        textToSpeech.stop()
        isPlaying = false
        isPaused = false
        currentWordIndex = 0
        updatePlayPauseButton()
        removeHighlight()
    }
    
    private fun previousWord() {
        if (currentWordIndex > 0) {
            currentWordIndex--
            highlightWordAtIndex(currentWordIndex)
            scrollToCurrentWord(wordPositions[currentWordIndex].first)
        }
    }
    
    private fun nextWord() {
        if (currentWordIndex < wordPositions.size - 1) {
            currentWordIndex++
            highlightWordAtIndex(currentWordIndex)
            scrollToCurrentWord(wordPositions[currentWordIndex].first)
        }
    }
    
    private fun highlightCurrentWord(start: Int, end: Int) {
        // Find the word index that contains this range
        for (i in wordPositions.indices) {
            val (wordStart, wordEnd) = wordPositions[i]
            if (start >= wordStart && start < wordEnd) {
                currentWordIndex = i
                highlightWordAtIndex(i)
                break
            }
        }
    }
    
    private fun highlightWordAtIndex(index: Int) {
        if (index < wordPositions.size) {
            val (start, end) = wordPositions[index]
            val spannable = SpannableString(pdfText)
            
            // Clear previous highlights
            val existingSpans = spannable.getSpans(0, spannable.length, BackgroundColorSpan::class.java)
            for (span in existingSpans) {
                spannable.removeSpan(span)
            }
            val existingTextSpans = spannable.getSpans(0, spannable.length, ForegroundColorSpan::class.java)
            for (span in existingTextSpans) {
                spannable.removeSpan(span)
            }
            
            // Add new highlight
            spannable.setSpan(
                BackgroundColorSpan(ContextCompat.getColor(this, R.color.highlight_background)),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(this, R.color.highlight_text)),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            
            binding.textDisplay.text = spannable
        }
    }
    
    private fun removeHighlight() {
        binding.textDisplay.text = pdfText
    }
    
    private fun scrollToCurrentWord(position: Int) {
        binding.textDisplay.post {
            val layout = binding.textDisplay.layout
            if (layout != null) {
                val line = layout.getLineForOffset(position)
                val lineTop = layout.getLineTop(line)
                val lineBottom = layout.getLineBottom(line)
                val lineHeight = lineBottom - lineTop
                
                // Scroll to center the line on screen
                val scrollY = lineTop - (binding.scrollView.height / 2) + (lineHeight / 2)
                binding.scrollView.smoothScrollTo(0, scrollY.coerceAtLeast(0))
            }
        }
    }
    
    private fun updatePlayPauseButton() {
        binding.playPauseButton.setImageResource(
            if (isPlaying) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play
        )
    }
    
    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }
}
