package com.tom.fourhourbody.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tom.fourhourbody.data.entity.StretchRoutine
import com.tom.fourhourbody.data.reference.ReferenceDoc
import com.tom.fourhourbody.ui.cold.ColdScreen
import com.tom.fourhourbody.ui.creatine.CreatineScreen
import com.tom.fourhourbody.ui.dashboard.DashboardScreen
import com.tom.fourhourbody.ui.deck.DeckScreen
import com.tom.fourhourbody.ui.more.MoreScreen
import com.tom.fourhourbody.ui.nav.Routes
import com.tom.fourhourbody.ui.nutrition.NutritionScreen
import com.tom.fourhourbody.ui.nutrition.ReferenceScreen
import com.tom.fourhourbody.ui.progress.ProgressScreen
import com.tom.fourhourbody.ui.settings.SettingsScreen
import com.tom.fourhourbody.ui.sleep.SleepScreen
import com.tom.fourhourbody.ui.stretches.StretchConfigScreen
import com.tom.fourhourbody.ui.stretches.StretchHubScreen
import com.tom.fourhourbody.ui.stretches.StretchRunnerScreen
import com.tom.fourhourbody.ui.training.ExerciseConfigScreen
import com.tom.fourhourbody.ui.training.SessionHistoryScreen
import com.tom.fourhourbody.ui.training.SessionScreen
import com.tom.fourhourbody.ui.training.TrainingHomeScreen

private data class BottomItem(val route: String, val label: String, val icon: ImageVector)

private val bottomItems = listOf(
    BottomItem(Routes.TODAY, "Today", Icons.Filled.Home),
    BottomItem(Routes.TRAINING, "Train", Icons.Filled.PlayArrow),
    BottomItem(Routes.DECK, "Deck", Icons.Filled.Star),
    BottomItem(Routes.MORE, "More", Icons.Filled.MoreVert)
)

/** Full-screen flows: a timer running under a bottom bar invites a mis-tap. */
private val fullScreenRoutes = setOf(Routes.SESSION, Routes.MOBILITY, Routes.DESK_RESET)

@Composable
fun AppRoot(pendingRoute: String?, onRouteConsumed: () -> Unit) {
    val navController = rememberNavController()

    LaunchedEffect(pendingRoute) {
        if (pendingRoute != null) {
            navController.navigate(pendingRoute) { launchSingleTop = true }
            onRouteConsumed()
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route?.substringBefore('?')
    val showBottomBar = currentRoute !in fullScreenRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = { navController.navigateTab(item.route) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            // Five items on a narrow phone wrapped "Stretch" onto two lines.
                            label = {
                                Text(
                                    item.label,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Visible,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TODAY,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.TODAY) {
                DashboardScreen(
                    onOpenPillar = { route -> navController.navigate(route) }
                )
            }

            composable(Routes.TRAINING) {
                TrainingHomeScreen(
                    onStartSession = { navController.navigate(Routes.SESSION) },
                    onOpenHistory = { navController.navigate(Routes.SESSION_HISTORY) },
                    onOpenExercises = { navController.navigate(Routes.EXERCISE_CONFIG) },
                    onOpenDeck = { navController.navigate(Routes.DECK) }
                )
            }
            composable(Routes.SESSION) {
                SessionScreen(onExit = { navController.popBackStack() })
            }
            composable(Routes.SESSION_HISTORY) {
                SessionHistoryScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.EXERCISE_CONFIG) {
                ExerciseConfigScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.STRETCHES) {
                StretchHubScreen(
                    onOpenMobility = { navController.navigate(Routes.MOBILITY) },
                    onOpenDeskReset = { weekly -> navController.navigate(Routes.deskReset(weekly)) },
                    onOpenConfig = { navController.navigate(Routes.STRETCH_CONFIG) }
                )
            }
            composable(Routes.MOBILITY) {
                StretchRunnerScreen(
                    routine = StretchRoutine.REST_DAY_MOBILITY,
                    weekly = false,
                    onExit = { navController.popBackStack() }
                )
            }
            composable(
                route = "${Routes.DESK_RESET}?weekly={weekly}",
                arguments = listOf(
                    navArgument("weekly") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { entry ->
                StretchRunnerScreen(
                    routine = StretchRoutine.DESK_RESET,
                    weekly = entry.arguments?.getBoolean("weekly") ?: false,
                    onExit = { navController.popBackStack() }
                )
            }
            composable(Routes.STRETCH_CONFIG) {
                StretchConfigScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.NUTRITION) {
                NutritionScreen(
                    onOpenReference = { doc -> navController.navigate(Routes.reference(doc.name)) }
                )
            }
            composable(
                route = "${Routes.REFERENCE}/{doc}",
                arguments = listOf(navArgument("doc") { type = NavType.StringType })
            ) { entry ->
                val docName = entry.arguments?.getString("doc") ?: ReferenceDoc.SLOW_CARB_RULES.name
                val doc = runCatching { ReferenceDoc.valueOf(docName) }
                    .getOrDefault(ReferenceDoc.SLOW_CARB_RULES)
                ReferenceScreen(doc = doc, onBack = { navController.popBackStack() })
            }

            composable(Routes.SLEEP) { SleepScreen() }
            composable(Routes.COLD) { ColdScreen() }
            composable(Routes.CREATINE) { CreatineScreen() }
            composable(Routes.DECK) { DeckScreen() }
            composable(Routes.PROGRESS) { ProgressScreen() }

            composable(Routes.MORE) {
                MoreScreen(onOpen = { route -> navController.navigate(route) })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
