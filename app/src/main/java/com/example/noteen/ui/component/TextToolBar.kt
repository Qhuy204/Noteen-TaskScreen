package com.example.noteen.ui.component

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Composable
fun ToolBar(
    jsonString: String,
    webView: WebView
) {
    val gson = remember { Gson() }

    val buttonMap = remember(jsonString) {
        val type = object : TypeToken<Map<String, ButtonState>>() {}.type
        runCatching {
            gson.fromJson<Map<String, ButtonState>>(jsonString, type)
        }.getOrElse { emptyMap() }
    }

    val buttons = listOf(
        ToolBarButton("Undo", "undo", "editor.chain().focus().undo().run();"),
        ToolBarButton("Redo", "redo", "editor.chain().focus().redo().run();"),

        ToolBarButton("H1", "heading1", "editor.chain().focus().toggleHeading({ level: 1 }).run();"),
        ToolBarButton("H2", "heading2", "editor.chain().focus().toggleHeading({ level: 2 }).run();"),
        ToolBarButton("H3", "heading3", "editor.chain().focus().toggleHeading({ level: 3 }).run();"),

        ToolBarButton("Bold", "bold", "editor.chain().focus().toggleBold().run();"),
        ToolBarButton("Italic", "italic", "editor.chain().focus().toggleItalic().run();"),
        ToolBarButton("Underline", "underline", "editor.chain().focus().toggleUnderline().run();"),
        ToolBarButton("Strike", "strikethrough", "editor.chain().focus().toggleStrike().run();"),
        ToolBarButton("Highlight", "highlight", "editor.chain().focus().toggleHighlight().run();"),
        ToolBarButton("Code", "code", "editor.chain().focus().toggleCode().run();"),

        ToolBarButton("Bullet", "bulletList", "editor.chain().focus().toggleBulletList().run();"),
        ToolBarButton("Number", "orderedList", "editor.chain().focus().toggleOrderedList().run();"),
        ToolBarButton("Quote", "blockquote", "editor.chain().focus().toggleBlockquote().run();"),
        ToolBarButton("Line", "horizontalRule", "editor.chain().focus().setHorizontalRule().run();"),

        ToolBarButton("Left", "alignLeft", "editor.chain().focus().setTextAlign('left').run();"),
        ToolBarButton("Center", "alignCenter", "editor.chain().focus().setTextAlign('center').run();"),
        ToolBarButton("Right", "alignRight", "editor.chain().focus().setTextAlign('right').run();"),
        ToolBarButton("Justify", "alignJustify", "editor.chain().focus().setTextAlign('justify').run();"),

        ToolBarButton("Image", "image", "insertImage()"), // Tuỳ bạn định nghĩa insertImage() trong JS
        ToolBarButton("Table", "table", "editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run();"),
        ToolBarButton("Task", "taskList", "editor.chain().focus().toggleTaskList().run();"),
        ToolBarButton("CodeBlk", "codeBlock", "editor.chain().focus().toggleCodeBlock().run();"),
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color.White),
        contentPadding = PaddingValues(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(buttons) { btn ->
            val state = buttonMap[btn.key]
            val isActive = state?.isActive == true
            val isEnabled = state?.isEnabled == true

            val background = when {
                !isEnabled -> Color.LightGray
                isActive -> Color.Black
                else -> Color.White
            }

            val border = when {
                !isEnabled -> Color.LightGray
                isActive -> Color.Black
                else -> Color.Black
            }

            val contentColor = when {
                !isEnabled -> Color.Gray
                isActive -> Color.White
                else -> Color.Black
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .border(0.5.dp, border, MaterialTheme.shapes.small)
                    .background(background, MaterialTheme.shapes.small)
                    .clickable(enabled = isEnabled) {
                        webView.evaluateJavascript(btn.jsCommand, null)
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(btn.text, color = contentColor, fontSize = 12.sp)
            }
        }
    }
}

data class ToolBarButton(
    val text: String,
    val key: String,
    val jsCommand: String
)

data class ButtonState(
    val isActive: Boolean = false,
    val isEnabled: Boolean = false
)
