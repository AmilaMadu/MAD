package com.example.wordgame

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
// Add these imports for screen transitions:
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
// -----------------------------------------
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.wordgame.ui.GameScreen
import com.example.wordgame.ui.LeaderboardScreen
import com.example.wordgame.ui.OnboardingScreen
import com.example.wordgame.ui.theme.WordGameTheme

object AppScreen {
    const val ONBOARDING = "onboarding"
    const val GAME_PATTERN = "game/{playerName}"
    fun gameRoute(playerName: String) = "game/$playerName"
    const val LEADERBOARD = "leaderboard"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = getSharedPreferences("WordGamePrefs", Context.MODE_PRIVATE)
        val startDestination = AppScreen.ONBOARDING
        Log.d("MainActivity", "Forcing start destination to ONBOARDING")

        enableEdgeToEdge()
        setContent {
            WordGameTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding),
                        sharedPreferences = sharedPref
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    sharedPreferences: android.content.SharedPreferences
) {

    val animationDuration = 500
    val enterTransition = fadeIn(animationSpec = tween(animationDuration)) +
            slideInHorizontally(initialOffsetX = { it / 2 }, animationSpec = tween(animationDuration)) // Slide in from right
    val exitTransition = fadeOut(animationSpec = tween(animationDuration)) +
            slideOutHorizontally(targetOffsetX = { -it / 2 }, animationSpec = tween(animationDuration)) // Slide out to left
    val popEnterTransition = fadeIn(animationSpec = tween(animationDuration)) +
            slideInHorizontally(initialOffsetX = { -it / 2 }, animationSpec = tween(animationDuration)) // Slide in from left (when popping)
    val popExitTransition = fadeOut(animationSpec = tween(animationDuration)) +
            slideOutHorizontally(targetOffsetX = { it / 2 }, animationSpec = tween(animationDuration)) // Slide out to right (when popping)

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(
            route = AppScreen.ONBOARDING,
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition }
        ) {
            OnboardingScreen(
                onNameSaved = { name ->
                    with(sharedPreferences.edit()) {
                        putString("PLAYER_NAME", name)
                        apply()
                        Log.d("AppNavHost", "Name saved to SharedPreferences: $name")
                    }
                    navController.navigate(AppScreen.gameRoute(name)) {
                        popUpTo(AppScreen.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = AppScreen.GAME_PATTERN,
            arguments = listOf(navArgument("playerName") { type = NavType.StringType }),
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition }
        ) { backStackEntry ->
            val playerName = backStackEntry.arguments?.getString("playerName")
            if (playerName == null) {
                Log.e("AppNavHost", "PlayerName is null for GameScreen route. Navigating back to Onboarding.")
                navController.popBackStack(AppScreen.ONBOARDING, inclusive = false)
            } else {
                GameScreen(
                    playerName = playerName,
                    onNavigateToLeaderboard = {
                        navController.navigate(AppScreen.LEADERBOARD)
                    }
                )
            }
        }

        composable(
            route = AppScreen.LEADERBOARD,
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition }
        ) {
            LeaderboardScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

