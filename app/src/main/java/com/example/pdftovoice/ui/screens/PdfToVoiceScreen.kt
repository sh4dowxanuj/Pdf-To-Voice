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
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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
import com.example.pdftovoice.ui.components.common.*
import com.example.pdftovoice.ui.components.player.MediaPlayerControls
import com.example.pdftovoice.ui.components.reader.SynchronizedTextDisplay
import com.example.pdftovoice.ui.components.reader.FullScreenTextPanel
import com.example.pdftovoice.ui.system.ResponsiveDimensions.horizontalPadding
import com.example.pdftovoice.ui.system.ResponsiveDimensions.verticalPadding
import com.example.pdftovoice.ui.system.ResponsiveDimensions.sectionSpacing
import com.example.pdftovoice.ui.system.ResponsiveDimensions.cornerRadius
import com.example.pdftovoice.ui.system.ResponsiveDimensions.itemSpacing
import com.example.pdftovoice.ui.system.ResponsiveDimensions.buttonSize
import com.example.pdftovoice.ui.system.ResponsiveDimensions.cardElevation
import com.example.pdftovoice.ui.system.ResponsiveDimensions.musicPlayerHeight
import com.example.pdftovoice.ui.system.ResponsiveLayout.contentMaxWidth
import com.example.pdftovoice.ui.system.ResponsiveLayout.shouldUseDoubleColumn
import com.example.pdftovoice.ui.system.ResponsiveLayout.isCompact
import com.example.pdftovoice.ui.system.ResponsiveLayout.isLandscape
import com.example.pdftovoice.ui.system.ResponsiveTypography.scaleFactor
import com.example.pdftovoice.ui.responsive.ResponsiveGrid.iconSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToVoiceScreen(
    windowSizeClass: WindowSizeClass,
    viewModel: PdfToVoiceViewModel = viewModel(),
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    
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
    
    // Enhanced memoization for better performance
    val textSegments = remember(state.extractedText) {
        if (state.extractedText.isBlank()) emptyList()
        else state.extractedText
            .splitToSequence(Regex("(?<=[.!?])\\s+|\\n"))
            .filter { it.isNotBlank() }
            .map { it.trim() }
            .toList()
    }
    
    // Optimized segment matching with caching
    val _currentSegmentIndex = remember(state.currentlyReadingSegment, textSegments) {
        if (state.currentlyReadingSegment.isBlank() || textSegments.isEmpty()) return@remember -1
        
        val readingSegment = state.currentlyReadingSegment
        
        // Fast exact match first
        textSegments.indexOfFirst { segment ->
            segment.equals(readingSegment, ignoreCase = true)
        }.takeIf { it >= 0 } ?: run {
            // Substring matching
            textSegments.indexOfFirst { segment ->
                segment.contains(readingSegment, ignoreCase = true) ||
                readingSegment.contains(segment, ignoreCase = true)
            }.takeIf { it >= 0 } ?: run {
                // Word-based matching as fallback
                val readingWords = readingSegment.lowercase().split(Regex("\\s+"))
                if (readingWords.size <= 2) return@run -1 // Skip very short segments
                
                textSegments.indexOfFirst { segment ->
                    val segmentWords = segment.lowercase().split(Regex("\\s+"))
                    val matches = readingWords.count { word ->
                        segmentWords.any { it.contains(word) || word.contains(it) }
                    }
                    matches >= maxOf(2, readingWords.size / 2)
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
    val isLandscape = windowSizeClass.isLandscape()
    val shouldUseDoubleColumn = windowSizeClass.shouldUseDoubleColumn()
    val horizontalPadding = windowSizeClass.horizontalPadding()
    val verticalPadding = windowSizeClass.verticalPadding()
    val sectionSpacing = windowSizeClass.sectionSpacing()
    val musicPlayerHeight = windowSizeClass.musicPlayerHeight()
    val _cornerRadius = windowSizeClass.cornerRadius() // reserved for shape adjustments
    val contentMaxWidth = windowSizeClass.contentMaxWidth()
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main content
        if (isLandscape && shouldUseDoubleColumn) {
            // Landscape layout with side-by-side arrangement
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontalPadding)
                    .padding(bottom = musicPlayerHeight + verticalPadding),
                horizontalArrangement = Arrangement.spacedBy(sectionSpacing)
            ) {
                // Left column - Main controls
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = verticalPadding),
                    verticalArrangement = Arrangement.spacedBy(sectionSpacing)
                ) {
                    TopBarSection(
                        windowSizeClass = windowSizeClass,
                        showLanguageSelector = showLanguageSelector,
                        onShowLanguageSelector = { showLanguageSelector = it },
                        currentLanguage = currentLanguage,
                        availableLanguages = availableLanguages,
                        onLanguageSelected = { language ->
                            viewModel.setLanguage(language)
                            showLanguageSelector = false
                        },
                        onLogout = onLogout
                    )
                    
                    FileSelectionSection(
                        windowSizeClass = windowSizeClass,
                        state = state,
                        pdfPickerLauncher = pdfPickerLauncher,
                        onClearPdf = { viewModel.clearPdf() }
                    )
                }
                
                // Right column - Text display and additional controls
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = verticalPadding),
                    verticalArrangement = Arrangement.spacedBy(sectionSpacing)
                ) {
                    TextDisplaySection(
                        windowSizeClass = windowSizeClass,
                        state = state,
                        isPlaying = isPlaying,
                        viewModel = viewModel
                    )
                }
            }
        } else {
            // Portrait/compact layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = contentMaxWidth)
                    .padding(horizontalPadding)
                    .padding(bottom = musicPlayerHeight + verticalPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(sectionSpacing)
            ) {
                Spacer(modifier = Modifier.height(verticalPadding))
                
                TopBarSection(
                    windowSizeClass = windowSizeClass,
                    showLanguageSelector = showLanguageSelector,
                    onShowLanguageSelector = { showLanguageSelector = it },
                    currentLanguage = currentLanguage,
                    availableLanguages = availableLanguages,
                    onLanguageSelected = { language ->
                        viewModel.setLanguage(language)
                        showLanguageSelector = false
                    },
                    onLogout = onLogout
                )
                
                FileSelectionSection(
                    windowSizeClass = windowSizeClass,
                    state = state,
                    pdfPickerLauncher = pdfPickerLauncher,
                    onClearPdf = { viewModel.clearPdf() }
                )
                
                TextDisplaySection(
                    windowSizeClass = windowSizeClass,
                    state = state,
                    isPlaying = isPlaying,
                    viewModel = viewModel
                )
            }
        }
        
        // Bottom Media Player Controls
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            MediaPlayerControls(
                windowSizeClass = windowSizeClass,
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
                onPitchChange = { viewModel.setPitch(it) },
                onFullScreen = {
                    if (state.extractedText.isNotEmpty()) {
                        val intent = com.example.pdftovoice.ui.activities.FullScreenReaderActivity.createIntent(
                            context = context,
                            pdfUri = state.selectedPdfFile?.uri?.toString(),
                            pdfName = state.selectedPdfFile?.name,
                            extractedText = state.extractedText
                        )
                        context.startActivity(intent)
                    }
                }
            )
        }
    }
}

