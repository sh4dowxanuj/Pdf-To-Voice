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
import com.example.pdftovoice.ui.components.MusicPlayerControls
import com.example.pdftovoice.ui.components.SynchronizedLyricsDisplay

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
    var showLanguageSelector by rememberSaveable { mutableStateOf(false) }
    var showTextPanel by rememberSaveable { mutableStateOf(false) }
    
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
            
            // Synchronized Lyrics Display - Spotify Style
            if (state.extractedText.isNotBlank()) {
                SynchronizedLyricsDisplay(
                    text = state.extractedText,
                    currentlyReadingSegment = state.currentlyReadingSegment,
                    isPlaying = isPlaying,
                    modifier = Modifier.weight(1f)
                )
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
