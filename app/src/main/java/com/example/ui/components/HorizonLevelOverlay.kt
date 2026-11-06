package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.abs

@Composable
fun HorizonLevelOverlay(
    tiltAngle: Float,
    isLevel: Boolean,
    modifier: Modifier = Modifier
) {
    val levelColor = if (isLevel) Color(0xFF64FFDA) else Color.White.copy(alpha = 0.6f)
    val angleFormatted = String.format(Locale.US, "%.1f", tiltAngle)
    val liveDesc = if (isLevel) "Horizon level locked" else "Tilt $angleFormatted degrees"

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                this.contentDescription = liveDesc
                this.liveRegion = LiveRegionMode.Polite
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val barHalfLength = size.width * 0.22f

            // Center target crosshairs
            drawLine(
                color = levelColor,
                start = Offset(centerX - 16.dp.toPx(), centerY),
                end = Offset(centerX + 16.dp.toPx(), centerY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = levelColor,
                start = Offset(centerX, centerY - 16.dp.toPx()),
                end = Offset(centerX, centerY + 16.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Tilt indicator line
            val rad = Math.toRadians(-tiltAngle.toDouble())
            val dx = (barHalfLength * kotlin.math.cos(rad)).toFloat()
            val dy = (barHalfLength * kotlin.math.sin(rad)).toFloat()

            drawLine(
                color = levelColor,
                start = Offset(centerX - dx, centerY - dy),
                end = Offset(centerX + dx, centerY + dy),
                strokeWidth = if (isLevel) 4.dp.toPx() else 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Accessible text badge at top
        Surface(
            shape = MaterialTheme.shapes.small,
            color = if (isLevel) Color(0xFF004D40).copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            Text(
                text = if (isLevel) "Level: 0 deg" else "Tilt: $angleFormatted deg",
                color = if (isLevel) Color(0xFF64FFDA) else Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}
