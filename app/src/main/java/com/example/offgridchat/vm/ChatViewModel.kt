package com.example.offgridchat.vm

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.offgridchat.ChatMessage
import com.example.offgridchat.MessageType
import com.example.offgridchat.ble.BleClient
import com.example.offgridchat.ble.BleUuids
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicLong

class ChatViewModel : ViewModel() {

    var isBleConnected: Boolean = false
        private set

    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> get() = _messages

    private val idGen = AtomicLong(0L)
    
    // BLE and recording state
    private var bleClient: BleClient? = null
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = mutableStateOf(false)
    private var recordingFile: File? = null
    private var context: Context? = null
    
    // Initialize BLE connection
    fun initializeBle(context: Context) {
        this.context = context
        bleClient = BleClient(context)
        
        // Auto-connect to the first available board
        if (bleClient?.canUseBle() == true) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    println("[DEBUG] Scanning for ESP32 devices...")
                    // Try to connect to any ESP32 with Nordic UART service
                    bleClient?.scanAndConnect { status ->
                        println("[DEBUG] Connection status: $status")
                        isBleConnected = (status == "connected")
                        if (isBleConnected) {
                            println("[DEBUG] Successfully connected to ESP32!")
                        } else {
                            println("[DEBUG] Connection failed or disconnected: $status")
                        }
                    }
                } catch (e: Exception) {
                    println("[DEBUG] Connection failed: ${e.message}")
                }
            }
        } else {
            println("[DEBUG] BLE permissions not available")
        }
    }

    fun setBleConnected(connected: Boolean) {
        isBleConnected = connected
        // You might want to add a dummy message or handle connection status changes
        // For example, to test, add a system message:
        // _messages.add(ChatMessage(idGen.getAndIncrement(), "System: BLE connection status changed to $connected", false, System.currentTimeMillis()))
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val message = ChatMessage(
            id = idGen.getAndIncrement(),
            text = text.trim(),
            isMine = true,
            timestampMillis = System.currentTimeMillis(),
            type = MessageType.TEXT
        )
        _messages.add(message)
        
        // Send via BLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = "TEXT:${message.text}".toByteArray()
                bleClient?.write(data)
            } catch (e: Exception) {
                // Handle BLE write error
            }
        }
    }

    // Example of receiving a message (for testing UI)
    fun receiveMessage(text: String) {
        if (text.isBlank()) return

        _messages.add(
            ChatMessage(
                id = idGen.getAndIncrement(),
                text = text.trim(),
                isMine = false, // Message from someone else
                timestampMillis = System.currentTimeMillis(),
                type = MessageType.TEXT
            )
        )
    }

    fun attachFile() {
        // This will be handled by the UI layer with a gallery picker
        // The actual implementation will be in the Compose UI
    }
    
    fun handlePhotoSelected(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = context?.contentResolver?.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                
                if (bytes != null) {
                    // Save photo locally for display
                    val fileName = "photo_${System.currentTimeMillis()}.jpg"
                    val file = File(context?.cacheDir, fileName)
                    file.writeBytes(bytes)
                    
                    // Send photo data via BLE
                    val header = "PHOTO:${bytes.size}:".toByteArray()
                    bleClient?.write(header + bytes)
                    
                    // Add message to chat with image
                    _messages.add(
                        ChatMessage(
                            id = idGen.getAndIncrement(),
                            text = "📷 Photo",
                            isMine = true,
                            timestampMillis = System.currentTimeMillis(),
                            type = MessageType.IMAGE,
                            filePath = file.absolutePath,
                            fileUri = uri
                        )
                    )
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun startRecording() {
        if (isRecording.value) return
        
        try {
            recordingFile = File.createTempFile("recording", ".3gp", context?.cacheDir)
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(recordingFile?.absolutePath)
                prepare()
                start()
            }
            isRecording.value = true
        } catch (e: Exception) {
            // Handle recording error
        }
    }

    fun stopRecording() {
        if (!isRecording.value) return
        
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording.value = false
            
            // Send audio file via BLE
            recordingFile?.let { file ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val bytes = FileInputStream(file).readBytes()
                        val header = "AUDIO:${bytes.size}:".toByteArray()
                        bleClient?.write(header + bytes)
                        
                        // Add message to chat with audio file
                        _messages.add(
                            ChatMessage(
                                id = idGen.getAndIncrement(),
                                text = "🎤 Voice message",
                                isMine = true,
                                timestampMillis = System.currentTimeMillis(),
                                type = MessageType.VOICE,
                                filePath = file.absolutePath
                            )
                        )
                        
                        // Don't delete file - keep it for playback
                    } catch (e: Exception) {
                        // Handle error
                    }
                }
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    fun getRecordingState(): Boolean = isRecording.value
}


