package com.example.noteen.modulartest

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun RevealOverlayDemo() {
    var revealed by remember { mutableStateOf(false) }
    val radius = remember { Animatable(0f) }

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    // Tính toán kích thước tối đa từ @Composable context
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val maxRadius = hypot(screenWidthPx, screenHeightPx)

    // Gọi animation sau khi revealed = true
    LaunchedEffect(revealed) {
        if (revealed) {
            radius.animateTo(
                targetValue = maxRadius,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        if (revealed || radius.value > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0x4D2196F3),
                    radius = radius.value,
                    center = Offset(x = size.width, y = size.height)
                )
            }
        }

        FloatingActionButton(
            onClick = {
                revealed = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
    }
}
