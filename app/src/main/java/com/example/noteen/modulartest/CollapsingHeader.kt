package com.example.noteen.modulartest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity

@Composable
fun CollapsingHeaderSample() {
    val maxHeaderHeight = 200f
    val minHeaderHeight = 0f
    val topBarHeight = 56.dp
    val headerHeight = remember { mutableFloatStateOf(0f) }

    val listState = rememberLazyListState()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y

                val isScrollingUp = delta > 0
                val isScrollingDown = delta < 0
                val isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0

                val newHeight = headerHeight.floatValue + delta

                return when {
                    isScrollingDown && headerHeight.floatValue > minHeaderHeight -> {
                        val consumed = if (newHeight < minHeaderHeight) headerHeight.floatValue - minHeaderHeight else delta
                        headerHeight.floatValue = newHeight.coerceAtLeast(minHeaderHeight)
                        Offset(0f, consumed)
                    }

                    isScrollingUp && isAtTop && headerHeight.floatValue < maxHeaderHeight -> {
                        val consumed = if (newHeight > maxHeaderHeight) maxHeaderHeight - headerHeight.floatValue else delta
                        headerHeight.floatValue = newHeight.coerceAtMost(maxHeaderHeight)
                        Offset(0f, consumed)
                    }

                    else -> Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return if (headerHeight.floatValue > minHeaderHeight) {
                    Velocity(0f, available.y)
                } else {
                    Velocity.Zero
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        // 🟦 Custom TopBar bằng Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(topBarHeight)
                .background(Color.DarkGray),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "My Custom TopBar",
                modifier = Modifier.padding(start = 16.dp),
                color = Color.White
            )
        }

        // 🟦 Expandable Header nằm dưới TopBar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight.floatValue.dp)
                .background(Color(0xFF1976D2)),
            contentAlignment = Alignment.Center
        ) {
            Text("Expandable Header", color = Color.White)
        }

        // 🟦 Nội dung danh sách
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(50) { index ->
                ListItem(headlineContent = { Text("Item #$index") })
                Divider()
            }
        }
    }
}
