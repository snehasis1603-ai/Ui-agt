package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.MasterAgentViewModel
import com.example.viewmodel.SpacesViewModel
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.ui.graphics.graphicsLayer
import android.os.Bundle
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ErrorOutline
import com.example.network.NetworkErrorType

@Composable
fun MasterAgentScreen(
    viewModel: MasterAgentViewModel = viewModel(),
    spacesViewModel: SpacesViewModel = viewModel()
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val networkError by viewModel.networkError.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    
    // Spaces management
    val selectedDomain by spacesViewModel.selectedDomain.collectAsStateWithLifecycle()
    val spacesList = listOf(
        "Trading & SIP",
        "Accounting",
        "E-Commerce",
        "HR & Ops",
        "Code Auto-Defense",
        "Global Surveillance"
    )

    // Safety Diagnostic States
    var showSafetyConsole by remember { mutableStateOf(true) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var isMicrophoneActive by remember { mutableStateOf(false) }
    var micPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    // Continuous Hands-free Loop & TTS states
    var isHandsFreeVoiceLoopActive by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var lastSpokenMessageIndex by remember { mutableStateOf(-1) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    // Initialize TextToSpeech engine
    DisposableEffect(context) {
        val ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    // Activity launcher for Speech recognition
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isMicrophoneActive = false
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val words = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = words?.firstOrNull() ?: ""
            if (text.isNotBlank()) {
                val lowercaseText = text.lowercase().trim()
                
                // Keep check for stop triggers
                if (lowercaseText == "stop" || lowercaseText == "stop speaking" || lowercaseText == "shut up" || lowercaseText == "exit" || lowercaseText == "quit" || lowercaseText.contains("stop loop") || lowercaseText.contains("quit loop")) {
                    isHandsFreeVoiceLoopActive = false
                    Toast.makeText(context, "Ommni Voice Loop Stopped.", Toast.LENGTH_SHORT).show()
                    viewModel.insertLocalMessage("System (Ommni): Hands-free voice loop stopped by request.")
                    tts?.speak("Hands-free conversation loop deactivated. Standing by.", TextToSpeech.QUEUE_FLUSH, null, "stop_confirm")
                } else {
                    inputText = ""
                    // Save voice log entry to room DB
                    spacesViewModel.saveVoiceLog(
                        domainTitle = selectedDomain,
                        transcription = text,
                        isVoice = true
                    )
                    // Auto-send voice queries directly to the network AI engine!
                    viewModel.sendMessage(text)
                }
            }
        } else {
            // Cancelled or erred, disable hands-free loop to prevent recursive loop visual lockups
            isHandsFreeVoiceLoopActive = false
        }
    }

    // Set up Utterance Progress Listener
    LaunchedEffect(tts, isHandsFreeVoiceLoopActive) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                if (isHandsFreeVoiceLoopActive) {
                    val lastMsgText = messages.lastOrNull()?.text?.lowercase() ?: ""
                    val containsStop = lastMsgText.contains("stop loop") || lastMsgText.contains("deactivated hands-free")
                    
                    if (!containsStop) {
                        handler.post {
                            if (micPermissionGranted) {
                                isMicrophoneActive = true
                                startListening(context) { intent -> speechLauncher.launch(intent) }
                            }
                        }
                    } else {
                        isHandsFreeVoiceLoopActive = false
                    }
                }
            }

            override fun onError(utteranceId: String?) {
                isSpeaking = false
            }
        })
    }

    // React to new incoming model messages by automatically playing TTS
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            val lastIndex = messages.lastIndex
            val lastMsg = messages[lastIndex]
            if (!lastMsg.isUser && lastIndex > lastSpokenMessageIndex) {
                lastSpokenMessageIndex = lastIndex
                
                if (isHandsFreeVoiceLoopActive) {
                    val textToSpeak = lastMsg.text
                    val params = Bundle().apply {
                        putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "ommni_utterance_${lastIndex}")
                    }
                    tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, "ommni_utterance_${lastIndex}")
                }
            }
        }
    }

    // Dynamic Permission requesting
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        micPermissionGranted = isGranted
        if (isGranted) {
            Toast.makeText(context, "Microphone access granted. Ready for voice directives!", Toast.LENGTH_SHORT).show()
            startListening(context) { intent -> speechLauncher.launch(intent) }
        } else {
            Toast.makeText(context, "Microphone access is required for Voice Translation functions.", Toast.LENGTH_LONG).show()
        }
    }

    val activateHandsFreeLoop = {
        if (!micPermissionGranted) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            isHandsFreeVoiceLoopActive = true
            val greeting = "Ommni Voice Core is online. Continuous hands-free conversation layer is activated. Speak your directive, supervisor."
            viewModel.insertLocalMessage("System (Ommni): Hands-free continuous voice loop activated.")
            
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "ommni_init_greeting")
            }
            tts?.speak(greeting, TextToSpeech.QUEUE_FLUSH, params, "ommni_init_greeting")
        }
    }

    // Safety pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Bar & Console expander
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Level 1: Core Intelligence",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Auto-Defensive Voice & Environmental Security",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier.testTag("toggle_history_from_chat")
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Open Command History",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(
                        onClick = { showSafetyConsole = !showSafetyConsole },
                        modifier = Modifier.testTag("toggle_console")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Toggle Safety Console",
                            tint = if (showSafetyConsole) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Expanded Advanced AI & Security Console
        AnimatedVisibility(visible = showSafetyConsole) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Environmental Defense Metrics",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .scale(if (isMicrophoneActive) pulseScale else 1f)
                                .clip(CircleShape)
                                .background(if (isMicrophoneActive) Color.Green else Color.Red)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Microphone Status: " + if (micPermissionGranted) "Authorized (Active)" else "Awaiting Permission Flow",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Safety Mode: Combined Surveillance & Biometric Defense (Cam Active for Candidates)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                if (micPermissionGranted) {
                                    startListening(context) { intent -> speechLauncher.launch(intent) }
                                } else {
                                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            label = { Text("Mic Access Test") },
                            leadingIcon = { Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        AssistChip(
                            onClick = {
                                Toast.makeText(context, "Simulated Environment Translation Initialized", Toast.LENGTH_SHORT).show()
                            },
                            label = { Text("Translate") },
                            leadingIcon = { Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }
        }

        // Target Agent Selector Row
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
                Text(
                    text = "TARGET AGENT SPACE (Direct Voice Routing)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    spacesList.forEach { space ->
                        val isSelected = space == selectedDomain
                        FilterChip(
                            selected = isSelected,
                            onClick = { spacesViewModel.selectDomain(space) },
                            label = { Text(space) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected
                            )
                        )
                    }
                }
            }
        }

        // Ommni Continuous Hands-free Voice Agent Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("ommni_voice_panel"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isHandsFreeVoiceLoopActive) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f) 
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (isHandsFreeVoiceLoopActive) 
                    MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isHandsFreeVoiceLoopActive) 
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) 
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isHandsFreeVoiceLoopActive) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = null,
                                tint = if (isHandsFreeVoiceLoopActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column {
                            Text(
                                text = "Code Name: ommni",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "OMNI Gateway: Active • Owner: Snehasis",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Continuous Audio Waveform Indicator
                    VoiceWaveformVisualizer(isActive = isHandsFreeVoiceLoopActive && (isSpeaking || isMicrophoneActive))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Badge Indicator
                    val statusText = when {
                        !isHandsFreeVoiceLoopActive -> "Asleep"
                        isSpeaking -> "ommni is speaking..."
                        isMicrophoneActive -> "Listening..."
                        else -> "ommni waiting..."
                    }
                    val statusColor = when {
                        !isHandsFreeVoiceLoopActive -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        isSpeaking -> MaterialTheme.colorScheme.secondary
                        isMicrophoneActive -> Color(0xFF00C853)
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(
                                text = statusText.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }

                    // Start/Stop Control trigger action buttons
                    if (isHandsFreeVoiceLoopActive) {
                        Button(
                            onClick = { 
                                isHandsFreeVoiceLoopActive = false
                                tts?.stop()
                                Toast.makeText(context, "ommni Voice Loop Stopped", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("ommni_stop_loop_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MicOff,
                                    contentDescription = "Stop Loop",
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Stop Loop", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    } else {
                        Button(
                            onClick = { activateHandsFreeLoop() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("ommni_activate_loop_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start Loop",
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Activate Hands-Free", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Active, Optimize & Operate Quick Directive Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                            if (!isHandsFreeVoiceLoopActive) {
                                activateHandsFreeLoop()
                            }
                        },
                        label = { Text("⚡ Active Loop") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isHandsFreeVoiceLoopActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.testTag("action_active_loop_chip")
                    )
                    AssistChip(
                        onClick = {
                            viewModel.optimizeNetwork()
                            Toast.makeText(context, "OMNI Engine Optimized for Snehasis", Toast.LENGTH_SHORT).show()
                        },
                        label = { Text("🚀 Optimize Network") },
                        modifier = Modifier.testTag("action_optimize_chip")
                    )
                    AssistChip(
                        onClick = {
                            viewModel.sendMessage("Operate full telemetry sweep and report domain status for Owner Snehasis.")
                        },
                        label = { Text("🎮 Operate Agents") },
                        modifier = Modifier.testTag("action_operate_chip")
                    )
                }
            }
        }

        // Connection status notification
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .padding(vertical = 4.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Captured voice status logs are saved directly to $selectedDomain history.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Diagnostic Network Error Banner
        AnimatedVisibility(visible = networkError != null) {
            networkError?.let { err ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("network_error_banner"),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Network Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                val badgeText = when {
                                    err.statusCode != null -> "HTTP ${err.statusCode}"
                                    err.type == NetworkErrorType.TIMEOUT -> "TIMEOUT (408)"
                                    err.type == NetworkErrorType.CONNECTION_FAILURE -> "OFFLINE / NO CONNECTION"
                                    else -> err.type.name.replace('_', ' ')
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.dismissError() },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("dismiss_network_error_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss error",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = err.userMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        if (err.isRetryable) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { viewModel.retryLastMessage() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("retry_network_request_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Retry Directive",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Chat History
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            reverseLayout = false
        ) {
            items(messages) { message ->
                if (message.isError) {
                    // Distinct Error Chat Message Bubble
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Surface(
                            modifier = Modifier
                                .widthIn(max = 330.dp)
                                .testTag("chat_error_message"),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    val errBadge = message.errorDetails?.statusCode?.let { "HTTP $it" }
                                        ?: message.errorDetails?.type?.name?.replace('_', ' ')
                                        ?: "NETWORK ERROR"
                                    Text(
                                        text = errBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = message.text,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                if (message.errorDetails?.isRetryable == true) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { viewModel.retryLastMessage() },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Tap to Retry", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
                    val bgColor = if (message.isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                    val textColor = if (message.isUser) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = alignment
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 300.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(bgColor)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = message.text,
                                color = textColor,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Input Field
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Button
                IconButton(
                    onClick = {
                        if (micPermissionGranted) {
                            isMicrophoneActive = true
                            startListening(context) { intent -> speechLauncher.launch(intent) }
                        } else {
                            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier
                        .testTag("microphone_button")
                        .scale(if (isMicrophoneActive) pulseScale else 1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Speak Directive",
                        tint = if (isMicrophoneActive) Color.Green else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input"),
                    shape = RoundedCornerShape(24.dp),
                    placeholder = { Text("Directive for $selectedDomain...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                FloatingActionButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            // Save command as text to database as well!
                            spacesViewModel.saveVoiceLog(
                                domainTitle = selectedDomain,
                                transcription = inputText,
                                isVoice = false
                            )
                            inputText = ""
                        }
                    },
                    modifier = Modifier.testTag("chat_send_button"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send directive")
                }
            }
        }
    }

    if (showHistoryDialog) {
        VoiceCommandHistoryDialog(
            spacesViewModel = spacesViewModel,
            onDismiss = { showHistoryDialog = false }
        )
    }
}

private fun startListening(context: android.content.Context, launch: (Intent) -> Unit) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "OmniVoice Translator Active. Say a command or enterprise directive:")
    }
    try {
        launch(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Speech recognizer is not available on this device environment.", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun VoiceWaveformVisualizer(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    
    val animScales = (0..4).map { index ->
        val duration = remember { 350 + index * 90 }
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(duration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$index"
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        animScales.forEach { scaleState ->
            val finalScale = if (isActive) scaleState.value else 0.15f
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(26.dp)
                    .graphicsLayer { scaleY = finalScale }
                    .background(
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

