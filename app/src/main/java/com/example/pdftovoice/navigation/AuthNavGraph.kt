package com.example.pdftovoice.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pdftovoice.auth.AuthState
import com.example.pdftovoice.auth.AuthViewModel
import com.example.pdftovoice.auth.LoginScreen
import com.example.pdftovoice.auth.SignUpScreen
import com.example.pdftovoice.ui.screens.PdfToVoiceScreen

@Composable
fun AuthNavGraph(windowSizeClass: WindowSizeClass) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState
    
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                navController.navigate("home") {
                    popUpTo(0) { inclusive = true }
                }
            }
            is AuthState.Idle -> {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
            else -> { /* Handle other states */ }
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = if (authViewModel.authService.isUserLoggedIn()) "home" else "login"
    ) {
        composable("login") {
            LoginScreen(
                windowSizeClass = windowSizeClass,
                onNavigateToSignUp = {
                    navController.navigate("signup")
                },
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        
        composable("signup") {
            SignUpScreen(
                windowSizeClass = windowSizeClass,
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onSignUpSuccess = {
                    navController.navigate("home") {
                        popUpTo("signup") { inclusive = true }
                    }
                }
            )
        }
        
        composable("home") {
            PdfToVoiceScreen(
                windowSizeClass = windowSizeClass,
                onLogout = {
                    authViewModel.signOut()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
