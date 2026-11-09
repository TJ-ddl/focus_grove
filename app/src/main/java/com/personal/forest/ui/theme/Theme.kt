package com.personal.forest.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Cream = Color(0xFFF7F2E7)
val CreamDeep = Color(0xFFEFE7D6)
val Ink = Color(0xFF24352B)
val Forest = Color(0xFF2F4A32)
val Sage = Color(0xFF8FAE8B)
val SageLight = Color(0xFFDCE7D6)
val Clay = Color(0xFFC98A5B)
val Withered = Color(0xFFA8A29A)
val Mist = Color(0xFFEDF1E6)
val Muted = Color(0xFF5A6659)

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = Cream,
    secondary = Sage,
    onSecondary = Ink,
    tertiary = Clay,
    onTertiary = Cream,
    background = Cream,
    onBackground = Ink,
    surface = Cream,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = Muted,
    outline = Color(0xFFB9C4B3),
    error = Color(0xFF9E4B3F),
)

private val ForestShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

@Composable
fun ForestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        shapes = ForestShapes,
        content = content,
    )
}
