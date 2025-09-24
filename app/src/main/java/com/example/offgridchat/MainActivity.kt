package com.example.offgridchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.offgridchat.ui.theme.OffGridTheme
import com.example.offgridchat.vm.ChatViewModel

class MainActivity : ComponentActivity() {

    // Declare as an Activity property (works with the `by viewModels()` delegate)
    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize BLE connection
        chatViewModel.initializeBle(this)
        
        setContent {
            OffGridTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OffGridNavHost(chatViewModel)
                }
            }
        }
    }
}

@Composable
fun OffGridNavHost(chatViewModel: ChatViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "chat_list"
    ) {
        composable(route = "chat_list") {
            ChatListScreen(
                onOpenChat = { navController.navigate("chat/1") } // single conversation
            )
        }

        composable(
            route = "chat/{convId}",
            arguments = listOf(navArgument("convId") { type = NavType.StringType })
        ) { backStack ->
            // We don’t use convId further since there’s only one chat; keep for future-proofing.
            val conversationId = backStack.arguments?.getString("convId") ?: "1" // Changed _ to conversationId

            ChatScreen(
                isBleConnected = chatViewModel.isBleConnected,
                messages = chatViewModel.messages,
                onBack = { navController.popBackStack() },
                onSendText = { chatViewModel.sendMessage(it) },
                onAttach = { chatViewModel.attachFile() },
                onStartMic = { chatViewModel.startRecording() },
                onStopMic = { chatViewModel.stopRecording() },
                onPhotoSelected = { uri -> chatViewModel.handlePhotoSelected(uri) }
            )
        }
    }
}
