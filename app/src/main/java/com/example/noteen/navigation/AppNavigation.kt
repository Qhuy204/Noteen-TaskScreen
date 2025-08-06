@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.example.noteen.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.noteen.ui.screen.HomeScreen
import com.example.noteen.ui.screen.NoteScreen
import com.example.noteen.ui.screen.TaskScreen

object AppRoutes {
    const val HOME = "home"
    const val TASK = "task"
    const val NOTE = "note"
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = AppRoutes.HOME
        ) {
            composable(
                route = AppRoutes.HOME
                // Không thêm enter/exit transition để giữ HomeScreen cố định
            ) {
                HomeScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }
            composable(
                route = AppRoutes.TASK
                // Loại bỏ enter/exit transition vì sharedElement đã xử lý hiệu ứng
            ) {
                TaskScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }
            composable(
                route = "${AppRoutes.NOTE}/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.IntType }),
                enterTransition = {
                    scaleIn(
                        initialScale = 0.5f,
                        animationSpec = tween(
                            durationMillis = 1000,
                            easing = LinearOutSlowInEasing
                        )
                    ) + fadeIn(
                        animationSpec = tween(
                            durationMillis = 600,
                            easing = LinearOutSlowInEasing
                        )
                    )
                },
                exitTransition = {
                    scaleOut(
                        targetScale = 0.5f,
                        animationSpec = tween(
                            durationMillis = 1000,
                            easing = LinearOutSlowInEasing
                        )
                    ) + fadeOut(
                        animationSpec = tween(
                            durationMillis = 600,
                            easing = LinearOutSlowInEasing
                        )
                    )
                }
            ) {
                NoteScreen()
            }
        }
    }
}
