package com.example.pdftovoice.pdf

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.coroutines.coroutineContext

class PdfProcessor(private val context: Context) {
    
    companion object {
        private const val TEMP_FILE_PREFIX = "temp_pdf_"
        private const val BUFFER_SIZE = 8192 // Optimal buffer size for file copying
    }
    
    suspend fun extractTextFromPdf(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        var fileDescriptor: ParcelFileDescriptor? = null
        var pdfRenderer: PdfRenderer? = null
        
        try {
            // Check if coroutine is still active
            coroutineContext.ensureActive()
            
            inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Could not open PDF file"))
            
            // Create temporary file with better naming
            tempFile = File(context.cacheDir, "$TEMP_FILE_PREFIX${System.currentTimeMillis()}.pdf")
            outputStream = FileOutputStream(tempFile)
            
            // Optimized file copying with buffer
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                coroutineContext.ensureActive() // Check for cancellation
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.flush()
            
            // Close streams before opening PDF
            inputStream.close()
            outputStream.close()
            inputStream = null
            outputStream = null
            
            // Try to get basic PDF info using PdfRenderer
            fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor)
            
            val pageCount = pdfRenderer.pageCount
            
            // Generate optimized sample text based on page count
            val sampleText = generateOptimizedSampleText(pageCount)
            
            Result.success(sampleText)
            
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            // Proper resource cleanup
            try {
                inputStream?.close()
                outputStream?.close()
                pdfRenderer?.close()
                fileDescriptor?.close()
                tempFile?.delete()
            } catch (e: Exception) {
                // Log but don't throw cleanup errors
            }
        }
    }
    
    suspend fun extractTextFromPdfWithPages(uri: Uri): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            // Extract text and split into artificial pages for demonstration
            extractTextFromPdf(uri).fold(
                onSuccess = { text ->
                    val words = text.split(" ")
                    val pages = mutableListOf<String>()
                    val wordsPerPage = 50
                    
                    for (i in words.indices step wordsPerPage) {
                        val pageWords = words.subList(i, minOf(i + wordsPerPage, words.size))
                        pages.add(pageWords.joinToString(" "))
                    }
                    
                    Result.success(pages)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun generateOptimizedSampleText(pageCount: Int): String {
        return """
            Welcome to PDF to Voice Reader! Your document contains $pageCount page${if (pageCount != 1) "s" else ""}.
            
            This is an optimized demonstration showcasing advanced text-to-speech capabilities. The app intelligently processes your content for superior audio quality and user experience.
            
            Key features include adaptive speech speed from 0.1x to 3.0x, precise pitch control, real-time progress tracking, and smart text segmentation for natural-sounding speech.
            
            The application utilizes modern Android architecture with Jetpack Compose, Material Design 3, and efficient memory management for smooth performance across all devices.
            
            Voice synthesis is powered by Android's native TextToSpeech engine with enhanced error handling and recovery mechanisms. The interface provides intuitive controls with large touch targets for accessibility.
            
            Advanced features include automatic text chunking for optimal TTS processing, intelligent pause points at sentence boundaries, and comprehensive state management for reliable playback control.
            
            This sample demonstrates the app's ability to handle various text lengths and complexity levels while maintaining consistent audio quality and responsive user interaction.
            
            Thank you for experiencing the future of accessible document reading!
        """.trimIndent()
    }
}
