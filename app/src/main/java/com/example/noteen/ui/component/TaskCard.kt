//package com.example.noteen.ui.component
//
//import android.os.Build
//import androidx.annotation.RequiresApi
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.core.animateFloatAsState
//import androidx.compose.animation.core.tween
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.MoreVert
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.draw.rotate
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextDecoration
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.example.noteen.R
//import com.example.noteen.data.model.SubTask
//import com.example.noteen.data.model.Task
//import com.example.noteen.viewmodel.TaskViewModel
//import java.time.LocalDateTime
//import java.time.format.DateTimeFormatter
//import java.util.Locale
//import java.util.UUID
//
//@RequiresApi(Build.VERSION_CODES.O)
//@Composable
//fun TaskCard(
//    task: Task,
//    viewModel: TaskViewModel
//) {
//    val completedSubTasks = task.subTasks.count { it.isCompleted }
//    val totalSubTasks = task.subTasks.size
//    val progress = if (totalSubTasks > 0) completedSubTasks.toFloat() / totalSubTasks.toFloat() else 0f
//    val isTaskCompleted = progress == 1f && totalSubTasks > 0
//
//    val animatedProgress by animateFloatAsState(
//        targetValue = progress,
//        animationSpec = tween(durationMillis = 500),
//        label = "progressAnimation"
//    )
//
//    val rotationState by animateFloatAsState(
//        targetValue = if (task.isExpanded) 180f else 0f,
//        label = "rotationAnimation"
//    )
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(containerColor = Color.White),
//        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .padding(horizontal = 16.dp, vertical = 12.dp)
//        ) {
//            // Hàng chứa Checkbox chính, Tiêu đề và nút Mở rộng/Thu gọn
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Checkbox(
//                    checked = isTaskCompleted,
//                    onCheckedChange = {
//                        viewModel.toggleMasterTaskCompletion(task.id, !isTaskCompleted)
//                    },
//                    enabled = totalSubTasks > 0 // Chỉ bật khi có sub-task
//                )
//                Spacer(modifier = Modifier.width(8.dp))
//                Text(
//                    text = task.title,
//                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
//                    modifier = Modifier.weight(1f)
//                )
//                Spacer(modifier = Modifier.width(8.dp))
//                IconButton(onClick = { /* TODO: Show context menu */ }) {
//                    Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn khác")
//                }
//                IconButton(onClick = { viewModel.toggleTaskExpansion(task.id) }) {
//                    Icon(
//                        painter = painterResource(id = R.drawable.ic_mark),
//                        contentDescription = "Mở rộng/Thu gọn",
//                        modifier = Modifier.rotate(rotationState)
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            // Hàng chứa Hạn chót và Trạng thái hoàn thành
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.SpaceBetween,
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                task.dueDate?.let {
//                    DueDateDisplay(it)
//                } ?: Spacer(modifier = Modifier.height(1.dp)) // Để giữ layout ổn định
//
//                if (totalSubTasks > 0) {
//                    Text(
//                        text = "Hoàn thành $completedSubTasks/$totalSubTasks",
//                        style = MaterialTheme.typography.bodySmall,
//                        color = MaterialTheme.colorScheme.primary
//                    )
//                }
//            }
//
//            // Thanh tiến trình
//            if (totalSubTasks > 0) {
//                Spacer(modifier = Modifier.height(8.dp))
//                LinearProgressIndicator(
//                    progress = { animatedProgress },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(8.dp)
//                        .clip(RoundedCornerShape(4.dp)),
//                )
//            }
//
//            // Danh sách công việc con
//            AnimatedVisibility(visible = task.isExpanded) {
//                Column(modifier = Modifier.padding(top = 8.dp)) {
//                    task.subTasks.forEach { subTask ->
//                        SubTaskItem(
//                            subTask = subTask,
//                            onCheckedChange = {
//                                viewModel.toggleSubTaskCompletion(task.id, subTask.id)
//                            }
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun SubTaskItem(
//    subTask: SubTask,
//    onCheckedChange: (Boolean) -> Unit
//) {
//    Row(
//        verticalAlignment = Alignment.CenterVertically,
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable { onCheckedChange(!subTask.isCompleted) }
//            .padding(vertical = 4.dp)
//    ) {
//        Checkbox(
//            checked = subTask.isCompleted,
//            onCheckedChange = { onCheckedChange(it) }
//        )
//        Spacer(modifier = Modifier.width(8.dp))
//        Text(
//            text = subTask.title,
//            style = MaterialTheme.typography.bodyMedium.copy(
//                textDecoration = if (subTask.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
//                color = if (subTask.isCompleted) Color.Gray else Color.Black
//            )
//        )
//    }
//}
//
//@RequiresApi(Build.VERSION_CODES.O)
//@Composable
//private fun DueDateDisplay(dueDate: LocalDateTime) {
//    val formatter = remember {
//        DateTimeFormatter.ofPattern("HH:mm E, dd MMM", Locale("vi"))
//    }
//    Row(verticalAlignment = Alignment.CenterVertically) {
//        Icon(
//            painter = painterResource(id = R.drawable.ic_mark),
//            contentDescription = "Hạn chót",
//            modifier = Modifier.size(14.dp),
//            tint = Color.Gray
//        )
//        Spacer(modifier = Modifier.width(4.dp))
//        Text(
//            text = "Hạn lúc ${dueDate.format(formatter)}",
//            style = MaterialTheme.typography.bodySmall,
//            color = Color.Gray
//        )
//    }
//}
