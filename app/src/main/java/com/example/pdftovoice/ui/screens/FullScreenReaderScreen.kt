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
import com.example.pdftovoice.ui.components.reader.SynchronizedTextDisplay
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
    
    // Collect word tracking state for Spotify-style highlighting
    val currentWord by viewModel.currentWord.collectAsState()
    val wordIndex by viewModel.wordIndex.collectAsState()
    
    // UI visibility states
    var showControls by remember { mutableStateOf(true) }
    var showPlayerControls by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    
    // Auto-hide controls after 5 seconds of inactivity (longer for better UX)
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(5000)
            showControls = false
            showPlayerControls = false
        }
    }
    
    // Show controls when playback state changes
    LaunchedEffect(isPlaying) {
        showControls = true
        showPlayerControls = true
    }
    
    // Initialize with provided data and add debugging
    LaunchedEffect(initialExtractedText) {
        if (!initialExtractedText.isNullOrEmpty()) {
            android.util.Log.d("FullScreenReader", "Setting initial text: ${initialExtractedText.take(100)}...")
            viewModel.setExtractedText(initialExtractedText)
        }
    }
    
    // Debug current state
    LaunchedEffect(state.currentlyReadingSegment, isPlaying) {
        android.util.Log.d("FullScreenReader", "State update - Playing: $isPlaying")
        android.util.Log.d("FullScreenReader", "Current segment: '${state.currentlyReadingSegment}'")
        android.util.Log.d("FullScreenReader", "Current word: '$currentWord' (index: $wordIndex)")
        android.util.Log.d("FullScreenReader", "Text length: ${state.extractedText.length}")
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
        // Main content area with text - improved layout and spacing
        FullScreenTextContent(
            text = state.extractedText,
            currentSegment = state.currentlyReadingSegment,
            currentWord = currentWord,
            wordIndex = wordIndex,
            isPlaying = isPlaying,
            windowSizeClass = windowSizeClass,
            isCompact = isCompact,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = if (showControls) controlsHeight + 8.dp else 8.dp,
                    bottom = if (showPlayerControls) controlsHeight + 8.dp else 8.dp,
                    start = 8.dp,
                    end = 8.dp
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
        shadowElevation = 8.dp,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
                )
            }
            
            // Title with better text handling
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = if (isCompact) 16.sp else 18.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
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
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
                )
            }
        }
    }
}

@Composable
private fun FullScreenTextContent(
    text: String,
    currentSegment: String,
    currentWord: String,
    wordIndex: Int,
    isPlaying: Boolean,
    windowSizeClass: WindowSizeClass,
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
    
    // Use the enhanced SynchronizedTextDisplay with improved highlighting
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SynchronizedTextDisplay(
            text = text,
            currentlyReadingSegment = currentSegment,
            currentWord = currentWord,
            wordIndex = wordIndex,
            isPlaying = isPlaying,
            windowSizeClass = windowSizeClass,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
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
        shadowElevation = 8.dp,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // Main controls row with improved spacing
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
                
                // Play/Pause button (larger and more prominent)
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
                
                // Stop button with improved visibility
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
            
            // Status indicator with improved spacing
            if (state.extractedText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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