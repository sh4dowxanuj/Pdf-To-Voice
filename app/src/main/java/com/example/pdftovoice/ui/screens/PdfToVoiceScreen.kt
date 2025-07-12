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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToVoiceScreen(
    viewModel: PdfToVoiceViewModel = viewModel(),
    onLogout: () -> Unit = {}
) {
    // Optimize state collection with keys to prevent unnecessary recomposition
    val state by viewModel.state.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val speed by viewModel.speed.collectAsState()
    val pitch by viewModel.pitch.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val availableLanguages by viewModel.availableLanguages.collectAsState()
    
    // Use rememberSaveable to persist UI state across configuration changes
    var showControls by rememberSaveable { mutableStateOf(false) }
    var showLanguageSelector by rememberSaveable { mutableStateOf(false) }
    
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
        
        // Error Message
        state.errorMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        // Text Content and Controls
        if (state.extractedText.isNotBlank()) {
            // Playback Controls Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Playback Controls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    // Main Control Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                when {
                                    isPlaying -> viewModel.pauseReading()
                                    isPaused -> viewModel.resumeReading()
                                    else -> viewModel.playText()
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                when {
                                    isPlaying -> Icons.Default.Pause
                                    else -> Icons.Default.PlayArrow
                                },
                                contentDescription = when {
                                    isPlaying -> "Pause"
                                    isPaused -> "Resume"
                                    else -> "Play"
                                },
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        FloatingActionButton(
                            onClick = { viewModel.stopReading() },
                            containerColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Stop",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        FloatingActionButton(
                            onClick = { showControls = !showControls },
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // Additional Control Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        // Retry button for failed extractions
                        if (state.errorMessage?.contains("Failed", ignoreCase = true) == true) {
                            OutlinedButton(
                                onClick = { viewModel.retryExtraction() }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Retry")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retry")
                            }
                        }
                        
                        // Show extraction method info
                        if (state.extractionMethod != null) {
                            OutlinedButton(
                                onClick = { viewModel.showMethodInfo() }
                            ) {
                                Icon(Icons.Default.Info, contentDescription = "Method Info")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Method")
                            }
                        }
                    }
                    
                    // Advanced Controls (Show/Hide)
                    if (showControls) {
                        Divider()
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Speed Control
                            Column {
                                Text(
                                    text = "Speed: ${String.format("%.1f", speed)}x",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Slider(
                                    value = speed,
                                    onValueChange = { viewModel.setSpeed(it) },
                                    valueRange = 0.1f..3.0f,
                                    steps = 29,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            
                            // Pitch Control
                            Column {
                                Text(
                                    text = "Pitch: ${String.format("%.1f", pitch)}x",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Slider(
                                    value = pitch,
                                    onValueChange = { viewModel.setPitch(it) },
                                    valueRange = 0.1f..2.0f,
                                    steps = 19,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
            
            // Currently Reading Section
            if (state.currentlyReadingSegment.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Currently Reading:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = state.currentlyReadingSegment,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Extracted Text Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Extracted Text",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = state.extractedText,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}
