package com.example.cinetracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.StringRes
import com.example.cinetracker.R

sealed class Route(val path: String) {
    data object Search : Route("search")
    data object MyList : Route("mylist")
    data object Stats : Route("stats")
    data class Detail(val tmdbId: Int) : Route("detail/$tmdbId") {
        companion object {
            const val PATH_TEMPLATE = "detail/{tmdbId}"
            const val ARG_TMDB_ID = "tmdbId"
        }
    }
}

data class BottomNavItem(
    val route: Route,
    @param:StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val bottomNavItems: List<BottomNavItem> = listOf(
    BottomNavItem(
        route = Route.Search,
        labelRes = R.string.nav_search,
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search,
    ),
    BottomNavItem(
        route = Route.MyList,
        labelRes = R.string.nav_mylist,
        selectedIcon = Icons.Filled.Bookmarks,
        unselectedIcon = Icons.Outlined.Bookmarks,
    ),
    BottomNavItem(
        route = Route.Stats,
        labelRes = R.string.nav_stats,
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart,
    ),
)
