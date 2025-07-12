package com.example.pdftovoice.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

@Composable
fun GoogleIcon(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.size(20.dp)
    ) {
        drawGoogleIcon()
    }
}

private fun DrawScope.drawGoogleIcon() {
    val size = this.size.minDimension
    val centerX = size / 2f
    val centerY = size / 2f
    val radius = size * 0.4f
    
    // Google "G" colors
    val googleRed = Color(0xFFEA4335)
    val googleYellow = Color(0xFFFBBC05)
    val googleGreen = Color(0xFF34A853)
    val googleBlue = Color(0xFF4285F4)
    
    // Draw the Google "G" shape
    // Red part (top)
    val redPath = Path().apply {
        moveTo(centerX + radius * 0.5f, centerY - radius * 0.8f)
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius
            ),
            startAngleDegrees = -60f,
            sweepAngleDegrees = 120f,
            forceMoveTo = false
        )
        lineTo(centerX + radius * 0.3f, centerY)
        lineTo(centerX + radius * 0.5f, centerY - radius * 0.3f)
        close()
    }
    
    drawPath(redPath, googleRed)
    
    // Yellow part (bottom right)
    val yellowPath = Path().apply {
        moveTo(centerX + radius * 0.5f, centerY + radius * 0.8f)
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius
            ),
            startAngleDegrees = 60f,
            sweepAngleDegrees = 60f,
            forceMoveTo = false
        )
    }
    
    drawPath(yellowPath, googleYellow)
    
    // Green part (bottom left)
    val greenPath = Path().apply {
        moveTo(centerX - radius * 0.5f, centerY + radius * 0.8f)
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius
            ),
            startAngleDegrees = 120f,
            sweepAngleDegrees = 60f,
            forceMoveTo = false
        )
    }
    
    drawPath(greenPath, googleGreen)
    
    // Blue part (left)
    val bluePath = Path().apply {
        moveTo(centerX - radius * 0.5f, centerY - radius * 0.8f)
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius
            ),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 120f,
            forceMoveTo = false
        )
    }
    
    drawPath(bluePath, googleBlue)
    
    // Draw center white circle for the "G" hole
    drawCircle(
        color = Color.White,
        radius = radius * 0.3f,
        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
    )
    
    // Draw the horizontal line of the "G"
    drawRect(
        color = googleBlue,
        topLeft = androidx.compose.ui.geometry.Offset(centerX, centerY - radius * 0.1f),
        size = androidx.compose.ui.geometry.Size(radius * 0.5f, radius * 0.2f)
    )
}
