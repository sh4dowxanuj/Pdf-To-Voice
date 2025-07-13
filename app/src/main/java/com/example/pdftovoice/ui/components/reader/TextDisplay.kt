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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    onOpenFullScreen: () -> Unit = {}
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
                    isPlaying = isPlaying
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
    isPlaying: Boolean
) {
    val scrollState = rememberScrollState()
    
    SelectionContainer {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = createHighlightedText(text, currentlyReadingSegment, isPlaying),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurface
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
 * Synchronized text display with line-by-line highlighting
 */
@Composable
fun SynchronizedTextDisplay(
    text: String,
    currentlyReadingSegment: String,
    isPlaying: Boolean,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier
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
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-scroll to current line
    LaunchedEffect(currentLineIndex, isPlaying) {
        if (currentLineIndex >= 0 && isPlaying && textLines.isNotEmpty()) {
            coroutineScope.launch {
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
            // Header with progress
            SynchronizedTextHeader(
                currentLineIndex = currentLineIndex,
                totalLines = textLines.size,
                isPlaying = isPlaying
            )
            
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            
            // Synchronized text content
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
                AppEmptyState(
                    icon = Icons.Default.TextFields,
                    title = "No Text Available",
                    description = "Extract text from a PDF to see synchronized reading",
                    windowSizeClass = windowSizeClass,
                    modifier = Modifier.padding(32.dp)
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

// Helper function to create highlighted text
@Composable
private fun createHighlightedText(
    text: String,
    highlightSegment: String,
    isPlaying: Boolean
): AnnotatedString {
    return buildAnnotatedString {
        if (highlightSegment.isNotBlank() && text.contains(highlightSegment, ignoreCase = true)) {
            val startIndex = text.indexOf(highlightSegment, ignoreCase = true)
            val endIndex = startIndex + highlightSegment.length
            
            // Text before highlight
            append(text.substring(0, startIndex))
            
            // Highlighted text
            withStyle(
                style = SpanStyle(
                    background = if (isPlaying) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    },
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            ) {
                append(text.substring(startIndex, endIndex))
            }
            
            // Text after highlight
            append(text.substring(endIndex))
        } else {
            append(text)
        }
    }
}
