package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.data.model.FaceAlignmentState

@Composable
fun FaceAlignmentOverlay(
    state: FaceAlignmentState,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!enabled || state.guidanceMessage.isBlank()) return

    val (bg, fg, icon) = when {
        !state.hasFace -> Triple(
            Color.Black.copy(alpha = 0.75f),
            Color.White,
            Icons.Default.Face
        )
        state.isWellAligned -> Triple(
            Color(0xFF004D40).copy(alpha = 0.9f),
            Color(0xFF64FFDA),
            Icons.Default.CheckCircle
        )
        else -> Triple(
            Color(0xFF4A3800).copy(alpha = 0.9f),
            Color(0xFFFFD166),
            Icons.Default.Warning
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 64.dp)
            .semantics {
                this.contentDescription = "Face alignment: ${state.guidanceMessage}"
                this.liveRegion = LiveRegionMode.Polite
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = bg,
            shadowElevation = 6.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 6.dp)
                )
                Text(
                    text = state.guidanceMessage,
                    color = fg,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
