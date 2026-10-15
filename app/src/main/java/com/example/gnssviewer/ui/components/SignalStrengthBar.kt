package com.example.gnssviewer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gnssviewer.ui.theme.Amber500
import com.example.gnssviewer.ui.theme.Emerald500
import com.example.gnssviewer.ui.theme.Rose500
import com.example.gnssviewer.ui.theme.Sky500

@Composable
fun SignalStrengthBar(
    cn0DbHz: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    maxCn0: Float = 50.0f
) {
    val progress = (cn0DbHz / maxCn0).coerceIn(0.0f, 1.0f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 350),
        label = "cn0Progress"
    )

    val barColor = when {
        cn0DbHz >= 40.0f -> Sky500
        cn0DbHz >= 30.0f -> Emerald500
        cn0DbHz >= 20.0f -> Amber500
        else -> Rose500
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .clip(RoundedCornerShape(height / 2))
                .background(barColor)
        )
    }
}

