package com.duong.udhoctap.core.ui.navigation
// Force recompile after AiHubScreen update

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
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
import com.duong.udhoctap.core.data.repository.DeckRepository
import com.duong.udhoctap.feature.aichat.presentation.AiChatScreen
import com.duong.udhoctap.feature.aihub.presentation.AiHubScreen
import com.duong.udhoctap.feature.aiquestion.presentation.AiQuestionScreen
import com.duong.udhoctap.feature.auth.presentation.LoginScreen
import com.duong.udhoctap.feature.auth.presentation.SignupScreen
import com.duong.udhoctap.feature.deck.presentation.AddEditFlashcardScreen
import com.duong.udhoctap.feature.deck.presentation.DeckDetailScreen
import com.duong.udhoctap.feature.draft.presentation.DraftReviewScreen
import com.duong.udhoctap.feature.home.presentation.HomeScreen
import com.duong.udhoctap.feature.knowledgebase.presentation.KnowledgeBaseScreen
import com.duong.udhoctap.feature.library.presentation.LibraryScreen
import com.duong.udhoctap.feature.quiz.presentation.QuizScreen
import com.duong.udhoctap.feature.review.presentation.ReviewScreen
import com.duong.udhoctap.feature.settings.presentation.SettingsScreen
import com.duong.udhoctap.feature.stats.presentation.StatsScreen
import com.duong.udhoctap.feature.weakspot.presentation.WeakSpotScreen
import com.duong.udhoctap.feature.search.presentation.SearchScreen
import com.duong.udhoctap.feature.settings.presentation.ReminderSettingsScreen

sealed class Screen(val route: String) {
    // ── Auth screens ───────────────────────────────────────────────────────────
    data object Login  : Screen("login")
    data object Signup : Screen("signup")

    // ── Bottom tabs ────────────────────────────────────────────────────────────
    data object Home     : Screen("home")
    data object AiHub    : Screen("ai_hub")
    data object Stats    : Screen("stats")
    data object Library  : Screen("library")
    data object Settings : Screen("settings")

    // ── Detail screens (bottom bar hidden) ────────────────────────────────────
    data object WeakSpot : Screen("weakspot")
    data object Search   : Screen("search")
    data object ReminderSettings : Screen("reminder_settings")

    // AI features
    data object AiChat     : Screen("ai_chat")
    data object AiQuestion : Screen("ai_question")

    // Library sub-screens
    data object KnowledgeBase : Screen("knowledge_base")

    // Deck management
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
    data object KbQuiz : Screen("quiz_kb?kbName={kbName}&topic={topic}&count={count}&difficulty={difficulty}") {
        fun createRoute(kbName: String, topic: String, count: Int = 5, difficulty: String = "medium") =
            "quiz_kb?kbName=${android.net.Uri.encode(kbName)}&topic=${android.net.Uri.encode(topic)}&count=$count&difficulty=$difficulty"
    }
    data object DraftReview : Screen("deck/{deckId}/drafts") {
        fun createRoute(deckId: Long) = "deck/$deckId/drafts"
    }
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home,     "Trang chủ", Icons.Filled.Home,         Icons.Outlined.Home),
    BottomNavItem(Screen.AiHub,    "AI Studio", Icons.Filled.AutoAwesome,  Icons.Outlined.AutoAwesome),
    BottomNavItem(Screen.Stats,    "Thống kê",  Icons.Filled.BarChart,     Icons.Outlined.BarChart),
    BottomNavItem(Screen.Library,  "Thư viện",  Icons.AutoMirrored.Filled.LibraryBooks, Icons.AutoMirrored.Outlined.LibraryBooks),
    BottomNavItem(Screen.Settings, "Cài đặt",   Icons.Filled.Settings,     Icons.Outlined.Settings)
)

