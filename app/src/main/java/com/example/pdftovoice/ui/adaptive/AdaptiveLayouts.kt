package com.example.pdftovoice.ui.adaptive

import androidx.compose.foundation.layout.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pdftovoice.ui.responsive.ResponsiveBreakpoints.adaptiveLayoutMode
import com.example.pdftovoice.ui.responsive.ResponsiveDimensions.horizontalPadding
import com.example.pdftovoice.ui.responsive.ResponsiveDimensions.verticalPadding
import com.example.pdftovoice.ui.responsive.ResponsiveDimensions.sectionSpacing
import com.example.pdftovoice.ui.responsive.ResponsiveLayout.contentMaxWidth
import com.example.pdftovoice.ui.responsive.AdaptiveLayoutMode

/**
 * Adaptive container that adjusts layout based on screen size
 */
@Composable
fun AdaptiveContainer(
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val horizontalPadding = windowSizeClass.horizontalPadding()
    val verticalPadding = windowSizeClass.verticalPadding()
    val contentMaxWidth = windowSizeClass.contentMaxWidth()
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = contentMaxWidth)
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            content()
        }
    }
}

/**
 * Adaptive two-pane layout that switches between single and dual pane based on screen size
 */
@Composable
fun AdaptiveTwoPaneLayout(
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    primaryContent: @Composable () -> Unit,
    secondaryContent: @Composable () -> Unit
) {
    val layoutMode = windowSizeClass.adaptiveLayoutMode()
    val sectionSpacing = windowSizeClass.sectionSpacing()
    
    when (layoutMode) {
        AdaptiveLayoutMode.SINGLE_COLUMN -> {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(sectionSpacing)
            ) {
                primaryContent()
                secondaryContent()
            }
        }
        AdaptiveLayoutMode.COMPACT_DUAL_PANE,
        AdaptiveLayoutMode.DUAL_COLUMN -> {
            Row(
                modifier = modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(sectionSpacing)
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    primaryContent()
                }
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    secondaryContent()
                }
            }
        }
        AdaptiveLayoutMode.TRIPLE_COLUMN -> {
            Row(
                modifier = modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(sectionSpacing)
            ) {
                Column(
                    modifier = Modifier.weight(1.5f)
                ) {
                    primaryContent()
                }
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    secondaryContent()
                }
            }
        }
    }
}

/**
 * Adaptive grid layout that adjusts columns based on screen size
 */
@Composable
fun AdaptiveGrid(
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    items: List<@Composable () -> Unit>
) {
    val layoutMode = windowSizeClass.adaptiveLayoutMode()
    val sectionSpacing = windowSizeClass.sectionSpacing()
    
    val columns = when (layoutMode) {
        AdaptiveLayoutMode.SINGLE_COLUMN -> 1
        AdaptiveLayoutMode.COMPACT_DUAL_PANE,
        AdaptiveLayoutMode.DUAL_COLUMN -> 2
        AdaptiveLayoutMode.TRIPLE_COLUMN -> 3
    }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
    ) {
        items.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(sectionSpacing)
            ) {
                rowItems.forEach { item ->
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        item()
                    }
                }
                // Fill remaining space if row is not complete
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Responsive spacer that adjusts size based on screen size
 */
@Composable
fun ResponsiveSpacer(
    windowSizeClass: WindowSizeClass,
    multiplier: Float = 1f
) {
    val spacing = windowSizeClass.sectionSpacing() * multiplier
    Spacer(modifier = Modifier.height(spacing))
}
