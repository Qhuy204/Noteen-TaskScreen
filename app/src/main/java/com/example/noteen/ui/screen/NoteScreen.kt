@file:Suppress("UNREACHABLE_CODE")

package com.example.noteen.ui.screen

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import com.example.noteen.R
import com.example.noteen.ui.component.ButtonState
import com.example.noteen.ui.component.PressEffectIconButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NoteScreen() {
    val context = LocalContext.current

    var buttonStatesJson by remember { mutableStateOf("{}") }
    LaunchedEffect(buttonStatesJson) {
        Log.d("EditorState", "buttonStatesJson: $buttonStatesJson")
    }

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                loadWithOverviewMode = true
                useWideViewPort = true
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun updateStateFromWeb(json: String) {
                        buttonStatesJson = json
                    }
                }, "AndroidBridge")
            }

            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(context))
                .build()

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ) = request?.url?.let { assetLoader.shouldInterceptRequest(it) }
            }

            loadUrl("https://appassets.androidplatform.net/index.html")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize().background(Color.White)
            .padding(WindowInsets.statusBars.asPaddingValues())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .imePadding()
        ) {
            AndroidView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                factory = { webView }
            )
            AnimatedToolBar(buttonStatesJson, webView)
        }
    }
}

data class ToolBarButton(
    val icon: Painter,
    val key: String,
    val jsCommand: String
)

@SuppressLint("RememberReturnType")
@Composable
fun AnimatedToolBar(
    jsonString: String,
    webView: WebView
) {
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var showRow by remember { mutableStateOf(false) }
    var showLazyRow by remember { mutableStateOf(true) }

    val rotation = remember { Animatable(0f) }
    val offsetX = remember { Animatable(0f) }

    val horizontalPadding = 16.dp
    val toolbarHeight = 56.dp
    val buttonSize = 30.dp

    val density = LocalDensity.current


    val gson = remember { Gson() }
    val buttonMap = remember(jsonString) {
        val type = object : TypeToken<Map<String, ButtonState>>() {}.type
        runCatching {
            gson.fromJson<Map<String, ButtonState>>(jsonString, type)
        }.getOrElse { emptyMap() }
    }
    val buttons = listOf(
        ToolBarButton(painterResource(R.drawable.ic_undo), "undo", "editor.chain().focus().undo().run();"),
        ToolBarButton(painterResource(R.drawable.ic_redo), "redo", "editor.chain().focus().redo().run();"),

        ToolBarButton(painterResource(R.drawable.ic_h1), "heading1", "editor.chain().focus().toggleHeading({ level: 1 }).run();"),
        ToolBarButton(painterResource(R.drawable.ic_h2), "heading2", "editor.chain().focus().toggleHeading({ level: 2 }).run();"),
        ToolBarButton(painterResource(R.drawable.ic_h1), "heading3", "editor.chain().focus().toggleHeading({ level: 3 }).run();"),

        ToolBarButton(painterResource(R.drawable.ic_bold), "bold", "editor.chain().focus().toggleBold().run();"),
        ToolBarButton(painterResource(R.drawable.ic_italic), "italic", "editor.chain().focus().toggleItalic().run();"),
        ToolBarButton(painterResource(R.drawable.ic_underline), "underline", "editor.chain().focus().toggleUnderline().run();"),
        ToolBarButton(painterResource(R.drawable.ic_strike), "strikethrough", "editor.chain().focus().toggleStrike().run();"),
        ToolBarButton(painterResource(R.drawable.ic_mark), "highlight", "editor.chain().focus().toggleHighlight().run();"),
        ToolBarButton(painterResource(R.drawable.ic_code), "code", "editor.chain().focus().toggleCode().run();"),

        ToolBarButton(painterResource(R.drawable.ic_bulletlist), "bulletList", "editor.chain().focus().toggleBulletList().run();"),
        ToolBarButton(painterResource(R.drawable.ic_numberlist), "orderedList", "editor.chain().focus().toggleOrderedList().run();"),
        ToolBarButton(painterResource(R.drawable.ic_quote), "blockquote", "editor.chain().focus().toggleBlockquote().run();"),
//        ToolBarButton(painterResource(R.drawable.ic_h1), "horizontalRule", "editor.chain().focus().setHorizontalRule().run();"),

        ToolBarButton(painterResource(R.drawable.ic_align_left), "alignLeft", "editor.chain().focus().setTextAlign('left').run();"),
        ToolBarButton(painterResource(R.drawable.ic_align_center), "alignCenter", "editor.chain().focus().setTextAlign('center').run();"),
        ToolBarButton(painterResource(R.drawable.ic_align_right), "alignRight", "editor.chain().focus().setTextAlign('right').run();"),
        ToolBarButton(painterResource(R.drawable.ic_align_justify), "alignJustify", "editor.chain().focus().setTextAlign('justify').run();"),

        ToolBarButton(painterResource(R.drawable.ic_image), "image", "insertImage()"), // Tuỳ bạn định nghĩa insertImage() trong JS
//        ToolBarButton("Table", "table", "editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run();"),
        ToolBarButton(painterResource(R.drawable.ic_tasklist), "taskList", "editor.chain().focus().toggleTaskList().run();"),
        ToolBarButton(painterResource(R.drawable.ic_codeblock), "codeBlock", "editor.chain().focus().toggleCodeBlock().run();"),
    )


    LaunchedEffect(expanded) {
        if (expanded) {
            showLazyRow = false
        } else {
            showLazyRow = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(toolbarHeight)
            .background(Color.White)
            .padding(horizontal = horizontalPadding)
    ) {
        val boxWidth = with(density) {
            LocalConfiguration.current.screenWidthDp.dp.toPx() -
                    (horizontalPadding * 2).toPx() - buttonSize.toPx()
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Floating button
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.value.toInt(), 0) }
                    .size(buttonSize)
                    .graphicsLayer {
                        rotationZ = rotation.value
                    }
                    .background(
                        color = if (!expanded) Color(0xFF1966FF) else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable(
                        enabled = !rotation.isRunning,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        expanded = !expanded
                        showRow = false // ẩn Row trước

                        scope.launch {
                            launch {
                                rotation.animateTo(
                                    targetValue = if (expanded) 540f else 0f,
                                    animationSpec = tween(700)
                                )
                            }

                            offsetX.animateTo(
                                targetValue = if (expanded) boxWidth else 0f,
                                animationSpec = tween(600)
                            )

                            showRow = expanded
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (!expanded) Icons.Default.Add else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (!expanded) Color.White else Color.Black
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Dãy nút sau khi expand
            AnimatedVisibility(
                visible = showRow,
                enter = scaleIn(tween(100)),
                exit = scaleOut()
            ) {
                Row(
                    modifier = Modifier.width(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(5) {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        AnimatedVisibility(
            visible = showLazyRow,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(500)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(300)
            ),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(toolbarHeight)
                .fillMaxWidth()
                .padding(start = buttonSize * 1.3f)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(toolbarHeight),
                horizontalArrangement = Arrangement.spacedBy(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(buttons) { btn ->
                    val state = buttonMap[btn.key]
                    val isActive = state?.isActive == true
                    val isEnabled = state?.isEnabled == true

                    PressEffectIconButton(
                        onClick = { webView.evaluateJavascript(btn.jsCommand, null) },
                        selected = isActive,
                        icon = btn.icon,
                        selectedIconColor = Color(0xFF1966FF),
                        unselectedIconColor = Color.Black,
                        iconPadding = 3.dp,
                        isEnabled = isEnabled
                    )
                }
            }
        }
    }
}
