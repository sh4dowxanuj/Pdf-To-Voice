package com.example.pdftovoice.pdf

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfTypeDetector(private val context: Context) {
    
    companion object {
        private const val TAG = "PdfTypeDetector"
        private const val TEMP_FILE_PREFIX = "pdf_detect_"
        private const val BUFFER_SIZE = 8192
    }
    
    data class PdfInfo(
        val isValid: Boolean,
        val pageCount: Int,
        val isPasswordProtected: Boolean,
        val isEncrypted: Boolean,
        val hasSelectableText: Boolean,
        val isImagePdf: Boolean,
        val fileSize: Long,
        val version: String?,
        val supportedMethods: List<ExtractionMethod>,
        val errorMessage: String? = null
    )
    
    enum class ExtractionMethod {
        PDFBOX_ANDROID,
        ANDROID_RENDERER_OCR,
        FALLBACK_SAMPLE
    }
    
    suspend fun analyzePdf(uri: Uri): PdfInfo = withContext(Dispatchers.IO) {
        val tempFile = createTempFile(uri)
        
        try {
            val fileSize = tempFile.length()
            val supportedMethods = mutableListOf<ExtractionMethod>()
            var pageCount = 0
            var isPasswordProtected = false
            var isEncrypted = false
            var hasSelectableText = false
            var isImagePdf = false
            var version: String? = null
            
            // Test PDFBox compatibility first
            try {
                val document = PDDocument.load(tempFile)
                pageCount = document.numberOfPages
                
                // Check for text content using PDFBox
                val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
                stripper.startPage = 1
                stripper.endPage = minOf(3, pageCount)
                val text = stripper.getText(document)
                if (text.trim().length > 10) {
                    hasSelectableText = true
                }
                
                document.close()
                supportedMethods.add(ExtractionMethod.PDFBOX_ANDROID)
                Log.d(TAG, "PDFBox compatible")
            } catch (e: Exception) {
                when {
                    e.message?.contains("password", ignoreCase = true) == true -> {
                        isPasswordProtected = true
                    }
                    e.message?.contains("encrypt", ignoreCase = true) == true -> {
                        isEncrypted = true
                    }
                }
                Log.w(TAG, "PDFBox not compatible: ${e.message}")
            }
            
            // Test Android PdfRenderer (for OCR potential)
            try {
                val fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val pdfRenderer = PdfRenderer(fileDescriptor)
                if (pageCount == 0) pageCount = pdfRenderer.pageCount
                
                // If no text found but renderer works, it's likely an image PDF
                if (!hasSelectableText && pageCount > 0) {
                    isImagePdf = true
                }
                
                pdfRenderer.close()
                fileDescriptor.close()
                supportedMethods.add(ExtractionMethod.ANDROID_RENDERER_OCR)
                Log.d(TAG, "Android PdfRenderer compatible")
            } catch (e: Exception) {
                Log.w(TAG, "Android PdfRenderer not compatible: ${e.message}")
            }
            
            // Fallback is always available
            supportedMethods.add(ExtractionMethod.FALLBACK_SAMPLE)
            
            PdfInfo(
                isValid = pageCount > 0,
                pageCount = pageCount,
                isPasswordProtected = isPasswordProtected,
                isEncrypted = isEncrypted,
                hasSelectableText = hasSelectableText,
                isImagePdf = isImagePdf,
                fileSize = fileSize,
                version = version,
                supportedMethods = supportedMethods
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to analyze PDF", e)
            PdfInfo(
                isValid = false,
                pageCount = 0,
                isPasswordProtected = false,
                isEncrypted = false,
                hasSelectableText = false,
                isImagePdf = false,
                fileSize = 0,
                version = null,
                supportedMethods = listOf(ExtractionMethod.FALLBACK_SAMPLE),
                errorMessage = e.message
            )
        } finally {
            tempFile.delete()
        }
    }
    
    private suspend fun createTempFile(uri: Uri): File = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Could not open PDF file")
        
        val tempFile = File(context.cacheDir, "$TEMP_FILE_PREFIX${System.currentTimeMillis()}.pdf")
        val outputStream = FileOutputStream(tempFile)
        
        try {
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.flush()
        } finally {
            inputStream.close()
            outputStream.close()
        }
        
        tempFile
    }
    
    fun getSupportedFileTypes(): List<String> {
        return listOf(
            "application/pdf",
            "application/x-pdf",
            "application/acrobat",
            "applications/vnd.pdf",
            "text/pdf",
            "text/x-pdf"
        )
    }
    
    fun getRecommendedMethod(pdfInfo: PdfInfo): ExtractionMethod {
        return when {
            pdfInfo.isPasswordProtected || pdfInfo.isEncrypted -> {
                ExtractionMethod.FALLBACK_SAMPLE
            }
            pdfInfo.hasSelectableText && pdfInfo.supportedMethods.contains(ExtractionMethod.PDFBOX_ANDROID) -> {
                ExtractionMethod.PDFBOX_ANDROID
            }
            pdfInfo.isImagePdf && pdfInfo.supportedMethods.contains(ExtractionMethod.ANDROID_RENDERER_OCR) -> {
                ExtractionMethod.ANDROID_RENDERER_OCR
            }
            else -> {
                ExtractionMethod.FALLBACK_SAMPLE
            }
        }
    }
}
