package com.untold.android.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.untold.android.data.api.ApiClient
import com.untold.android.data.local.TokenManager
import com.untold.android.data.model.LoginRequest
import com.untold.android.data.model.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val username: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenManager = TokenManager(application)
    private val apiService = ApiClient.getApiService(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _isLoggedIn = MutableStateFlow(tokenManager.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    fun getUsername(): String = tokenManager.getUsername() ?: ""
    fun getEmail(): String = tokenManager.getEmail() ?: ""
    fun getUserId(): Long = tokenManager.getUserId()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = apiService.login(LoginRequest(email = email, password = password))
                if (response.isSuccessful && response.body()?.success == true) {
                    val authData = response.body()!!.data
                    tokenManager.saveAuthData(
                        token = authData.token,
                        userId = authData.user.id,
                        username = authData.user.username,
                        email = authData.user.email
                    )
                    _isLoggedIn.value = true
                    _authState.value = AuthState.Success(authData.user.username)
                } else {
                    val errMsg = response.body()?.message ?: "Invalid email or password"
                    _authState.value = AuthState.Error(errMsg)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Connection error: ${e.localizedMessage ?: "Failed to connect to server"}")
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = apiService.register(RegisterRequest(username = username, email = email, password = password, userType = "ATTENDEE"))
                if (response.isSuccessful && response.body()?.success == true) {
                    val authData = response.body()!!.data
                    tokenManager.saveAuthData(
                        token = authData.token,
                        userId = authData.user.id,
                        username = authData.user.username,
                        email = authData.user.email
                    )
                    _isLoggedIn.value = true
                    _authState.value = AuthState.Success(authData.user.username)
                } else {
                    val errMsg = response.body()?.message ?: "Registration failed. Username/Email might be taken."
                    _authState.value = AuthState.Error(errMsg)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Connection error: ${e.localizedMessage ?: "Failed to connect to server"}")
            }
        }
    }

    fun logout() {
        tokenManager.clearAuthData()
        _isLoggedIn.value = false
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
