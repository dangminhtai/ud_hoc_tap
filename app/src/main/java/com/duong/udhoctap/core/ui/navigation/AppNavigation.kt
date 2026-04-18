package com.duong.udhoctap.core.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.duong.udhoctap.feature.deck.presentation.AddEditFlashcardScreen
import com.duong.udhoctap.feature.deck.presentation.DeckDetailScreen
import com.duong.udhoctap.feature.home.presentation.HomeScreen
import com.duong.udhoctap.feature.quiz.presentation.QuizScreen
import com.duong.udhoctap.feature.review.presentation.ReviewScreen
import com.duong.udhoctap.feature.stats.presentation.StatsScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Stats : Screen("stats")
    data object DeckDetail : Screen("deck/{deckId}") {
        fun createRoute(deckId: Long) = "deck/$deckId"
    }
    data object AddEditFlashcard : Screen("deck/{deckId}/flashcard?flashcardId={flashcardId}") {
        fun createRoute(deckId: Long, flashcardId: Long? = null) =
            "deck/$deckId/flashcard?flashcardId=${flashcardId ?: -1}"
    }
    data object Review : Screen("review/{deckId}") {
        fun createRoute(deckId: Long) = "review/$deckId"
    }
    data object Quiz : Screen("quiz/{deckId}") {
        fun createRoute(deckId: Long) = "quiz/$deckId"
    }
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Trang chủ", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Stats, "Thống kê", Icons.Filled.BarChart, Icons.Outlined.BarChart)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in listOf(Screen.Home.route, Screen.Stats.route)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(MaterialTheme.shapes.large),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 8.dp,
                    shadowElevation = 6.dp,
                    shape = MaterialTheme.shapes.large
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 0.dp
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == item.screen.route
                            } == true

                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = { Text(item.label) },
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally { it / 4 } },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally { -it / 4 } },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onDeckClick = { deckId ->
                        navController.navigate(Screen.DeckDetail.createRoute(deckId))
                    }
                )
            }

            composable(Screen.Stats.route) {
                StatsScreen()
            }

            composable(
                route = Screen.DeckDetail.route,
                arguments = listOf(navArgument("deckId") { type = NavType.LongType })
            ) {
                DeckDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onAddFlashcard = { deckId ->
                        navController.navigate(Screen.AddEditFlashcard.createRoute(deckId))
                    },
                    onEditFlashcard = { deckId, flashcardId ->
                        navController.navigate(Screen.AddEditFlashcard.createRoute(deckId, flashcardId))
                    },
                    onStartReview = { deckId ->
                        navController.navigate(Screen.Review.createRoute(deckId))
                    },
                    onStartQuiz = { deckId ->
                        navController.navigate(Screen.Quiz.createRoute(deckId))
                    }
                )
            }

            composable(
                route = Screen.AddEditFlashcard.route,
                arguments = listOf(
                    navArgument("deckId") { type = NavType.LongType },
                    navArgument("flashcardId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) {
                AddEditFlashcardScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Review.route,
                arguments = listOf(navArgument("deckId") { type = NavType.LongType })
            ) {
                ReviewScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Quiz.route,
                arguments = listOf(navArgument("deckId") { type = NavType.LongType })
            ) {
                QuizScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
