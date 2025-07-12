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
import com.example.pdftovoice.ui.theme.GoogleBlue
import com.example.pdftovoice.ui.responsive.ResponsiveDimensions.horizontalPadding
import com.example.pdftovoice.ui.responsive.ResponsiveDimensions.verticalPadding
import com.example.pdftovoice.ui.responsive.ResponsiveDimensions.sectionSpacing
import com.example.pdftovoice.ui.responsive.ResponsiveDimensions.mainButtonSize
import com.example.pdftovoice.ui.responsive.ResponsiveDimensions.cornerRadius
import com.example.pdftovoice.ui.responsive.ResponsiveLayout.contentMaxWidth
import com.example.pdftovoice.ui.responsive.ResponsiveLayout.isCompact
import com.example.pdftovoice.ui.responsive.ResponsiveTypography.scaleFactor
import com.example.pdftovoice.ui.components.SynchronizedLyricsDisplay
import com.example.pdftovoice.ui.components.TextHighlightingPanel

@Composable
fun HomeScreen(
    windowSizeClass: WindowSizeClass,
    onNavigateToPdfReader: () -> Unit = {},
    // Optional parameters for enhanced functionality
    extractedText: String = "",
    currentlyReadingSegment: String = "",
    isPlaying: Boolean = false
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
    
    // State for fullscreen text panel and demo text
    var showFullscreenText by remember { mutableStateOf(false) }
    var showDemoText by remember { mutableStateOf(false) }
    var currentDemoSegment by remember { mutableStateOf("") }
    var demoIsPlaying by remember { mutableStateOf(false) }
    
    // Demo text for showcasing autoscroll and highlighting features
    val demoText = """
        Welcome to the PDF to Voice Reader! This application transforms your PDF documents into an engaging audio experience.
        
        Our innovative autoscrolling technology follows along as text is being read aloud. The highlighted sections move smoothly through the document, ensuring you never lose your place.
        
        The synchronized lyrics display works just like your favorite music apps. Each sentence is highlighted in real-time as it's being spoken.
        
        You can also open any text in fullscreen mode for an immersive reading experience. The fullscreen view includes enhanced typography and text selection capabilities.
        
        This demo showcases the core features: automatic scrolling, real-time highlighting, and responsive design that adapts to any screen size.
        
        Try the fullscreen button to see how the text appears in a dedicated reading environment with enhanced readability features.
        
        The application supports PDF files of any size and automatically breaks them into manageable segments for optimal listening experience.
    """.trimIndent()
    
    // Demo segments for auto-highlight simulation
    val demoSegments = remember {
        demoText.split(". ").filter { it.isNotBlank() }.map { "$it." }
    }
    
    // Auto-advance demo highlighting every 3 seconds
    LaunchedEffect(demoIsPlaying) {
        if (demoIsPlaying && showDemoText) {
            var currentIndex = 0
            while (demoIsPlaying && currentIndex < demoSegments.size) {
                currentDemoSegment = demoSegments[currentIndex]
                kotlinx.coroutines.delay(3000) // 3 seconds per segment
                currentIndex++
            }
            if (currentIndex >= demoSegments.size) {
                demoIsPlaying = false
                currentDemoSegment = ""
            }
        }
    }
    
    // Use either provided text or demo text
    val displayText = if (extractedText.isNotEmpty()) extractedText else if (showDemoText) demoText else ""
    val currentSegment = if (extractedText.isNotEmpty()) currentlyReadingSegment else currentDemoSegment
    val isCurrentlyPlaying = if (extractedText.isNotEmpty()) isPlaying else demoIsPlaying
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = contentMaxWidth)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding * 2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (displayText.isNotEmpty()) Arrangement.Top else Arrangement.Center
    ) {
        // Welcome Section
        Text(
            text = "Welcome to PDF to Voice!",
            fontSize = (24 * scaleFactor).sp,
            fontWeight = FontWeight.Bold,
            color = GoogleBlue
        )
        
        Spacer(modifier = Modifier.height(sectionSpacing))
        
        Text(
            text = when {
                extractedText.isNotEmpty() -> "Reading in progress with autoscrolling highlights"
                showDemoText -> "Demo: Autoscrolling text with live highlighting"
                else -> "You have successfully logged in."
            },
            fontSize = (16 * scaleFactor).sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        
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
                        contentDescription = null,
                        modifier = Modifier.size((18 * scaleFactor).dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Start Reading PDFs",
                        fontSize = (16 * scaleFactor).sp
                    )
                }
                
                // Demo button
                if (!showDemoText) {
                    OutlinedButton(
                        onClick = { 
                            showDemoText = true
                            demoIsPlaying = true
                        },
                        modifier = Modifier.height(mainButtonSize)
                    ) {
                        Text(
                            "Try Demo Features",
                            fontSize = (16 * scaleFactor).sp
                        )
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { demoIsPlaying = !demoIsPlaying },
                            modifier = Modifier.height(mainButtonSize),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (demoIsPlaying) MaterialTheme.colorScheme.secondary else GoogleBlue
                            )
                        ) {
                            Text(
                                if (demoIsPlaying) "Pause Demo" else "Play Demo",
                                fontSize = (14 * scaleFactor).sp
                            )
                        }
                        
                        OutlinedButton(
                            onClick = { 
                                showDemoText = false
                                demoIsPlaying = false
                                currentDemoSegment = ""
                            },
                            modifier = Modifier.height(mainButtonSize)
                        ) {
                            Text(
                                "Hide Demo",
                                fontSize = (14 * scaleFactor).sp
                            )
                        }
                    }
                }
                
                // Fullscreen button (if text is available)
                if (displayText.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showFullscreenText = true },
                        modifier = Modifier.height(mainButtonSize)
                    ) {
                        Icon(
                            Icons.Default.Fullscreen,
                            contentDescription = null,
                            modifier = Modifier.size((18 * scaleFactor).dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Fullscreen Text",
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
                        contentDescription = null,
                        modifier = Modifier.size((18 * scaleFactor).dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Start Reading PDFs",
                        fontSize = (16 * scaleFactor).sp
                    )
                }
                
                // Demo button
                if (!showDemoText) {
                    OutlinedButton(
                        onClick = { 
                            showDemoText = true
                            demoIsPlaying = true
                        },
                        modifier = Modifier.height(mainButtonSize)
                    ) {
                        Text(
                            "Try Demo Features",
                            fontSize = (16 * scaleFactor).sp
                        )
                    }
                } else {
                    Button(
                        onClick = { demoIsPlaying = !demoIsPlaying },
                        modifier = Modifier.height(mainButtonSize),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (demoIsPlaying) MaterialTheme.colorScheme.secondary else GoogleBlue
                        )
                    ) {
                        Text(
                            if (demoIsPlaying) "Pause Demo" else "Play Demo",
                            fontSize = (16 * scaleFactor).sp
                        )
                    }
                    
                    OutlinedButton(
                        onClick = { 
                            showDemoText = false
                            demoIsPlaying = false
                            currentDemoSegment = ""
                        },
                        modifier = Modifier.height(mainButtonSize)
                    ) {
                        Text(
                            "Hide Demo",
                            fontSize = (16 * scaleFactor).sp
                        )
                    }
                }
                
                // Fullscreen button (if text is available)
                if (displayText.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showFullscreenText = true },
                        modifier = Modifier.height(mainButtonSize)
                    ) {
                        Icon(
                            Icons.Default.Fullscreen,
                            contentDescription = null,
                            modifier = Modifier.size((18 * scaleFactor).dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Fullscreen Text",
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
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size((20 * scaleFactor).dp)
                        )
                        Text(
                            text = "Live Text with Autoscroll",
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
                                    text = if (showDemoText) "Demo Playing" else "Reading",
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
                    
                    // Synchronized lyrics display with autoscrolling
                    SynchronizedLyricsDisplay(
                        text = displayText,
                        currentlyReadingSegment = currentSegment,
                        isPlaying = isCurrentlyPlaying,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    
    // Fullscreen Text Panel
    TextHighlightingPanel(
        text = displayText,
        currentlyReadingSegment = currentSegment,
        isVisible = showFullscreenText,
        onDismiss = { showFullscreenText = false }
    )
}
