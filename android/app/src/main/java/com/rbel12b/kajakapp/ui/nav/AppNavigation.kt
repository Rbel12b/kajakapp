package com.rbel12b.kajakapp.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rbel12b.kajakapp.KajakApplication
import com.rbel12b.kajakapp.R
import com.rbel12b.kajakapp.ui.athletes.AthleteDetailScreen
import com.rbel12b.kajakapp.ui.athletes.AthleteDetailViewModel
import com.rbel12b.kajakapp.ui.athletes.AthletesScreen
import com.rbel12b.kajakapp.ui.athletes.AthletesViewModel
import com.rbel12b.kajakapp.ui.competitions.CompetitionDetailScreen
import com.rbel12b.kajakapp.ui.competitions.CompetitionDetailViewModel
import com.rbel12b.kajakapp.ui.competitions.CompetitionsScreen
import com.rbel12b.kajakapp.ui.competitions.CompetitionsViewModel
import com.rbel12b.kajakapp.ui.me.MeScreen
import com.rbel12b.kajakapp.ui.race.RaceDetailScreen
import com.rbel12b.kajakapp.ui.race.RaceDetailViewModel
import com.rbel12b.kajakapp.ui.settings.SettingsScreen
import com.rbel12b.kajakapp.ui.settings.SettingsViewModel
import com.rbel12b.kajakapp.ui.upcoming.UpcomingScreen
import com.rbel12b.kajakapp.ui.upcoming.UpcomingViewModel
import kotlinx.coroutines.launch

private sealed class BottomDest(val route: String, val label: String, val iconRes: Int) {
    data object Competitions : BottomDest("competitions", "Competitions", R.drawable.trophy)
    data object Upcoming : BottomDest("upcoming", "Upcoming", R.drawable.kayaking)
    data object Athletes : BottomDest("athletes", "Athletes", R.drawable.group)
    data object Me : BottomDest("me", "Me", R.drawable.person)
}

private val bottomDests = listOf(
    BottomDest.Competitions,
    BottomDest.Upcoming,
    BottomDest.Athletes,
    BottomDest.Me,
)

@Composable
fun AppNavigation(app: KajakApplication) {
    val navController = rememberNavController()
    val backstackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backstackEntry?.destination?.route

    val bottomRoutes = bottomDests.map { it.route }
    val showBottomBar = currentRoute in bottomRoutes

    val scope = rememberCoroutineScope()
    val favoriteIds by app.settingsRepository.favoriteIdsFlow.collectAsState(initial = emptySet())
    val onFavoriteToggle: (String) -> Unit = { id -> scope.launch { app.settingsRepository.toggleFavorite(id) } }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val hierarchy = backstackEntry?.destination?.hierarchy
                    bottomDests.forEach { dest ->
                        NavigationBarItem(
                            selected = hierarchy?.any { it.route == dest.route } == true,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(painterResource(dest.iconRes), contentDescription = dest.label)
                            },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomDest.Competitions.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(BottomDest.Competitions.route) {
                val vm: CompetitionsViewModel = viewModel(factory = CompetitionsViewModel.factory(app.kajakRepository))
                CompetitionsScreen(
                    vm = vm,
                    onCompetitionClick = { id, name, icon ->
                        navController.navigate("competition_detail/$id?name=${encode(name)}&icon=${encode(icon)}")
                    },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }

            composable(
                "competition_detail/{id}?name={name}&icon={icon}",
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType; defaultValue = "" },
                    navArgument("icon") { type = NavType.StringType; defaultValue = "" },
                )
            ) { entry ->
                val id = entry.arguments?.getString("id") ?: return@composable
                val name = entry.arguments?.getString("name") ?: ""
                val icon = entry.arguments?.getString("icon") ?: ""
                val vm: CompetitionDetailViewModel = viewModel(
                    key = id,
                    factory = CompetitionDetailViewModel.factory(app.kajakRepository, id)
                )
                CompetitionDetailScreen(
                    vm = vm,
                    competitionName = name,
                    competitionIcon = icon,
                    onBack = { navController.popBackStack() },
                    onRaceClick = { raceId, raceName ->
                        navController.navigate("race_detail/$id/$raceId?name=${encode(raceName)}")
                    }
                )
            }

            composable(
                "race_detail/{compId}/{raceId}?name={name}",
                arguments = listOf(
                    navArgument("compId") { type = NavType.StringType },
                    navArgument("raceId") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType; defaultValue = "" },
                )
            ) { entry ->
                val compId = entry.arguments?.getString("compId") ?: return@composable
                val raceId = entry.arguments?.getString("raceId") ?: return@composable
                val name = entry.arguments?.getString("name") ?: ""
                val vm: RaceDetailViewModel = viewModel(
                    key = "$compId/$raceId",
                    factory = RaceDetailViewModel.factory(app.kajakRepository, compId, raceId)
                )
                RaceDetailScreen(
                    vm = vm,
                    raceName = name,
                    onBack = { navController.popBackStack() },
                    onAthleteClick = { athleteId, athleteName ->
                        navController.navigate("athlete_detail/$athleteId?name=${encode(athleteName)}")
                    },
                    favoriteIds = favoriteIds,
                )
            }

            composable(BottomDest.Upcoming.route) {
                val vm: UpcomingViewModel = viewModel(factory = UpcomingViewModel.factory(app.kajakRepository))
                UpcomingScreen(
                    vm = vm,
                    onRaceClick = { compId, raceId, raceName ->
                        navController.navigate("race_detail/$compId/$raceId?name=${encode(raceName)}")
                    },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }

            composable(BottomDest.Athletes.route) {
                val vm: AthletesViewModel = viewModel(factory = AthletesViewModel.factory(app.kajakRepository, app.settingsRepository))
                AthletesScreen(
                    vm = vm,
                    onAthleteClick = { id, name ->
                        navController.navigate("athlete_detail/$id?name=${encode(name)}")
                    },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }

            composable(
                "athlete_detail/{id}?name={name}",
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType; defaultValue = "" },
                )
            ) { entry ->
                val id = entry.arguments?.getString("id") ?: return@composable
                val name = entry.arguments?.getString("name") ?: ""
                val vm: AthleteDetailViewModel = viewModel(
                    key = id,
                    factory = AthleteDetailViewModel.factory(app.kajakRepository, id)
                )
                AthleteDetailScreen(
                    vm = vm,
                    athleteName = name,
                    onBack = { navController.popBackStack() },
                    onRaceClick = { compId, raceId, raceName ->
                        navController.navigate("race_detail/$compId/$raceId?name=${encode(raceName)}")
                    },
                    favoriteIds = favoriteIds,
                    onFavoriteToggle = onFavoriteToggle,
                    onSetAsMe = { athleteId, athleteName ->
                        scope.launch {
                            app.settingsRepository.saveSelectedAthlete(athleteId, athleteName)
                        }
                        navController.navigate(BottomDest.Me.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(BottomDest.Me.route) {
                MeScreen(
                    settingsRepo = app.settingsRepository,
                    makeAthleteDetailVm = { athleteId ->
                        AthleteDetailViewModel(app.kajakRepository, athleteId)
                    },
                    onRaceClick = { compId, raceId, raceName ->
                        navController.navigate("race_detail/$compId/$raceId?name=${encode(raceName)}")
                    },
                    onClearMe = {
                        scope.launch { app.settingsRepository.clearSelectedAthlete() }
                    }
                )
            }

            composable("settings") {
                val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(app.settingsRepository))
                SettingsScreen(vm = vm, onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun encode(s: String): String =
    java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20")
