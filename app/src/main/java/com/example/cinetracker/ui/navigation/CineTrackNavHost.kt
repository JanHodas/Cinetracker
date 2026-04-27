package com.example.cinetracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.cinetracker.ui.screens.detail.DetailScreen
import com.example.cinetracker.ui.screens.mylist.MyListScreen
import com.example.cinetracker.ui.screens.search.SearchScreen
import com.example.cinetracker.ui.screens.stats.StatsScreen

@Composable
fun CineTrackNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Route.Search.path,
        modifier = modifier,
    ) {
        composable(Route.Search.path) {
            SearchScreen(
                onMovieClick = { tmdbId ->
                    navController.navigate(Route.Detail(tmdbId).path)
                },
            )
        }
        composable(Route.MyList.path) {
            MyListScreen(
                onMovieClick = { tmdbId ->
                    navController.navigate(Route.Detail(tmdbId).path)
                },
            )
        }
        composable(Route.Stats.path) {
            StatsScreen()
        }
        composable(
            route = Route.Detail.PATH_TEMPLATE,
            arguments = listOf(
                navArgument(Route.Detail.ARG_TMDB_ID) { type = NavType.IntType }
            ),
        ) { backStackEntry ->
            val tmdbId = backStackEntry.arguments?.getInt(Route.Detail.ARG_TMDB_ID) ?: 0
            DetailScreen(
                tmdbId = tmdbId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
