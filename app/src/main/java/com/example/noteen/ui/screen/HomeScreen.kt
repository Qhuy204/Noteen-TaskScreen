package com.example.noteen.ui.screen

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize.Companion.contentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.noteen.R
import com.example.noteen.navigation.AppRoutes
import com.example.noteen.ui.component.*
import com.example.noteen.viewmodel.TaskGroup
import com.example.noteen.viewmodel.TasksViewModel
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalSharedTransitionApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope
) {
    val tasksViewModel: TasksViewModel = viewModel()
    val nearestTask by tasksViewModel.nearestUpcomingTask.collectAsState()

    var selectedSortType by remember { mutableIntStateOf(0) }
    var isGridLayout by remember { mutableStateOf(true) }

    val chipItems = listOf(
        "All" to 2, "Images" to 5, "Videos" to 12, "Documents" to 99, "Others" to 123
    )
    var selectedChip by remember { mutableStateOf("All") }

    val notes = remember { getSampleNotes() }

    val whiteBox1Dp = 55.dp
    val whiteBox2Dp = 50.dp

    val density = LocalDensity.current
    val whiteBox1Px = with(density) { whiteBox1Dp.toPx() }
    val whiteBox2Px = with(density) { whiteBox2Dp.toPx() }
    val collapsableHeightPx = whiteBox1Px + whiteBox2Px

    val offsetAnimatable = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0) {
                    val newOffset = (offsetAnimatable.value + delta).coerceIn(-collapsableHeightPx, 0f)
                    val consumed = newOffset - offsetAnimatable.value
                    coroutineScope.launch { offsetAnimatable.snapTo(newOffset) }
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta > 0) {
                    val newOffset = (offsetAnimatable.value + delta).coerceIn(-collapsableHeightPx, 0f)
                    val postConsumed = newOffset - offsetAnimatable.value
                    coroutineScope.launch { offsetAnimatable.snapTo(newOffset) }
                    return Offset(0f, postConsumed)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val targetValue = if (offsetAnimatable.value < -collapsableHeightPx / 2) -collapsableHeightPx else 0f
                coroutineScope.launch {
                    offsetAnimatable.animateTo(targetValue, tween(300, easing = FastOutSlowInEasing))
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    val offsetPx = offsetAnimatable.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFf2f2f2))
            .nestedScroll(nestedScrollConnection)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(horizontal = 18.dp)
    ) {
        val whiteBox1Height = with(density) { (whiteBox1Px + (offsetPx + whiteBox2Px)).coerceIn(0f, whiteBox1Px).toDp() }
        val whiteBox2Height = with(density) { (whiteBox2Px + offsetPx).coerceIn(0f, whiteBox2Px).toDp() }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Noteen",
                    modifier = Modifier.padding(start = 15.dp),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
            IconButton(onClick = { /* TODO: Handle click */ }) {
                Icon(painter = painterResource(id = R.drawable.ellipsis_vertical), contentDescription = "More options")
            }
        }

        Box(modifier = Modifier
            .fillMaxWidth()
            .height(whiteBox1Height)) { SearchBar() }

        CategoryBar(chipLists = chipItems, selectedChip = selectedChip, onChipClick = { selectedChip = it })

        Box(modifier = Modifier
            .fillMaxWidth()
            .height(whiteBox2Height)) {
            SortAndLayoutToggle(
                selectedSortType = selectedSortType,
                isGridLayout = isGridLayout,
                onSortTypeClick = { selectedSortType = (selectedSortType + 1) % 3 },
                onLayoutToggleClick = { isGridLayout = !isGridLayout },
                modifier = Modifier.fillMaxSize()
            )
        }

        with(sharedTransitionScope) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalItemSpacing = 12.dp,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "To-do list",
                            modifier = Modifier.padding(start = 15.dp),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            lineHeight = 15.sp
                        )
                    }
                }
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(
                        Modifier
                            .graphicsLayer {
                                clip = true
                                compositingStrategy = CompositingStrategy.Auto
                            }
                            .sharedElement(
                                state = rememberSharedContentState("task-board"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ ->
                                    tween(
                                        durationMillis = 800,
                                        easing = FastOutSlowInEasing
                                    )
                                },
                                placeHolderSize = contentSize,
                                zIndexInOverlay = 1f
                            )
                            .clickable { navController.navigate(AppRoutes.TASK) }
                    ) {
                        TaskBoard(task = nearestTask)
                    }
                }
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Note list",
                            modifier = Modifier.padding(start = 15.dp),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
                items(notes) { note ->
                    NoteCard(
                        note = note,
                        onClick = { navController.navigate("${AppRoutes.NOTE}/${note.id}") },
                        modifier = Modifier.sharedElement(
                            state = rememberSharedContentState(key = "note-${note.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    )
                }
                item(span = StaggeredGridItemSpan.FullLine) { Spacer(modifier = Modifier.height(50.dp)) }
            }
        }
    }
}

@Composable
fun CustomCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    size: Dp = 20.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(5.dp))
            .background(if (checked) Color(0xFF1966FF) else Color.Transparent)
            .border(1.5.dp, if (checked) Color(0xFF1966FF) else Color.Gray, RoundedCornerShape(5.dp))
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Checked",
                tint = Color.White,
                modifier = Modifier.size(size * 0.7f)
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("RememberReturnType")
@Composable
fun TaskBoard(task: TaskGroup?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(28.dp)
    ) {
        if (task == null) {
            // Hiển thị khi không có task nào sắp hết hạn
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Tuyệt vời!",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Không có nhiệm vụ nào sắp hết hạn.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        } else {
            // Hiển thị thông tin task sắp hết hạn
            val completedCount = task.subTasks.count { it.isCompleted }
            val totalCount = task.subTasks.size
            val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
            val percentage = (progress * 100).toInt()

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 3.dp)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                task.subTasks.take(3).forEach { subTask -> // Chỉ hiển thị 3 subtask đầu tiên
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 2.dp)
                    ) {
                        Text(
                            text = subTask.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if(subTask.isCompleted) Color.Gray else Color.Black,
                            textDecoration = if (subTask.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        CustomCheckbox(
                            checked = subTask.isCompleted,
                            onCheckedChange = { /* No-op on home screen */ }
                        )
                    }
                }
                if (task.subTasks.size > 3) {
                    Text(
                        text = "+ ${task.subTasks.size - 3} mục khác",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }


                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.alarm_clock),
                            contentDescription = "Alarm Icon",
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Hết hạn: ${task.dueDate?.format(DateTimeFormatter.ofPattern("HH:mm, E, dd MMM", Locale("vi")))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF97316),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1966FF),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(bottom = 4.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .align(Alignment.BottomStart)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE0E0E0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(8.dp)
                                .background(Color(0xFF1966FF), RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}
