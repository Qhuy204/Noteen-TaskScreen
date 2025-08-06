package com.example.noteen.data.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * Model cho một nhiệm vụ con (SubTask).
 * @property id ID duy nhất cho subtask.
 * @property title Tên/mô tả của subtask.
 * @property isCompleted Trạng thái hoàn thành của subtask.
 */
data class SubTask(
    val id: UUID = UUID.randomUUID(),
    var title: String,
    var isCompleted: Boolean = false
)

/**
 * Model cho một nhóm nhiệm vụ (Task Group).
 * @property id ID duy nhất cho Task.
 * @property title Tiêu đề chính của nhóm nhiệm vụ.
 * @property subTasks Danh sách các nhiệm vụ con thuộc nhóm này.
 * @property dueDate Hạn chót của Task (có thể được sử dụng cho nhắc nhở).
 * @property isExpanded Trạng thái giao diện, cho biết nhóm nhiệm vụ đang được mở rộng hay thu gọn.
 */
data class Task(
    val id: UUID = UUID.randomUUID(),
    var title: String,
    var subTasks: MutableList<SubTask> = mutableListOf(),
    var dueDate: LocalDateTime? = null,
    // Trạng thái UI để quản lý việc mở rộng/thu gọn
    var isExpanded: Boolean = true
)
