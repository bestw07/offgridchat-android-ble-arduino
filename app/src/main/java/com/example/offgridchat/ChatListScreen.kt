package com.example.offgridchat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api // Import this
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Simple “one channel only” home screen.
 * Shows just a single conversation called “Chat 1”.
 */
@OptIn(ExperimentalMaterial3Api::class) // Add this annotation
@Composable
fun ChatListScreen(
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar( // This is likely the experimental API
                title = { Text(text = "OffGridChat") }
            )
        }
    ) { pad ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            val items = listOf("Chat 1")
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(items) { title ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenChat() }
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
