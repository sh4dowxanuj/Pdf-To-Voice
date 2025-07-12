package com.example.pdftovoice.ui.responsive

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Responsive utilities for adapting UI to different screen sizes
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
    fun WindowSizeClass.cornerRadius(): Dp {
        return selectByWidth(
            ResponsiveValues(
                compact = 8.dp,
                medium = 12.dp,
                expanded = 16.dp
            )
        )
    }
}

object ResponsiveLayout {
    
    @Composable
    fun WindowSizeClass.isCompact(): Boolean {
        return widthSizeClass == WindowWidthSizeClass.Compact
    }
    
    @Composable
    fun WindowSizeClass.isMedium(): Boolean {
        return widthSizeClass == WindowWidthSizeClass.Medium
    }
    
    @Composable
    fun WindowSizeClass.isExpanded(): Boolean {
        return widthSizeClass == WindowWidthSizeClass.Expanded
    }
    
    @Composable
    fun WindowSizeClass.isLandscape(): Boolean {
        val configuration = LocalConfiguration.current
        return configuration.screenWidthDp > configuration.screenHeightDp
    }
    
    @Composable
    fun WindowSizeClass.shouldUseBottomSheet(): Boolean {
        return isCompact() || (isMedium() && !isLandscape())
    }
    
    @Composable
    fun WindowSizeClass.shouldUseSidePanel(): Boolean {
        return isExpanded() || (isMedium() && isLandscape())
    }
    
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
    fun WindowSizeClass.shouldUseDoubleColumn(): Boolean {
        return !isCompact()
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
 * Typography scaling based on screen size
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
 * Responsive grid and layout utilities
 */
object ResponsiveGrid {
    
    @Composable
    fun WindowSizeClass.gridColumns(): Int {
        return selectByWidth(
            ResponsiveValues(
                compact = 1,
                medium = 2,
                expanded = 3
            )
        )
    }
    
    @Composable
    fun WindowSizeClass.listItemHeight(): Dp {
        return selectByWidth(
            ResponsiveValues(
                compact = 56.dp,
                medium = 64.dp,
                expanded = 72.dp
            )
        )
    }
    
    @Composable
    fun WindowSizeClass.fabSize(): Dp {
        return selectByWidth(
            ResponsiveValues(
                compact = 56.dp,
                medium = 64.dp,
                expanded = 72.dp
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
}

/**
 * Responsive breakpoint utilities
 */
object ResponsiveBreakpoints {
    
    @Composable
    fun WindowSizeClass.isPhonePortrait(): Boolean {
        val configuration = LocalConfiguration.current
        return widthSizeClass == WindowWidthSizeClass.Compact && configuration.screenHeightDp > configuration.screenWidthDp
    }
    
    @Composable
    fun WindowSizeClass.isPhoneLandscape(): Boolean {
        val configuration = LocalConfiguration.current
        return widthSizeClass == WindowWidthSizeClass.Compact && configuration.screenHeightDp < configuration.screenWidthDp
    }
    
    @Composable
    fun WindowSizeClass.isTabletPortrait(): Boolean {
        val configuration = LocalConfiguration.current
        return (widthSizeClass == WindowWidthSizeClass.Medium || widthSizeClass == WindowWidthSizeClass.Expanded) && configuration.screenHeightDp > configuration.screenWidthDp
    }
    
    @Composable
    fun WindowSizeClass.isTabletLandscape(): Boolean {
        val configuration = LocalConfiguration.current
        return (widthSizeClass == WindowWidthSizeClass.Medium || widthSizeClass == WindowWidthSizeClass.Expanded) && configuration.screenHeightDp < configuration.screenWidthDp
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

enum class AdaptiveLayoutMode {
    SINGLE_COLUMN,
    COMPACT_DUAL_PANE,
    DUAL_COLUMN,
    TRIPLE_COLUMN
}
