package com.example.pdftovoice.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SynchronizedLyricsDisplay(
    text: String,
    currentlyReadingSegment: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    // Split text into lines/sentences for synchronized display - matching demo segmentation
    val textLines = remember(text) {
        if (text.isBlank()) emptyList()
        else {
            // Split by sentences (periods, exclamation marks, question marks)
            // and clean up empty lines and extra whitespace
            text.split(Regex("[.!?]"))
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { line ->
                    // Add back the punctuation for display consistency
                    if (!line.matches(Regex(".*[.!?]$"))) {
                        when {
                            text.contains("$line!") -> "$line!"
                            text.contains("$line?") -> "$line?"
                            else -> "$line."
                        }
                    } else line
                }
        }
    }
    
    // Find current line index with improved matching logic
    val currentLineIndex = remember(currentlyReadingSegment, textLines) {
        if (currentlyReadingSegment.isBlank() || textLines.isEmpty()) -1
        else {
            // Clean the segment for matching (remove punctuation)
            val cleanSegment = currentlyReadingSegment.replace(Regex("[.!?]"), "").trim()
            
            // First try exact contains match (line contains segment)
            var foundIndex = textLines.indexOfFirst { line ->
                val cleanLine = line.replace(Regex("[.!?]"), "").trim()
                cleanLine.contains(cleanSegment, ignoreCase = true)
            }
            
            // If not found, try reverse match (segment contains line)
            if (foundIndex == -1) {
                foundIndex = textLines.indexOfFirst { line ->
                    val cleanLine = line.replace(Regex("[.!?]"), "").trim()
                    cleanSegment.contains(cleanLine, ignoreCase = true)
                }
            }
            
            // If still not found, try word-based matching
            if (foundIndex == -1) {
                val segmentWords = cleanSegment.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
                foundIndex = textLines.indexOfFirst { line ->
                    val cleanLine = line.replace(Regex("[.!?]"), "").trim()
                    val lineWords = cleanLine.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
                    
                    // Check if the first few words match
                    if (segmentWords.size >= 3 && lineWords.size >= 3) {
                        segmentWords.take(3) == lineWords.take(3)
                    } else {
                        // For shorter segments, require at least 50% word overlap
                        val matchCount = segmentWords.count { segmentWord ->
                            lineWords.any { lineWord ->
                                segmentWord == lineWord || 
                                segmentWord.contains(lineWord) || 
                                lineWord.contains(segmentWord)
                            }
                        }
                        matchCount >= maxOf(1, segmentWords.size / 2)
                    }
                }
            }
            
            foundIndex
        }
    }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-scroll to current line when playing
    LaunchedEffect(currentLineIndex, isPlaying) {
        if (currentLineIndex >= 0 && isPlaying && textLines.isNotEmpty()) {
            coroutineScope.launch {
                // Scroll to center the current line
                val targetIndex = (currentLineIndex - 2).coerceAtLeast(0)
                listState.animateScrollToItem(
                    index = targetIndex,
                    scrollOffset = 0
                )
            }
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.TextFields,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Reading Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Enhanced Progress indicator - Larger and more prominent
                if (textLines.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = if (currentLineIndex >= 0) {
                                "${currentLineIndex + 1}/${textLines.size}"
                            } else {
                                "0/${textLines.size}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            
            // Synchronized lyrics display
            if (textLines.isNotEmpty()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(textLines) { index, line ->
                        SynchronizedTextLine(
                            text = line,
                            isCurrentLine = index == currentLineIndex,
                            isPlaying = isPlaying,
                            isPastLine = index < currentLineIndex,
                            isFutureLine = index > currentLineIndex
                        )
                    }
                }
            } else {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.TextFields,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "No text available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Select a PDF file to start reading",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SynchronizedTextLine(
    text: String,
    isCurrentLine: Boolean,
    isPlaying: Boolean,
    isPastLine: Boolean,
    isFutureLine: Boolean,
    modifier: Modifier = Modifier
) {
    // Enhanced animated background for current line - more prominent
    val animatedBackgroundColor by animateColorAsState(
        targetValue = when {
            isCurrentLine && isPlaying -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            isCurrentLine && !isPlaying -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
            isPastLine -> MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 300, easing = EaseInOutCubic),
        label = "background_color"
    )
    
    // Enhanced text color with better contrast
    val animatedTextColor by animateColorAsState(
        targetValue = when {
            isCurrentLine && isPlaying -> MaterialTheme.colorScheme.primary
            isCurrentLine -> MaterialTheme.colorScheme.secondary
            isPastLine -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            isFutureLine -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        },
        animationSpec = tween(durationMillis = 300, easing = EaseInOutCubic),
        label = "text_color"
    )
    
    val animatedTextSize by animateFloatAsState(
        targetValue = if (isCurrentLine) 20f else 16f, // Larger current text
        animationSpec = tween(durationMillis = 300, easing = EaseInOutCubic),
        label = "text_size"
    )
    
    // Enhanced pulsing effect for current line when playing
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    
    val finalTextColor = if (isCurrentLine && isPlaying) {
        animatedTextColor.copy(alpha = pulseAlpha)
    } else {
        animatedTextColor
    }
    
    // Spotify-style highlight bar for current line
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Enhanced highlight bar (like Spotify's current line indicator)
        AnimatedVisibility(
            visible = isCurrentLine,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(300, easing = EaseOutCubic)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(300, easing = EaseInCubic)
            ) + fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp) // Wider highlight bar
                    .height(if (isCurrentLine) 32.dp else 20.dp) // Taller highlight bar
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isPlaying) {
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    MaterialTheme.colorScheme.secondary
                                )
                            } else {
                                listOf(
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                                )
                            }
                        )
                    )
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp)) // More spacing
        
        // Enhanced text content with better background and padding
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(animatedBackgroundColor)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = animatedTextSize.sp,
                    fontWeight = if (isCurrentLine) FontWeight.Bold else FontWeight.Normal,
                    lineHeight = (animatedTextSize * 1.5f).sp
                ),
                color = finalTextColor,
                textAlign = TextAlign.Start
            )
        }
    }
}
