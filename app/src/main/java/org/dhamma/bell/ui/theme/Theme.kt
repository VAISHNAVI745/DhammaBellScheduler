package org.dhamma.bell.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SaffronOrange = Color(0xFFE67E22)
private val DeepMaroon = Color(0xFF4A1E1E)
private val WarmCream = Color(0xFFF5EFE6)
private val BackgroundBlack = Color(0xFF10100E)
private val SurfaceDark = Color(0xFF1C1B18)

private val DhammaDarkColors = darkColorScheme(
    primary = SaffronOrange,
    secondary = DeepMaroon,
    background = BackgroundBlack,
    surface = SurfaceDark,
    onPrimary = Color.Black,
    onBackground = WarmCream,
    onSurface = WarmCream
)

@Composable
fun DhammaBellTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DhammaDarkColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
