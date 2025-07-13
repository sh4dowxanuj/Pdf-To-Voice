package com.example.pdftovoice.ui.components.reader

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.pdftovoice.ui.components.common.AppEmptyState
import com.example.pdftovoice.ui.components.common.ReadingIndicator
import com.example.pdftovoice.ui.system.ResponsiveDimensions.cornerRadius
import com.example.pdftovoice.ui.system.ResponsiveDimensions.sectionSpacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Consolidated text display component
 * Merges functionality from EnhancedTextDisplay, SynchronizedLyricsDisplay, and TextHighlightingPanel
 */
@Composable
fun TextDisplay(
    text: String,
    currentlyReadingSegment: String,
    isPlaying: Boolean,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    onOpenFullScreen: () -> Unit = {},
    // Word-by-word highlighting parameters
    currentWord: String = "",
    wordIndex: Int = -1
) {
    val cornerRadius = windowSizeClass.cornerRadius()
    val sectionSpacing = windowSizeClass.sectionSpacing()
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header
            TextDisplayHeader(
                text = text,
                isPlaying = isPlaying,
                onOpenFullScreen = onOpenFullScreen
            )
            
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            
            // Content
            if (text.isNotBlank()) {
                TextDisplayContent(
                    text = text,
                    currentlyReadingSegment = currentlyReadingSegment,
                    isPlaying = isPlaying,
                    currentWord = currentWord,
                    wordIndex = wordIndex
                )
                
                // Currently reading preview
                if (currentlyReadingSegment.isNotBlank()) {
                    CurrentlyReadingPreview(
                        segment = currentlyReadingSegment,
                        cornerRadius = cornerRadius
                    )
                }
            } else {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AppEmptyState(
                        icon = Icons.Default.TextFields,
                        title = "No Text Available",
                        description = "Select a PDF file to extract and display text",
                        windowSizeClass = windowSizeClass
                    )
                }
            }
        }
    }
}

@Composable
private fun TextDisplayHeader(
    text: String,
    isPlaying: Boolean,
    onOpenFullScreen: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.TextFields,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        
        Text(
            text = "Extracted Text",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // Reading indicator
        if (isPlaying) {
            ReadingIndicator()
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Word count
        if (text.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${text.split("\\s+".toRegex()).size} words",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        
        // Expand button
        IconButton(
            onClick = onOpenFullScreen,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Icon(
                Icons.Default.OpenInFull,
                contentDescription = "Open in full screen",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun TextDisplayContent(
    text: String,
    currentlyReadingSegment: String,
    isPlaying: Boolean,
    currentWord: String = "",
    wordIndex: Int = -1
) {
    val scrollState = rememberScrollState()
    
    // Animated values for smooth transitions
    val currentWordScale by animateFloatAsState(
        targetValue = if (currentWord.isNotBlank() && isPlaying) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "wordScale"
    )
    
    val textAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.8f,
        animationSpec = tween(durationMillis = 300),
        label = "textAlpha"
    )
    
    SelectionContainer {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = createSpotifyStyleHighlightedText(text, currentlyReadingSegment, currentWord, wordIndex, isPlaying),
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 28.sp // Increased line height for better readability
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
                modifier = Modifier.graphicsLayer(
                    scaleX = currentWordScale,
                    scaleY = currentWordScale
                )
            )
        }
    }
}

@Composable
private fun CurrentlyReadingPreview(
    segment: String,
    cornerRadius: androidx.compose.ui.unit.Dp
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(size = cornerRadius)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.RecordVoiceOver,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = segment,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )
        }
    }
}

/**
 * Enhanced synchronized text display with Spotify-style karaoke highlighting
 */
