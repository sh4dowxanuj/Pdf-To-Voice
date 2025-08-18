package com.example.pdftovoice.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    windowSizeClass: WindowSizeClass,
    onNavigateToSignUp: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    val context = LocalContext.current

    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                viewModel.handleGoogleSignInResult(account?.idToken) { success, error ->
                    isLoading = false
                    if (success) {
                        onLoginSuccess()
                    } else {
                        errorMessage = error ?: "Google sign in failed"
                    }
                }
            } catch (e: ApiException) {
                isLoading = false
                errorMessage = when (e.statusCode) {
                    10 -> "Developer Error: Check SHA-1 fingerprint and Web Client ID configuration"
                    12501 -> "Google Sign-In was cancelled by user"
                    7 -> "Network error: Check internet connection"
                    else -> "Google sign in failed (${e.statusCode}): ${e.message}"
                }
                println("Google Sign-In Error: ${e.statusCode} - ${e.message}")
            }
        } else {
            isLoading = false
            errorMessage = "Google sign in was cancelled"
            println("Google Sign-In result code: ${result.resultCode}")
        }
    }

    LaunchedEffect(viewModel.authState.value) {
        if (viewModel.authState.value is AuthState.Success) {
            onLoginSuccess()
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
        Spacer(modifier = Modifier.height(verticalPadding * 2))
        
        // App Logo/Title
        Text(
            text = stringResource(id = R.string.app_name),
            fontSize = if (windowSizeClass.isCompact()) 28.sp else 32.sp,
            fontWeight = FontWeight.Bold,
            color = GoogleBlue,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(verticalPadding / 2))
        
        Text(
            text = stringResource(id = R.string.sign_in_to_continue),
            fontSize = if (windowSizeClass.isCompact()) 14.sp else 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(sectionSpacing * 2))
        
        // Login Form Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(cornerRadius),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(sectionSpacing),
                verticalArrangement = Arrangement.spacedBy(sectionSpacing)
            ) {
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
                    placeholder = stringResource(id = R.string.enter_your_password),
                    isError = errorMessage.isNotEmpty(),
                    errorMessage = if (errorMessage.isNotEmpty()) errorMessage else null
                )
                
                // Login Button
                ResponsiveButton(
                    onClick = {
                        if (email.isNotEmpty() && password.isNotEmpty()) {
                            isLoading = true
                            viewModel.signIn(email, password) { success, error ->
                                isLoading = false
                                if (!success) {
                                    errorMessage = error ?: context.getString(R.string.login_failed)
                                }
                            }
                        } else {
                            errorMessage = context.getString(R.string.fill_in_all_fields)
                        }
                    },
                    windowSizeClass = windowSizeClass,
            text = if (isLoading) stringResource(id = R.string.signing_in) else stringResource(id = R.string.sign_in),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue),
                    modifier = Modifier.fillMaxWidth()
                )
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
                // Google Sign In Button
                OutlinedButton(
                    onClick = { 
                        isLoading = true
                        errorMessage = ""
                        try {
                            val googleSignInClient = viewModel.authService.getGoogleSignInClient(context)
                            val signInIntent = googleSignInClient.signInIntent
                            googleSignInLauncher.launch(signInIntent)
                        } catch (e: IllegalStateException) {
                            isLoading = false
                            errorMessage = "Google Sign-In not configured. Check setup instructions."
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = "Google Sign-In error: ${e.message}"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(cornerRadius),
                    enabled = !isLoading
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
                            text = stringResource(id = R.string.sign_in_with_google),
                            fontSize = (16 * windowSizeClass.scaleFactor()).sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Sign Up Link
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.dont_have_account),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = (14 * windowSizeClass.scaleFactor()).sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    ResponsiveTextButton(
                        onClick = onNavigateToSignUp,
                        windowSizeClass = windowSizeClass,
                        text = stringResource(id = R.string.sign_up)
                    )
                }
            }
        }
    }
}
