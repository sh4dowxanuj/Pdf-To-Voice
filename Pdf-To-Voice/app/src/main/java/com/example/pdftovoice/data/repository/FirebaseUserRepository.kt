package com.example.pdftovoice.data.repository

import com.example.pdftovoice.data.entity.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class FirebaseUserRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    
    suspend fun registerUser(email: String, username: String, password: String): Result<String> {
        return try {
            // Check if email already exists
            val emailQuery = usersCollection
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            
            if (!emailQuery.isEmpty) {
                return Result.failure(Exception("Email already exists"))
            }
            
            // Check if username already exists
            val usernameQuery = usersCollection
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .await()
            
            if (!usernameQuery.isEmpty) {
                return Result.failure(Exception("Username already exists"))
            }
            
            // Hash password
            val hashedPassword = hashPassword(password)
            
            // Create user document
            val user = User(
                email = email,
                username = username,
                password = hashedPassword
            )
            
            // Add user to Firestore
            val documentRef = usersCollection.add(user).await()
            Result.success(documentRef.id)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun loginUser(emailOrUsername: String, password: String): Result<User> {
        return try {
            val hashedPassword = hashPassword(password)
            
            // Try to find user by email first
            var query = usersCollection
                .whereEqualTo("email", emailOrUsername)
                .whereEqualTo("password", hashedPassword)
                .limit(1)
                .get()
                .await()
            
            // If not found by email, try by username
            if (query.isEmpty) {
                query = usersCollection
                    .whereEqualTo("username", emailOrUsername)
                    .whereEqualTo("password", hashedPassword)
                    .limit(1)
                    .get()
                    .await()
            }
            
            if (!query.isEmpty) {
                val userDoc = query.documents[0]
                val user = userDoc.toObject(User::class.java)?.copy(id = userDoc.id)
                if (user != null) {
                    Result.success(user)
                } else {
                    Result.failure(Exception("Invalid email/username or password"))
                }
            } else {
                Result.failure(Exception("Invalid email/username or password"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserById(userId: String): Result<User?> {
        return try {
            val document = usersCollection.document(userId).get().await()
            if (document.exists()) {
                val user = document.toObject(User::class.java)?.copy(id = document.id)
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.id).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            usersCollection.document(userId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val snapshot = usersCollection
                .orderBy("created_at", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val users = snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(id = doc.id)
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
