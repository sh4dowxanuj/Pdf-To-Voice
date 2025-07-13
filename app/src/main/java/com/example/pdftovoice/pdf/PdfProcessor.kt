package com.example.pdftovoice.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.coroutines.coroutineContext

class PdfProcessor(private val context: Context) {
    
    companion object {
        private const val TEMP_FILE_PREFIX = "temp_pdf_"
        private const val BUFFER_SIZE = 16384 // Increased buffer size for better I/O performance
        private const val TAG = "PdfProcessor"
        private const val MIN_TEXT_LENGTH = 10 // Minimum text length to consider extraction successful
        private const val OCR_BITMAP_WIDTH = 1024 // Reduced for better performance
        private const val OCR_BITMAP_HEIGHT = 1024 // Reduced for better performance
        private const val MAX_OCR_PAGES = 15 // Increased limit for better coverage
    }

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    init {
        // Initialize PDFBox for Android
        PDFBoxResourceLoader.init(context)
    }

    data class ExtractionResult(
        val text: String,
        val method: ExtractionMethod,
        val pageCount: Int,
        val hasImages: Boolean = false,
        val isPasswordProtected: Boolean = false,
        val isEncrypted: Boolean = false
    )

    enum class ExtractionMethod {
        PDFBOX_ANDROID,
        ANDROID_RENDERER_OCR,
        HYBRID
    }

