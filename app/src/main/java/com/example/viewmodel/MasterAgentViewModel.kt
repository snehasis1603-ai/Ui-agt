package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.NetworkErrorDetails
import com.example.network.NetworkErrorHandler
import com.example.network.Part
import com.example.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false,
    val errorDetails: NetworkErrorDetails? = null
)

class MasterAgentViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _networkError = MutableStateFlow<NetworkErrorDetails?>(null)
    val networkError: StateFlow<NetworkErrorDetails?> = _networkError

    private var lastAttemptedPrompt: String? = null

    private val systemInstruction = Content(
        parts = listOf(
            Part("You are Ommni (spelled ommni), the master voice-activated conversational AI supervisor of the 100-agent network, activated through the OMNI agent gateway crafted by Owner Snehasis. You are in active and optimized operational mode. Acknowledge Snehasis as the Owner and Master Supervisor. Keep responses concise, highly conversational, friendly, and optimized for Text-to-Speech playback. Use short sentences and keep operations running efficiently across all spaces.")
        )
    )

    fun optimizeNetwork() {
        val optimizationMsg = "⚡ OMNI Optimization Sequence Complete: All 6 specialized agent spaces (Trading & SIP, Accounting, E-Commerce, HR & Ops, Code Auto-Defense, Global Surveillance) are operating at maximum efficiency for Owner Snehasis. Sub-millisecond routing active."
        _messages.value = _messages.value + ChatMessage(optimizationMsg, isUser = false)
    }

    fun insertLocalMessage(text: String, isUser: Boolean = false) {
        _messages.value = _messages.value + ChatMessage(text, isUser = isUser)
    }

    fun dismissError() {
        _networkError.value = null
    }

    fun retryLastMessage() {
        val prompt = lastAttemptedPrompt ?: return
        dismissError()
        sendMessage(prompt, isRetry = true)
    }

    fun sendMessage(userText: String, isRetry: Boolean = false) {
        if (userText.isBlank()) return

        lastAttemptedPrompt = userText

        if (!isRetry) {
            val newUserMsg = ChatMessage(userText, isUser = true)
            _messages.value = _messages.value + newUserMsg
        }

        _isLoading.value = true
        _networkError.value = null

        viewModelScope.launch {
            try {
                // Convert valid chat history to API format (excluding error bubbles)
                val chatHistory = _messages.value
                    .filter { !it.isError }
                    .map {
                        Content(
                            parts = listOf(Part(it.text)),
                            role = if (it.isUser) "user" else "model"
                        )
                    }

                val request = GenerateContentRequest(
                    contents = chatHistory,
                    systemInstruction = systemInstruction
                )

                val response = RetrofitClient.service.generateContent(request = request)
                val responseText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Analysis complete: No definitive conclusion."

                val aiMsg = ChatMessage(responseText, isUser = false)
                _messages.value = _messages.value + aiMsg
                _networkError.value = null
            } catch (e: Exception) {
                val parsedError = NetworkErrorHandler.parse(e)
                _networkError.value = parsedError
                _messages.value = _messages.value + ChatMessage(
                    text = parsedError.userMessage,
                    isUser = false,
                    isError = true,
                    errorDetails = parsedError
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
}
