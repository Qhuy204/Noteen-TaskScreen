package com.example.noteen.ui.screen

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.noteen.ui.theme.NoteenTheme
import com.example.noteen.utils.NotificationReceiver

// Biến toàn cục để giữ tham chiếu đến chuông báo
private var ringtone: Ringtone? = null

class ReminderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Các cờ để hiển thị Activity trên màn hình khóa
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        // Lấy thông tin task từ Intent
        val taskTitle = intent.getStringExtra(NotificationReceiver.EXTRA_TASK_TITLE) ?: "Nhiệm vụ đến hạn!"

        // Phát chuông báo
        playAlarm(this)

        setContent {
            // SỬA ĐỔI: Áp dụng theme của ứng dụng cho dialog
            NoteenTheme {
                ReminderDialog(
                    taskTitle = taskTitle,
                    onDismiss = {
                        stopAlarm() // Dừng chuông báo
                        finish()    // Đóng Activity
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm() // Đảm bảo chuông báo dừng khi Activity bị hủy
    }

    private fun playAlarm(context: Context) {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(context, alarmUri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarm() {
        ringtone?.stop()
        ringtone = null
    }
}

@Composable
fun ReminderDialog(taskTitle: String, onDismiss: () -> Unit) {
    // Sử dụng Dialog để có nền mờ
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                // SỬA ĐỔI: Sử dụng màu nền từ theme
                .background(MaterialTheme.colorScheme.surface),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Báo thức!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                // SỬA ĐỔI: Sử dụng màu chữ từ theme
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = taskTitle,
                fontSize = 20.sp,
                // SỬA ĐỔI: Sử dụng màu chữ phụ từ theme
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                // SỬA ĐỔI: Sử dụng màu chính từ theme cho button
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    "Tắt",
                    fontSize = 18.sp,
                    // SỬA ĐỔI: Sử dụng màu chữ trên màu chính từ theme
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
