package com.example.noteen.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.commandiron.wheel_picker_compose.WheelDateTimePicker
import com.commandiron.wheel_picker_compose.core.TimeFormat
import com.commandiron.wheel_picker_compose.core.WheelPickerDefaults
import com.example.noteen.R
import com.example.noteen.utils.AlarmScheduler
import com.example.noteen.utils.AlarmSchedulerImpl
import com.example.noteen.viewmodel.DueDateStatus
import com.example.noteen.viewmodel.SubTask
import com.example.noteen.viewmodel.TaskGroup
import com.example.noteen.viewmodel.TasksViewModel
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import android.app.AlarmManager

// --- Định nghĩa màu sắc và hằng số ---
private val brandBlue = Color(0xFF1966FF)
private val brandGreen = Color(0xFF10B981)
private val brandYellow = Color(0xFFFFC107)
val screenBg = Color(0xFFf2f2f2)
private val cardBg = Color.White
private val textPrimary = Color(0xFF1F2937)
private val textSecondary = Color(0xFF6B7280)
private val colorOrange = Color(0xFFF97316)
private val colorRed = Color(0xFFEF4444)


@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TaskScreen(
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val viewModel: TasksViewModel = viewModel()
    val tasks by viewModel.taskGroups.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val selectedTaskIds by viewModel.selectedTaskIds.collectAsState()

    val (todoTasks, completedTasks) = tasks.partition { !it.isCompleted }

    var showTaskDetailsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskGroup?>(null) }
    val haptics = LocalHapticFeedback.current

    val context = LocalContext.current
    val alarmScheduler: AlarmScheduler = remember { AlarmSchedulerImpl(context) }

    // Cấu hình state cho việc sắp xếp lại
    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to -> viewModel.moveTask(from.index, to.index) },
        onDragEnd = { _, _ -> viewModel.saveTaskOrder() }
    )

    with(sharedTransitionScope) {
        Box(
            Modifier
                .fillMaxSize()
                .sharedElement(
                    state = rememberSharedContentState("task-board"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> tween(durationMillis = 1000, easing = LinearOutSlowInEasing) },
                    placeHolderSize = SharedTransitionScope.PlaceHolderSize.contentSize,
                    zIndexInOverlay = 1f
                )
                .graphicsLayer {
                    clip = true
                    compositingStrategy = CompositingStrategy.Auto
                }
        ) {
            Column(modifier = Modifier
                .fillMaxSize()
                .background(screenBg)) {
                AnimatedContent(targetState = isEditMode, label = "Header Animation") { inEditMode ->
                    if (inEditMode) {
                        EditModeHeader(
                            selectedCount = selectedTaskIds.size,
                            onCancel = { viewModel.exitEditMode() },
                            onSelectAll = { viewModel.selectAll() }
                        )
                    } else {
                        TaskScreenHeader()
                    }
                }

                LazyColumn(
                    state = reorderState.listState,
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .reorderable(reorderState)
                ) {
                    itemsIndexed(todoTasks, key = { _, item -> item.id }) { index, task ->
                        ReorderableItem(reorderableState = reorderState, key = task.id) { isDragging ->
                            LaunchedEffect(isDragging) {
                                if (isDragging) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                            val scale by animateFloatAsState(if (isDragging) 1.05f else 1f, label = "scaleAnim")
                            val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevationAnim")

                            TaskGroupCard(
                                modifier = Modifier
                                    .animateItemPlacement(tween(durationMillis = 300, easing = LinearOutSlowInEasing))
                                    .detectReorderAfterLongPress(reorderState)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        shadowElevation = elevation.toPx()
                                    },
                                taskGroup = task,
                                isEditMode = isEditMode,
                                isSelected = task.id in selectedTaskIds,
                                onMasterCheckChange = { isChecked ->
                                    viewModel.toggleMasterCheckbox(task.id, isChecked)
                                    if (isChecked) {
                                        alarmScheduler.cancel(task)
                                    } else {
                                        alarmScheduler.schedule(task)
                                    }
                                },
                                onCardClick = {
                                    if (isEditMode) {
                                        viewModel.toggleSelection(task.id)
                                    } else {
                                        taskToEdit = task
                                        showTaskDetailsDialog = true
                                    }
                                },
                                onExpandToggle = { viewModel.toggleExpansion(task.id) },
                                onSubTaskCheckChange = { subTaskId -> viewModel.toggleSubTaskCheckbox(task.id, subTaskId) }
                            )
                        }
                    }

                    if (completedTasks.isNotEmpty()) {
                        item(key = "completed_header") {
                            Text(
                                "HOÀN TẤT",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = textSecondary),
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp)
                            )
                        }
                        itemsIndexed(completedTasks, key = { _, item -> item.id }) { _, task ->
                            TaskGroupCard(
                                modifier = Modifier.animateItemPlacement(tween(durationMillis = 300, easing = LinearOutSlowInEasing)),
                                taskGroup = task,
                                isEditMode = isEditMode,
                                isSelected = task.id in selectedTaskIds,
                                onMasterCheckChange = { isChecked -> viewModel.toggleMasterCheckbox(task.id, isChecked) },
                                onCardClick = {
                                    if (isEditMode) {
                                        viewModel.toggleSelection(task.id)
                                    }
                                },
                                onExpandToggle = { viewModel.toggleExpansion(task.id) },
                                onSubTaskCheckChange = { subTaskId -> viewModel.toggleSubTaskCheckbox(task.id, subTaskId) }
                            )
                        }
                    }
                }
            }

            BottomActionBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                isVisible = isEditMode,
                isDeleteEnabled = selectedTaskIds.isNotEmpty(),
                onDelete = { showDeleteConfirmDialog = true }
            )

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val fabSize = 56.dp
                val paddingEnd = 24.dp
                val paddingBottom = 24.dp
                val xOffset = maxWidth - fabSize - paddingEnd
                val yOffset = maxHeight - fabSize - paddingBottom

                AddTaskFab(
                    modifier = Modifier
                        .absoluteOffset(x = xOffset, y = yOffset)
                        .size(fabSize),
                    isVisible = !isEditMode,
                    onClick = {
                        taskToEdit = null
                        showTaskDetailsDialog = true
                    }
                )
            }

            AnimatedVisibility(
                visible = showTaskDetailsDialog || showDeleteConfirmDialog,
                enter = fadeIn(animationSpec = tween(400)),
                exit = fadeOut(animationSpec = tween(400))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(enabled = false, onClick = {})
                )
            }

            AnimatedVisibility(
                visible = showTaskDetailsDialog,
                enter = slideInVertically(animationSpec = tween(400, easing = LinearOutSlowInEasing)) { it },
                exit = slideOutVertically(animationSpec = tween(400, easing = LinearOutSlowInEasing)) { it }
            ) {
                TaskDetailsDialog(
                    existingTask = taskToEdit,
                    onDismiss = { showTaskDetailsDialog = false },
                    onSaveTask = { taskToSave ->
                        viewModel.upsertTask(taskToSave)
                        if (taskToSave.dueDate != null && !taskToSave.isCompleted) {
                            alarmScheduler.schedule(taskToSave)
                        } else {
                            taskToEdit?.let { alarmScheduler.cancel(it) }
                        }
                        showTaskDetailsDialog = false
                    },
                    tasksCount = tasks.size
                )
            }

            if (showDeleteConfirmDialog) {
                DeleteConfirmationDialog(
                    count = selectedTaskIds.size,
                    onConfirm = {
                        val tasksToDelete = tasks.filter { it.id in selectedTaskIds }
                        tasksToDelete.forEach { alarmScheduler.cancel(it) }
                        viewModel.deleteSelected()
                        showDeleteConfirmDialog = false
                    },
                    onDismiss = { showDeleteConfirmDialog = false }
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Xác nhận xóa", fontWeight = FontWeight.Bold) },
        text = { Text("Bạn có chắc chắn muốn xóa $count nhiệm vụ đã chọn?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Xóa", color = colorRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}


@Composable
private fun TaskScreenHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 40.dp, end = 20.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Nhiệm vụ",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = textPrimary
        )
        Spacer(Modifier.weight(1f))
        val viewModel: TasksViewModel = viewModel()
        CustomIconButton(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    viewModel.enterEditMode()
                } else {
                    // TODO(): Handle API level < 26
                }
            }
        ) {
            Icon(Icons.Default.Edit, "Chỉnh sửa", tint = textSecondary)
        }

    }
}

