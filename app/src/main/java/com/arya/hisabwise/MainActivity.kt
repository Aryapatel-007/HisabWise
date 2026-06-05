package com.arya.hisabwise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arya.hisabwise.data.UserPreferencesRepository
import com.arya.hisabwise.ui.create_hisab.CreateHisabScreen
import com.arya.hisabwise.ui.home.HomeScreen
import com.arya.hisabwise.ui.onboarding.OnboardingScreen
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.arya.hisabwise.ui.auth.AuthChoiceScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContent {
            val navController = rememberNavController()
            val isOnboardingDone by userPreferencesRepository.isOnboardingDone.collectAsState(initial = null)
            val authTypeOrNull by userPreferencesRepository.authTypeOrNull.collectAsState(initial = "LOADING")
            val isLoggedIn by userPreferencesRepository.isLoggedIn.collectAsState(initial = null)
            
            // Wait until data store initial values are loaded
            if (isOnboardingDone != null && authTypeOrNull != "LOADING" && isLoggedIn != null) {
                // SMART STARTUP ROUTING
                val startDest = when {
                    // First time opening or no explicit mode chosen yet
                    isOnboardingDone == false && authTypeOrNull == null -> "auth_choice"
                    // If user chose local mode
                    authTypeOrNull == "local" -> "home"
                    // If user chose global mode and is successfully logged in
                    authTypeOrNull == "global" && isLoggedIn == true -> "home"
                    // If user chose global mode but is NOT logged in (e.g. killed app midway)
                    authTypeOrNull == "global" && isLoggedIn == false -> "onboarding"
                    // Fallback
                    else -> "auth_choice"
                }

                NavHost(
                    navController = navController,
                    startDestination = startDest,
                    enterTransition = { slideInHorizontally(animationSpec = tween(300)) { fullWidth -> fullWidth } + fadeIn(animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(animationSpec = tween(300)) { fullWidth -> -fullWidth } + fadeOut(animationSpec = tween(300)) },
                    popEnterTransition = { slideInHorizontally(animationSpec = tween(300)) { fullWidth -> -fullWidth } + fadeIn(animationSpec = tween(300)) },
                    popExitTransition = { slideOutHorizontally(animationSpec = tween(300)) { fullWidth -> fullWidth } + fadeOut(animationSpec = tween(300)) }
                ) {
                    composable("auth_choice") {
                        AuthChoiceScreen(
                            onNavigateToHome = {
                                navController.navigate("home") {
                                    popUpTo("auth_choice") { inclusive = true }
                                }
                            },
                            onNavigateToOnboarding = {
                                navController.navigate("onboarding")
                            }
                        )
                    }
                    composable("onboarding") {
                        OnboardingScreen(
                            onGetStarted = {
                                navController.navigate("home") {
                                    popUpTo("auth_choice") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("home") {
                        HomeScreen(
                            onNavigateToAddHisab = {
                                navController.navigate("create_hisab")
                            },
                            onNavigateToHisabDetail = { id ->
                                navController.navigate("hisab_detail/$id")
                            },
                            onNavigateToAuth = { 
                                navController.navigate("auth_choice") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                            onNavigateToOnboarding = {
                                navController.navigate("onboarding")
                            }
                        )
                    }
                    composable(
                        route = "create_hisab?hisabId={hisabId}",
                        arguments = listOf(androidx.navigation.navArgument("hisabId") { 
                            type = androidx.navigation.NavType.StringType
                            nullable = true
                            defaultValue = null
                        })
                    ) {
                        CreateHisabScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onNavigateToHisabDetail = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable(
                        route = "hisab_detail/{hisabId}",
                        arguments = listOf(androidx.navigation.navArgument("hisabId") { 
                            type = androidx.navigation.NavType.IntType 
                        })
                    ) {
                        com.arya.hisabwise.ui.hisab_detail.HisabDetailScreen(
                            onNavigateToAddEntry = { hisabId ->
                                navController.navigate("all_entries/$hisabId")
                            },
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onNavigateToEditHisab = { hisabId ->
                                navController.navigate("create_hisab?hisabId=$hisabId")
                            }
                        )
                    }
                    composable(
                        route = "all_entries/{hisabId}",
                        arguments = listOf(androidx.navigation.navArgument("hisabId") { 
                            type = androidx.navigation.NavType.IntType 
                        })
                    ) {
                        com.arya.hisabwise.ui.all_entries.AllEntriesScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
