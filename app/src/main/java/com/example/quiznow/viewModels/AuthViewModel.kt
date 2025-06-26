package com.example.quiznow.viewModels

import QuizViewModel
import UserManager
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quiznow.authentication.AuthResult
import kotlinx.coroutines.launch

class AuthViewModel(private val userManager: UserManager , private val quizViewModel: QuizViewModel) : ViewModel() {
    private val _authState = mutableStateOf<AuthResult?>(null)
    val authState: State<AuthResult?> = _authState

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = userManager.signUpWithEmail(email, password)
            _authState.value = result
            if (result is AuthResult.Success) {
                quizViewModel.refreshUserData() // Refresh data after successful sign up
            }
            _isLoading.value = false
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = userManager.signInWithEmail(email, password)
            _authState.value = result
            if (result is AuthResult.Success) {
                quizViewModel.refreshUserData() // Refresh data after successful sign in
            }
            _isLoading.value = false
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = userManager.signInWithGoogle(idToken)
            _authState.value = result
            if (result is AuthResult.Success) {
                quizViewModel.refreshUserData() // Refresh data after successful Google sign in
            }
            _isLoading.value = false
        }
    }

    fun handleGoogleSignInResult(data: android.content.Intent?) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = userManager.handleGoogleSignInResult(data)
            _authState.value = result
            if (result is AuthResult.Success) {
                quizViewModel.refreshUserData() // Refresh data after successful Google sign in
            }
            _isLoading.value = false
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = userManager.resetPassword(email)
            _isLoading.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = userManager.signOut()
            _authState.value = result
            quizViewModel.refreshUserData() // Clear data after sign out
            _isLoading.value = false
        }
    }

    fun clearAuthState() {
        _authState.value = null
    }

    fun isUserLoggedIn(): Boolean = userManager.isUserLoggedIn()
}