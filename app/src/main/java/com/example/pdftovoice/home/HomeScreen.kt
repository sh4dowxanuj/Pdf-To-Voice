package com.example.pdftovoice.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.pdftovoice.ui.theme.GoogleBlue
import com.example.pdftovoice.ui.responsive.ResponsiveDimensions.horizontalPadding
import com.example.pdftovoice.ui.responsive.ResponsiveDimensions.verticalPadding
import com.example.pdftovoice.ui.responsive.ResponsiveDimensions.sectionSpacing
import com.example.pdftovoice.ui.responsive.ResponsiveDimensions.mainButtonSize
import com.example.pdftovoice.ui.responsive.ResponsiveLayout.contentMaxWidth
import com.example.pdftovoice.ui.responsive.ResponsiveTypography.scaleFactor

@Composable
fun HomeScreen(
    windowSizeClass: WindowSizeClass,
    onNavigateToPdfReader: () -> Unit = {}
) {
    // Responsive dimensions
    val horizontalPadding = windowSizeClass.horizontalPadding()
    val verticalPadding = windowSizeClass.verticalPadding()
    val sectionSpacing = windowSizeClass.sectionSpacing()
    val mainButtonSize = windowSizeClass.mainButtonSize()
    val contentMaxWidth = windowSizeClass.contentMaxWidth()
    val scaleFactor = windowSizeClass.scaleFactor()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = contentMaxWidth)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding * 2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to PDF to Voice!",
            fontSize = (24 * scaleFactor).sp,
            fontWeight = FontWeight.Bold,
            color = GoogleBlue
        )
        
        Spacer(modifier = Modifier.height(sectionSpacing))
        
        Text(
            text = "You have successfully logged in.",
            fontSize = (16 * scaleFactor).sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(sectionSpacing * 2))
        
        Button(
            onClick = onNavigateToPdfReader,
            modifier = Modifier.height(mainButtonSize),
            colors = ButtonDefaults.buttonColors(
                containerColor = GoogleBlue
            )
        ) {
            Text(
                "Start Reading PDFs",
                fontSize = (16 * scaleFactor).sp
            )
        }
    }
}
