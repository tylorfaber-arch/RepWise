package com.tylor.repwise.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Ember = Color(0xFFE4572E)
private val Slate = Color(0xFF29335C)

@Composable
fun RepWiseTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme())
        darkColorScheme(primary = Ember, secondary = Color(0xFF8EA4D2))
    else
        lightColorScheme(primary = Ember, secondary = Slate)
    MaterialTheme(colorScheme = colors, content = content)
}