@Composable
fun SynchronizedTextDisplay(
    text: String,
    currentlyReadingSegment: String,
    isPlaying: Boolean,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    // Word-by-word highlighting parameters
    currentWord: String = "",
    wordIndex: Int = -1
) {
    val textLines = remember(text) { 
        text.split("\n").filter { it.isNotBlank() }
    }
    
    val currentLineIndex = remember(currentlyReadingSegment, textLines) {
        if (currentlyReadingSegment.isNotBlank()) {
            textLines.indexOfFirst { line ->
                line.contains(currentlyReadingSegment, ignoreCase = true)
            }
        } else -1
    }
    
    val listState = rememberLazyListState()
    
    // Auto-scroll when current line changes
    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0) {
            listState.animateScrollToItem(
                index = maxOf(0, currentLineIndex - 1)
            )
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(textLines) { index, line ->
                val isCurrentLine = index == currentLineIndex
                
                Text(
                    text = if (isCurrentLine && isPlaying) {
                        createSpotifyStyleHighlightedText(
                            text = line,
                            currentSegment = currentlyReadingSegment,
                            currentWord = currentWord,
                            wordIndex = wordIndex,
                            isPlaying = isPlaying
                        )
                    } else {
                        buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = if (isCurrentLine) 0.9f else 0.6f
                                    ),
                                    fontSize = 16.sp,
                                    fontWeight = if (isCurrentLine) FontWeight.Medium else FontWeight.Normal
                                )
                            ) {
                                append(line)
                            }
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isCurrentLine && isPlaying) 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) 
                            else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun SynchronizedTextHeader(
    currentLineIndex: Int,
    totalLines: Int,
    isPlaying: Boolean
) {
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
        if (totalLines > 0) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = if (currentLineIndex >= 0) {
                        "${currentLineIndex + 1}/$totalLines"
                    } else {
                        "0/$totalLines"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
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
    isFutureLine: Boolean
) {
    // Animated values for smooth transitions
    val animatedTextSize by animateFloatAsState(
        targetValue = if (isCurrentLine) 18f else 16f,
        animationSpec = tween(300),
        label = "textSize"
    )
    
    val animatedBackgroundColor by animateColorAsState(
        targetValue = when {
            isCurrentLine && isPlaying -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            isCurrentLine -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            isPastLine -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else -> Color.Transparent
        },
        animationSpec = tween(300),
        label = "backgroundColor"
    )
    
    val finalTextColor = when {
        isCurrentLine -> MaterialTheme.colorScheme.onPrimaryContainer
        isPastLine -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        isFutureLine -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Highlight indicator
        AnimatedVisibility(
            visible = isCurrentLine,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(if (isCurrentLine) 32.dp else 20.dp)
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
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Text content
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

/**
 * Full screen text highlighting panel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenTextPanel(
    text: String,
    currentlyReadingSegment: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Card(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.TextFields,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Reading Text",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    
                    Divider()
                    
                    // Currently reading indicator
                    if (currentlyReadingSegment.isNotBlank()) {
                        CurrentlyReadingHeader(currentlyReadingSegment)
                    }
                    
                    // Full text content
                    SelectionContainer {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Text(
                                    text = createHighlightedText(text, currentlyReadingSegment, true),
                                    style = MaterialTheme.typography.bodyLarge,
                                    lineHeight = 28.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentlyReadingHeader(segment: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Currently Reading",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Text(
                text = segment,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// Helper function to create word-by-word highlighted text
@Composable
private fun createHighlightedText(
    text: String,
    highlightSegment: String,
    isPlaying: Boolean
): AnnotatedString {
    return createSpotifyStyleHighlightedText(text, highlightSegment, "", -1, isPlaying)
}

// Enhanced Spotify-style karaoke highlighting
@Composable 
private fun createSpotifyStyleHighlightedText(
    text: String,
    currentSegment: String,
    currentWord: String,
    wordIndex: Int,
    isPlaying: Boolean
): AnnotatedString {
    return buildAnnotatedString {
        if (currentSegment.isBlank() || !text.contains(currentSegment, ignoreCase = true)) {
            // Default text styling for non-highlighted content
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 16.sp
                )
            ) {
                append(text)
            }
            return@buildAnnotatedString
        }
        
        val segmentStartIndex = text.indexOf(currentSegment, ignoreCase = true)
        if (segmentStartIndex < 0) {
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 16.sp
                )
            ) {
                append(text)
            }
            return@buildAnnotatedString
        }
        
        // Text before the current segment (dimmed)
        withStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontSize = 15.sp
            )
        ) {
            append(text.substring(0, segmentStartIndex))
        }
        
        // Process the current segment with Spotify-style highlighting
        val segmentText = text.substring(segmentStartIndex, segmentStartIndex + currentSegment.length)
        val words = segmentText.split(Regex("\\s+")).filter { it.isNotBlank() }
        
        words.forEachIndexed { index, word ->
            if (index > 0) {
                // Space styling
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                ) {
                    append(" ")
                }
            }
            
            val isCurrentWord = isPlaying && currentWord.isNotBlank() && 
                              (word.equals(currentWord, ignoreCase = true) || 
                               word.replace(Regex("[^\\w]"), "").equals(currentWord.replace(Regex("[^\\w]"), ""), ignoreCase = true))
            
            val isCompletedWord = isPlaying && wordIndex > -1 && index < wordIndex
            
            when {
                isCurrentWord -> {
                    // Current word - Spotify-style active highlighting with glow effect
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            background = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shadow = Shadow(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                offset = Offset(0f, 2f),
                                blurRadius = 6f
                            )
                        )
                    ) {
                        append(word)
                    }
                }
                isCompletedWord -> {
                    // Previously highlighted words - completed state with subtle glow
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            shadow = Shadow(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                offset = Offset(0f, 1f),
                                blurRadius = 2f
                            )
                        )
                    ) {
                        append(word)
                    }
                }
                else -> {
                    // Upcoming words in current segment - preview state
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                    ) {
                        append(word)
                    }
                }
            }
        }
        
        // Text after the current segment (dimmed)
        val segmentEndIndex = segmentStartIndex + currentSegment.length
        withStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontSize = 15.sp
            )
        ) {
            append(text.substring(segmentEndIndex))
        }
    }
}

@Composable
private fun SpotifyStyleTextLine(
    text: String,
    isCurrentLine: Boolean,
    currentlyReadingSegment: String,
    currentWord: String,
    wordIndex: Int,
    isPlaying: Boolean,
    lineIndex: Int
) {
    // Animation for current line highlighting
    val lineScale by animateFloatAsState(
        targetValue = if (isCurrentLine && isPlaying) 1.02f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "lineScale"
    )
    
    val lineAlpha by animateFloatAsState(
        targetValue = when {
            isCurrentLine -> 1.0f
            isPlaying -> 0.6f
            else -> 0.8f
        },
        animationSpec = tween(durationMillis = 300),
        label = "lineAlpha"
    )
    
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (isCurrentLine && isPlaying) 0.1f else 0.0f,
        animationSpec = tween(durationMillis = 400),
        label = "backgroundAlpha"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = backgroundAlpha),
                RoundedCornerShape(8.dp)
            )
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .graphicsLayer(
                scaleX = lineScale,
                scaleY = lineScale,
                alpha = lineAlpha
            )
    ) {
        SelectionContainer {
            Text(
                text = if (isCurrentLine && isPlaying) {
                    createSpotifyStyleHighlightedText(
                        text = text,
                        currentSegment = currentlyReadingSegment,
                        currentWord = currentWord,
                        wordIndex = wordIndex,
                        isPlaying = isPlaying
                    )
                } else {
                    buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = if (isCurrentLine) 0.9f else 0.7f
                                ),
                                fontSize = 16.sp,
                                fontWeight = if (isCurrentLine) FontWeight.Medium else FontWeight.Normal
                            )
                        ) {
                            append(text)
                        }
                    }
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 24.sp
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SpotifyStyleProgressIndicator(
    currentWord: String,
    wordIndex: Int,
    totalWords: Int,
    isPlaying: Boolean
) {
    val progress = if (totalWords > 0 && wordIndex >= 0) {
        (wordIndex.toFloat() / totalWords.toFloat()).coerceIn(0f, 1f)
    } else 0f
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 200),
        label = "progress"
    )
    
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (isPlaying && currentWord.isNotBlank()) 1f else 0.6f,
        animationSpec = tween(durationMillis = 300),
        label = "indicatorAlpha"
    )
    
    if (isPlaying && totalWords > 0) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .graphicsLayer(alpha = indicatorAlpha)
        ) {
            // Current word display
            if (currentWord.isNotBlank()) {
                Text(
                    text = "♪ $currentWord",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            
            // Progress bar
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
            
            // Progress text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${wordIndex + 1}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "$totalWords words",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}
