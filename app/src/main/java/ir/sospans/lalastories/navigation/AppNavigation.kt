package ir.sospans.lalastories.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.adivery.sdk.Adivery
import com.adivery.sdk.AdiveryListener
import ir.sospans.lalastories.repository.LullabyRepository
import ir.sospans.lalastories.repository.PoemRepository
import ir.sospans.lalastories.repository.ProgressRepository
import ir.sospans.lalastories.repository.StoryRepository
import ir.sospans.lalastories.ui.detail.StoryDetailScreen
import ir.sospans.lalastories.ui.home.HomeScreen
import ir.sospans.lalastories.ui.listening.ListeningScreen
import ir.sospans.lalastories.ui.lullabies.LullabiesScreen
import ir.sospans.lalastories.ui.lullaby.LullabyPlayerScreen
import ir.sospans.lalastories.ui.poems.PoemsScreen
import ir.sospans.lalastories.ui.reading.ReadingScreen
import ir.sospans.lalastories.ui.settings.SettingsScreen
import ir.sospans.lalastories.ui.stories.StoriesScreen

private const val INTERSTITIAL_PLACEMENT_ID = "e3d7931e-195b-4ee7-b621-e3b1dbd0a569"

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Stories : Screen("stories")
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
    object Poems : Screen("poems")
    object Lullabies : Screen("lullabies")
    object LullabyPlayer : Screen("lullabyPlayer/{lullabyId}") {
        fun createRoute(lullabyId: String) = "lullabyPlayer/$lullabyId"
    }
}

@Composable
fun AppNavigation(
    storyRepository: StoryRepository,
    poemRepository: PoemRepository,
    lullabyRepository: LullabyRepository,
    progressRepository: ProgressRepository
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onStoriesClick = { navController.navigate(Screen.Stories.route) },
                onPoemsClick = { navController.navigate(Screen.Poems.route) },
                onLullabiesClick = { navController.navigate(Screen.Lullabies.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Stories.route) {
            LaunchedEffect(Unit) {
                Adivery.prepareInterstitialAd(context, INTERSTITIAL_PLACEMENT_ID)
            }
            StoriesScreen(
                stories = storyRepository.loadStories(),
                onStoryClick = { story ->
                    if (Adivery.isLoaded(INTERSTITIAL_PLACEMENT_ID)) {
                        Adivery.addPlacementListener(INTERSTITIAL_PLACEMENT_ID, object : AdiveryListener() {
                            override fun onInterstitialAdClosed(placementId: String) {
                                Adivery.removePlacementListener(INTERSTITIAL_PLACEMENT_ID)
                                navController.navigate(Screen.Detail.createRoute(story.id))
                            }
                        })
                        Adivery.showAd(INTERSTITIAL_PLACEMENT_ID)
                    } else {
                        navController.navigate(Screen.Detail.createRoute(story.id))
                    }
                },
                onBack = { navController.popBackStack() }
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
        composable(Screen.Poems.route) {
            PoemsScreen(
                poemRepository = poemRepository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Lullabies.route) {
            LaunchedEffect(Unit) {
                Adivery.prepareInterstitialAd(context, INTERSTITIAL_PLACEMENT_ID)
            }
            LullabiesScreen(
                lullabies = lullabyRepository.loadLullabies(),
                onLullabyClick = { lullaby ->
                    if (Adivery.isLoaded(INTERSTITIAL_PLACEMENT_ID)) {
                        Adivery.addPlacementListener(INTERSTITIAL_PLACEMENT_ID, object : AdiveryListener() {
                            override fun onInterstitialAdClosed(placementId: String) {
                                Adivery.removePlacementListener(INTERSTITIAL_PLACEMENT_ID)
                                navController.navigate(Screen.LullabyPlayer.createRoute(lullaby.id))
                            }
                        })
                        Adivery.showAd(INTERSTITIAL_PLACEMENT_ID)
                    } else {
                        navController.navigate(Screen.LullabyPlayer.createRoute(lullaby.id))
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.LullabyPlayer.route,
            arguments = listOf(navArgument("lullabyId") { type = NavType.StringType })
        ) { backStack ->
            val lullabyId = backStack.arguments?.getString("lullabyId")!!
            LullabyPlayerScreen(
                lullabies = lullabyRepository.loadLullabies(),
                startLullabyId = lullabyId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
