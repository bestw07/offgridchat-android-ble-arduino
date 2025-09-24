package com.example.offgridchat.vm

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Single-conversation model. We always use convId = "1".
 * If later you want to back it with Room, you can still keep this API.
 */
class ChatListViewModel : ViewModel() {

    private val _title = MutableStateFlow("Chat 1")
    val title: StateFlow<String> = _title

    // The only conversation id we use in the app.
    private val _convId = MutableStateFlow("1")
    val convId: StateFlow<String> = _convId
}
