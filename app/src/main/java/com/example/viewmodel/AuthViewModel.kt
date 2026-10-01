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
            _errorMessage.value = "Username and Security Code are required to access the OMNI Agent Gateway."
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

            // If username or security code doesn't match the owner-crafted credentials
            if (!cleanUser.equals(OWNER_USERNAME, ignoreCase = true)) {
                _errorMessage.value = "Access Denied: Invalid Username. The OMNI Agent Gateway requires the owner username '$OWNER_USERNAME' crafted by the owner."
            } else {
                _errorMessage.value = "Access Denied: Invalid Security Code. The OMNI Agent Gateway requires the security code '$OWNER_SECURITY_CODE' crafted by the owner."
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
        val cleanCode = javaPassword.trim()
        if (cleanEmail.isBlank() || cleanCode.isBlank()) {
            _errorMessage.value = "Username and Security Code are required."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        _networkErrorDetails.value = null

        viewModelScope.launch {
            if (cleanEmail.equals(OWNER_USERNAME, ignoreCase = true) && cleanCode == OWNER_SECURITY_CODE) {
                sharedPrefs.edit().apply {
                    putString("pwd_${OWNER_USERNAME.lowercase()}", OWNER_SECURITY_CODE)
                    putString("email", OWNER_USERNAME)
                    putString("token", "omni_owner_deck_${System.currentTimeMillis()}")
                    apply()
                }
                _userEmail.value = OWNER_USERNAME
                _errorMessage.value = null
                _networkErrorDetails.value = null
            } else {
                _errorMessage.value = "The OMNI Agent Gateway is restricted. Use the owner credentials crafted by the owner: Username '$OWNER_USERNAME' and Security Code '$OWNER_SECURITY_CODE'."
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