@Composable
private fun TopBarSection(
    windowSizeClass: WindowSizeClass,
    showLanguageSelector: Boolean,
    onShowLanguageSelector: (Boolean) -> Unit,
    currentLanguage: Language,
    availableLanguages: List<Language>,
    onLanguageSelected: (Language) -> Unit,
    onLogout: () -> Unit
) {
    val itemSpacing = windowSizeClass.itemSpacing()
    val scaleFactor = windowSizeClass.scaleFactor()
    val isCompact = windowSizeClass.isCompact()
    
    // Responsive layout based on screen size
    if (isCompact) {
        // Compact layout - vertical arrangement for very small screens
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            Text(
                text = stringResource(R.string.pdf_to_voice_reader),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize * scaleFactor
                ),
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopBarActions(
                    windowSizeClass = windowSizeClass,
                    onShowLanguageSelector = { onShowLanguageSelector(true) },
                    onLogout = onLogout
                )
            }
        }
    } else {
        // Normal layout - horizontal arrangement
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.pdf_to_voice_reader),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize * scaleFactor
                ),
                fontWeight = FontWeight.Bold
            )
            
            TopBarActions(
                windowSizeClass = windowSizeClass,
                onShowLanguageSelector = { onShowLanguageSelector(true) },
                onLogout = onLogout
            )
        }
    }
    
    // Language Selection Dialog
    if (showLanguageSelector) {
        LanguageSelectionDialog(
            windowSizeClass = windowSizeClass,
            currentLanguage = currentLanguage,
            availableLanguages = availableLanguages,
            onLanguageSelected = onLanguageSelected,
            onDismiss = { onShowLanguageSelector(false) }
        )
    }
}

@Composable
private fun TopBarActions(
    windowSizeClass: WindowSizeClass,
    onShowLanguageSelector: () -> Unit,
    onLogout: () -> Unit
) {
    val buttonSize = windowSizeClass.buttonSize()
    val itemSpacing = windowSizeClass.itemSpacing()
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Language Selector
        IconButton(
            onClick = onShowLanguageSelector,
            modifier = Modifier.size(buttonSize)
        ) {
            Icon(
                Icons.Default.Language,
                contentDescription = stringResource(R.string.select_language),
                modifier = Modifier.size(windowSizeClass.iconSize())
            )
        }
        
        // Logout Button
        IconButton(
            onClick = onLogout,
            modifier = Modifier.size(buttonSize),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                Icons.Default.ExitToApp,
                contentDescription = stringResource(R.string.logout),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(windowSizeClass.iconSize())
            )
        }
    }
}

