package com.example.pdftovoice.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.pdftovoice.R
import androidx.compose.ui.unit.sp
import com.example.pdftovoice.ui.responsive.ResponsiveDimensions.horizontalPadding
import com.example.pdftovoice.ui.responsive.ResponsiveDimensions.verticalPadding
import com.example.pdftovoice.ui.responsive.ResponsiveDimensions.buttonSize
import com.example.pdftovoice.ui.responsive.ResponsiveDimensions.mainButtonSize
import com.example.pdftovoice.ui.responsive.ResponsiveDimensions.cornerRadius
import com.example.pdftovoice.ui.responsive.ResponsiveDimensions.itemSpacing
import com.example.pdftovoice.ui.responsive.ResponsiveDimensions.sectionSpacing
import com.example.pdftovoice.ui.responsive.ResponsiveLayout.shouldUseDoubleColumn
import com.example.pdftovoice.ui.responsive.ResponsiveLayout.isCompact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerControls(
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
    modifier: Modifier = Modifier
) {
    var showAdvancedControls by remember { mutableStateOf(false) }
    
    // Responsive dimensions
    val horizontalPadding = windowSizeClass.horizontalPadding()
    // Removed unused vertical padding to reduce warnings
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
    
    // Pulsing animation for playing state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
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
                    NowPlayingInfo(
                        fileName = fileName,
                        currentlyReadingSegment = currentlyReadingSegment,
                        isPlaying = isPlaying,
                        isCompact = false
                    )
                    
                    if (isPlaying) {
                        PlayingStatusIndicator(pulseAlpha = pulseAlpha)
                    }
                }
                
                // Right column - Controls
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(itemSpacing),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MainControlButtons(
                        isPlaying = isPlaying,
                        isPaused = isPaused,
                        onPlayPause = onPlayPause,
                        onStop = onStop,
                        showAdvancedControls = showAdvancedControls,
                        onToggleAdvanced = { showAdvancedControls = !showAdvancedControls },
                        buttonSize = buttonSize,
                        mainButtonSize = mainButtonSize,
                        spacing = itemSpacing,
                        isCompact = false
                    )
                    
                    if (showAdvancedControls) {
                        AdvancedControls(
                            speed = speed,
                            pitch = pitch,
                            onSpeedChange = onSpeedChange,
                            onPitchChange = onPitchChange,
                            isCompact = false
                        )
                    }
                }
            }
        } else {
            // Compact layout - stacked
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(sectionSpacing),
                verticalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
                NowPlayingInfo(
                    fileName = fileName,
                    currentlyReadingSegment = currentlyReadingSegment,
                    isPlaying = isPlaying,
                    isCompact = true
                )
                
                if (isPlaying) {
                    PlayingStatusIndicator(pulseAlpha = pulseAlpha)
                }
                
                MainControlButtons(
                    isPlaying = isPlaying,
                    isPaused = isPaused,
                    onPlayPause = onPlayPause,
                    onStop = onStop,
                    showAdvancedControls = showAdvancedControls,
                    onToggleAdvanced = { showAdvancedControls = !showAdvancedControls },
                    buttonSize = buttonSize,
                    mainButtonSize = mainButtonSize,
                    spacing = itemSpacing,
                    isCompact = true
                )
                
                if (showAdvancedControls) {
                    AdvancedControls(
                        speed = speed,
                        pitch = pitch,
                        onSpeedChange = onSpeedChange,
                        onPitchChange = onPitchChange,
                        isCompact = true
                    )
                }
            }
        }
    }
}

@Composable
private fun NowPlayingInfo(
    fileName: String?,
    currentlyReadingSegment: String,
    isPlaying: Boolean,
    isCompact: Boolean
) {
    // Now Playing Info
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = fileName ?: stringResource(id = R.string.no_file_selected),
                style = if (isCompact) {
                    MaterialTheme.typography.titleSmall
                } else {
                    MaterialTheme.typography.titleMedium
                },
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (isCompact) 1 else 2,
                overflow = TextOverflow.Ellipsis
            )
            
            if (currentlyReadingSegment.isNotBlank()) {
                Text(
                    text = currentlyReadingSegment,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = if (isCompact) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = if (isPlaying) stringResource(id = R.string.playing) else stringResource(id = R.string.ready_to_play),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun PlayingStatusIndicator(pulseAlpha: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha)
                )
        )
        Text(
            text = stringResource(id = R.string.reading_aloud),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MainControlButtons(
    isPlaying: Boolean,
    isPaused: Boolean,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    showAdvancedControls: Boolean,
    onToggleAdvanced: () -> Unit,
    buttonSize: androidx.compose.ui.unit.Dp,
    mainButtonSize: androidx.compose.ui.unit.Dp,
    spacing: androidx.compose.ui.unit.Dp,
    isCompact: Boolean
) {
    // Main Control Buttons
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
        PlayerControlButton(
            icon = Icons.Default.Stop,
            contentDescription = stringResource(id = R.string.stop),
            onClick = onStop,
            size = buttonSize,
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        )
        
        // Play/Pause Button (Main)
        PlayerControlButton(
            icon = when {
                isPlaying -> Icons.Default.Pause
                else -> Icons.Default.PlayArrow
            },
            contentDescription = when {
                isPlaying -> stringResource(id = R.string.pause)
                isPaused -> stringResource(id = R.string.resume)
                else -> stringResource(id = R.string.play)
            },
            onClick = onPlayPause,
            size = mainButtonSize,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            iconSize = if (isCompact) 28.dp else 36.dp
        )
        
        // Settings Button
        PlayerControlButton(
            icon = if (showAdvancedControls) Icons.Default.ExpandLess else Icons.Default.Settings,
            contentDescription = stringResource(id = R.string.reading_settings),
            onClick = onToggleAdvanced,
            size = buttonSize,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        )
    }
}

@Composable
private fun AdvancedControls(
    speed: Float,
    pitch: Float,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    isCompact: Boolean
) {
    // Advanced Controls (Expandable)
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
                    // Speed Control
                    ControlSlider(
                        label = stringResource(id = R.string.speed),
                        value = speed,
                        valueRange = 0.5f..2.0f,
                        onValueChange = onSpeedChange,
                        valueDisplay = "${String.format("%.1f", speed)}x"
                    )
                    
                    // Pitch Control
                    ControlSlider(
                        label = stringResource(id = R.string.pitch),
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
                    // Speed Control
                    ControlSlider(
                        label = stringResource(id = R.string.speed),
                        value = speed,
                        valueRange = 0.5f..2.0f,
                        onValueChange = onSpeedChange,
                        valueDisplay = "${String.format("%.1f", speed)}x",
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Pitch Control
                    ControlSlider(
                        label = stringResource(id = R.string.pitch),
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
private fun ControlSlider(
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

@Composable
private fun PlayerControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = size * 0.6f
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(size),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize)
        )
    }
}
