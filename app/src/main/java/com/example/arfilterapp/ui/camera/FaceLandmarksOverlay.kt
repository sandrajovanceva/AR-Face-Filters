package com.example.arfilterapp.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FaceLandmarksOverlay(
    landmarks: List<Pair<Float, Float>>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (landmarks.isEmpty()) return@Canvas
        val pointRadius = 1.5.dp.toPx()
        landmarks.forEach { (nx, ny) ->
            drawCircle(
                color = Color(0xFF00E676),
                radius = pointRadius,
                center = Offset(nx * size.width, ny * size.height)
            )
        }
    }
}
