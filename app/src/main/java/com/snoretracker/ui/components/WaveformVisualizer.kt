package com.snoretracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.snoretracker.ui.theme.WaveformActive
import com.snoretracker.ui.theme.WaveformIdle
import com.snoretracker.ui.theme.WaveformSpike

@Composable
fun WaveformVisualizer(
    amplitudes: List<Float>,
    threshold: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val barCount = 40
        val barWidth = 4.dp.toPx()
        val barGap = 2.dp.toPx()
        
        val totalWidth = size.width
        val effectiveBarWidth = (totalWidth - (barGap * (barCount - 1))) / barCount
        val maxDb = 90f 
        
        val displayAmps = if (amplitudes.size < barCount) {
            List(barCount - amplitudes.size) { 0f } + amplitudes
        } else {
            amplitudes.takeLast(barCount)
        }

        displayAmps.forEachIndexed { index, db ->
            val heightRatio = (db / maxDb).coerceIn(0.1f, 1f)
            val barHeight = size.height * heightRatio
            
            val color = when {
                db == 0f -> WaveformIdle
                db >= threshold -> WaveformSpike
                else -> WaveformActive
            }

            val x = index * (effectiveBarWidth + barGap)
            val y = (size.height - barHeight) / 2 

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(effectiveBarWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }
    }
}
