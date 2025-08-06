package com.example.noteen.ui.component

import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.example.noteen.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

data class Note(
    val id: Int,
    val title: String,
    val content: String,
    val createdDate: String, // ISO 8601 format recommended
    val imageFileName: String? = null
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NoteCard(
    note: Note,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    val context = LocalContext.current
    val displayDate = formatNoteDate(note.createdDate)

    val scale = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()

    // Box tàng hình giữ layout cố định
    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // Box có hiệu ứng scale
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .clip(RoundedCornerShape(15.dp))
                .background(Color.White)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            coroutineScope.launch {
                                scale.animateTo(0.96f, animationSpec = tween(100))
                                scale.animateTo(1f, animationSpec = tween(150, easing = FastOutSlowInEasing))
                            }
                            onClick()
                        },
                        onLongPress = {
                            coroutineScope.launch {
                                scale.animateTo(0.96f, animationSpec = tween(100))
                                scale.animateTo(1f, animationSpec = tween(150, easing = FastOutSlowInEasing))
                            }
                            onLongPress()
                        }
                    )
                }
                .padding(12.dp)
        ) {
            Column {
                note.imageFileName?.let { fileName ->
                    val imageFile = File(context.getExternalFilesDir("images"), fileName)
                    val bitmap = imageFile.takeIf { it.exists() }?.let {
                        BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap()
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Note Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f / 4f)
                                .clip(RoundedCornerShape(15.dp))
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.default_image),
                            contentDescription = "Default Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(15.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (note.imageFileName == null) {
                    Spacer(modifier = Modifier.height(8.dp))

                    val contentLength = note.content.length
                    val maxLines = when {
                        contentLength < 50 -> 1
                        contentLength < 99 -> 3
                        else -> 5
                    }

                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.DarkGray),
                        maxLines = maxLines,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun formatNoteDate(dateString: String): String {
    return try {
        val dateTime = LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val yesterday = today.minusDays(1)
        val inputDate = dateTime.toLocalDate()
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        when {
            inputDate == today -> "Today ${dateTime.format(timeFormatter)}"
            inputDate == yesterday -> "Yesterday ${dateTime.format(timeFormatter)}"
            inputDate.year == today.year -> dateTime.format(DateTimeFormatter.ofPattern("MMM dd"))
            else -> dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        }
    } catch (e: Exception) {
        "Invalid date"
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun NoteCardPreview() {
    val sampleNote = Note(
        id = 1,
        title = "Image Note Example",
        content = "",
        createdDate = LocalDateTime.now().minusHours(1).format(DateTimeFormatter.ISO_DATE_TIME),
        imageFileName = "sample.jpg"
    )

    Column(modifier = Modifier.padding(16.dp)) {
        NoteCard(note = sampleNote)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun getSampleNotes(): List<Note> {
    val formatter = DateTimeFormatter.ISO_DATE_TIME
    return listOf(
        Note(
            id = 1,
            title = "Short text note",
            content = "Quick idea",
            createdDate = LocalDateTime.now().minusMinutes(10).format(formatter),
            imageFileName = null
        ),
        Note(
            id = 2,
            title = "Yesterday's journal",
            content = "This is a short journal entry I wrote yesterday about how my day went.",
            createdDate = LocalDateTime.now().minusDays(1).withHour(18).withMinute(30).format(formatter),
            imageFileName = null
        ),
        Note(
            id = 3,
            title = "Weekend Photo",
            content = "",
            createdDate = LocalDateTime.now().minusDays(2).format(formatter),
            imageFileName = "weekend_photo.jpg"
        ),
        Note(
            id = 4,
            title = "Long Note",
            content = "This is a very long note intended to test the max lines logic in the UI layout. " +
                    "It should stretch multiple lines and trigger the ellipsis after the fifth line " +
                    "to prevent overflow and ensure that the user interface remains consistent and clean.",
            createdDate = LocalDateTime.now().minusWeeks(1).format(formatter),
            imageFileName = null
        ),
        Note(
            id = 5,
            title = "Travel Memory",
            content = "",
            createdDate = LocalDateTime.now().minusMonths(2).format(formatter),
            imageFileName = "beach_trip.jpg"
        ),
        Note(
            id = 6,
            title = "Meeting Summary",
            content = "Summary of the Monday meeting discussing quarterly goals, current progress, blockers, and next steps.",
            createdDate = LocalDateTime.now().minusDays(3).format(formatter),
            imageFileName = null
        ),
        Note(
            id = 7,
            title = "Old Image Note",
            content = "",
            createdDate = LocalDateTime.now().minusYears(1).format(formatter),
            imageFileName = "old_memory.jpg"
        )
    )
}