private val bottomBarRoutes = bottomNavItems.map { it.screen.route }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    darkTheme: Boolean = false,
    onThemeChanged: (Boolean) -> Unit = {},
    deckRepository: DeckRepository
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in bottomBarRoutes

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
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
            // ── Auth screens ───────────────────────────────────────────────────
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
                    onNavigateToSignup = { navController.navigate(Screen.Signup.route) }
                )
            }

            composable(Screen.Signup.route) {
                SignupScreen(
                    onSignupSuccess = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Signup.route) { inclusive = true } } },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }

            // ── Bottom tabs ────────────────────────────────────────────────────
            composable(Screen.Home.route) {
                HomeScreen(
                    onDeckClick = { navController.navigate(Screen.DeckDetail.createRoute(it)) },
                    onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) }
                )
            }

            composable(Screen.AiHub.route) {
                AiHubScreen(
                    onNavigateToChat = { navController.navigate(Screen.AiChat.route) },
                    onNavigateToQuestion = { navController.navigate(Screen.AiQuestion.route) }
                )
            }

            composable(Screen.Library.route) {
                LibraryScreen(
                    onNavigateToKnowledgeBase = { navController.navigate(Screen.KnowledgeBase.route) }
                )
            }

            composable(Screen.Stats.route) {
                StatsScreen(onNavigateToWeakSpot = { navController.navigate(Screen.WeakSpot.route) })
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onThemeChanged = onThemeChanged,
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                    onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToReminderSettings = { navController.navigate(Screen.ReminderSettings.route) }
                )
            }

            // ── Stats (no bottom bar) ──────────────────────────────────────────

            composable(Screen.WeakSpot.route) {
                WeakSpotScreen(onNavigateBack = { navController.popBackStack() })
            }

            // ── AI Features ───────────────────────────────────────────────────
            composable(Screen.AiChat.route) {
                AiChatScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.AiQuestion.route) {
                AiQuestionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    deckRepository = deckRepository
                )
            }

            // ── Library sub-screens ───────────────────────────────────────────

            composable(Screen.KnowledgeBase.route) {
                KnowledgeBaseScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onStartKbQuiz = { kbName, topic, count, difficulty ->
                        navController.navigate(Screen.KbQuiz.createRoute(kbName, topic, count, difficulty))
                    }
                )
            }

            composable(
                route = Screen.KbQuiz.route,
                arguments = listOf(
                    navArgument("kbName") { type = NavType.StringType },
                    navArgument("topic") { type = NavType.StringType },
                    navArgument("count") { type = NavType.IntType; defaultValue = 5 },
                    navArgument("difficulty") { type = NavType.StringType; defaultValue = "medium" }
                )
            ) {
                QuizScreen(onNavigateBack = { navController.popBackStack() })
            }

            // ── Deck management ───────────────────────────────────────────────
            composable(
                route = Screen.DeckDetail.route,
                arguments = listOf(navArgument("deckId") { type = NavType.LongType })
            ) {
                DeckDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onAddFlashcard = { navController.navigate(Screen.AddEditFlashcard.createRoute(it)) },
                    onEditFlashcard = { deckId, fcId -> navController.navigate(Screen.AddEditFlashcard.createRoute(deckId, fcId)) },
                    onStartReview = { navController.navigate(Screen.Review.createRoute(it)) },
                    onStartQuiz = { navController.navigate(Screen.Quiz.createRoute(it)) },
                    onReviewDrafts = { navController.navigate(Screen.DraftReview.createRoute(it)) }
                )
            }

            composable(
                route = Screen.DraftReview.route,
                arguments = listOf(navArgument("deckId") { type = NavType.LongType })
            ) {
                DraftReviewScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.AddEditFlashcard.route,
                arguments = listOf(
                    navArgument("deckId") { type = NavType.LongType },
                    navArgument("flashcardId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) {
                AddEditFlashcardScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.Review.route,
                arguments = listOf(navArgument("deckId") { type = NavType.LongType })
            ) {
                ReviewScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.Quiz.route,
                arguments = listOf(navArgument("deckId") { type = NavType.LongType })
            ) {
                QuizScreen(onNavigateBack = { navController.popBackStack() })
            }

            // ── Search screen ──────────────────────────────────────────────────
            composable(Screen.Search.route) {
                SearchScreen(onNavigateBack = { navController.popBackStack() })
            }

            // ── Reminder Settings screen ───────────────────────────────────────
            composable(Screen.ReminderSettings.route) {
                ReminderSettingsScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
