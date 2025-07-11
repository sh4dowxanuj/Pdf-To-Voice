package com.example.pdftovoice.auth

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    val authService = AuthService()
    val authState = mutableStateOf<AuthState>(AuthState.Idle)
    
    fun signIn(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            authState.value = AuthState.Loading
            val result = authService.signInWithEmailAndPassword(email, password)
            
            if (result.isSuccess) {
                authState.value = AuthState.Success
                onResult(true, null)
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Sign in failed"
                authState.value = AuthState.Error(errorMessage)
                onResult(false, errorMessage)
            }
        }
    }
    
    fun signUp(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            authState.value = AuthState.Loading
            val result = authService.createUserWithEmailAndPassword(email, password)
            
            if (result.isSuccess) {
                authState.value = AuthState.Success
                onResult(true, null)
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Sign up failed"
                authState.value = AuthState.Error(errorMessage)
                onResult(false, errorMessage)
            }
        }
    }
    
    fun signOut() {
        authService.signOut()
        authState.value = AuthState.Idle
    }

    fun signInWithGoogle(context: Context, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            authState.value = AuthState.Loading
            try {
                val googleSignInClient = authService.getGoogleSignInClient(context)
                val task = googleSignInClient.silentSignIn()
                
                if (task.isSuccessful) {
                    // Already signed in
                    val account = task.result
                    val idToken = account?.idToken
                    if (idToken != null) {
                        val result = authService.signInWithGoogle(idToken)
                        if (result.isSuccess) {
                            authState.value = AuthState.Success
                            onResult(true, null)
                        } else {
                            val errorMessage = result.exceptionOrNull()?.message ?: "Google sign in failed"
                            authState.value = AuthState.Error(errorMessage)
                            onResult(false, errorMessage)
                        }
                    }
                } else {
                    // Need to sign in
                    authState.value = AuthState.Error("Please use the Google Sign-In button")
                    onResult(false, "Please use the Google Sign-In button")
                }
            } catch (e: Exception) {
                authState.value = AuthState.Error(e.message ?: "Google sign in failed")
                onResult(false, e.message)
            }
        }
    }

    fun handleGoogleSignInResult(idToken: String?, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            authState.value = AuthState.Loading
            if (idToken != null) {
                val result = authService.signInWithGoogle(idToken)
                if (result.isSuccess) {
                    authState.value = AuthState.Success
                    onResult(true, null)
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Google sign in failed"
                    authState.value = AuthState.Error(errorMessage)
                    onResult(false, errorMessage)
                }
            } else {
                authState.value = AuthState.Error("Google sign in was cancelled")
                onResult(false, "Google sign in was cancelled")
            }
        }
    }
}
