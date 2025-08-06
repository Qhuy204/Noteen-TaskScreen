package com.example.noteen.viewmodel

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteen.data.SubTaskEntity
import com.example.noteen.data.TaskDatabase
import com.example.noteen.data.TaskGroupEntity
import com.example.noteen.data.TaskGroupWithSubTasks
import com.example.noteen.repository.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID

// --- Data Models for UI ---
// Các lớp data này được sử dụng trong UI, tách biệt với các Entity của database.
enum class DueDateStatus {
    OVERDUE, UPCOMING, LONG_TERM
}

data class SubTask(
    val id: UUID,
    val taskGroupId: UUID,
    val title: String,
    var isCompleted: Boolean,
    var order: Int
)

data class TaskGroup(
    val id: UUID,
    val title: String,
    val subTasks: List<SubTask>,
    val dueDate: LocalDateTime?,
    var isExpanded: Boolean,
    var isCompleted: Boolean,
    var order: Int
)

// --- Mapper Functions ---
// Chuyển đổi giữa Database Entities và UI Models
@RequiresApi(Build.VERSION_CODES.O)
fun TaskGroupWithSubTasks.toModel(): TaskGroup {
    val sortedSubTasks = subTasks.sortedBy { it.order }.map { it.toModel() }
    return TaskGroup(
        id = this.taskGroup.id,
        title = this.taskGroup.title,
        subTasks = sortedSubTasks,
        dueDate = this.taskGroup.dueDate,
        isExpanded = this.taskGroup.isExpanded,
        isCompleted = this.taskGroup.isCompleted,
        order = this.taskGroup.order
    )
}

fun SubTaskEntity.toModel(): SubTask {
    return SubTask(
        id = this.id,
        taskGroupId = this.taskGroupId,
        title = this.title,
        isCompleted = this.isCompleted,
        order = this.order
    )
}

fun TaskGroup.toEntity(): TaskGroupEntity {
    return TaskGroupEntity(
        id = this.id,
        title = this.title,
        dueDate = this.dueDate,
        isExpanded = this.isExpanded,
        isCompleted = this.isCompleted,
        order = this.order
    )
}

fun SubTask.toEntity(): SubTaskEntity {
    return SubTaskEntity(
        id = this.id,
        taskGroupId = this.taskGroupId,
        title = this.title,
        isCompleted = this.isCompleted,
        order = this.order
    )
}


