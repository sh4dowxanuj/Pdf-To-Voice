package com.example.pdftovoice.ui.screens

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pdftovoice.ui.components.common.ReadingIndicator
import com.example.pdftovoice.ui.system.ResponsiveDimensions.horizontalPadding
import com.example.pdftovoice.ui.system.ResponsiveLayout.isCompact
import com.example.pdftovoice.viewmodel.PdfToVoiceViewModel
import com.example.pdftovoice.viewmodel.PdfToVoiceState
import kotlinx.coroutines.delay

/**
 * Full-screen immersive reader that combines media player UI with text highlighting
 * 
 * Features:
 * - Media player-style controls (play/pause/stop/speed/pitch)
 * - Synchronized text highlighting
 * - Auto-scroll to current reading position
 * - Immersive full-screen experience
 * - Gesture controls for showing/hiding UI
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FullScreenReaderScreen(
    windowSizeClass: WindowSizeClass,
    viewModel: PdfToVoiceViewModel = viewModel(),
    initialPdfUri: String? = null,
    initialPdfName: String? = null,
    initialExtractedText: String? = null,
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val state by viewModel.combinedState.collectAsState(initial = PdfToVoiceState())
    val isPlaying by viewModel.isPlaying.collectAsState()
    
    // UI visibility states
    var showControls by remember { mutableStateOf(true) }
    var showPlayerControls by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    
    // Auto-hide controls after 3 seconds of inactivity
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3000)
            showControls = false
        }
    }
    
    // Initialize with provided data
    LaunchedEffect(initialExtractedText) {
        if (!initialExtractedText.isNullOrEmpty()) {
            // Set the extracted text in the viewmodel if provided
            viewModel.setExtractedText(initialExtractedText)
        }
    }
    
    // Responsive dimensions
    val isCompact = windowSizeClass.isCompact()
    val horizontalPadding = windowSizeClass.horizontalPadding()
    val controlsHeight = if (isCompact) 80.dp else 100.dp
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        // Toggle controls visibility on tap
                        showControls = !showControls
                    }
                )
            }
    ) {
        // Main content area with text
        FullScreenTextContent(
            text = state.extractedText,
            currentSegment = state.currentlyReadingSegment,
            isPlaying = isPlaying,
            isCompact = isCompact,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = if (showControls) controlsHeight else 0.dp,
                    bottom = if (showPlayerControls) controlsHeight else 0.dp
                )
        )
        
        // Top controls (close button, title, settings)
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(300)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(300)
            ) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(10f)
        ) {
            TopControlsBar(
                title = initialPdfName ?: "PDF Reader",
                isCompact = isCompact,
                onClose = onClose,
                onSettings = { showSettings = true },
                modifier = Modifier.height(controlsHeight)
            )
        }
        
        // Bottom media player controls
        AnimatedVisibility(
            visible = showPlayerControls,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(10f)
        ) {
            MediaPlayerBottomControls(
                viewModel = viewModel,
                state = state,
                isCompact = isCompact,
                modifier = Modifier.height(controlsHeight)
            )
        }
        
        // Settings dialog
        if (showSettings) {
            FullScreenSettingsDialog(
                viewModel = viewModel,
                onDismiss = { showSettings = false }
            )
        }
        
        // Loading indicator
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = state.processingStatus ?: "Loading...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun TopControlsBar(
    title: String,
    isCompact: Boolean,
    onClose: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(if (isCompact) 40.dp else 48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = if (isCompact) 16.sp else 18.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            
            // Settings button
            IconButton(
                onClick = onSettings,
                modifier = Modifier.size(if (isCompact) 40.dp else 48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun FullScreenTextContent(
    text: String,
    currentSegment: String,
    isPlaying: Boolean,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    if (text.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No text available",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        return
    }
    
    // Split text into sentences for highlighting - match TTS manager segmentation
    val sentences = remember(text) {
        text.split(Regex("(?<=[.!?])\\s+"))
            .filter { it.isNotBlank() }
            .map { it.trim() }
            .map { segment ->
                // Ensure segment ends with punctuation like TTS manager does
                if (!segment.matches(Regex(".*[.!?]$"))) "$segment." else segment
            }
    }
    
    val listState = rememberLazyListState()
    
    // Debug logging for current segment changes
    LaunchedEffect(currentSegment) {
        if (currentSegment.isNotEmpty()) {
            android.util.Log.d("FullScreenReader", "Current segment updated: '$currentSegment'")
            android.util.Log.d("FullScreenReader", "Total sentences: ${sentences.size}")
        }
    }
    
    // Auto-scroll to current reading position
    LaunchedEffect(currentSegment, isPlaying) {
        if (currentSegment.isNotEmpty() && isPlaying) {
            val index = sentences.indexOfFirst { sentence ->
                // First try exact match
                sentence.equals(currentSegment, ignoreCase = true) ||
                // Then try normalized match (remove punctuation)
                sentence.replace(Regex("[.!?]+$"), "").equals(currentSegment.replace(Regex("[.!?]+$"), ""), ignoreCase = true) ||
                // Then try containment
                sentence.contains(currentSegment, ignoreCase = true) ||
                currentSegment.contains(sentence, ignoreCase = true) ||
                // Fallback: word overlap (for better matching with TTS segments)
                hasSignificantWordOverlap(sentence, currentSegment)
            }
            if (index >= 0) {
                android.util.Log.d("FullScreenReader", "Scrolling to sentence $index: '${sentences[index]}'")
                // Scroll to show current item with some context
                val targetIndex = (index - 1).coerceAtLeast(0)
                listState.animateScrollToItem(targetIndex)
            } else {
                android.util.Log.d("FullScreenReader", "No matching sentence found for segment: '$currentSegment'")
            }
        }
    }
    
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = if (isCompact) 16.dp else 24.dp,
            vertical = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 16.dp)
    ) {
        items(sentences) { sentence ->
            val isCurrentlyReading = isPlaying && currentSegment.isNotEmpty() && (
                // Exact match
                sentence.equals(currentSegment, ignoreCase = true) ||
                // Normalized match (remove punctuation)
                sentence.replace(Regex("[.!?]+$"), "").equals(currentSegment.replace(Regex("[.!?]+$"), ""), ignoreCase = true) ||
                // Containment match
                sentence.contains(currentSegment, ignoreCase = true) ||
                currentSegment.contains(sentence, ignoreCase = true) ||
                // Word overlap match
                hasSignificantWordOverlap(sentence, currentSegment)
            )
            
            HighlightedTextSentence(
                text = sentence,
                isCurrentlyReading = isCurrentlyReading,
                isCompact = isCompact
            )
        }
    }
}

/**
 * Helper function to check if two text segments have significant word overlap
 * This helps match TTS segments with UI text segments when they're split differently
 */
