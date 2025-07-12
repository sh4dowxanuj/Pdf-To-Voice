package com.example.pdftovoice.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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

@Composable
fun EnhancedTextDisplay(
    text: String,
    currentlyReadingSegment: String,
    isPlaying: Boolean,
    onOpenFullText: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    // Enhanced auto-scroll to highlighted text with smooth animation
    LaunchedEffect(currentlyReadingSegment, isPlaying) {
        if (currentlyReadingSegment.isNotBlank() && text.contains(currentlyReadingSegment, ignoreCase = true)) {
            // Calculate more accurate scroll position
            val segmentPosition = text.indexOf(currentlyReadingSegment, ignoreCase = true)
            val totalLines = text.count { it == '\n' } + 1
            val linesBeforeSegment = text.substring(0, segmentPosition).count { it == '\n' }
            
            // Estimate line height and calculate scroll position
            val estimatedLineHeight = 24f // Based on our typography
            val targetScrollPosition = (linesBeforeSegment * estimatedLineHeight).toInt()
            
            // Animate to the target position with smooth scrolling
            if (isPlaying) {
                scrollState.animateScrollTo(
                    value = maxOf(0, targetScrollPosition - 100), // Offset to show context
                    animationSpec = tween(durationMillis = 800, easing = EaseOutCubic)
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
            // Header with title and expand button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
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
                        text = "Extracted Text",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Reading indicator
                    if (isPlaying) {
                        ReadingIndicator()
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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
                        onClick = onOpenFullText,
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
            
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            
            // Text content
            if (text.isNotBlank()) {
                SelectionContainer {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp) // Fixed height for preview
                            .padding(16.dp)
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            text = createEnhancedHighlightedText(text, currentlyReadingSegment, isPlaying),
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                // Currently reading preview at bottom
                if (currentlyReadingSegment.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(12.dp)
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
                                text = currentlyReadingSegment,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2
                            )
                        }
                    }
                }
            } else {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.TextFields,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "No text extracted yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Select a PDF file to extract and read text",
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
private fun ReadingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "reading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
        )
        Text(
            text = "Reading",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun createEnhancedHighlightedText(
    fullText: String,
    highlightSegment: String,
    isPlaying: Boolean
): AnnotatedString {
    return buildAnnotatedString {
        if (highlightSegment.isBlank() || !fullText.contains(highlightSegment, ignoreCase = true)) {
            append(fullText)
        } else {
            val startIndex = fullText.indexOf(highlightSegment, ignoreCase = true)
            if (startIndex >= 0) {
                val endIndex = startIndex + highlightSegment.length
                
                // Text before highlight
                append(fullText.substring(0, startIndex))
                
                // Enhanced highlighted text with animation consideration
                withStyle(
                    style = SpanStyle(
                        background = if (isPlaying) {
                            Color(0xFFFFD54F) // Bright amber when playing
                        } else {
                            Color(0xFFE8F5E8) // Light green when paused
                        },
                        fontWeight = if (isPlaying) FontWeight.ExtraBold else FontWeight.Bold,
                        color = if (isPlaying) {
                            Color(0xFF1A237E) // Deep indigo when playing
                        } else {
                            Color(0xFF2E7D32) // Medium green when paused
                        },
                        letterSpacing = if (isPlaying) 0.5.sp else 0.sp // Add spacing when playing
                    )
                ) {
                    append(fullText.substring(startIndex, endIndex))
                }
                
                // Text after highlight
                append(fullText.substring(endIndex))
            } else {
                append(fullText)
            }
        }
    }
}
