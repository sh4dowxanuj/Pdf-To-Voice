package com.example.pdftovoice.auth

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val authService = AuthService()
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
}
