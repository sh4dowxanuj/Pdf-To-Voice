package com.example.pdftovoice.ui.system

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Unified responsive system for the PDF-to-Voice app
 * Replaces scattered responsive utilities with a single, consistent system
 */

data class ResponsiveValues<T>(
    val compact: T,
    val medium: T,
    val expanded: T
)

@Composable
fun <T> WindowSizeClass.selectByWidth(values: ResponsiveValues<T>): T {
    return when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> values.compact
        WindowWidthSizeClass.Medium -> values.medium
        WindowWidthSizeClass.Expanded -> values.expanded
        else -> values.compact
    }
}

@Composable
fun <T> WindowSizeClass.selectByHeight(values: ResponsiveValues<T>): T {
    return when (heightSizeClass) {
        WindowHeightSizeClass.Compact -> values.compact
        WindowHeightSizeClass.Medium -> values.medium
        WindowHeightSizeClass.Expanded -> values.expanded
        else -> values.compact
    }
}

/**
 * Layout breakpoints and sizes
 */
object ResponsiveLayout {
    
    @Composable
    fun WindowSizeClass.isCompact(): Boolean = 
        widthSizeClass == WindowWidthSizeClass.Compact
    
    @Composable
    fun WindowSizeClass.isMedium(): Boolean = 
        widthSizeClass == WindowWidthSizeClass.Medium
    
    @Composable
    fun WindowSizeClass.isExpanded(): Boolean = 
        widthSizeClass == WindowWidthSizeClass.Expanded
    
    @Composable
    fun WindowSizeClass.isLandscape(): Boolean {
        val configuration = LocalConfiguration.current
        return configuration.screenWidthDp > configuration.screenHeightDp
    }
    
    @Composable
    fun WindowSizeClass.shouldUseDoubleColumn(): Boolean = !isCompact()
    
    @Composable
    fun WindowSizeClass.contentMaxWidth(): Dp {
        return selectByWidth(
            ResponsiveValues(
                compact = Dp.Unspecified,
                medium = 600.dp,
                expanded = 800.dp
            )
        )
    }
    
    @Composable
    fun WindowSizeClass.columnsCount(): Int {
        return selectByWidth(
            ResponsiveValues(
                compact = 1,
                medium = 2,
                expanded = 3
            )
        )
    }
}

/**
 * Spacing and dimensions
 */
object ResponsiveDimensions {
    
    @Composable
    fun WindowSizeClass.horizontalPadding(): Dp {
        return selectByWidth(
            ResponsiveValues(
                compact = 16.dp,
                medium = 24.dp,
                expanded = 32.dp
            )
        )
    }
    
    @Composable
    fun WindowSizeClass.verticalPadding(): Dp {
        return selectByHeight(
            ResponsiveValues(
                compact = 8.dp,
                medium = 16.dp,
                expanded = 24.dp
            )
        )
    }
    
    @Composable
    fun WindowSizeClass.sectionSpacing(): Dp {
        return selectByWidth(
            ResponsiveValues(
                compact = 16.dp,
                medium = 20.dp,
                expanded = 24.dp
            )
        )
    }
    
    @Composable
    fun WindowSizeClass.itemSpacing(): Dp {
        return selectByWidth(
            ResponsiveValues(
                compact = 8.dp,
                medium = 12.dp,
                expanded = 16.dp
            )
        )
    }
    
    @Composable
    fun WindowSizeClass.cornerRadius(): Dp {
        return selectByWidth(
            ResponsiveValues(
                compact = 8.dp,
                medium = 12.dp,
                expanded = 16.dp
            )
        )
    }
    
    @Composable
    fun WindowSizeClass.buttonSize(): Dp {
        return selectByWidth(
            ResponsiveValues(
                compact = 48.dp,
                medium = 56.dp,
                expanded = 64.dp
            )
        )
    }
    
    @Composable
    fun WindowSizeClass.mainButtonSize(): Dp {
        return selectByWidth(
            ResponsiveValues(
                compact = 64.dp,
                medium = 72.dp,
                expanded = 80.dp
            )
        )
    }
    
    @Composable
    fun WindowSizeClass.iconSize(): Dp {
        return selectByWidth(
            ResponsiveValues(
                compact = 24.dp,
                medium = 28.dp,
                expanded = 32.dp
            )
        )
    }
    
    @Composable
    fun WindowSizeClass.cardElevation(): Dp {
        return selectByWidth(
            ResponsiveValues(
                compact = 4.dp,
                medium = 6.dp,
                expanded = 8.dp
            )
        )
    }
    
    @Composable
    fun WindowSizeClass.musicPlayerHeight(): Dp {
        return selectByHeight(
            ResponsiveValues(
                compact = 160.dp,
                medium = 200.dp,
                expanded = 240.dp
            )
        )
    }
}

/**
 * Typography scaling
 */
object ResponsiveTypography {
    
    @Composable
    fun WindowSizeClass.scaleFactor(): Float {
        return selectByWidth(
            ResponsiveValues(
                compact = 1.0f,
                medium = 1.1f,
                expanded = 1.2f
            )
        )
    }
    
    @Composable
    fun WindowSizeClass.titleTextSize(): Float {
        return selectByWidth(
            ResponsiveValues(
                compact = 20f,
                medium = 24f,
                expanded = 28f
            )
        )
    }
    
    @Composable
    fun WindowSizeClass.bodyTextSize(): Float {
        return selectByWidth(
            ResponsiveValues(
                compact = 14f,
                medium = 16f,
                expanded = 18f
            )
        )
    }
}

/**
 * Adaptive layout modes
 */
enum class AdaptiveLayoutMode {
    SINGLE_COLUMN,
    COMPACT_DUAL_PANE,
    DUAL_COLUMN,
    TRIPLE_COLUMN
}

object ResponsiveBreakpoints {
    
    @Composable
    fun WindowSizeClass.isPhonePortrait(): Boolean {
        val configuration = LocalConfiguration.current
        return widthSizeClass == WindowWidthSizeClass.Compact && 
               configuration.screenHeightDp > configuration.screenWidthDp
    }
    
    @Composable
    fun WindowSizeClass.isPhoneLandscape(): Boolean {
        val configuration = LocalConfiguration.current
        return widthSizeClass == WindowWidthSizeClass.Compact && 
               configuration.screenHeightDp < configuration.screenWidthDp
    }
    
    @Composable
    fun WindowSizeClass.isTabletPortrait(): Boolean {
        val configuration = LocalConfiguration.current
        return (widthSizeClass == WindowWidthSizeClass.Medium || 
                widthSizeClass == WindowWidthSizeClass.Expanded) && 
               configuration.screenHeightDp > configuration.screenWidthDp
    }
    
    @Composable
    fun WindowSizeClass.isTabletLandscape(): Boolean {
        val configuration = LocalConfiguration.current
        return (widthSizeClass == WindowWidthSizeClass.Medium || 
                widthSizeClass == WindowWidthSizeClass.Expanded) && 
               configuration.screenHeightDp < configuration.screenWidthDp
    }
    
    @Composable
    fun WindowSizeClass.adaptiveLayoutMode(): AdaptiveLayoutMode {
        return when {
            isPhonePortrait() -> AdaptiveLayoutMode.SINGLE_COLUMN
            isPhoneLandscape() -> AdaptiveLayoutMode.COMPACT_DUAL_PANE
            isTabletPortrait() -> AdaptiveLayoutMode.DUAL_COLUMN
            isTabletLandscape() -> AdaptiveLayoutMode.TRIPLE_COLUMN
            else -> AdaptiveLayoutMode.SINGLE_COLUMN
        }
    }
}
