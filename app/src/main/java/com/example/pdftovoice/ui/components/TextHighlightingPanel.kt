package com.example.pdftovoice.ui.components

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
import androidx.compose.ui.res.stringResource
import com.example.pdftovoice.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextHighlightingPanel(
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
                                    contentDescription = androidx.compose.ui.res.stringResource(id = com.example.pdftovoice.R.string.extracted_text_title),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = androidx.compose.ui.res.stringResource(id = com.example.pdftovoice.R.string.extracted_text_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(id = R.string.close)
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
                                        text = stringResource(id = R.string.currently_reading),
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
                    
                    // Main Text Content with Highlighting and Auto-scroll
                    SelectionContainer {
                        val scrollState = rememberScrollState()
                        
                        // Auto-scroll to highlighted text
                        LaunchedEffect(currentlyReadingSegment) {
                            if (currentlyReadingSegment.isNotBlank() && text.contains(currentlyReadingSegment, ignoreCase = true)) {
                                // Calculate approximate scroll position based on text position
                                val segmentPosition = text.indexOf(currentlyReadingSegment, ignoreCase = true)
                                val approximateScrollPosition = (segmentPosition.toFloat() / text.length) * scrollState.maxValue
                                scrollState.animateScrollTo(approximateScrollPosition.toInt())
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .verticalScroll(scrollState)
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
                                            contentDescription = androidx.compose.ui.res.stringResource(id = com.example.pdftovoice.R.string.no_text_available_short),
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                                Text(
                                                    text = androidx.compose.ui.res.stringResource(id = com.example.pdftovoice.R.string.no_text_available_message),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.outline,
                                                    textAlign = TextAlign.Center
                                                )
                                                Text(
                                                    text = androidx.compose.ui.res.stringResource(id = com.example.pdftovoice.R.string.select_pdf_to_extract),
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
                
                // Highlighted text
                withStyle(
                    style = SpanStyle(
                        background = Color(0xFFFFEB3B), // Bright yellow highlight
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20) // Dark green text for contrast
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
