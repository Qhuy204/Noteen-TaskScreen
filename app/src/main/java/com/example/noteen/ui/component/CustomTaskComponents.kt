package com.example.noteen.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A custom checkbox component that replaces the Material UI Checkbox.
 * It features smooth animations for checking and unchecking.
 *
 * @param checked The current checked state of the checkbox.
 * @param onCheckedChange A callback invoked when the checkbox is clicked.
 * @param modifier A Modifier for this component.
 * @param size The size of the checkbox.
 * @param checkedColor The color of the checkbox when it is checked.
 * @param uncheckedColor The color of the checkbox border when it is unchecked.
 * @param checkmarkColor The color of the checkmark symbol.
 * @param cornerRadius The corner radius of the checkbox.
 */
@Composable
fun CustomCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    checkedColor: Color = Color(0xFF409BFF),
    uncheckedColor: Color = Color(0xFFCCCCCC),
    checkmarkColor: Color = Color.White,
    cornerRadius: Dp = 6.dp
) {
    val interactionSource = remember { MutableInteractionSource() }

    val checkboxColor by animateColorAsState(
        targetValue = if (checked) checkedColor else Color.Transparent,
        animationSpec = tween(durationMillis = 300),
        label = "checkboxColor"
    )

    val checkmarkProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "checkmarkProgress"
    )

    Canvas(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onCheckedChange?.invoke(!checked)
            }
    ) {
        val strokeWidth = (this.size.width * 0.1f).coerceAtLeast(1.dp.toPx())
        val cornerRadiusPx = cornerRadius.toPx()
        val canvasSize = this.size // Get the size from the DrawScope

        // Draw the border or the filled background
        drawRoundRect(
            color = if (checked) checkboxColor else uncheckedColor,
            size = canvasSize,
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
            style = if (checked) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width = strokeWidth)
        )

        // Draw the checkmark
        if (checkmarkProgress > 0) {
            val checkPath = Path().apply {
                moveTo(canvasSize.width * 0.25f, canvasSize.height * 0.5f)
                lineTo(canvasSize.width * 0.45f, canvasSize.height * 0.7f)
                lineTo(canvasSize.width * 0.75f, canvasSize.height * 0.3f)
            }

            val pathMeasure = PathMeasure()
            pathMeasure.setPath(checkPath, false)

            val partialPath = Path()
            pathMeasure.getSegment(0f, pathMeasure.length * checkmarkProgress, partialPath, true)

            drawPath(
                path = partialPath,
                color = checkmarkColor,
                style = Stroke(
                    width = strokeWidth * 1.2f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}


/**
 * A custom linear progress bar that replaces Material UI's LinearProgressIndicator.
 *
 * @param progress The progress value, between 0.0 and 1.0.
 * @param modifier A Modifier for this component.
 * @param color The color of the progress indicator.
 * @param backgroundColor The color of the background track.
 * @param height The height of the progress bar.
 */
@Composable
fun CustomProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF409BFF),
    backgroundColor: Color = Color(0xFFE0E0E0),
    height: Dp = 8.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500),
        label = "progressAnimation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(backgroundColor, RoundedCornerShape(height / 2))
            .clip(RoundedCornerShape(height / 2))
    ) {
        Box(
            modifier = Modifier
                .background(color)
                .fillMaxHeight()
                .fillMaxWidth(fraction = animatedProgress)
        )
    }
}
