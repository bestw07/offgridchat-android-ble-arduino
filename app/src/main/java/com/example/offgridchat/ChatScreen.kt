package com.example.offgridchat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Minimal chat screen that compiles cleanly with Material3.
 * - Connection status in the top app bar
 * - Messages list
 * - Bottom bar with: Attach, Mic/Stop toggle, text field, Send
 *
 * NOTE: Buttons use text labels to avoid icon dependency issues.
 * You can swap them to Icons later if you add material-icons-extended.
 */
@OptIn(ExperimentalMaterial3Api::class) // Opt-in for experimental Material 3 APIs
@Composable
fun ChatScreen(
    isBleConnected: Boolean,
    messages: List<ChatMessage>, // Assumes ChatMessage is defined in the same package
    onBack: () -> Unit,
    onSendText: (String) -> Unit,
    onAttach: () -> Unit,
    onStartMic: () -> Unit,
    onStopMic: () -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var isRecording by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val status = if (isBleConnected) "Connected" else "Disconnected"
                    Text(text = "Chat 1 • $status")
                },
                navigationIcon = {
                    TextButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp)) {
                        Text("Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomBar(
                text = input,
                onTextChange = { input = it },
                onSend = {
                    val text = input.text.trim()
                    if (text.isNotEmpty()) {
                        onSendText(text)
                        input = TextFieldValue("")
                    }
                },
                onAttach = onAttach,
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
        }
    ) { pad ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(pad),
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp),
            reverseLayout = true // Optional: To show latest messages at the bottom
        ) {
            items(messages.reversed()) { msg -> // Reverse messages if reverseLayout is true
                MessageBubble(message = msg)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun BottomBar(
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
        shadowElevation = 8.dp // Optional: add some elevation
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onAttach, modifier = Modifier.padding(end = 8.dp)) {
                Text("Attach")
            }
            Button(onClick = onMic, modifier = Modifier.padding(end = 8.dp)) {
                Text(if (isRecording) "Stop" else "Mic")
            }
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message") }
            )
            Button(onClick = onSend, modifier = Modifier.padding(start = 8.dp)) {
                Text("Send")
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    // Basic bubble, align based on `isMine`
    val horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isMine) Color(0xFFDCF8C6) else Color(0xFFFFFFFF) // WhatsApp-like colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = horizontalAlignment
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            color = bubbleColor,
            shadowElevation = 1.dp
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(all = 12.dp),
                fontSize = 16.sp
            )
        }
        // You could add sender name or timestamp here if needed
        // Text(text = "Sender: ${message.senderId} at ${message.timestamp}", fontSize = 10.sp, color = Color.Gray)
    }
}
