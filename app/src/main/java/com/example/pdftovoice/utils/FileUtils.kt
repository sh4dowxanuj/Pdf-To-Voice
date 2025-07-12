package com.example.pdftovoice.utils

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.InputStream

object FileUtils {
    
    private const val TAG = "FileUtils"
    
    // Comprehensive list of PDF MIME types
    private val PDF_MIME_TYPES = setOf(
        "application/pdf",
        "application/x-pdf",
        "application/acrobat",
        "applications/vnd.pdf",
        "text/pdf",
        "text/x-pdf",
        "application/vnd.pdf",
        "application/pdf;charset=utf-8"
    )
    
    // PDF file signatures (magic bytes)
    private val PDF_SIGNATURES = listOf(
        byteArrayOf(0x25, 0x50, 0x44, 0x46), // %PDF
        byteArrayOf(0x50, 0x44, 0x46)        // PDF
    )
    
    fun getFileName(context: Context, uri: Uri): String? {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayName = it.getString(it.getColumnIndexOrThrow("_display_name"))
                    displayName ?: extractFileNameFromUri(uri)
                } else extractFileNameFromUri(uri)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get file name: ${e.message}")
            extractFileNameFromUri(uri)
        }
    }
    
    private fun extractFileNameFromUri(uri: Uri): String? {
        return try {
            val path = uri.path
            if (!path.isNullOrEmpty()) {
                val lastSlash = path.lastIndexOf('/')
                if (lastSlash >= 0 && lastSlash < path.length - 1) {
                    path.substring(lastSlash + 1)
                } else {
                    path
                }
            } else {
                "Unknown PDF"
            }
        } catch (e: Exception) {
            "Unknown PDF"
        }
    }
    
    fun getFileSize(context: Context, uri: Uri): Long? {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex("_size")
                    if (sizeIndex >= 0) {
                        it.getLong(sizeIndex)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get file size: ${e.message}")
            null
        }
    }
    
    fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1 -> String.format("%.1f GB", gb)
            mb >= 1 -> String.format("%.1f MB", mb)
            kb >= 1 -> String.format("%.1f KB", kb)
            else -> "$bytes B"
        }
    }
    
    fun isPdfFile(context: Context, uri: Uri): Boolean {
        // Check MIME type first
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType != null && PDF_MIME_TYPES.contains(mimeType.lowercase())) {
            return true
        }
        
        // Check file extension
        val fileName = getFileName(context, uri)?.lowercase()
        if (fileName?.endsWith(".pdf") == true) {
            return true
        }
        
        // Check file signature (magic bytes) as final verification
        return checkPdfSignature(context, uri)
    }
    
    private fun checkPdfSignature(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val buffer = ByteArray(8) // Read first 8 bytes
                val bytesRead = inputStream.read(buffer)
                
                if (bytesRead >= 4) {
                    // Check for PDF signatures
                    PDF_SIGNATURES.any { signature ->
                        buffer.sliceArray(0 until signature.size).contentEquals(signature)
                    }
                } else {
                    false
                }
            } ?: false
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check PDF signature: ${e.message}")
            false
        }
    }
    
    fun getFileExtension(context: Context, uri: Uri): String? {
        val fileName = getFileName(context, uri)
        return if (fileName != null) {
            val lastDot = fileName.lastIndexOf('.')
            if (lastDot >= 0 && lastDot < fileName.length - 1) {
                fileName.substring(lastDot + 1).lowercase()
            } else null
        } else null
    }
    
    fun getMimeType(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri)
    }
    
    fun isDocumentFile(context: Context, uri: Uri): Boolean {
        val mimeType = getMimeType(context, uri)
        return when {
            mimeType != null -> {
                PDF_MIME_TYPES.contains(mimeType.lowercase()) ||
                mimeType.startsWith("application/") ||
                mimeType.startsWith("text/")
            }
            else -> {
                val extension = getFileExtension(context, uri)
                extension in listOf("pdf", "txt", "doc", "docx")
            }
        }
    }
    
    fun validatePdfFile(context: Context, uri: Uri): ValidationResult {
        try {
            // Check if file exists and is accessible
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return ValidationResult(false, "Cannot access the selected file")
            
            inputStream.close()
            
            // Check if it's a PDF file
            if (!isPdfFile(context, uri)) {
                return ValidationResult(false, "Selected file is not a valid PDF document")
            }
            
            // Check file size (warn for very large files)
            val fileSize = getFileSize(context, uri)
            if (fileSize != null && fileSize > 100 * 1024 * 1024) { // 100MB
                return ValidationResult(
                    true, 
                    "This is a large PDF file (${formatFileSize(fileSize)}). Processing may take some time."
                )
            }
            
            return ValidationResult(true, "PDF file is valid and ready for processing")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to validate PDF file", e)
            return ValidationResult(false, "Error validating PDF file: ${e.message}")
        }
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )
}
