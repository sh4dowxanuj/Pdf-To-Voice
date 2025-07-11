package com.example.pdftovoice.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdftovoice.data.entity.User
import com.example.pdftovoice.data.repository.FirebaseUserRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    
    private val userRepository = FirebaseUserRepository()
    
    private val _loginResult = MutableLiveData<Result<User>>()
    val loginResult: LiveData<Result<User>> = _loginResult
    
    private val _registerResult = MutableLiveData<Result<String>>()
    val registerResult: LiveData<Result<String>> = _registerResult
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    fun login(emailOrUsername: String, password: String) {
        if (!isValidInput(emailOrUsername, password)) {
            _loginResult.value = Result.failure(Exception("Please fill in all fields"))
            return
        }
        
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = userRepository.loginUser(emailOrUsername, password)
                _loginResult.value = result
            } catch (e: Exception) {
                _loginResult.value = Result.failure(Exception("Login failed: ${e.message ?: "Unknown error"}"))
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun register(email: String, username: String, password: String, confirmPassword: String) {
        if (!isValidInput(email, username, password, confirmPassword)) {
            _registerResult.value = Result.failure(Exception("Please fill in all fields"))
            return
        }
        
        if (!isValidEmail(email)) {
            _registerResult.value = Result.failure(Exception("Please enter a valid email"))
            return
        }
        
        if (!isValidPassword(password)) {
            _registerResult.value = Result.failure(Exception("Password must be at least 6 characters long"))
            return
        }
        
        if (password != confirmPassword) {
            _registerResult.value = Result.failure(Exception("Passwords do not match"))
            return
        }
        
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = userRepository.registerUser(email, username, password)
                _registerResult.value = result
            } catch (e: Exception) {
                _registerResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun isValidInput(vararg inputs: String): Boolean {
        return inputs.all { it.isNotBlank() }
    }
    
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    private fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }
}
