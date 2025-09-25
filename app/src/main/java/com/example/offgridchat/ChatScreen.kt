package com.example.offgridchat

import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File

/**
 * Minimal chat screen that compiles cleanly with Material3.
 * - Connection status in the top app bar
 * - Messages list
 * - Bottom bar with: Attach, Mic/Stop toggle, text field, Send
 *
 * NOTE: Buttons use text labels to avoid icon dependency issues.
 * You can swap them to Icons later if you add material-icons-extended.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    isBleConnected: Boolean,
    messages: List<ChatMessage>,
    onBack: () -> Unit,
    onSendText: (String) -> Unit,
    onAttach: () -> Unit,
    onStartMic: () -> Unit,
    onStopMic: () -> Unit,
    onPhotoSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var isRecording by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    
    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onPhotoSelected(it) }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "OffGrid Chat",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isBleConnected) Color(0xFF4CAF50) else Color(0xFFF44336)
                                    )
                            )
                            Text(
                                text = if (isBleConnected) "🟢 Connected to ESP32" else "🔴 Disconnected - Check BLE",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isBleConnected) Color(0xFF4CAF50) else Color(0xFFF44336),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(
                            text = "← Back",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            ModernInputBar(
                text = input,
                onTextChange = { input = it },
                onSend = {
                    val text = input.text.trim()
                    if (text.isNotEmpty()) {
                        onSendText(text)
                        input = TextFieldValue("")
                    }
                },
                onAttach = { photoPickerLauncher.launch("image/*") },
                isRecording = isRecording,
                onMic = {
                    if (isRecording) {
                        onStopMic()
                    } else {
                        onStartMic()
                    }
                    isRecording = !isRecording
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = modifier.fillMaxSize()
        ) {
            // Connection status banner
            if (!isBleConnected) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF44336).copy(alpha = 0.1f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, 
                        Color(0xFFF44336).copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⚠️",
                            fontSize = 20.sp
                        )
                        Column {
                            Text(
                                text = "Not Connected",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF44336)
                            )
                            Text(
                                text = "Make sure your ESP32 is powered on and nearby",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (messages.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "💬",
                        fontSize = 64.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Start a conversation",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Send messages, photos, or voice notes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    reverseLayout = true
                ) {
                    items(messages.reversed()) { msg ->
                        ModernMessageBubble(message = msg)
                    }
                }
            }
            }
        }
    }
}

@Composable
fun ModernInputBar(
    text: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    isRecording: Boolean,
    onMic: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Attach button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onAttach,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "📎",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 18.sp
                    )
                }
                Text(
                    text = "Photo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
            
            // Text input
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { 
                    Text(
                        "Type a message...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )
            
            // Mic/Send button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = if (text.text.isNotEmpty()) onSend else onMic,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color(0xFFF44336) 
                        else if (text.text.isNotEmpty()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(
                        text = if (text.text.isNotEmpty()) "➤" 
                        else if (isRecording) "⏹" 
                        else "🎤",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
                Text(
                    text = if (text.text.isNotEmpty()) "Send" 
                    else if (isRecording) "Stop" 
                    else "Voice",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun ModernMessageBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    val isMine = message.isMine
    val bubbleColor = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp
            ),
            color = bubbleColor,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                when (message.type) {
                    MessageType.TEXT -> {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor,
                            lineHeight = 20.sp
                        )
                    }
                    MessageType.VOICE -> {
                        VoiceMessageContent(
                            message = message,
                            textColor = textColor
                        )
                    }
                    MessageType.IMAGE -> {
                        ImageMessageContent(
                            message = message,
                            textColor = textColor
                        )
                    }
                    MessageType.SYSTEM -> {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor.copy(alpha = 0.8f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = formatTime(message.timestampMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.align(if (isMine) Alignment.End else Alignment.Start)
                )
            }
        }
    }
}

@Composable
fun VoiceMessageContent(
    message: ChatMessage,
    textColor: Color
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    
    Row(
        modifier = Modifier
            .clickable {
                try {
                    if (isPlaying) {
                        mediaPlayer?.stop()
                        mediaPlayer?.release()
                        mediaPlayer = null
                        isPlaying = false
                    } else {
                        message.filePath?.let { filePath ->
                            val file = File(filePath)
                            if (file.exists()) {
                                mediaPlayer = MediaPlayer().apply {
                                    setDataSource(filePath)
                                    prepare()
                                    start()
                                    setOnCompletionListener {
                                        isPlaying = false
                                        release()
                                    }
                                }
                                isPlaying = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Handle playback error
                }
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isPlaying) "🔊 Playing..." else "🎤 Tap to play",
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )
    }
}

@Composable
fun ImageMessageContent(
    message: ChatMessage,
    textColor: Color
) {
    var showFullImage by remember { mutableStateOf(false) }
    
    Column {
        Text(
            text = message.text,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        message.fileUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = "Image",
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showFullImage = true },
                contentScale = ContentScale.Crop
            )
        }
        
        if (showFullImage) {
            FullScreenImageDialog(
                imageUri = message.fileUri,
                onDismiss = { showFullImage = false }
            )
        }
    }
}

@Composable
fun FullScreenImageDialog(
    imageUri: Uri?,
    onDismiss: () -> Unit
) {
    if (imageUri != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Image") },
            text = {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Full size image",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}

private fun formatTime(timestampMillis: Long): String {
    val date = java.util.Date(timestampMillis)
    val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return format.format(date)
}
