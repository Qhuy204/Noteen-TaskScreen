package com.example.noteen.modulartest

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen() {
    val overscrollOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Tạo dữ liệu tĩnh với chiều cao ngẫu nhiên nhưng ổn định
    val sampleItems = remember {
        List(30) { index -> "Record #$index" to (100..220).random() }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val overscrollDelta = available.y
                if (source == NestedScrollSource.Drag && overscrollDelta != 0f) {
                    coroutineScope.launch {
                        val damped = overscrollDelta * 0.3f / (1f + abs(overscrollOffset.value) / 300f)
                        overscrollOffset.snapTo(overscrollOffset.value + damped)
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                overscrollOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
                return Velocity.Zero
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFf2f2f2))
            .padding(WindowInsets.statusBars.asPaddingValues())
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(50.dp))
        Box(modifier = Modifier.fillMaxWidth().height(50.dp).background(Color.White))
        Box(modifier = Modifier.fillMaxWidth().height(50.dp).background(Color.Blue))
        Box(modifier = Modifier.fillMaxWidth().height(50.dp).background(Color.White))

        // Chỉ cần vô hiệu hóa glow và bọc phần lưới
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
                    .offset { IntOffset(x = 0, y = overscrollOffset.value.toInt()) }
                    .padding(horizontal = 8.dp),
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(sampleItems) { (item, height) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(12.dp)
                    ) {
                        Text(text = item, color = Color.Black)
                    }
                }
            }
        }
    }
}
