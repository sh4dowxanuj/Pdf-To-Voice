package com.example.pdftovoice.utils

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import kotlin.math.roundToInt

/**
 * Utility class for performance monitoring and memory management
 */
object PerformanceUtils {
    const val TAG = "PerformanceUtils"
    
    /**
     * Get current memory usage information
     */
    fun getMemoryInfo(context: Context): MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val availableMemory = maxMemory - usedMemory
        
        return MemoryInfo(
            usedMemoryMB = (usedMemory / 1024.0 / 1024.0).roundToInt(),
            maxMemoryMB = (maxMemory / 1024.0 / 1024.0).roundToInt(),
            availableMemoryMB = (availableMemory / 1024.0 / 1024.0).roundToInt(),
            systemAvailableMemoryMB = (memInfo.availMem / 1024.0 / 1024.0).roundToInt(),
            isLowMemory = memInfo.lowMemory,
            memoryUsagePercent = ((usedMemory.toDouble() / maxMemory) * 100).roundToInt()
        )
    }
    
    /**
     * Log current memory usage
     */
    fun logMemoryUsage(context: Context, tag: String = TAG) {
        val memInfo = getMemoryInfo(context)
        Log.d(tag, "Memory Usage: ${memInfo.usedMemoryMB}MB / ${memInfo.maxMemoryMB}MB (${memInfo.memoryUsagePercent}%)")
        
        if (memInfo.isLowMemory) {
            Log.w(tag, "LOW MEMORY WARNING - System memory is running low")
        }
    }
    
    /**
     * Force garbage collection and log memory before/after
     */
    fun performGarbageCollection(context: Context, tag: String = TAG) {
        val beforeMemory = getMemoryInfo(context)
        Log.d(tag, "Before GC: ${beforeMemory.usedMemoryMB}MB")
        
        System.gc()
        
        val afterMemory = getMemoryInfo(context)
        val freedMemory = beforeMemory.usedMemoryMB - afterMemory.usedMemoryMB
        Log.d(tag, "After GC: ${afterMemory.usedMemoryMB}MB (freed ${freedMemory}MB)")
    }
    
    /**
     * Check if memory usage is above threshold
     */
    fun isMemoryUsageHigh(context: Context, thresholdPercent: Int = 80): Boolean {
        val memInfo = getMemoryInfo(context)
        return memInfo.memoryUsagePercent >= thresholdPercent
    }
    
    /**
     * Measure execution time of a block
     */
    inline fun <T> measureTime(tag: String = TAG, operation: String, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val endTime = System.currentTimeMillis()
        Log.d(tag, "$operation took ${endTime - startTime}ms")
        return result
    }
}

data class MemoryInfo(
    val usedMemoryMB: Int,
    val maxMemoryMB: Int,
    val availableMemoryMB: Int,
    val systemAvailableMemoryMB: Int,
    val isLowMemory: Boolean,
    val memoryUsagePercent: Int
)