@Composable
private fun EditModeHeader(selectedCount: Int, onCancel: () -> Unit, onSelectAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 40.dp, end = 20.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        CustomIconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Hủy") }
        Text(
            text = if (selectedCount == 0) "Chọn mục" else "Đã chọn $selectedCount mục",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        CustomIconButton(onClick = onSelectAll) { Icon(Icons.Default.List, "Chọn tất cả") }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun TaskGroupCard(
    modifier: Modifier = Modifier,
    taskGroup: TaskGroup,
    isEditMode: Boolean,
    isSelected: Boolean,
    onMasterCheckChange: (Boolean) -> Unit,
    onCardClick: () -> Unit,
    onExpandToggle: () -> Unit,
    onSubTaskCheckChange: (UUID) -> Unit
) {
    val completedCount = taskGroup.subTasks.count { it.isCompleted }
    val totalCount = taskGroup.subTasks.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
    val cardAlpha by animateFloatAsState(if (taskGroup.isCompleted && !isEditMode) 0.7f else 1f, label = "cardAlpha")
    val rotationAngle by animateFloatAsState(
        targetValue = if (taskGroup.isExpanded) 0f else -90f,
        animationSpec = tween(durationMillis = 300),
        label = "rotationAngle"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .clickable(onClick = onCardClick)
            .padding(vertical = 8.dp, horizontal = 12.dp)
            .graphicsLayer(alpha = cardAlpha)
            .animateContentSize(animationSpec = tween(300))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isEditMode) {
                CustomCheckbox(
                    checked = isSelected,
                    onCheckedChange = { onCardClick() },
                    checkedColor = brandYellow,
                    size = 24.dp,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                if (totalCount > 0) {
                    CustomIconButton(onClick = onExpandToggle) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Mở rộng/Thu gọn",
                            modifier = Modifier.rotate(rotationAngle),
                            tint = textSecondary
                        )
                    }
                } else {
                    Spacer(Modifier.size(36.dp))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    taskGroup.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary)
                )
                DueDateInfo(dueDate = taskGroup.dueDate)
            }

            if (!isEditMode) {
                CustomCheckbox(
                    checked = taskGroup.isCompleted,
                    onCheckedChange = onMasterCheckChange,
                    checkedColor = brandBlue,
                    size = 24.dp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = taskGroup.isExpanded && totalCount > 0 && !isEditMode,
            enter = expandVertically(expandFrom = Alignment.Top, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
            exit = shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        ) {
            Column(
                modifier = Modifier.padding(start = 36.dp, end = 36.dp, top = 4.dp, bottom = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    CustomProgressBar(progress = progress, isCompleted = taskGroup.isCompleted, modifier = Modifier.weight(1f))
                    Text(
                        "$completedCount/$totalCount",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = textSecondary),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                taskGroup.subTasks.forEach { subTask ->
                    SubTaskDisplayItem(
                        subTask = subTask,
                        onCheckedChange = { onSubTaskCheckChange(subTask.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SubTaskDisplayItem(subTask: SubTask, onCheckedChange: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CustomCheckbox(
            checked = subTask.isCompleted,
            onCheckedChange = { onCheckedChange() },
            size = 18.dp
        )
        Text(
            text = subTask.title,
            modifier = Modifier.weight(1f),
            color = if (subTask.isCompleted) textSecondary else textPrimary,
            textDecoration = if (subTask.isCompleted) TextDecoration.LineThrough else null,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DueDateInfo(dueDate: LocalDateTime?) {
    val viewModel: TasksViewModel = viewModel()
    val status = viewModel.getDueDateStatus(dueDate)
    if (status != null && dueDate != null) {
        val (iconRes, color, text) = when (status) {
            DueDateStatus.OVERDUE -> Triple(R.drawable.alarm_clock, colorRed, "Đã hết hạn: ${dueDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
            DueDateStatus.UPCOMING -> Triple(R.drawable.alarm_clock, colorOrange, "Hết hạn: ${dueDate.format(DateTimeFormatter.ofPattern("HH:mm, E, dd MMM", Locale("vi")))}")
            DueDateStatus.LONG_TERM -> Triple(R.drawable.alarm_clock, textSecondary, "Hết hạn: ${dueDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = "Trạng thái",
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = color
            )
        }
    }
}

@Composable
private fun SubTaskItem(
    modifier: Modifier = Modifier,
    subTask: SubTask,
    onCheckedChange: () -> Unit,
    onValueChange: (String) -> Unit,
    onRemove: () -> Unit,
    focusRequester: FocusRequester
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CustomCheckbox(checked = subTask.isCompleted, onCheckedChange = { onCheckedChange() })
        BasicTextField(
            value = subTask.title,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                textDecoration = if (subTask.isCompleted) TextDecoration.LineThrough else null,
                color = if (subTask.isCompleted) textSecondary else textPrimary
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        CustomIconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = "Xóa", tint = textSecondary)
        }
    }
}

@Composable
private fun AddTaskFab(modifier: Modifier = Modifier, isVisible: Boolean, onClick: () -> Unit) {
    AnimatedVisibility(visible = isVisible, enter = scaleIn(), exit = scaleOut()) {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(brandBlue)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm nhiệm vụ", tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun TaskDetailsDialog(
    existingTask: TaskGroup?,
    onDismiss: () -> Unit,
    onSaveTask: (TaskGroup) -> Unit,
    tasksCount: Int
) {
    var title by remember { mutableStateOf(existingTask?.title ?: "") }
    val initialSubtasks = existingTask?.subTasks?.toMutableList() ?: mutableListOf()
    var subtasks by remember { mutableStateOf<MutableList<SubTask>>(initialSubtasks) }

    var newSubtaskText by remember { mutableStateOf("") }
    var showReminderSection by remember { mutableStateOf(existingTask?.dueDate != null) }
    var selectedDateTime by remember { mutableStateOf(existingTask?.dueDate ?: LocalDateTime.now().plusMinutes(1)) }

    val isSaveEnabled = title.isNotBlank()

    val focusManager = LocalFocusManager.current
    val newSubtaskFocus = remember { FocusRequester() }

    val subtaskReorderState = rememberReorderableLazyListState(onMove = { from, to ->
        subtasks = subtasks.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    })

    val resetState = {
        title = ""
        subtasks = mutableListOf()
        newSubtaskText = ""
        showReminderSection = false
        selectedDateTime = LocalDateTime.now().plusMinutes(1)
    }

    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
            if (isGranted) showReminderSection = true
        }
    )

    var showExactAlarmPermissionDialog by remember { mutableStateOf(false) }
    val alarmManager = context.getSystemService(AlarmManager::class.java)

    val settingsLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
        // Sau khi người dùng quay lại từ cài đặt, ta có thể kiểm tra lại quyền và lưu task
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            val finalDueDate = if (showReminderSection) selectedDateTime else null
            val taskGroupId = existingTask?.id ?: UUID.randomUUID()
            val finalSubtasks = subtasks.mapIndexed { index, subTask -> subTask.copy(order = index, taskGroupId = taskGroupId) }
            val allCompleted = finalSubtasks.isNotEmpty() && finalSubtasks.all { it.isCompleted }
            val taskToSave = TaskGroup(
                id = taskGroupId, title = title, subTasks = finalSubtasks, dueDate = finalDueDate,
                isExpanded = existingTask?.isExpanded ?: true, isCompleted = allCompleted, order = existingTask?.order ?: tasksCount
            )
            onSaveTask(taskToSave)
            resetState()
        }
    }

    if (showExactAlarmPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showExactAlarmPermissionDialog = false },
            title = { Text("Yêu cầu quyền", fontWeight = FontWeight.Bold) },
            text = { Text("Để đảm bảo bạn không bỏ lỡ nhiệm vụ, ứng dụng cần quyền \"Báo thức và lời nhắc\" để gửi thông báo đúng giờ.") },
            confirmButton = {
                TextButton(onClick = {
                    showExactAlarmPermissionDialog = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            settingsLauncher.launch(this)
                        }
                    }
                }) {
                    Text("Đến cài đặt")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExactAlarmPermissionDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(bottom = 5.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .fillMaxWidth()
                .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.9f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .pointerInput(Unit) { detectTapGestures(onTap = {}) }
        ) {
            Text(
                if (existingTask == null) "Thêm nhiệm vụ mới" else "Chỉnh sửa nhiệm vụ",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(20.dp))

            BasicTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(screenBg, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                decorationBox = { innerTextField ->
                    if (title.isEmpty()) Text("Tên nhiệm vụ...", color = textSecondary, style = MaterialTheme.typography.bodyLarge)
                    innerTextField()
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Nhiệm vụ con", style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                state = subtaskReorderState.listState,
                modifier = Modifier
                    .reorderable(subtaskReorderState)
                    .heightIn(max = 200.dp)
            ) {
                itemsIndexed(subtasks, key = { _, item -> item.id }) { index, subtask ->
                    ReorderableItem(reorderableState = subtaskReorderState, key = subtask.id) {
                        val focusRequester = remember { FocusRequester() }
                        SubTaskItem(
                            modifier = Modifier.detectReorderAfterLongPress(subtaskReorderState),
                            subTask = subtask,
                            onCheckedChange = { subtasks = subtasks.toMutableList().apply { set(index, subtask.copy(isCompleted = !subtask.isCompleted)) } },
                            onValueChange = { newText -> subtasks = subtasks.toMutableList().apply { set(index, subtask.copy(title = newText)) } },
                            onRemove = { subtasks = subtasks.toMutableList().apply { removeAt(index) } },
                            focusRequester = focusRequester,
                        )
                    }
                }
            }

            Row(modifier = Modifier.padding(top = 8.dp)) {
                BasicTextField(
                    value = newSubtaskText,
                    onValueChange = { newSubtaskText = it },
                    modifier = Modifier
                        .weight(1f)
                        .background(screenBg, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .focusRequester(newSubtaskFocus),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (newSubtaskText.isNotBlank()) {
                            val newSub = SubTask(id = UUID.randomUUID(), taskGroupId = existingTask?.id ?: UUID.randomUUID(), title = newSubtaskText, isCompleted = false, order = subtasks.size)
                            subtasks = (subtasks + newSub) as MutableList<SubTask>
                            newSubtaskText = ""
                            newSubtaskFocus.requestFocus()
                        } else {
                            focusManager.clearFocus()
                        }
                    }),
                    decorationBox = { innerTextField ->
                        if (newSubtaskText.isEmpty()) Text("Thêm mục...", color = textSecondary, style = MaterialTheme.typography.bodyMedium)
                        innerTextField()
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray)
                    .clickable {
                        if (newSubtaskText.isNotBlank()) {
                            val newSub = SubTask(id = UUID.randomUUID(), taskGroupId = existingTask?.id ?: UUID.randomUUID(), title = newSubtaskText, isCompleted = false, order = subtasks.size)
                            subtasks = (subtasks + newSub) as MutableList<SubTask>
                            newSubtaskText = ""
                            newSubtaskFocus.requestFocus()
                        }
                    }
                    .padding(12.dp)) {
                    Text("Thêm", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = !showReminderSection) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (!hasNotificationPermission) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    showReminderSection = true
                                }
                            } else {
                                showReminderSection = true
                            }
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = "Thêm nhắc nhở", tint = textSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Thêm nhắc nhở", color = textSecondary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            AnimatedVisibility(
                visible = showReminderSection,
                enter = expandVertically(animationSpec = tween(400)) + fadeIn(animationSpec = tween(200, delayMillis = 200)),
                exit = shrinkVertically(animationSpec = tween(400)) + fadeOut(animationSpec = tween(200))
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Đặt nhắc nhở", style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold))
                        CustomIconButton(onClick = { showReminderSection = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Hủy nhắc nhở", tint = textSecondary)
                        }
                    }
                    DateTimePicker(
                        initialDateTime = selectedDateTime,
                        onDateTimeSelected = { selectedDateTime = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.LightGray)
                    .clickable {
                        resetState()
                        onDismiss()
                    }, contentAlignment = Alignment.Center) {
                    Text("Hủy", fontWeight = FontWeight.Bold, color = textPrimary)
                }
                val saveButtonColor by animateColorAsState(if (isSaveEnabled) brandBlue else Color.Gray, label = "save_button_color")
                Box(modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(saveButtonColor)
                    .clickable(enabled = isSaveEnabled) {
                        val finalDueDate = if (showReminderSection) selectedDateTime else null
                        val proceedToSave = {
                            val taskGroupId = existingTask?.id ?: UUID.randomUUID()
                            val finalSubtasks = subtasks.mapIndexed { index, subTask -> subTask.copy(order = index, taskGroupId = taskGroupId) }
                            val allCompleted = finalSubtasks.isNotEmpty() && finalSubtasks.all { it.isCompleted }
                            val taskToSave = TaskGroup(
                                id = taskGroupId, title = title, subTasks = finalSubtasks, dueDate = finalDueDate,
                                isExpanded = existingTask?.isExpanded ?: true, isCompleted = allCompleted, order = existingTask?.order ?: tasksCount
                            )
                            onSaveTask(taskToSave)
                            resetState()
                        }

                        if (finalDueDate != null) {
                            if(finalDueDate.isAfter(LocalDateTime.now())){
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    if (alarmManager.canScheduleExactAlarms()) {
                                        proceedToSave()
                                    } else {
                                        showExactAlarmPermissionDialog = true
                                    }
                                } else {
                                    proceedToSave()
                                }
                            } else {
                                proceedToSave()
                            }
                        } else {
                            proceedToSave()
                        }
                    }, contentAlignment = Alignment.Center) {
                    Text("Lưu", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DateTimePicker(
    initialDateTime: LocalDateTime,
    onDateTimeSelected: (LocalDateTime) -> Unit
) {
    WheelDateTimePicker(
        modifier = Modifier.fillMaxWidth(),
        startDateTime = initialDateTime,
        minDateTime = LocalDateTime.now(),
        maxDateTime = LocalDateTime.now().plusYears(100),
        timeFormat = TimeFormat.HOUR_24,
        size = DpSize(360.dp, 160.dp),
        rowCount = 5,
        textStyle = MaterialTheme.typography.bodyLarge,
        textColor = textSecondary,
        selectorProperties = WheelPickerDefaults.selectorProperties(
            enabled = true,
            shape = RoundedCornerShape(16.dp),
            color = brandBlue.copy(alpha = 0.1f),
            border = null
        ),
        onSnappedDateTime = { snapped ->
            if (snapped.isAfter(LocalDateTime.now())) {
                onDateTimeSelected(snapped)
            } else {
                onDateTimeSelected(LocalDateTime.now().plusMinutes(1))
            }
        }
    )
}


@Composable
private fun CustomProgressBar(progress: Float, isCompleted: Boolean, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress", animationSpec = tween(400))
    val color = if (isCompleted) Color.Gray.copy(alpha = 0.5f) else brandBlue
    Box(modifier = modifier
        .height(8.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(Color(0xFFE0E0E0))) {
        Box(modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(animatedProgress)
            .clip(RoundedCornerShape(4.dp))
            .background(color))
    }
}

@Composable
private fun CustomCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 20.dp,
    checkedColor: Color = brandBlue
) {
    val shape = RoundedCornerShape(5.dp)
    val borderColor by animateColorAsState(if (checked) checkedColor else Color.Gray.copy(alpha = 0.5f), label = "checkbox_border")
    val bgColor by animateColorAsState(if (checked) checkedColor else Color.Transparent, label = "checkbox_bg")

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(bgColor)
            .border(1.5.dp, borderColor, shape)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Checked",
                tint = Color.White,
                modifier = Modifier.size(size * 0.7f)
            )
        }
    }
}

@Composable
private fun CustomIconButton(modifier: Modifier = Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun BottomActionBar(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    isDeleteEnabled: Boolean,
    onDelete: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(animationSpec = tween(300)) { it },
        exit = slideOutVertically(animationSpec = tween(300)) { it },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val color = if (isDeleteEnabled) colorRed else Color.Gray
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(enabled = isDeleteEnabled, onClick = onDelete)
            ) {
                Icon(Icons.Default.Delete, "Xóa", tint = color)
                Text("Xóa", color = color, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
