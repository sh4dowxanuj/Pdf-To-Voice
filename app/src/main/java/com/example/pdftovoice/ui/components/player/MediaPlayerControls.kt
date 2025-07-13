package com.example.pdftovoice.ui.components.player

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pdftovoice.ui.components.common.AppIconButton
import com.example.pdftovoice.ui.components.common.ReadingIndicator
import com.example.pdftovoice.ui.system.ResponsiveDimensions.buttonSize
import com.example.pdftovoice.ui.system.ResponsiveDimensions.cornerRadius
import com.example.pdftovoice.ui.system.ResponsiveDimensions.horizontalPadding
import com.example.pdftovoice.ui.system.ResponsiveDimensions.itemSpacing
import com.example.pdftovoice.ui.system.ResponsiveDimensions.mainButtonSize
import com.example.pdftovoice.ui.system.ResponsiveDimensions.sectionSpacing
import com.example.pdftovoice.ui.system.ResponsiveLayout.isCompact
import com.example.pdftovoice.ui.system.ResponsiveLayout.shouldUseDoubleColumn

/**
 * Consolidated player controls component
 * Replaces MusicPlayerControls with cleaner, more organized code
 */
@Composable
fun MediaPlayerControls(
    windowSizeClass: WindowSizeClass,
    isPlaying: Boolean,
    isPaused: Boolean,
    currentlyReadingSegment: String,
    fileName: String?,
    speed: Float,
    pitch: Float,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onFullScreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showAdvancedControls by remember { mutableStateOf(false) }
    
    // Responsive dimensions
    val horizontalPadding = windowSizeClass.horizontalPadding()
    val buttonSize = windowSizeClass.buttonSize()
    val mainButtonSize = windowSizeClass.mainButtonSize()
    val cornerRadius = windowSizeClass.cornerRadius()
    val itemSpacing = windowSizeClass.itemSpacing()
    val sectionSpacing = windowSizeClass.sectionSpacing()
    val isCompact = windowSizeClass.isCompact()
    val shouldUseDoubleColumn = windowSizeClass.shouldUseDoubleColumn()
    
    // Animated background for playing state
    val backgroundColor by animateColorAsState(
        targetValue = if (isPlaying) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
        },
        animationSpec = tween(300),
        label = "backgroundColor"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontalPadding),
        shape = RoundedCornerShape(cornerRadius * 1.5f),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        if (shouldUseDoubleColumn && !isCompact) {
            // Wide layout - side by side
            PlayerWideLayout(
                fileName = fileName,
                currentlyReadingSegment = currentlyReadingSegment,
                isPlaying = isPlaying,
                isPaused = isPaused,
                onPlayPause = onPlayPause,
                onStop = onStop,
                speed = speed,
                pitch = pitch,
                onSpeedChange = onSpeedChange,
                onPitchChange = onPitchChange,
                showAdvancedControls = showAdvancedControls,
                onToggleAdvanced = { showAdvancedControls = !showAdvancedControls },
                onFullScreen = onFullScreen,
                windowSizeClass = windowSizeClass
            )
        } else {
            // Compact layout - stacked
            PlayerCompactLayout(
                fileName = fileName,
                currentlyReadingSegment = currentlyReadingSegment,
                isPlaying = isPlaying,
                isPaused = isPaused,
                onPlayPause = onPlayPause,
                onStop = onStop,
                speed = speed,
                pitch = pitch,
                onSpeedChange = onSpeedChange,
                onPitchChange = onPitchChange,
                showAdvancedControls = showAdvancedControls,
                onToggleAdvanced = { showAdvancedControls = !showAdvancedControls },
                onFullScreen = onFullScreen,
                windowSizeClass = windowSizeClass
            )
        }
    }
}

@Composable
private fun PlayerWideLayout(
    fileName: String?,
    currentlyReadingSegment: String,
    isPlaying: Boolean,
    isPaused: Boolean,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    speed: Float,
    pitch: Float,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    showAdvancedControls: Boolean,
    onToggleAdvanced: () -> Unit,
    onFullScreen: (() -> Unit)?,
    windowSizeClass: WindowSizeClass
) {
    val itemSpacing = windowSizeClass.itemSpacing()
    val sectionSpacing = windowSizeClass.sectionSpacing()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(sectionSpacing),
        horizontalArrangement = Arrangement.spacedBy(sectionSpacing)
    ) {
        // Left column - Info and status
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            PlayerInfo(
                fileName = fileName,
                currentlyReadingSegment = currentlyReadingSegment,
                isPlaying = isPlaying,
                isCompact = false
            )
            
            if (isPlaying) {
                ReadingIndicator()
            }
        }
        
        // Right column - Controls
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlayerControlButtons(
                isPlaying = isPlaying,
                isPaused = isPaused,
                onPlayPause = onPlayPause,
                onStop = onStop,
                showAdvancedControls = showAdvancedControls,
                onToggleAdvanced = onToggleAdvanced,
                onFullScreen = onFullScreen,
                windowSizeClass = windowSizeClass,
                isCompact = false
            )
            
            if (showAdvancedControls) {
                PlayerAdvancedControls(
                    speed = speed,
                    pitch = pitch,
                    onSpeedChange = onSpeedChange,
                    onPitchChange = onPitchChange,
                    isCompact = false
                )
            }
        }
    }
}

