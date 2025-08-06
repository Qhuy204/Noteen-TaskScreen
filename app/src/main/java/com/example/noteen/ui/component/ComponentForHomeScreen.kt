package com.example.noteen.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.noteen.R

@Composable
fun SortAndLayoutToggle(
    selectedSortType: Int,
    isGridLayout: Boolean,
    onSortTypeClick: () -> Unit,
    onLayoutToggleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sortTypeLabels = listOf("Last Modified", "Last Created", "Name")
//    val sortTypeLabels = listOf("Sửa gần đây", "Tạo gần đây", "Tiêu đề")

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(135.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSortTypeClick() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.list_filter),
                        contentDescription = "Sort Icon",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = sortTypeLabels.getOrNull(selectedSortType) ?: "Sort",
                        color = Color.Black,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.width(3.dp))

            IconButton(
                onClick = onLayoutToggleClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isGridLayout) R.drawable.layout_grid else R.drawable.layout_list
                    ),
                    contentDescription = "Toggle layout",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SearchBar() {
    val iconSize = 25.dp
    val iconSizePx = with(LocalDensity.current) { iconSize.toPx() }

    var boxHeightPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White, shape = RoundedCornerShape(28.dp))
            .padding(horizontal = 28.dp)
            .onGloballyPositioned { layoutCoordinates ->
                boxHeightPx = layoutCoordinates.size.height.toFloat()
            },
        contentAlignment = Alignment.CenterStart
    ) {
        if (boxHeightPx >= iconSizePx) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.search),
                    contentDescription = "Search Icon",
                    tint = Color.Gray,
                    modifier = Modifier.size(iconSize)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Search",
                    color = Color.Gray,
                    fontSize = 18.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun TagChip(
    text: String,
    count: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) Color(0xFF1966FF) else Color.White
    val countBackgroundColor = if (isSelected) Color.White else Color(0xFFF2F2F2)
    val contentColor = if (isSelected) Color.White else Color.Black
    val countTextColor = if (isSelected) Color(0xFF1966FF) else Color.Black

    val displayCount = when {
        count > 99 -> "99+"
        count > 9 -> "9+"
        else -> count.toString()
    }

    val height = 40.dp
    val textStyle = TextStyle(fontSize = 14.sp, lineHeight = 14.sp)

    Row(
        modifier = modifier
            .height(height)
            .background(color = backgroundColor, shape = RoundedCornerShape(15.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = contentColor,
            style = textStyle
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 20.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(countBackgroundColor)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayCount,
                style = textStyle,
                color = countTextColor,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryBar(
    chipLists: List<Pair<String, Int>>,
    selectedChip: String,
    onChipClick: (String) -> Unit
) {
    val chipHeight = 40.dp
    val blueColor = Color(0xFF1966FF)
    val fadeColor = Color(0xFFF2F2F2)
    val listState = rememberLazyListState()

    val canScrollBackward by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    val canScrollForward by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem?.index != totalItems - 1
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier
            .weight(1f)
            .height(chipHeight)
        ) {
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(chipLists) { (label, count) ->
                    TagChip(
                        text = label,
                        count = count,
                        isSelected = label == selectedChip,
                        modifier = Modifier
                            .combinedClickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = { onChipClick(label) }
                            )
                    )
                }

                item {
                    Surface(
                        onClick = { /* handle +New click */ },
                        shape = RoundedCornerShape(15.dp),
                        color = Color.Transparent,
                        modifier = Modifier.height(chipHeight),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(start = 6.dp, end = 12.dp)
                                .fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = blueColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "New",
                                color = blueColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Fade trái
            if (canScrollBackward) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(16.dp)
                        .align(Alignment.CenterStart)
                        .background(
                            Brush.horizontalGradient(
                                listOf(fadeColor, Color.Transparent)
                            )
                        )
                )
            }

            // Fade phải
            if (canScrollForward) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(16.dp)
                        .align(Alignment.CenterEnd)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, fadeColor)
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            modifier = Modifier.size(chipHeight),
            shape = RoundedCornerShape(15.dp),
            color = Color.White,
            tonalElevation = 1.dp,
            onClick = { /* handle folder click */ }
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.folder),
                    contentDescription = "Folder Icon",
                    tint = blueColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
