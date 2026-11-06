package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GridOverlay(
    gridType: String,
    modifier: Modifier = Modifier
) {
    if (gridType.equals("NONE", ignoreCase = true)) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val lineColor = Color.White.copy(alpha = 0.4f)
        val strokeW = 1.dp.toPx()

        if (gridType.equals("RULE_OF_THIRDS", ignoreCase = true) || gridType.equals("3X3", ignoreCase = true)) {
            val oneThirdW = size.width / 3f
            val twoThirdW = size.width * 2f / 3f
            val oneThirdH = size.height / 3f
            val twoThirdH = size.height * 2f / 3f

            // Vertical lines
            drawLine(color = lineColor, start = Offset(oneThirdW, 0f), end = Offset(oneThirdW, size.height), strokeWidth = strokeW)
            drawLine(color = lineColor, start = Offset(twoThirdW, 0f), end = Offset(twoThirdW, size.height), strokeWidth = strokeW)

            // Horizontal lines
            drawLine(color = lineColor, start = Offset(0f, oneThirdH), end = Offset(size.width, oneThirdH), strokeWidth = strokeW)
            drawLine(color = lineColor, start = Offset(0f, twoThirdH), end = Offset(size.width, twoThirdH), strokeWidth = strokeW)
        } else if (gridType.equals("GOLDEN_RATIO", ignoreCase = true)) {
            val phi = 0.618f
            val w1 = size.width * (1f - phi)
            val w2 = size.width * phi
            val h1 = size.height * (1f - phi)
            val h2 = size.height * phi

            drawLine(color = lineColor, start = Offset(w1, 0f), end = Offset(w1, size.height), strokeWidth = strokeW)
            drawLine(color = lineColor, start = Offset(w2, 0f), end = Offset(w2, size.height), strokeWidth = strokeW)
            drawLine(color = lineColor, start = Offset(0f, h1), end = Offset(size.width, h1), strokeWidth = strokeW)
            drawLine(color = lineColor, start = Offset(0f, h2), end = Offset(size.width, h2), strokeWidth = strokeW)
        }
    }
}
