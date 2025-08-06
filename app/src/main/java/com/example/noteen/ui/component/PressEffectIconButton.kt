package com.example.noteen.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PressEffectIconButton(
    selected: Boolean = false,
    onClick: () -> Unit,
    icon: Painter,
    selectedIconColor: Color? = null,
    unselectedIconColor: Color? = null,
    selectedBackgroundColor: Color = Color.Transparent,
    unselectedBackgroundColor: Color = Color.Transparent,
    disabledIconColor: Color = Color.Gray,
    disabledBackgroundColor: Color = Color.Transparent,
    cornerShape: Shape = CircleShape,
    iconPadding: Dp = 4.dp,
    enablePressEffect: Boolean = true,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier.size(30.dp)
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enablePressEffect && isEnabled) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val iconTint = when {
        !isEnabled -> disabledIconColor
        selected -> selectedIconColor ?: unselectedIconColor ?: Color.Unspecified
        else -> unselectedIconColor ?: Color.Unspecified
    }

    val background = when {
        !isEnabled -> disabledBackgroundColor
        selected -> selectedBackgroundColor
        else -> unselectedBackgroundColor
    }

    Box(
        modifier = modifier
            .then(
                if (isEnabled) {
                    Modifier.pointerInput(enablePressEffect) {
                        if (enablePressEffect) {
                            detectTapGestures(
                                onPress = {
                                    isPressed = true
                                    try {
                                        awaitRelease()
                                    } finally {
                                        isPressed = false
                                    }
                                    onClick()
                                }
                            )
                        } else {
                            detectTapGestures(onTap = { onClick() })
                        }
                    }
                } else Modifier
            )
            .background(background, shape = cornerShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
                .fillMaxSize()
                .padding(iconPadding)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale
                )
        )
    }
}