@Composable
private fun LanguageSelectionDialog(
    windowSizeClass: WindowSizeClass,
    currentLanguage: Language,
    availableLanguages: List<Language>,
    onLanguageSelected: (Language) -> Unit,
    onDismiss: () -> Unit
) {
    val itemSpacing = windowSizeClass.itemSpacing()
    val scaleFactor = windowSizeClass.scaleFactor()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                stringResource(R.string.select_language),
                fontSize = (18 * scaleFactor).sp
            ) 
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
                items(availableLanguages) { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(language) }
                            .padding(vertical = itemSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = language.code == currentLanguage.code,
                            onClick = { onLanguageSelected(language) }
                        )
                        Spacer(modifier = Modifier.width(itemSpacing))
                        Text(
                            text = language.name,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = (14 * scaleFactor).sp
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = R.string.ok_button),
                    fontSize = (14 * scaleFactor).sp
                )
            }
        }
    )
}

@Composable
private fun FileSelectionSection(
    windowSizeClass: WindowSizeClass,
    state: PdfToVoiceState,
    pdfPickerLauncher: androidx.activity.compose.ManagedActivityResultLauncher<String, Uri?>,
    onClearPdf: () -> Unit
) {
    val cornerRadius = windowSizeClass.cornerRadius()
    val sectionSpacing = windowSizeClass.sectionSpacing()
    val itemSpacing = windowSizeClass.itemSpacing()
    
    // File Selection Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = windowSizeClass.cardElevation())
    ) {
        Column(
            modifier = Modifier.padding(sectionSpacing),
            verticalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            Text(
                text = stringResource(R.string.select_pdf_file),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            if (windowSizeClass.shouldUseDoubleColumn()) {
                // Wide layout - buttons side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing)
                ) {
                    Button(
                        onClick = { pdfPickerLauncher.launch("application/pdf") },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isLoading
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = stringResource(R.string.choose_file))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.choose_file))
                    }
                    
                    if (state.selectedPdfFile != null) {
                        OutlinedButton(
                            onClick = onClearPdf,
                            enabled = !state.isLoading,
                            modifier = Modifier.weight(0.3f)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_selection))
                        }
                    }
                }
            } else {
                // Compact layout - buttons stacked
                Button(
                    onClick = { pdfPickerLauncher.launch("application/pdf") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = stringResource(R.string.choose_file))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.choose_file))
                }
                
                if (state.selectedPdfFile != null) {
                    OutlinedButton(
                        onClick = onClearPdf,
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_selection))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(androidx.compose.ui.res.stringResource(id = com.example.pdftovoice.R.string.clear_selection))
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
                        modifier = Modifier.padding(itemSpacing),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val selectedFile = state.selectedPdfFile
                        Text(
                            text = stringResource(
                                id = R.string.selected_file_label,
                                selectedFile?.name ?: stringResource(id = R.string.unknown_label)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(
                                id = R.string.file_size_label,
                                selectedFile?.size?.let { size ->
                                    stringResource(id = R.string.kilobytes_format, size / 1024)
                                } ?: stringResource(id = R.string.unknown_label)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TextDisplaySection(
    windowSizeClass: WindowSizeClass,
    state: PdfToVoiceState,
    isPlaying: Boolean,
    viewModel: PdfToVoiceViewModel
) {
    val cornerRadius = windowSizeClass.cornerRadius()
    val sectionSpacing = windowSizeClass.sectionSpacing()
    
    // Display extracted text or status
    if (state.extractedText.isNotEmpty()) {
        // Text Display with Synchronized Lyrics
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = if (windowSizeClass.isCompact()) 400.dp else 600.dp),
            shape = RoundedCornerShape(cornerRadius),
            elevation = CardDefaults.cardElevation(defaultElevation = windowSizeClass.cardElevation())
        ) {
            Column(
                modifier = Modifier.padding(sectionSpacing)
            ) {
                Text(
                    text = stringResource(R.string.extracted_text),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                SynchronizedTextDisplay(
                    text = state.extractedText,
                    currentlyReadingSegment = state.currentlyReadingSegment,
                    currentWord = "", // Simplified for now
                    wordIndex = -1, // Simplified for now  
                    isPlaying = isPlaying,
                    windowSizeClass = windowSizeClass,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    } else if (state.isLoading) {
        // Loading State
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(cornerRadius),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            AppLoadingIndicator(
                text = stringResource(R.string.processing_pdf),
                modifier = Modifier.padding(sectionSpacing)
            )
        }
    } else if (state.selectedPdfFile != null) {
        // Ready to Process
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(cornerRadius),
            elevation = CardDefaults.cardElevation(defaultElevation = windowSizeClass.cardElevation())
        ) {
                Column(
                modifier = Modifier.padding(sectionSpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(id = com.example.pdftovoice.R.string.ready_to_process_pdf),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Button(
                    onClick = { viewModel.retryExtraction() },
                    enabled = !state.isLoading
                ) {
                    Icon(Icons.Default.TextFields, contentDescription = stringResource(R.string.extract_text))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(androidx.compose.ui.res.stringResource(id = com.example.pdftovoice.R.string.extract_text))
                }
            }
        }
    }
    
    // Error Message
    state.errorMessage?.let { error ->
        AppErrorCard(
            error = error,
            windowSizeClass = windowSizeClass,
            onRetry = { viewModel.retryExtraction() }
        )
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