@Composable
private fun PlayerCompactLayout(
    fileName: String?,
    currentlyReadingSegment: String,
    isPlaying: Boolean,
    isPaused: Boolean,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    speed: Float,
    pitch: Float,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    showAdvancedControls: Boolean,
    onToggleAdvanced: () -> Unit,
    onFullScreen: (() -> Unit)?,
    windowSizeClass: WindowSizeClass
) {
    val itemSpacing = windowSizeClass.itemSpacing()
    val sectionSpacing = windowSizeClass.sectionSpacing()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(sectionSpacing),
        verticalArrangement = Arrangement.spacedBy(itemSpacing)
    ) {
        PlayerInfo(
            fileName = fileName,
            currentlyReadingSegment = currentlyReadingSegment,
            isPlaying = isPlaying,
            isCompact = true
        )
        
        if (isPlaying) {
            ReadingIndicator()
        }
        
        PlayerControlButtons(
            isPlaying = isPlaying,
            isPaused = isPaused,
            onPlayPause = onPlayPause,
            onStop = onStop,
            showAdvancedControls = showAdvancedControls,
            onToggleAdvanced = onToggleAdvanced,
            onFullScreen = onFullScreen,
            windowSizeClass = windowSizeClass,
            isCompact = true
        )
        
        if (showAdvancedControls) {
            PlayerAdvancedControls(
                speed = speed,
                pitch = pitch,
                onSpeedChange = onSpeedChange,
                onPitchChange = onPitchChange,
                isCompact = true
            )
        }
    }
}

@Composable
private fun PlayerInfo(
    fileName: String?,
    currentlyReadingSegment: String,
    isPlaying: Boolean,
    isCompact: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // File name
        fileName?.let { name ->
            Text(
                text = name,
                style = if (isCompact) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.titleLarge
                },
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Currently reading segment
        if (currentlyReadingSegment.isNotBlank()) {
            Text(
                text = currentlyReadingSegment,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isCompact) 2 else 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PlayerControlButtons(
    isPlaying: Boolean,
    isPaused: Boolean,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    showAdvancedControls: Boolean,
    onToggleAdvanced: () -> Unit,
    onFullScreen: (() -> Unit)?,
    windowSizeClass: WindowSizeClass,
    isCompact: Boolean
) {
    val buttonSize = windowSizeClass.buttonSize()
    val mainButtonSize = windowSizeClass.mainButtonSize()
    val spacing = windowSizeClass.itemSpacing()
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCompact) {
            Arrangement.spacedBy(spacing * 2, Alignment.CenterHorizontally)
        } else {
            Arrangement.spacedBy(spacing, Alignment.CenterHorizontally)
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Stop Button
        AppIconButton(
            icon = Icons.Default.Stop,
            contentDescription = "Stop",
            onClick = onStop,
            windowSizeClass = windowSizeClass,
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        )
        
        // Play/Pause Button (Main)
        AppIconButton(
            icon = when {
                isPlaying -> Icons.Default.Pause
                else -> Icons.Default.PlayArrow
            },
            contentDescription = when {
                isPlaying -> "Pause"
                isPaused -> "Resume"
                else -> "Play"
            },
            onClick = onPlayPause,
            windowSizeClass = windowSizeClass,
            modifier = Modifier.size(mainButtonSize),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
        
        // Settings Button
        AppIconButton(
            icon = if (showAdvancedControls) Icons.Default.ExpandLess else Icons.Default.Settings,
            contentDescription = "Settings",
            onClick = onToggleAdvanced,
            windowSizeClass = windowSizeClass,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        )
        
        // Fullscreen Button (only show if callback is provided)
        if (onFullScreen != null) {
            AppIconButton(
                icon = Icons.Default.Fullscreen,
                contentDescription = "Full Screen",
                onClick = onFullScreen,
                windowSizeClass = windowSizeClass,
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        }
    }
}

@Composable
private fun PlayerAdvancedControls(
    speed: Float,
    pitch: Float,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    isCompact: Boolean
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isCompact) {
                // Compact layout - stacked controls
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlayerControlSlider(
                        label = "Speed",
                        value = speed,
                        valueRange = 0.5f..2.0f,
                        onValueChange = onSpeedChange,
                        valueDisplay = "${String.format("%.1f", speed)}x"
                    )
                    
                    PlayerControlSlider(
                        label = "Pitch",
                        value = pitch,
                        valueRange = 0.5f..2.0f,
                        onValueChange = onPitchChange,
                        valueDisplay = "${String.format("%.1f", pitch)}x"
                    )
                }
            } else {
                // Wide layout - side by side controls
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PlayerControlSlider(
                        label = "Speed",
                        value = speed,
                        valueRange = 0.5f..2.0f,
                        onValueChange = onSpeedChange,
                        valueDisplay = "${String.format("%.1f", speed)}x",
                        modifier = Modifier.weight(1f)
                    )
                    
                    PlayerControlSlider(
                        label = "Pitch",
                        value = pitch,
                        valueRange = 0.5f..2.0f,
                        onValueChange = onPitchChange,
                        valueDisplay = "${String.format("%.1f", pitch)}x",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerControlSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueDisplay: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueDisplay,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}
