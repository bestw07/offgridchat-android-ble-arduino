package com.example.offgridchat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val darkScheme = darkColorScheme(
    primary = Purple40
)

@Composable
fun OffGridTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
