package com.example.noteen.data

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class Converters {
    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return value?.let { LocalDateTime.ofEpochSecond(it, 0, ZoneOffset.UTC) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? {
        return date?.toEpochSecond(ZoneOffset.UTC)
    }

    @TypeConverter
    fun fromUUID(uuid: UUID?): String? {
        return uuid?.toString()
    }

    @TypeConverter
    fun toUUID(uuid: String?): UUID? {
        return uuid?.let { UUID.fromString(it) }
    }
}


// --- Entities (Định nghĩa bảng trong database) ---

@Entity(tableName = "task_groups")
data class TaskGroupEntity(
    @PrimaryKey val id: UUID,
    val title: String,
    val dueDate: LocalDateTime?,
    var isExpanded: Boolean = true,
    var isCompleted: Boolean = false,
    @ColumnInfo(name = "display_order")
    var order: Int
)

@Entity(
    tableName = "sub_tasks",
    foreignKeys = [ForeignKey(
        entity = TaskGroupEntity::class,
        parentColumns = ["id"],
        childColumns = ["taskGroupId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class SubTaskEntity(
    @PrimaryKey val id: UUID,
    val taskGroupId: UUID,
    val title: String,
    var isCompleted: Boolean,
    @ColumnInfo(name = "display_order")
    var order: Int
)

// Lớp này dùng để kết hợp TaskGroup và danh sách SubTask của nó khi truy vấn
data class TaskGroupWithSubTasks(
    @Embedded val taskGroup: TaskGroupEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "taskGroupId"
    )
    val subTasks: List<SubTaskEntity>
)


// --- DAO (Đối tượng truy cập dữ liệu) ---
// Interface này định nghĩa các phương thức để tương tác với database.
@Dao
interface TaskDao {
    @Transaction
    @Query("SELECT * FROM task_groups ORDER BY display_order ASC")
    fun getTaskGroupsWithSubTasks(): Flow<List<TaskGroupWithSubTasks>>

    @Transaction
    @Query("SELECT * FROM task_groups WHERE isCompleted = 0 AND dueDate IS NOT NULL AND dueDate > :currentTime ORDER BY dueDate ASC LIMIT 1")
    fun getNearestUpcomingTask(currentTime: Long): Flow<TaskGroupWithSubTasks?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskGroup(taskGroup: TaskGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTasks(subTasks: List<SubTaskEntity>)

    @Update
    suspend fun updateTaskGroup(taskGroup: TaskGroupEntity)

    @Update
    suspend fun updateSubTask(subTask: SubTaskEntity)

    @Update
    suspend fun updateSubTasks(subTasks: List<SubTaskEntity>)

    @Query("DELETE FROM task_groups WHERE id IN (:ids)")
    suspend fun deleteTasksByIds(ids: List<UUID>)

    @Query("DELETE FROM sub_tasks WHERE id = :subTaskId")
    suspend fun deleteSubTaskById(subTaskId: UUID)

    @Transaction
    suspend fun saveNewTask(taskGroup: TaskGroupEntity, subTasks: List<SubTaskEntity>) {
        insertTaskGroup(taskGroup)
        insertSubTasks(subTasks)
    }

    @Transaction
    suspend fun updateTask(taskGroup: TaskGroupEntity, subTasks: List<SubTaskEntity>) {
        updateTaskGroup(taskGroup)
        // Xóa các subtask cũ không còn trong danh sách mới
        val oldSubTasks = getSubTasksForGroup(taskGroup.id)
        val newSubTaskIds = subTasks.map { it.id }.toSet()
        val toDelete = oldSubTasks.filterNot { it.id in newSubTaskIds }
        toDelete.forEach { deleteSubTaskById(it.id) }
        // Cập nhật hoặc thêm các subtask mới
        insertSubTasks(subTasks)
    }

    @Query("SELECT * FROM sub_tasks WHERE taskGroupId = :groupId")
    suspend fun getSubTasksForGroup(groupId: UUID): List<SubTaskEntity>

}


// --- Database ---
// Lớp trừu tượng kế thừa từ RoomDatabase.
@Database(entities = [TaskGroupEntity::class, SubTaskEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: TaskDatabase? = null

        fun getDatabase(context: Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "task_database"
                )
                    .fallbackToDestructiveMigration() // Xóa và xây dựng lại db nếu schema thay đổi
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
