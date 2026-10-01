package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.network.NetworkErrorDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val OWNER_USERNAME = "Snehasis"
        const val OWNER_SECURITY_CODE = "Snehasis@2007"
    }

    private val sharedPrefs = application.getSharedPreferences("omniagent_auth", Context.MODE_PRIVATE)

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _networkErrorDetails = MutableStateFlow<NetworkErrorDetails?>(null)
    val networkErrorDetails: StateFlow<NetworkErrorDetails?> = _networkErrorDetails

    init {
        // Hydrate session if exists
        val savedEmail = sharedPrefs.getString("email", null)
        val token = sharedPrefs.getString("token", null)
        if (savedEmail != null && token != null) {
            _userEmail.value = savedEmail
        }
    }

    fun activateAsOwner() {
        signIn(OWNER_USERNAME, OWNER_SECURITY_CODE)
    }

    fun signIn(email: String, javaPassword: String) {
        val cleanUser = email.trim()
        val cleanCode = javaPassword.trim()
        if (cleanUser.isBlank() || cleanCode.isBlank()) {
            _errorMessage.value = "Username and Security Code are required."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        _networkErrorDetails.value = null

        viewModelScope.launch {
            // Check if Owner credentials crafted by owner
            if (cleanUser.equals(OWNER_USERNAME, ignoreCase = true) && cleanCode == OWNER_SECURITY_CODE) {
                sharedPrefs.edit().apply {
                    putString("pwd_${OWNER_USERNAME.lowercase()}", OWNER_SECURITY_CODE)
                    putString("email", OWNER_USERNAME)
                    putString("token", "omni_owner_deck_${System.currentTimeMillis()}")
                    apply()
                }
                _userEmail.value = OWNER_USERNAME
                _errorMessage.value = null
                _networkErrorDetails.value = null
                _isLoading.value = false
                return@launch
            }

            val storedPassword = sharedPrefs.getString("pwd_${cleanUser.lowercase()}", null)
            if (storedPassword != null) {
                if (storedPassword == cleanCode) {
                    sharedPrefs.edit().apply {
                        putString("email", cleanUser)
                        putString("token", "omni_auth_session_${System.currentTimeMillis()}")
                        apply()
                    }
                    _userEmail.value = cleanUser
                    _errorMessage.value = null
                    _networkErrorDetails.value = null
                } else {
                    _errorMessage.value = "Invalid Security Code for $cleanUser. Check security code crafted by owner."
                }
            } else {
                // If first time supervisor access with this username, establish deck
                sharedPrefs.edit().apply {
                    putString("pwd_${cleanUser.lowercase()}", cleanCode)
                    putString("email", cleanUser)
                    putString("token", "omni_auth_session_${System.currentTimeMillis()}")
                    apply()
                }
                _userEmail.value = cleanUser
                _errorMessage.value = null
                _networkErrorDetails.value = null
            }
            _isLoading.value = false
        }
    }

    fun loginLocalOffline(email: String = OWNER_USERNAME) {
        val cleanEmail = email.trim().ifEmpty { OWNER_USERNAME }
        sharedPrefs.edit().apply {
            putString("email", cleanEmail)
            putString("token", "local_offline_token_bypass")
            apply()
        }
        _userEmail.value = cleanEmail
    }

    fun signUp(email: String, javaPassword: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || javaPassword.isBlank()) {
            _errorMessage.value = "All fields are required."
            return
        }
        if (javaPassword.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        _networkErrorDetails.value = null

        viewModelScope.launch {
            val existing = sharedPrefs.getString("pwd_${cleanEmail.lowercase()}", null)
            if (existing != null) {
                _errorMessage.value = "Account already exists for $cleanEmail. Please sign in with your password."
            } else {
                sharedPrefs.edit().apply {
                    putString("pwd_${cleanEmail.lowercase()}", javaPassword)
                    putString("email", cleanEmail)
                    putString("token", "omni_auth_session_${System.currentTimeMillis()}")
                    apply()
                }
                _userEmail.value = cleanEmail
                _errorMessage.value = null
                _networkErrorDetails.value = null
            }
            _isLoading.value = false
        }
    }

    fun signOut() {
        sharedPrefs.edit().remove("email").remove("token").apply()
        _userEmail.value = null
    }

    fun clearError() {
        _errorMessage.value = null
        _networkErrorDetails.value = null
    }
}
