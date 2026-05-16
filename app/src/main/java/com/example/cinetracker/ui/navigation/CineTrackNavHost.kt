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
        startDestination = Route.MyList.path,
        modifier = modifier,
    ) {
        composable(Route.Search.path) {
            SearchScreen(
                onItemClick = { mediaType, tmdbId ->
                    navController.navigate(Route.Detail(mediaType, tmdbId).path)
                },
            )
        }
        composable(Route.MyList.path) {
            MyListScreen(
                onItemClick = { mediaType, tmdbId ->
                    navController.navigate(Route.Detail(mediaType, tmdbId).path)
                },
            )
        }
        composable(Route.Stats.path) {
            StatsScreen()
        }
        composable(
            route = Route.Detail.PATH_TEMPLATE,
            arguments = listOf(
                navArgument(Route.Detail.ARG_MEDIA_TYPE) { type = NavType.StringType },
                navArgument(Route.Detail.ARG_TMDB_ID) { type = NavType.IntType },
            ),
        ) {
            DetailScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
