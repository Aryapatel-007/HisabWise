package com.arya.hisabwise.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun GlowingBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // Glowing Orb background at top end
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryAccent.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(size.width * 0.8f, 0f),
                    radius = size.width * 1.2f
                ),
                radius = size.width * 1.2f,
                center = Offset(size.width * 0.8f, 0f)
            )
        }
        content()
    }
}
