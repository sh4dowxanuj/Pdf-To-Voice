package com.example.pdftovoice.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.pdftovoice.R
import com.example.pdftovoice.ui.responsive.ResponsiveTypography.scaleFactor

/**
 * Responsive text field component that adapts to different screen sizes
 */
@Composable
fun ResponsiveTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorMessage: String? = null,
    isPassword: Boolean = false,
    placeholder: String? = null
) {
    var passwordVisible by remember { mutableStateOf(!isPassword) }
    val scaleFactor = windowSizeClass.scaleFactor()
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { 
            Text(
                label,
                fontSize = (14 * scaleFactor).sp
            ) 
        },
        placeholder = placeholder?.let { { 
            Text(
                it,
                fontSize = (14 * scaleFactor).sp
            ) 
        } },
        leadingIcon = leadingIcon?.let { {
            Icon(
                imageVector = it,
                contentDescription = androidx.compose.ui.res.stringResource(id = com.example.pdftovoice.R.string.select_language)
            )
        } },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) stringResource(id = R.string.hide_password) else stringResource(id = R.string.show_password)
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        isError = isError,
        supportingText = if (isError && errorMessage != null) {
            {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = (12 * scaleFactor).sp
                )
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        textStyle = LocalTextStyle.current.copy(
            fontSize = (16 * scaleFactor).sp
        ),
        modifier = modifier
    )
}

/**
 * Responsive button component that adapts to different screen sizes
 */
@Composable
fun ResponsiveButton(
    onClick: () -> Unit,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors()
) {
    val scaleFactor = windowSizeClass.scaleFactor()
    
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = colors,
        modifier = modifier
    ) {
        Text(
            text = text,
            fontSize = (16 * scaleFactor).sp
        )
    }
}

/**
 * Responsive text button component
 */
@Composable
fun ResponsiveTextButton(
    onClick: () -> Unit,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true
) {
    val scaleFactor = windowSizeClass.scaleFactor()
    
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) {
        Text(
            text = text,
            fontSize = (14 * scaleFactor).sp
        )
    }
}
