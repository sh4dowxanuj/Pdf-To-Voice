package com.example.pdftovoice.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextHighlightingPanel(
    text: String,
    currentlyReadingSegment: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    // Enhanced auto-scroll for full-screen panel
    LaunchedEffect(currentlyReadingSegment) {
        if (currentlyReadingSegment.isNotBlank() && text.contains(currentlyReadingSegment, ignoreCase = true)) {
            val segmentPosition = text.indexOf(currentlyReadingSegment, ignoreCase = true)
            val totalLines = text.count { it == '\n' } + 1
            val linesBeforeSegment = text.substring(0, segmentPosition).count { it == '\n' }
            
            // Better estimation for full-screen view
            val estimatedLineHeight = 32f // Larger text in full screen
            val targetScrollPosition = (linesBeforeSegment * estimatedLineHeight).toInt()
            
            // Smooth scroll animation
            scrollState.animateScrollTo(
                value = maxOf(0, targetScrollPosition - 200), // More context in full screen
                animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic)
            )
        }
    }
    
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
                    
                    // Currently Reading Indicator
                    if (currentlyReadingSegment.isNotBlank()) {
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
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                    )
                                    Text(
                                        text = "Currently Reading",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    text = currentlyReadingSegment,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                    
                    // Main Text Content with Highlighting
                    SelectionContainer {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .verticalScroll(scrollState) // Use the scrollState with auto-scroll
                        ) {
                            if (text.isNotBlank()) {
                                Text(
                                    text = createHighlightedText(text, currentlyReadingSegment),
                                    style = MaterialTheme.typography.bodyLarge,
                                    lineHeight = 28.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
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
                                            text = "No text to display",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.outline,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Select a PDF file to view its text content",
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
            }
        }
    }
}

@Composable
private fun createHighlightedText(
    fullText: String,
    highlightSegment: String
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
                
                // Enhanced highlighted text with better contrast
                withStyle(
                    style = SpanStyle(
                        background = Color(0xFFFFD54F), // Bright amber highlight
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1A237E), // Deep indigo text for maximum contrast
                        letterSpacing = 0.5.sp // Slight letter spacing for readability
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
