package com.example.rmasprojekat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rmasprojekat.model.Status
import com.example.rmasprojekat.model.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext



class AuthViewModel : ViewModel() {
    private var auth: FirebaseAuth = Firebase.auth
    private val db = Firebase.firestore

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    val userCache = mutableMapOf<String, User?>() // User cache

    init {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    val user = getUserFromFirestore(currentUser.uid)
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated
                } catch (e: Exception) {
                    _authState.value = AuthState.Error(e.message ?: "Failed to fetch user data")
                }
            }
        } else {
            _authState.value = AuthState.Initial
        }
    }

    fun signUp(email: String, password: String, username: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user
                if (firebaseUser != null) {
                    val user = User(
                        id = firebaseUser.uid,
                        username = username,
                        email = email,
                        memberSince = Timestamp.now(),
                        postCount = 0,
                        likesCount = 0
                    )
                    saveUserToFirestore(user)
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value = AuthState.Error("Failed to create user")

                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign up failed")
                println("Error during signUp: ${e.message}")

            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user
                if (firebaseUser != null) {
                    val user = getUserFromFirestore(firebaseUser.uid)
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value = AuthState.Error("Failed to sign in")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _currentUser.value = null
        _authState.value = AuthState.Initial
    }

    fun fetchUserDetails(userRef: DocumentReference, forceRefresh: Boolean = false, onResult: (User?) -> Unit) {
        val userId = userRef.id

        if (!forceRefresh && userCache.containsKey(userId)) {
            onResult(userCache[userId])
            return
        }

        viewModelScope.launch {
            try {
                val user = getUserFromFirestore(userId)
                userCache[userId] = user // Cache the user data
                onResult(user)
            } catch (e: Exception) {
                // Handle error (e.g., log the error or show a message)
                onResult(null)
            }
        }
    }

    fun refreshCurrentUser() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            try {
                val user = getUserFromFirestore(userId)
                _currentUser.value = user
            } catch (e: Exception) {
                // Handle error, e.g., log or display a message
                println("Error refreshing user: ${e.message}")
            }
        }
    }



    fun updateProfileImageUrl(imageUrl: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .update("imageUrl", imageUrl)
                    .await()
                _currentUser.value = _currentUser.value?.copy(imageUrl = imageUrl)
            } catch (e: Exception) {
                println("Error updating profile image URL: ${e.message}")
            }
        }
    }

    fun deleteStatus(status: Status, canteenViewModel: CanteenViewModel) {
        val currentCanteen = canteenViewModel.getCurrentCanteen()
        val canteenId = currentCanteen?.id ?: ""  // Get the canteen ID

        viewModelScope.launch {
            try {
                status.id?.let { statusId ->
                    // Delete the status from the canteen's statuses subcollection
                    db.collection("canteens").document(canteenId)
                        .collection("statuses").document(statusId)
                        .delete().await()

                    println("Status deleted successfully from canteen $canteenId")

                    // Update the user's post count
                    val userId = auth.currentUser?.uid ?: return@launch
                    val userRef = db.collection("users").document(userId)

                    db.runTransaction { transaction ->
                        val snapshot = transaction.get(userRef)
                        val currentPostCount = snapshot.getLong("postCount") ?: 0
                        val newPostCount = maxOf(currentPostCount - 1, 0) // Ensure it doesn't go below 0
                        transaction.update(userRef, "postCount", newPostCount)
                    }.await()

                    println("User post count updated")

                    // Refresh the current user to reflect the updated post count
                    refreshCurrentUser()
                    canteenViewModel.fetchStatusesForCurrentCanteen()

                } ?: run {
                    println("Status ID is null. Cannot delete.")
                }
            } catch (e: Exception) {
                println("Error deleting status: ${e.message}")
                e.printStackTrace()
            }
        }
    }




// refreshCurrentUser()

    private suspend fun saveUserToFirestore(user: User) {
        withContext(Dispatchers.IO) {
            db.collection("users").document(user.id).set(user).await()
        }
    }

    private suspend fun getUserFromFirestore(uid: String): User {
        return withContext(Dispatchers.IO) {
            val document = db.collection("users").document(uid).get().await()
            document.toObject(User::class.java) ?: throw Exception("User data not found")
        }
    }
}


sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}