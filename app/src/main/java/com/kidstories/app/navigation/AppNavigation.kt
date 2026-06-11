package com.kidstories.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kidstories.app.repository.ProgressRepository
import com.kidstories.app.repository.StoryRepository
import com.kidstories.app.ui.detail.StoryDetailScreen
import com.kidstories.app.ui.home.HomeScreen
import com.kidstories.app.ui.listening.ListeningScreen
import com.kidstories.app.ui.reading.ReadingScreen
import com.kidstories.app.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Detail : Screen("detail/{storyId}") {
        fun createRoute(storyId: String) = "detail/$storyId"
    }
    object Reading : Screen("reading/{storyId}") {
        fun createRoute(storyId: String) = "reading/$storyId"
    }
    object Listening : Screen("listening/{storyId}") {
        fun createRoute(storyId: String) = "listening/$storyId"
    }
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation(storyRepository: StoryRepository, progressRepository: ProgressRepository) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                stories = storyRepository.loadStories(),
                onStoryClick = { navController.navigate(Screen.Detail.createRoute(it.id)) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(
            Screen.Detail.route,
            arguments = listOf(navArgument("storyId") { type = NavType.StringType })
        ) { backStack ->
            val storyId = backStack.arguments?.getString("storyId")!!
            val story = storyRepository.loadStories().first { it.id == storyId }
            StoryDetailScreen(
                story = story,
                onReadClick = { navController.navigate(Screen.Reading.createRoute(storyId)) },
                onListenClick = { navController.navigate(Screen.Listening.createRoute(storyId)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.Reading.route,
            arguments = listOf(navArgument("storyId") { type = NavType.StringType })
        ) { backStack ->
            val storyId = backStack.arguments?.getString("storyId")!!
            val story = storyRepository.loadStories().first { it.id == storyId }
            ReadingScreen(
                story = story,
                progressRepository = progressRepository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.Listening.route,
            arguments = listOf(navArgument("storyId") { type = NavType.StringType })
        ) { backStack ->
            val storyId = backStack.arguments?.getString("storyId")!!
            val story = storyRepository.loadStories().first { it.id == storyId }
            ListeningScreen(
                story = story,
                progressRepository = progressRepository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
