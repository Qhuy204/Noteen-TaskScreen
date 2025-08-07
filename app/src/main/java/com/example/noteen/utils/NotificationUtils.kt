package com.example.noteen.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.noteen.MainActivity // SỬA ĐỔI: Import MainActivity
import com.example.noteen.R
import com.example.noteen.ui.screen.ReminderActivity
import com.example.noteen.viewmodel.TaskGroup
import java.util.Calendar
import java.util.UUID

/**
 * Interface để lên lịch và hủy bỏ báo thức.
 * Giúp cho việc thay thế và kiểm thử dễ dàng hơn.
 */
interface AlarmScheduler {
    fun schedule(task: TaskGroup)
    fun cancel(task: TaskGroup)
}

/**
 * Lớp triển khai AlarmScheduler sử dụng AlarmManager của hệ thống.
 */
class AlarmSchedulerImpl(private val context: Context) : AlarmScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun schedule(task: TaskGroup) {
        val dueDate = task.dueDate ?: return

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EXTRA_TASK_ID, task.id.toString())
            putExtra(NotificationReceiver.EXTRA_TASK_TITLE, task.title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, dueDate.year)
            set(Calendar.MONTH, dueDate.monthValue - 1)
            set(Calendar.DAY_OF_MONTH, dueDate.dayOfMonth)
            set(Calendar.HOUR_OF_DAY, dueDate.hour)
            set(Calendar.MINUTE, dueDate.minute)
            set(Calendar.SECOND, dueDate.second)
            set(Calendar.MILLISECOND, 0)
        }
        val triggerAtMillis = calendar.timeInMillis

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                Log.w("AlarmScheduler", "Không thể đặt báo thức chính xác, quyền chưa được cấp.")
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    override fun cancel(task: TaskGroup) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}

/**
 * BroadcastReceiver lắng nghe Intent từ AlarmManager và hiển thị thông báo.
 */
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Nhiệm vụ sắp đến hạn!"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // (MỚI) Tạo Intent để mở MainActivity khi nhấn vào thông báo
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            (taskId.hashCode() + 1), // Sử dụng request code khác để tránh xung đột
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent để mở ReminderActivity (hộp thoại báo thức toàn màn hình)
        val fullScreenIntent = Intent(context, ReminderActivity::class.java).apply {
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, taskTitle)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tạo kênh thông báo cho Android 8.0 (API 26) trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nhắc nhở Nhiệm vụ",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kênh thông báo cho các nhắc nhở nhiệm vụ."
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Xây dựng thông báo
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.alarm_clock)
            .setContentTitle("Nhiệm vụ đến hạn!")
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent) // (MỚI) Đặt intent khi nhấn vào thông báo
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()

        notificationManager.notify(taskId.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "task_reminder_channel"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
    }
}
