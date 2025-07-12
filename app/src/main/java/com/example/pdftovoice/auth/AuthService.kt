package com.example.pdftovoice.auth

import android.content.Context
import com.example.pdftovoice.data.UserPreferences
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthService(private val userPreferences: UserPreferences? = null) {
    private val auth = FirebaseAuth.getInstance()
    
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null && (userPreferences?.isUserLoggedIn() ?: true)
    }

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val webClientId = try {
            context.getString(com.example.pdftovoice.R.string.default_web_client_id)
        } catch (e: Exception) {
            "YOUR_WEB_CLIENT_ID" // Fallback value
        }
        
        if (webClientId == "YOUR_WEB_CLIENT_ID") {
            throw IllegalStateException(
                "Google Sign-In not configured. Please follow the setup instructions in GOOGLE_SIGNIN_SETUP.md"
            )
        }
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user!!
            
            // Save user login state
            userPreferences?.saveUserLogin(user.uid, user.email ?: "")
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user!!
            
            // Save user login state
            userPreferences?.saveUserLogin(user.uid, user.email ?: "")
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createUserWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user!!
            
            // Save user login state
            userPreferences?.saveUserLogin(user.uid, user.email ?: "")
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun signOut() {
        auth.signOut()
        userPreferences?.clearUserLogin()
    }
}