    suspend fun extractTextFromPdf(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val result = extractTextWithMethod(uri)
            Result.success(result.text)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract text from PDF", e)
            Result.failure(e)
        }
    }

    suspend fun extractTextWithDetails(uri: Uri): Result<ExtractionResult> = withContext(Dispatchers.IO) {
        try {
            val result = extractTextWithMethod(uri)
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract text from PDF with details", e)
            Result.failure(e)
        }
    }

    private suspend fun extractTextWithMethod(uri: Uri): ExtractionResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting text extraction from URI: $uri")
        val tempFile = createTempFile(uri)
        
        try {
            // Try multiple extraction methods in order of reliability
            
            // Method 1: PDFBox Android (Good for text-based PDFs)
            Log.d(TAG, "Trying PDFBox extraction...")
            val pdfboxResult = tryPdfBoxExtraction(tempFile)
            if (pdfboxResult.text.length >= MIN_TEXT_LENGTH) {
                Log.d(TAG, "Successfully extracted using PDFBox: ${pdfboxResult.text.length} characters")
                return@withContext pdfboxResult
            } else {
                Log.d(TAG, "PDFBox extraction returned insufficient text: ${pdfboxResult.text.length} characters")
            }
            
            // Method 2: OCR with Android PdfRenderer (For scanned/image PDFs)
            Log.d(TAG, "Trying OCR extraction...")
            val ocrResult = tryOcrExtraction(tempFile)
            if (ocrResult.text.length >= MIN_TEXT_LENGTH) {
                Log.d(TAG, "Successfully extracted using OCR: ${ocrResult.text.length} characters")
                return@withContext ocrResult
            } else {
                Log.d(TAG, "OCR extraction returned insufficient text: ${ocrResult.text.length} characters")
            }
            
            // Method 3: Hybrid approach (Combine PDFBox + OCR)
            Log.d(TAG, "Trying hybrid extraction...")
            val hybridResult = tryHybridExtraction(tempFile)
            if (hybridResult.text.length >= MIN_TEXT_LENGTH) {
                Log.d(TAG, "Successfully extracted using hybrid approach: ${hybridResult.text.length} characters")
                return@withContext hybridResult
            } else {
                Log.d(TAG, "Hybrid extraction returned insufficient text: ${hybridResult.text.length} characters")
            }
            
            // All extraction methods failed - return empty result with error info
            Log.w(TAG, "All extraction methods failed, no text could be extracted")
            return@withContext ExtractionResult(
                text = "No text could be extracted from this PDF. The document may be:\n\n" +
                       "• Scanned images without recognizable text\n" +
                       "• Password protected or encrypted\n" +
                       "• Corrupted or in an unsupported format\n" +
                       "• Contains only images or graphics\n\n" +
                       "Please try a different PDF file with readable text content.",
                method = ExtractionMethod.HYBRID,
                pageCount = 1
            )
            
        } finally {
            tempFile.delete()
        }
    }

    private suspend fun createTempFile(uri: Uri): File = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Could not open PDF file")
        
        val tempFile = File(context.cacheDir, "$TEMP_FILE_PREFIX${System.currentTimeMillis()}.pdf")
        
        try {
            inputStream.use { input ->
                tempFile.outputStream().buffered(BUFFER_SIZE).use { output ->
                    input.copyTo(output, BUFFER_SIZE)
                }
            }
        } catch (e: Exception) {
            tempFile.delete() // Clean up on failure
            throw e
        }
        
        tempFile
    }

    private suspend fun tryPdfBoxExtraction(file: File): ExtractionResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting PDFBox extraction from file: ${file.name}, size: ${file.length()} bytes")
            
            val document = PDDocument.load(file)
            Log.d(TAG, "PDFBox loaded PDF successfully")
            
            val pageCount = document.numberOfPages
            Log.d(TAG, "PDF has $pageCount pages")
            
            val stripper = PDFTextStripper()
            
            // Extract all text at once for better performance
            val text = stripper.getText(document)
            Log.d(TAG, "PDFBox extracted ${text.length} characters")
            
            document.close()
            
            val trimmedText = text.trim()
            Log.d(TAG, "PDFBox final text length: ${trimmedText.length}")
            
            ExtractionResult(
                text = trimmedText,
                method = ExtractionMethod.PDFBOX_ANDROID,
                pageCount = pageCount
            )
        } catch (e: Exception) {
            Log.w(TAG, "PDFBox extraction failed: ${e.message}", e)
            when {
                e.message?.contains("password", ignoreCase = true) == true -> {
                    throw Exception("This PDF is password protected and cannot be read.")
                }
                e.message?.contains("encrypt", ignoreCase = true) == true -> {
                    throw Exception("This PDF is encrypted and cannot be read.")
                }
                else -> {
                    ExtractionResult("", ExtractionMethod.PDFBOX_ANDROID, 0)
                }
            }
        }
    }

    private suspend fun tryOcrExtraction(file: File): ExtractionResult = withContext(Dispatchers.IO) {
        var fileDescriptor: ParcelFileDescriptor? = null
        var pdfRenderer: PdfRenderer? = null
        
        try {
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor)
            val pageCount = pdfRenderer.pageCount
            val text = StringBuilder()
            
            // Process each page with OCR (optimized limit for better performance)
            val pagesToProcess = minOf(pageCount, MAX_OCR_PAGES)
            for (i in 0 until pagesToProcess) {
                coroutineContext.ensureActive()
                val page = pdfRenderer.openPage(i)
                
                // Create bitmap for OCR
                val bitmap = Bitmap.createBitmap(
                    OCR_BITMAP_WIDTH, 
                    OCR_BITMAP_HEIGHT, 
                    Bitmap.Config.ARGB_8888
                )
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                
                // Perform OCR on the bitmap
                val ocrText = performOcr(bitmap)
                if (ocrText.isNotBlank()) {
                    text.append("=== Page ${i + 1} ===\n")
                    text.append(ocrText).append("\n\n")
                }
                
                bitmap.recycle()
            }
            
            if (pageCount > MAX_OCR_PAGES) {
                text.append("\n[Note: Only first $MAX_OCR_PAGES pages processed with OCR for optimal performance]")
            }
            
            ExtractionResult(
                text = text.toString().trim(),
                method = ExtractionMethod.ANDROID_RENDERER_OCR,
                pageCount = pageCount,
                hasImages = true
            )
        } catch (e: Exception) {
            Log.w(TAG, "OCR extraction failed: ${e.message}")
            ExtractionResult("", ExtractionMethod.ANDROID_RENDERER_OCR, 0)
        } finally {
            pdfRenderer?.close()
            fileDescriptor?.close()
        }
    }

    private suspend fun performOcr(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = textRecognizer.process(image).await()
            return@withContext result.text
        } catch (e: Exception) {
            Log.w(TAG, "OCR failed: ${e.message}")
            return@withContext ""
        }
    }

    private suspend fun tryHybridExtraction(file: File): ExtractionResult = withContext(Dispatchers.IO) {
        try {
            // Combine PDFBox and OCR for better results
            val pdfboxResult = tryPdfBoxExtraction(file)
            val ocrResult = tryOcrExtraction(file)
            
            val combinedText = StringBuilder()
            
            // Use PDFBox text if available
            if (pdfboxResult.text.length >= MIN_TEXT_LENGTH) {
                combinedText.append("=== TEXT EXTRACTION ===\n")
                combinedText.append(pdfboxResult.text)
            }
            
            // Add OCR text if PDFBox failed or for additional content
            if (ocrResult.text.length >= MIN_TEXT_LENGTH) {
                if (combinedText.isNotEmpty()) {
                    combinedText.append("\n\n=== OCR EXTRACTION ===\n")
                }
                combinedText.append(ocrResult.text)
            }
            
            val maxPageCount = maxOf(pdfboxResult.pageCount, ocrResult.pageCount)
            
            ExtractionResult(
                text = combinedText.toString().trim(),
                method = ExtractionMethod.HYBRID,
                pageCount = maxPageCount,
                hasImages = true
            )
        } catch (e: Exception) {
            Log.w(TAG, "Hybrid extraction failed: ${e.message}")
            ExtractionResult("", ExtractionMethod.HYBRID, 0)
        }
    }

    private suspend fun createFallbackResult(file: File): ExtractionResult = withContext(Dispatchers.IO) {
        try {
            // Get basic file info using PdfRenderer
            val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)
            val pageCount = pdfRenderer.pageCount
            pdfRenderer.close()
            fileDescriptor.close()
            
            ExtractionResult(
                text = "Unable to extract text from this PDF document. The file may be image-based or encrypted.",
                method = ExtractionMethod.HYBRID,
                pageCount = pageCount
            )
        } catch (e: Exception) {
            Log.w(TAG, "Even fallback failed: ${e.message}")
            ExtractionResult(
                text = "Error processing PDF file: ${e.message}",
                method = ExtractionMethod.HYBRID,
                pageCount = 1
            )
        }
    }

    suspend fun extractTextFromPdfWithPages(uri: Uri): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val result = extractTextWithDetails(uri).getOrThrow()
            
            // Split text into pages (approximate)
            val text = result.text
            val words = text.split("\\s+".toRegex())
            val pages = mutableListOf<String>()
            val wordsPerPage = maxOf(50, words.size / maxOf(result.pageCount, 1))
            
            for (i in words.indices step wordsPerPage) {
                val pageWords = words.subList(i, minOf(i + wordsPerPage, words.size))
                pages.add(pageWords.joinToString(" "))
            }
            
            Result.success(pages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
