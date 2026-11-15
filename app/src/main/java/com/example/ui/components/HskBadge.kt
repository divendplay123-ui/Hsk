package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.HskLevel
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberSoft
import com.example.ui.theme.IndigoInk
import com.example.ui.theme.IndigoSoft
import com.example.ui.theme.JadeCeladon
import com.example.ui.theme.JadeSoft
import com.example.ui.theme.VermilionSeal
import com.example.ui.theme.VermilionSoft

@Composable
fun HskLevelBadge(
    level: HskLevel,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (level) {
        HskLevel.HSK1 -> VermilionSoft to VermilionSeal
        HskLevel.HSK2 -> JadeSoft to JadeCeladon
        HskLevel.HSK3 -> AmberSoft to AmberGold
        HskLevel.HSK4 -> IndigoSoft to IndigoInk
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = level.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

fun DrawScope.drawTianzigeGrid(lineColor: Color) {
    val w = size.width
    val h = size.height
    val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    val fineDash = PathEffect.dashPathEffect(floatArrayOf(6f, 12f), 0f)

    // Cross lines (田)
    drawLine(
        color = lineColor.copy(alpha = 0.55f),
        start = Offset(w / 2f, 0f),
        end = Offset(w / 2f, h),
        strokeWidth = 2f,
        pathEffect = dash
    )
    drawLine(
        color = lineColor.copy(alpha = 0.55f),
        start = Offset(0f, h / 2f),
        end = Offset(w, h / 2f),
        strokeWidth = 2f,
        pathEffect = dash
    )

    // Diagonals (米)
    drawLine(
        color = lineColor.copy(alpha = 0.28f),
        start = Offset(0f, 0f),
        end = Offset(w, h),
        strokeWidth = 1.5f,
        pathEffect = fineDash
    )
    drawLine(
        color = lineColor.copy(alpha = 0.28f),
        start = Offset(w, 0f),
        end = Offset(0f, h),
        strokeWidth = 1.5f,
        pathEffect = fineDash
    )
}