@RequiresApi(Build.VERSION_CODES.O)
class TasksViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository
    val nearestUpcomingTask: StateFlow<TaskGroup?>

    private val _taskGroups = MutableStateFlow<List<TaskGroup>>(emptyList())
    val taskGroups: StateFlow<List<TaskGroup>> = _taskGroups.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _selectedTaskIds = MutableStateFlow<Set<UUID>>(emptySet())
    val selectedTaskIds: StateFlow<Set<UUID>> = _selectedTaskIds.asStateFlow()

    init {
        val taskDao = TaskDatabase.getDatabase(application).taskDao()
        repository = TaskRepository(taskDao)

        viewModelScope.launch {
            repository.allTasks.collect { list ->
                // Sắp xếp danh sách theo 'order' khi tải lần đầu
                _taskGroups.value = list.map { it.toModel() }.sortedBy { it.order }
            }
        }

        nearestUpcomingTask = repository.getNearestUpcomingTask()
            .map { it?.toModel() }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)
    }

    fun enterEditMode() { _isEditMode.value = true }
    fun exitEditMode() {
        _isEditMode.value = false
        _selectedTaskIds.value = emptySet()
    }

    fun toggleSelection(groupId: UUID) {
        _selectedTaskIds.update { currentIds ->
            if (groupId in currentIds) currentIds - groupId else currentIds + groupId
        }
    }

    fun selectAll() {
        val allIds = _taskGroups.value.map { it.id }.toSet()
        if (_selectedTaskIds.value.size == allIds.size) {
            _selectedTaskIds.value = emptySet()
        } else {
            _selectedTaskIds.value = allIds
        }
    }

    fun deleteSelected() = viewModelScope.launch {
        repository.deleteTasks(_selectedTaskIds.value.toList())
        exitEditMode()
    }

    fun toggleMasterCheckbox(groupId: UUID, isChecked: Boolean) = viewModelScope.launch {
        _taskGroups.value.find { it.id == groupId }?.let { group ->
            val updatedSubTasks = group.subTasks.map { it.copy(isCompleted = isChecked) }
            val updatedGroup = group.copy(
                subTasks = updatedSubTasks,
                isCompleted = isChecked,
                isExpanded = if (isChecked) false else group.isExpanded
            )
            repository.updateTask(updatedGroup.toEntity(), updatedSubTasks.map { it.toEntity() })
        }
    }

    fun toggleSubTaskCheckbox(groupId: UUID, subTaskId: UUID) = viewModelScope.launch {
        _taskGroups.value.find { it.id == groupId }?.let { group ->
            val updatedSubTasks = group.subTasks.map { subTask ->
                if (subTask.id == subTaskId) subTask.copy(isCompleted = !subTask.isCompleted) else subTask
            }
            val allCompleted = updatedSubTasks.isNotEmpty() && updatedSubTasks.all { it.isCompleted }
            val updatedGroup = group.copy(subTasks = updatedSubTasks, isCompleted = allCompleted)
            repository.updateTask(updatedGroup.toEntity(), updatedSubTasks.map { it.toEntity() })
        }
    }

    fun toggleExpansion(groupId: UUID) = viewModelScope.launch {
        _taskGroups.value.find { it.id == groupId }?.let { group ->
            val updatedGroup = group.copy(isExpanded = !group.isExpanded)
            repository.updateTaskGroup(updatedGroup.toEntity())
        }
    }

    /**
     * Di chuyển một task trong danh sách các task chưa hoàn thành.
     * Hàm này sẽ tách danh sách, thực hiện di chuyển trên danh sách con, sau đó ghép lại.
     * Điều này đảm bảo các chỉ số (index) được sử dụng là chính xác và tránh lỗi crash.
     * @param fromIndexInTodo Chỉ số bắt đầu trong danh sách các task chưa hoàn thành.
     * @param toIndexInTodo Chỉ số kết thúc trong danh sách các task chưa hoàn thành.
     */
    fun moveTask(fromIndexInTodo: Int, toIndexInTodo: Int) {
        _taskGroups.update { currentTasks ->
            // 1. Tách danh sách thành "chưa hoàn thành" và "đã hoàn thành" để khớp với UI
            val todoTasks = currentTasks.filter { !it.isCompleted }.toMutableList()
            val completedTasks = currentTasks.filter { it.isCompleted }

            // 2. Kiểm tra xem chỉ số 'from' có hợp lệ không (như một biện pháp an toàn)
            if (fromIndexInTodo !in todoTasks.indices) {
                return@update currentTasks // Chỉ số không hợp lệ, không làm gì cả
            }

            // 3. Thực hiện thao tác di chuyển trên danh sách 'todoTasks'
            val itemToMove = todoTasks.removeAt(fromIndexInTodo)

            // 4. Đảm bảo chỉ số 'to' nằm trong giới hạn hợp lệ của danh sách sau khi đã xóa
            val targetIndex = toIndexInTodo.coerceIn(0, todoTasks.size)
            todoTasks.add(targetIndex, itemToMove)

            // 5. Kết hợp lại hai danh sách và trả về trạng thái mới
            todoTasks + completedTasks
        }
    }


    /**
     * Lưu thứ tự hiện tại của các task vào cơ sở dữ liệu.
     * Hàm này nên được gọi sau khi thao tác kéo-thả kết thúc.
     */
    fun saveTaskOrder() {
        viewModelScope.launch {
            val orderedTasks = _taskGroups.value.mapIndexed { index, taskGroup ->
                taskGroup.copy(order = index)
            }
            repository.updateTaskOrder(orderedTasks.map { it.toEntity() })
        }
    }


    fun upsertTask(taskToSave: TaskGroup) = viewModelScope.launch {
        // Kiểm tra xem đây là task mới hay task cũ
        val existingTask = _taskGroups.value.any { it.id == taskToSave.id }
        if (existingTask) {
            repository.updateTask(taskToSave.toEntity(), taskToSave.subTasks.map { it.toEntity() })
        } else {
            repository.insertTask(taskToSave.toEntity(), taskToSave.subTasks.map { it.toEntity() })
        }
    }

    fun getDueDateStatus(dueDate: LocalDateTime?): DueDateStatus? {
        if (dueDate == null) return null
        val now = LocalDateTime.now()
        return when {
            dueDate.isBefore(now) -> DueDateStatus.OVERDUE
            dueDate.isBefore(now.plusDays(2)) -> DueDateStatus.UPCOMING
            else -> DueDateStatus.LONG_TERM
        }
    }
}
