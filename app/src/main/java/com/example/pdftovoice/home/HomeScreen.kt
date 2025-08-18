package com.example.pdftovoice.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.pdftovoice.R
import com.example.pdftovoice.ui.theme.GoogleBlue
import com.example.pdftovoice.ui.components.common.AppButton
import com.example.pdftovoice.ui.components.reader.SynchronizedTextDisplay
import com.example.pdftovoice.ui.system.ResponsiveDimensions.horizontalPadding
import com.example.pdftovoice.ui.system.ResponsiveDimensions.verticalPadding
import com.example.pdftovoice.ui.system.ResponsiveDimensions.sectionSpacing
import com.example.pdftovoice.ui.system.ResponsiveDimensions.mainButtonSize
import com.example.pdftovoice.ui.system.ResponsiveDimensions.cornerRadius
import com.example.pdftovoice.ui.system.ResponsiveLayout.contentMaxWidth
import com.example.pdftovoice.ui.system.ResponsiveLayout.isCompact
import com.example.pdftovoice.ui.system.ResponsiveTypography.scaleFactor

@Composable
fun HomeScreen(
    windowSizeClass: WindowSizeClass,
    onNavigateToPdfReader: () -> Unit = {},
    // Optional parameters for enhanced functionality
    extractedText: String = "",
    currentlyReadingSegment: String = "",
    isPlaying: Boolean = false,
    // Word-level highlighting parameters
    currentWord: String = "",
    wordIndex: Int = -1
) {
    // Responsive dimensions
    val horizontalPadding = windowSizeClass.horizontalPadding()
    val verticalPadding = windowSizeClass.verticalPadding()
    val sectionSpacing = windowSizeClass.sectionSpacing()
    val mainButtonSize = windowSizeClass.mainButtonSize()
    val contentMaxWidth = windowSizeClass.contentMaxWidth()
    val scaleFactor = windowSizeClass.scaleFactor()
    val cornerRadius = windowSizeClass.cornerRadius()
    val isCompact = windowSizeClass.isCompact()
    
    // State for fullscreen text panel
    var showFullscreenText by remember { mutableStateOf(false) }
    
    // Use provided text
    val displayText = extractedText
    val currentSegment = currentlyReadingSegment
    val isCurrentlyPlaying = isPlaying
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = contentMaxWidth)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding * 2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Welcome Section
        Spacer(modifier = Modifier.height(sectionSpacing * 2))
        
        Text(
            text = androidx.compose.ui.res.stringResource(id = com.example.pdftovoice.R.string.welcome_message),
            fontSize = (24 * scaleFactor).sp,
            fontWeight = FontWeight.Bold,
            color = GoogleBlue
        )
        
        Spacer(modifier = Modifier.height(sectionSpacing))
        
        if (extractedText.isNotEmpty()) {
            Text(
                text = stringResource(id = R.string.reading_in_progress_autoscroll),
                fontSize = (16 * scaleFactor).sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Spacer(modifier = Modifier.height(sectionSpacing * 2))
        
        // Action Buttons Row
        if (isCompact) {
            // Compact layout - stacked buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(sectionSpacing)
            ) {
                Button(
                    onClick = onNavigateToPdfReader,
                    modifier = Modifier.height(mainButtonSize),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoogleBlue
                    )
                ) {
                    Icon(
                        Icons.Default.TextFields,
                        contentDescription = androidx.compose.ui.res.stringResource(id = com.example.pdftovoice.R.string.start_reading_pdfs),
                        modifier = Modifier.size((18 * scaleFactor).dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        androidx.compose.ui.res.stringResource(id = com.example.pdftovoice.R.string.start_reading_pdfs),
                        fontSize = (16 * scaleFactor).sp
                    )
                }
                
                // Fullscreen button (if text is available)
                if (displayText.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showFullscreenText = true },
                        modifier = Modifier.height(mainButtonSize)
                    ) {
                        Icon(
                            Icons.Default.Fullscreen,
                            contentDescription = androidx.compose.ui.res.stringResource(id = com.example.pdftovoice.R.string.fullscreen_text),
                            modifier = Modifier.size((18 * scaleFactor).dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(id = R.string.fullscreen_text),
                            fontSize = (16 * scaleFactor).sp
                        )
                    }
                }
            }
        } else {
            // Wide layout - side by side buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(sectionSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onNavigateToPdfReader,
                    modifier = Modifier.height(mainButtonSize),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoogleBlue
                    )
                ) {
                    Icon(
                        Icons.Default.TextFields,
                        contentDescription = androidx.compose.ui.res.stringResource(id = com.example.pdftovoice.R.string.start_reading_pdfs),
                        modifier = Modifier.size((18 * scaleFactor).dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            androidx.compose.ui.res.stringResource(id = com.example.pdftovoice.R.string.start_reading_pdfs),
                            fontSize = (16 * scaleFactor).sp
                        )
                }
                
                // Fullscreen button (if text is available)
                if (displayText.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showFullscreenText = true },
                        modifier = Modifier.height(mainButtonSize)
                    ) {
                        Icon(
                            Icons.Default.Fullscreen,
                            contentDescription = androidx.compose.ui.res.stringResource(id = com.example.pdftovoice.R.string.fullscreen_text),
                            modifier = Modifier.size((18 * scaleFactor).dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            androidx.compose.ui.res.stringResource(id = com.example.pdftovoice.R.string.fullscreen_text),
                            fontSize = (16 * scaleFactor).sp
                        )
                    }
                }
            }
        }
        
        // Autoscrolling Text Display (if text is available)
        if (displayText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(sectionSpacing * 2))
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 300.dp),
                shape = RoundedCornerShape(cornerRadius),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(sectionSpacing)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.TextFields,
                            contentDescription = androidx.compose.ui.res.stringResource(id = com.example.pdftovoice.R.string.extracted_text_title),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size((20 * scaleFactor).dp)
                        )
                        Text(
                            text = androidx.compose.ui.res.stringResource(id = com.example.pdftovoice.R.string.extracted_text_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = (18 * scaleFactor).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Reading status indicator
                        if (isCurrentlyPlaying) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size((6 * scaleFactor).dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        )
                                )
                                Text(
                                    text = stringResource(id = R.string.reading_aloud),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = (12 * scaleFactor).sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(sectionSpacing))
                    
                    SynchronizedTextDisplay(
                        text = displayText,
                        currentlyReadingSegment = currentSegment,
                        currentWord = currentWord,
                        wordIndex = wordIndex,
                        isPlaying = isCurrentlyPlaying,
                        windowSizeClass = windowSizeClass,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    
    // Fullscreen Text Panel
    if (showFullscreenText) {
        SynchronizedTextDisplay(
            text = displayText,
            currentlyReadingSegment = currentSegment,
            currentWord = currentWord,
            wordIndex = wordIndex,
            isPlaying = isCurrentlyPlaying,
            windowSizeClass = windowSizeClass,
            modifier = Modifier.fillMaxSize()
        )
    }
}
