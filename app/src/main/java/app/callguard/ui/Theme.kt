package app.callguard.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF0F6E56),
    primaryContainer = Color(0xFFE1F5EE),
    onPrimaryContainer = Color(0xFF04342C),
)
private val Dark = darkColorScheme(
    primary = Color(0xFF5DCAA5),
    primaryContainer = Color(0xFF085041),
    onPrimaryContainer = Color(0xFFE1F5EE),
)

@Composable
fun CallGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
