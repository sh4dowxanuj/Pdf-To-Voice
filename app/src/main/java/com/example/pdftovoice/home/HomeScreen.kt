package com.example.pdftovoice.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pdftovoice.ui.theme.GoogleBlue

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to PDF to Voice!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = GoogleBlue
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "You have successfully logged in.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { /* TODO: Add PDF upload functionality */ },
            colors = ButtonDefaults.buttonColors(
                containerColor = GoogleBlue
            )
        ) {
            Text("Upload PDF")
        }
    }
}