private fun hasSignificantWordOverlap(sentence: String, segment: String): Boolean {
    val sentenceWords = sentence.lowercase().split(Regex("\\s+")).filter { it.length > 2 }
    val segmentWords = segment.lowercase().split(Regex("\\s+")).filter { it.length > 2 }
    
    if (sentenceWords.isEmpty() || segmentWords.isEmpty()) return false
    
    val commonWords = sentenceWords.intersect(segmentWords.toSet())
    val overlapPercentage = commonWords.size.toFloat() / minOf(sentenceWords.size, segmentWords.size)
    
    return overlapPercentage >= 0.5f // At least 50% word overlap
}

@Composable
private fun HighlightedTextSentence(
    text: String,
    isCurrentlyReading: Boolean,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isCurrentlyReading) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(300, easing = EaseInOut), 
        label = "backgroundColor"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (isCurrentlyReading) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onBackground
        },
        animationSpec = tween(300, easing = EaseInOut), 
        label = "textColor"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isCurrentlyReading) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        animationSpec = tween(300, easing = EaseInOut), 
        label = "borderColor"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isCurrentlyReading) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(
                horizontal = if (isCurrentlyReading) 16.dp else 12.dp,
                vertical = if (isCurrentlyReading) 12.dp else 8.dp
            )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = if (isCompact) 16.sp else 18.sp,
                lineHeight = if (isCompact) 24.sp else 28.sp,
                fontWeight = if (isCurrentlyReading) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = textColor
        )
    }
}

@Composable
private fun MediaPlayerBottomControls(
    viewModel: PdfToVoiceViewModel,
    state: com.example.pdftovoice.viewmodel.PdfToVoiceState,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // Main controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Previous/Skip backward
                IconButton(
                    onClick = { /* TODO: Implement previous segment */ },
                    modifier = Modifier.size(if (isCompact) 48.dp else 56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(if (isCompact) 24.dp else 28.dp)
                    )
                }
                
                // Play/Pause button (larger)
                FloatingActionButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier.size(if (isCompact) 64.dp else 72.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                ) {
                    Icon(
                        imageVector = if (viewModel.isPlaying()) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (viewModel.isPlaying()) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(if (isCompact) 32.dp else 36.dp)
                    )
                }
                
                // Next/Skip forward
                IconButton(
                    onClick = { /* TODO: Implement next segment */ },
                    modifier = Modifier.size(if (isCompact) 48.dp else 56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(if (isCompact) 24.dp else 28.dp)
                    )
                }
                
                // Stop button
                IconButton(
                    onClick = { viewModel.stopReading() },
                    modifier = Modifier.size(if (isCompact) 48.dp else 56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(if (isCompact) 24.dp else 28.dp)
                    )
                }
            }
            
            // Speed and progress indicator row
            if (state.extractedText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reading indicator
                    if (viewModel.isPlaying()) {
                        ReadingIndicator()
                    } else {
                        Text(
                            text = "Ready to play",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FullScreenSettingsDialog(
    viewModel: PdfToVoiceViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Reading Settings")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Speed control
                Column {
                    Text(
                        text = "Speed: ${String.format("%.1f", viewModel.getSpeed())}x",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = viewModel.getSpeed(),
                        onValueChange = { viewModel.setSpeed(it) },
                        valueRange = 0.5f..2.0f,
                        steps = 6
                    )
                }
                
                // Pitch control
                Column {
                    Text(
                        text = "Pitch: ${String.format("%.1f", viewModel.getPitch())}x",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = viewModel.getPitch(),
                        onValueChange = { viewModel.setPitch(it) },
                        valueRange = 0.5f..2.0f,
                        steps = 6
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}