package com.example.pdftovoice.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pdftovoice.R
import com.example.pdftovoice.ui.theme.GoogleBlue
import com.example.pdftovoice.ui.components.ResponsiveTextField
import com.example.pdftovoice.ui.components.ResponsiveButton
import com.example.pdftovoice.ui.components.ResponsiveTextButton
import com.example.pdftovoice.ui.responsive.ResponsiveDimensions.horizontalPadding
import com.example.pdftovoice.ui.responsive.ResponsiveDimensions.verticalPadding
import com.example.pdftovoice.ui.responsive.ResponsiveDimensions.sectionSpacing
import com.example.pdftovoice.ui.responsive.ResponsiveDimensions.cornerRadius
import com.example.pdftovoice.ui.responsive.ResponsiveLayout.contentMaxWidth
import com.example.pdftovoice.ui.responsive.ResponsiveLayout.isCompact
import com.example.pdftovoice.ui.responsive.ResponsiveTypography.scaleFactor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    windowSizeClass: WindowSizeClass,
    onNavigateToLogin: () -> Unit,
    onSignUpSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    // Password field visibility handled within component
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(viewModel.authState.value) {
        if (viewModel.authState.value is AuthState.Success) {
            onSignUpSuccess()
        }
    }

    // Responsive dimensions
    val horizontalPadding = windowSizeClass.horizontalPadding()
    val verticalPadding = windowSizeClass.verticalPadding()
    val sectionSpacing = windowSizeClass.sectionSpacing()
    val cornerRadius = windowSizeClass.cornerRadius()
    val contentMaxWidth = windowSizeClass.contentMaxWidth()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .widthIn(max = contentMaxWidth)
            .padding(horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = verticalPadding),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onNavigateToLogin) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(id = R.string.back),
                    tint = GoogleBlue
                )
            }
        }
        
        Spacer(modifier = Modifier.height(sectionSpacing))
        
        // Title
        Text(
            text = stringResource(id = R.string.create_account),
            fontSize = if (windowSizeClass.isCompact()) 28.sp else 32.sp,
            fontWeight = FontWeight.Bold,
            color = GoogleBlue,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(verticalPadding / 2))
        
        Text(
            text = stringResource(id = R.string.sign_up_to_get_started),
            fontSize = (if (windowSizeClass.isCompact()) 14f else 16f * windowSizeClass.scaleFactor()).sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(sectionSpacing * 2))
        
        // Full Name Field
        ResponsiveTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = stringResource(id = R.string.full_name),
            windowSizeClass = windowSizeClass,
            leadingIcon = Icons.Default.Person,
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(id = R.string.enter_your_full_name)
        )
        
        // Email Field
        ResponsiveTextField(
            value = email,
            onValueChange = { email = it },
            label = stringResource(id = R.string.email),
            windowSizeClass = windowSizeClass,
            leadingIcon = Icons.Default.Email,
            keyboardType = KeyboardType.Email,
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(id = R.string.enter_your_email)
        )
        
        // Password Field
        ResponsiveTextField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(id = R.string.password),
            windowSizeClass = windowSizeClass,
            leadingIcon = Icons.Default.Lock,
            keyboardType = KeyboardType.Password,
            isPassword = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(id = R.string.enter_your_password)
        )
        
        // Confirm Password Field
        ResponsiveTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = stringResource(id = R.string.confirm_password),
            windowSizeClass = windowSizeClass,
            leadingIcon = Icons.Default.Lock,
            keyboardType = KeyboardType.Password,
            isPassword = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(id = R.string.confirm_your_password),
            isError = errorMessage.isNotEmpty(),
            errorMessage = if (errorMessage.isNotEmpty()) errorMessage else null
        )
        
        // Sign Up Button
        ResponsiveButton(
            onClick = {
                when {
                    fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() -> {
                        errorMessage = context.getString(R.string.fill_in_all_fields)
                    }
                    password != confirmPassword -> {
                        errorMessage = context.getString(R.string.passwords_dont_match)
                    }
                    password.length < 6 -> {
                        errorMessage = context.getString(R.string.password_too_short)
                    }
                    else -> {
                        isLoading = true
                        viewModel.signUp(email, password) { success, error ->
                            isLoading = false
                            if (!success) {
                                errorMessage = error ?: context.getString(R.string.signup_failed)
                            }
                        }
                    }
                }
            },
            windowSizeClass = windowSizeClass,
            text = if (isLoading) stringResource(id = R.string.creating_account) else stringResource(id = R.string.sign_up),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(sectionSpacing))
        
        // Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(id = R.string.or),
                modifier = Modifier.padding(horizontal = sectionSpacing),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = (14 * windowSizeClass.scaleFactor()).sp
            )
            Divider(modifier = Modifier.weight(1f))
        }
        
        // Google Sign Up Button (placeholder for now)
        OutlinedButton(
            onClick = { /* TODO: Implement Google Sign Up */ },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(cornerRadius)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Google icon placeholder
                Box(
                    modifier = Modifier
                        .size((20 * windowSizeClass.scaleFactor()).dp)
                        .background(Color.Red, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(sectionSpacing))
                Text(
                    text = stringResource(id = R.string.sign_up_with_google),
                    fontSize = (16 * windowSizeClass.scaleFactor()).sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Sign In Link
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.already_have_account),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = (14 * windowSizeClass.scaleFactor()).sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            ResponsiveTextButton(
                onClick = onNavigateToLogin,
                windowSizeClass = windowSizeClass,
                text = stringResource(id = R.string.sign_in)
            )
        }
    }
}
