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
import kotlinx.coroutines.launch

@Composable
fun SynchronizedLyricsDisplay(
    text: String,
    currentlyReadingSegment: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    // Split text into lines/sentences for synchronized display
    val textLines = remember(text) {
        if (text.isBlank()) emptyList()
        else text.split(Regex("(?<=[.!?])\\s+|\\n"))
            .filter { it.isNotBlank() }
            .map { it.trim() }
    }
    
    // Find current line index
    val currentLineIndex = remember(currentlyReadingSegment, textLines) {
        if (currentlyReadingSegment.isBlank()) -1
        else {
            textLines.indexOfFirst { line ->
                line.contains(currentlyReadingSegment, ignoreCase = true) ||
                currentlyReadingSegment.contains(line, ignoreCase = true)
            }
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
                
                // Progress indicator
                if (textLines.isNotEmpty() && currentLineIndex >= 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${currentLineIndex + 1}/${textLines.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
    // Animated background for current line
    val animatedBackgroundColor by animateColorAsState(
        targetValue = when {
            isCurrentLine && isPlaying -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            isCurrentLine && !isPlaying -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 300, easing = EaseInOutCubic),
        label = "background_color"
    )
    
    // Animated text color and size with future line consideration
    val animatedTextColor by animateColorAsState(
        targetValue = when {
            isCurrentLine -> MaterialTheme.colorScheme.primary
            isPastLine -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            isFutureLine -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        },
        animationSpec = tween(durationMillis = 300, easing = EaseInOutCubic),
        label = "text_color"
    )
    
    val animatedTextSize by animateFloatAsState(
        targetValue = if (isCurrentLine) 18f else 16f,
        animationSpec = tween(durationMillis = 300, easing = EaseInOutCubic),
        label = "text_size"
    )
    
    // Pulsing effect for current line when playing
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
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
        // Highlight bar (like Spotify's current line indicator)
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
                    .width(4.dp)
                    .height(if (isCurrentLine) 24.dp else 16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Text content with background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(animatedBackgroundColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = animatedTextSize.sp,
                    fontWeight = if (isCurrentLine) FontWeight.SemiBold else FontWeight.Normal,
                    lineHeight = (animatedTextSize * 1.4f).sp
                ),
                color = finalTextColor,
                textAlign = TextAlign.Start
            )
        }
    }
}
