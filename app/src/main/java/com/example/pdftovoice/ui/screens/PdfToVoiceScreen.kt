package com.example.pdftovoice.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pdftovoice.R
import com.example.pdftovoice.tts.Language
import com.example.pdftovoice.viewmodel.PdfToVoiceViewModel
import com.example.pdftovoice.viewmodel.PdfToVoiceState
import com.example.pdftovoice.ui.components.MusicPlayerControls
import com.example.pdftovoice.ui.components.SynchronizedLyricsDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToVoiceScreen(
    viewModel: PdfToVoiceViewModel = viewModel(),
    onLogout: () -> Unit = {}
) {
    // Optimize state collection - combine all related states
    val state by viewModel.combinedState.collectAsState(initial = PdfToVoiceState())
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val speed by viewModel.speed.collectAsState()
    val pitch by viewModel.pitch.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val availableLanguages by viewModel.availableLanguages.collectAsState()
    
    // UI state - use rememberSaveable for configuration changes
    var showLanguageSelector by rememberSaveable { mutableStateOf(false) }
    
    // Memoize heavy computations
    val textSegments = remember(state.extractedText) {
        if (state.extractedText.isBlank()) emptyList()
        else state.extractedText.split(Regex("(?<=[.!?])\\s+|\\n"))
            .filter { it.isNotBlank() }
            .map { it.trim() }
    }
    
    val currentSegmentIndex = remember(state.currentlyReadingSegment, textSegments) {
        if (state.currentlyReadingSegment.isBlank() || textSegments.isEmpty()) -1
        else {
            // Optimized matching with early exit
            textSegments.indexOfFirst { segment ->
                segment.contains(state.currentlyReadingSegment, ignoreCase = true) ||
                state.currentlyReadingSegment.contains(segment, ignoreCase = true)
            }.takeIf { it >= 0 } ?: run {
                // Fallback to word matching only if exact match fails
                val readingWords = state.currentlyReadingSegment.lowercase().split(Regex("\\s+"))
                textSegments.indexOfFirst { segment ->
                    val segmentWords = segment.lowercase().split(Regex("\\s+"))
                    readingWords.count { word ->
                        segmentWords.any { it.contains(word) || word.contains(it) }
                    } >= maxOf(1, readingWords.size / 2)
                }
            }
        }
    }
    
    // PDF picker launcher
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.selectPdf(it) }
    }
    
    // Error handling
    state.errorMessage?.let { error ->
        LaunchedEffect(key1 = error) {
            // Auto-clear error after 5 seconds
            kotlinx.coroutines.delay(5000)
            viewModel.clearError()
        }
    }
    
    // Create a layout with bottom music player controls
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(bottom = 200.dp), // Space for bottom player
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Bar with Logout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.pdf_to_voice_reader),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Language Selector
                    IconButton(
                        onClick = { showLanguageSelector = true }
                    ) {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = stringResource(R.string.select_language)
                        )
                    }
                    
                    // Logout Button
                    IconButton(
                        onClick = onLogout,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = stringResource(R.string.logout),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Language Selection Dialog
            if (showLanguageSelector) {
                AlertDialog(
                    onDismissRequest = { showLanguageSelector = false },
                    title = { Text(stringResource(R.string.select_language)) },
                    text = {
                        LazyColumn {
                            items(availableLanguages) { language ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setLanguage(language)
                                            showLanguageSelector = false
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = language.code == currentLanguage.code,
                                        onClick = {
                                            viewModel.setLanguage(language)
                                            showLanguageSelector = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = language.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showLanguageSelector = false }) {
                            Text("OK")
                        }
                    }
                )
            }
            
            // File Selection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.select_pdf_file),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { pdfPickerLauncher.launch("application/pdf") },
                            modifier = Modifier.weight(1f),
                            enabled = !state.isLoading
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.choose_file))
                        }
                        
                        if (state.selectedPdfFile != null) {
                            OutlinedButton(
                                onClick = { viewModel.clearPdf() },
                                enabled = !state.isLoading
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    }
                    
                    if (state.selectedPdfFile != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val selectedFile = state.selectedPdfFile
                                Text(
                                    text = "Selected: ${selectedFile?.name ?: "Unknown"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Size: ${selectedFile?.formattedSize ?: "Unknown"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                // Show analysis info if available
                                selectedFile?.analysisInfo?.let { analysisInfo ->
                                    Text(
                                        text = "Type: ${selectedFile.mimeType ?: "Unknown"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Pages: ${analysisInfo.pageCount}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (analysisInfo.hasSelectableText) {
                                            Text(
                                                text = "📄 Text",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (analysisInfo.isImagePdf) {
                                            Text(
                                                text = "🖼️ Images",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (analysisInfo.isPasswordProtected) {
                                            Text(
                                                text = "🔒 Protected",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    
                                    Text(
                                        text = "Methods: ${analysisInfo.supportedMethods.size} available",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                // Show extraction method if available
                                state.extractionMethod?.let { method ->
                                    Text(
                                        text = "Extracted using: ${method.name.replace("_", " ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Loading Indicators
            if (state.isLoading || state.isAnalyzing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = when {
                                    state.isAnalyzing -> "Analyzing PDF structure..."
                                    state.processingStatus != null -> state.processingStatus!!
                                    state.isLoading -> "Extracting text from PDF..."
                                    else -> "Processing..."
                                }
                            )
                            
                            // Show detailed status if available
                            state.processingStatus?.let { status ->
                                if (status != "Processing...") {
                                    Text(
                                        text = status,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Error Message with better styling and actions
            state.errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (error.contains("failed", ignoreCase = true) || 
                                              error.contains("error", ignoreCase = true)) {
                            MaterialTheme.colorScheme.errorContainer
                        } else if (error.contains("complete", ignoreCase = true) ||
                                   error.contains("success", ignoreCase = true)) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (error.contains("failed", ignoreCase = true) || 
                                                 error.contains("error", ignoreCase = true)) {
                                    Icons.Default.Error
                                } else if (error.contains("complete", ignoreCase = true) ||
                                          error.contains("success", ignoreCase = true)) {
                                    Icons.Default.CheckCircle
                                } else {
                                    Icons.Default.Info
                                },
                                contentDescription = null,
                                tint = if (error.contains("failed", ignoreCase = true) || 
                                          error.contains("error", ignoreCase = true)) {
                                    MaterialTheme.colorScheme.error
                                } else if (error.contains("complete", ignoreCase = true) ||
                                          error.contains("success", ignoreCase = true)) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = error,
                                    color = if (error.contains("failed", ignoreCase = true) || 
                                              error.contains("error", ignoreCase = true)) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else if (error.contains("complete", ignoreCase = true) ||
                                              error.contains("success", ignoreCase = true)) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 20.sp
                                )
                                
                                // Add action buttons for certain error types
                                if (error.contains("failed", ignoreCase = true) && 
                                    state.selectedPdfFile != null) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.retryExtraction() },
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Retry")
                                        }
                                        
                                        OutlinedButton(
                                            onClick = { viewModel.showMethodInfo() },
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Info,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Help")
                                        }
                                    }
                                }
                            }
                            
                            // Dismiss button
                            IconButton(
                                onClick = { viewModel.clearError() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Text Display Area - Either content or debug info
            if (state.extractedText.isNotBlank()) {
                SynchronizedLyricsDisplay(
                    text = state.extractedText,
                    currentlyReadingSegment = state.currentlyReadingSegment,
                    isPlaying = isPlaying,
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Enhanced debug information panel
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.errorMessage?.contains("failed", ignoreCase = true) == true) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Status icon with animation
                        Icon(
                            imageVector = when {
                                state.isLoading || state.isAnalyzing -> Icons.Default.Sync
                                state.errorMessage?.contains("failed", ignoreCase = true) == true -> Icons.Default.Error
                                state.selectedPdfFile != null -> Icons.Default.Description
                                else -> Icons.Default.TextFields
                            },
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = when {
                                state.isLoading || state.isAnalyzing -> MaterialTheme.colorScheme.primary
                                state.errorMessage?.contains("failed", ignoreCase = true) == true -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.outline
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Main status message
                        Text(
                            text = when {
                                state.isLoading -> "Processing PDF..."
                                state.isAnalyzing -> "Analyzing Document..."
                                state.errorMessage?.contains("failed", ignoreCase = true) == true -> "Extraction Failed"
                                state.selectedPdfFile != null -> "Ready to Extract"
                                else -> "No PDF Selected"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Detailed status
                        Text(
                            text = when {
                                state.isLoading -> "Extracting text using the best available method..."
                                state.isAnalyzing -> "Analyzing PDF structure and determining optimal extraction method..."
                                state.selectedPdfFile == null -> "Select a PDF file to get started with text extraction and voice reading"
                                state.errorMessage?.contains("failed", ignoreCase = true) == true -> "Text extraction encountered issues. Try a different PDF or check the debug information below."
                                else -> "PDF is ready for text extraction. Processing will begin automatically."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                        
                        // Action suggestions
                        if (state.selectedPdfFile == null) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { pdfPickerLauncher.launch("application/pdf") },
                                modifier = Modifier.fillMaxWidth(0.6f)
                            ) {
                                Icon(Icons.Default.FileOpen, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select PDF File")
                            }
                        }
                        
                        // Debug information (collapsible)
                        if (state.selectedPdfFile != null) {
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            var showDebugInfo by rememberSaveable { mutableStateOf(false) }
                            
                            OutlinedButton(
                                onClick = { showDebugInfo = !showDebugInfo },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                Icon(
                                    if (showDebugInfo) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (showDebugInfo) "Hide Debug Info" else "Show Debug Info")
                            }
                            
                            if (showDebugInfo) {
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Debug Information",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        
                                        Divider(
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                        
                                        DebugInfoRow("File", state.selectedPdfFile?.name ?: "None")
                                        DebugInfoRow("Size", state.selectedPdfFile?.formattedSize ?: "Unknown")
                                        DebugInfoRow("Text Length", "${state.extractedText.length} characters")
                                        DebugInfoRow("Loading", state.isLoading.toString())
                                        DebugInfoRow("Analyzing", state.isAnalyzing.toString())
                                        DebugInfoRow("Method", state.extractionMethod?.name?.replace("_", " ") ?: "None")
                                        DebugInfoRow("TTS Ready", state.isTtsInitialized.toString())
                                        DebugInfoRow("Current Segment", if (state.currentlyReadingSegment.isNotBlank()) {
                                            "'${state.currentlyReadingSegment.take(50)}${if (state.currentlyReadingSegment.length > 50) "..." else ""}'"
                                        } else "None")
                                        
                                        state.selectedPdfFile?.analysisInfo?.let { info ->
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "PDF Analysis",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            DebugInfoRow("Pages", info.pageCount.toString())
                                            DebugInfoRow("Has Text", info.hasSelectableText.toString())
                                            DebugInfoRow("Has Images", info.isImagePdf.toString())
                                            DebugInfoRow("Protected", info.isPasswordProtected.toString())
                                            DebugInfoRow("Methods Available", info.supportedMethods.size.toString())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Enhanced Reading Progress Bar above Music Player Controls
        if (state.extractedText.isNotBlank() && textSegments.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 200.dp) // Above music controls
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Reading progress header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Reading Progress",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            
                            // Progress indicator
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (currentSegmentIndex >= 0) {
                                        "${currentSegmentIndex + 1}/${textSegments.size}"
                                    } else {
                                        "0/${textSegments.size}"
                                    },
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                                )
                            }
                        }
                        
                        // Progress bar
                        val progress = if (currentSegmentIndex >= 0) {
                            (currentSegmentIndex + 1).toFloat() / textSegments.size.toFloat()
                        } else {
                            0f
                        }
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Start",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}% Complete",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "End",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Bottom Music Player Controls
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            MusicPlayerControls(
                isPlaying = isPlaying,
                isPaused = isPaused,
                currentlyReadingSegment = state.currentlyReadingSegment,
                fileName = state.selectedPdfFile?.name,
                speed = speed,
                pitch = pitch,
                onPlayPause = {
                    when {
                        isPlaying -> viewModel.pauseReading()
                        isPaused -> viewModel.resumeReading()
                        else -> viewModel.playText()
                    }
                },
                onStop = { viewModel.stopReading() },
                onSpeedChange = { viewModel.setSpeed(it) },
                onPitchChange = { viewModel.setPitch(it) }
            )
        }
    }
}

@Composable
private fun DebugInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            modifier = Modifier.weight(2f),
            textAlign = TextAlign.End
        )
    }
}
