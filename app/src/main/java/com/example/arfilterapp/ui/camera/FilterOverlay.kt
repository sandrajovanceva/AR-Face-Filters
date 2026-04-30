package com.example.arfilterapp.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.arfilterapp.filters.FilterType
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FilterOverlay(
    filter: FilterType,
    landmarks: List<Pair<Float, Float>>,
    modifier: Modifier = Modifier
) {
    if (filter == FilterType.NONE || landmarks.size < 478) return

    Canvas(modifier = modifier) {
        when (filter) {
            FilterType.GLASSES -> drawGlasses(landmarks)
            FilterType.DOG_EARS -> drawDogEars(landmarks)
            FilterType.CAT_FACE -> drawCatFace(landmarks)
            FilterType.FUNNY_MASK -> drawFunnyMask(landmarks)
            FilterType.NONE -> Unit
        }
    }
}

// --- Drawing helpers --------------------------------------------------------

private fun DrawScope.point(lm: List<Pair<Float, Float>>, index: Int): Offset {
    val (nx, ny) = lm[index]
    return Offset(nx * size.width, ny * size.height)
}

private fun DrawScope.drawGlasses(lm: List<Pair<Float, Float>>) {
    val leftOuter = point(lm, 33)
    val leftInner = point(lm, 133)
    val rightInner = point(lm, 362)
    val rightOuter = point(lm, 263)

    val leftCenter = (leftOuter + leftInner) / 2f
    val rightCenter = (rightInner + rightOuter) / 2f
    val lensRadius = (leftInner - leftOuter).getDistance() * 0.75f

    val frameStroke = Stroke(width = 8f)

    // Tinted lenses
    drawCircle(Color(0x99000000), lensRadius - 4f, leftCenter)
    drawCircle(Color(0x99000000), lensRadius - 4f, rightCenter)
    // Frames
    drawCircle(Color.Black, lensRadius, leftCenter, style = frameStroke)
    drawCircle(Color.Black, lensRadius, rightCenter, style = frameStroke)
    // Bridge
    drawLine(Color.Black, leftInner, rightInner, strokeWidth = 8f)
}

private fun DrawScope.drawDogEars(lm: List<Pair<Float, Float>>) {
    val forehead = point(lm, 10)
    val leftCheek = point(lm, 234)
    val rightCheek = point(lm, 454)
    val faceWidth = (rightCheek - leftCheek).getDistance()
    val earHeight = faceWidth * 0.5f

    val brown = Color(0xFF6D4C41)
    val pink = Color(0xFFFFAB91)

    // Left ear
    val leftBase = forehead + Offset(-faceWidth * 0.3f, -faceWidth * 0.1f)
    drawTriangle(
        leftBase + Offset(-faceWidth * 0.15f, 0f),
        leftBase + Offset(faceWidth * 0.15f, 0f),
        leftBase + Offset(-faceWidth * 0.05f, -earHeight),
        brown
    )
    // Inner pink
    drawTriangle(
        leftBase + Offset(-faceWidth * 0.07f, -faceWidth * 0.05f),
        leftBase + Offset(faceWidth * 0.07f, -faceWidth * 0.05f),
        leftBase + Offset(-faceWidth * 0.03f, -earHeight * 0.7f),
        pink
    )

    // Right ear (mirrored)
    val rightBase = forehead + Offset(faceWidth * 0.3f, -faceWidth * 0.1f)
    drawTriangle(
        rightBase + Offset(-faceWidth * 0.15f, 0f),
        rightBase + Offset(faceWidth * 0.15f, 0f),
        rightBase + Offset(faceWidth * 0.05f, -earHeight),
        brown
    )
    drawTriangle(
        rightBase + Offset(-faceWidth * 0.07f, -faceWidth * 0.05f),
        rightBase + Offset(faceWidth * 0.07f, -faceWidth * 0.05f),
        rightBase + Offset(faceWidth * 0.03f, -earHeight * 0.7f),
        pink
    )
}

private fun DrawScope.drawCatFace(lm: List<Pair<Float, Float>>) {
    val leftCheek = point(lm, 234)
    val rightCheek = point(lm, 454)
    val noseTip = point(lm, 1)
    val faceWidth = (rightCheek - leftCheek).getDistance()
    val whiskerLength = faceWidth * 0.4f

    // Left whiskers (3 lines fanning out)
    for (i in -1..1) {
        val angleRad = Math.toRadians(i * 12.0)
        val end = Offset(
            leftCheek.x - whiskerLength * cos(angleRad).toFloat(),
            leftCheek.y - whiskerLength * sin(angleRad).toFloat()
        )
        drawLine(Color.Black, leftCheek, end, strokeWidth = 4f)
    }
    // Right whiskers
    for (i in -1..1) {
        val angleRad = Math.toRadians(i * 12.0)
        val end = Offset(
            rightCheek.x + whiskerLength * cos(angleRad).toFloat(),
            rightCheek.y - whiskerLength * sin(angleRad).toFloat()
        )
        drawLine(Color.Black, rightCheek, end, strokeWidth = 4f)
    }

    // Pink cat nose triangle
    val noseSize = faceWidth * 0.05f
    drawTriangle(
        Offset(noseTip.x - noseSize, noseTip.y - noseSize * 0.4f),
        Offset(noseTip.x + noseSize, noseTip.y - noseSize * 0.4f),
        Offset(noseTip.x, noseTip.y + noseSize),
        Color(0xFFFF69B4)
    )
}

private fun DrawScope.drawFunnyMask(lm: List<Pair<Float, Float>>) {
    val forehead = point(lm, 10)
    val chin = point(lm, 152)
    val leftCheek = point(lm, 234)
    val rightCheek = point(lm, 454)

    val centerX = (leftCheek.x + rightCheek.x) / 2f
    val centerY = (forehead.y + chin.y) / 2f
    val width = (rightCheek.x - leftCheek.x) * 1.15f
    val height = (chin.y - forehead.y) * 1.1f

    drawOval(
        color = Color(0x99FFEB3B),
        topLeft = Offset(centerX - width / 2f, centerY - height / 2f),
        size = Size(width, height)
    )
}

private fun DrawScope.drawTriangle(a: Offset, b: Offset, c: Offset, color: Color) {
    val path = Path().apply {
        moveTo(a.x, a.y)
        lineTo(b.x, b.y)
        lineTo(c.x, c.y)
        close()
    }
    drawPath(path, color)
}
