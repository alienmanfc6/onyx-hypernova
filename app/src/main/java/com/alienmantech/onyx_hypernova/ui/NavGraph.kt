package com.alienmantech.onyx_hypernova.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alienmantech.onyx_hypernova.ui.home.HomeScreen
import com.alienmantech.onyx_hypernova.ui.listdetail.ListDetailScreen

private object Routes {
    const val HOME = "home"
    const val LIST_DETAIL = "list/{listId}"
    fun listDetail(id: Long) = "list/$id"
}

@Composable
fun RankItNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                onListClick = { listId ->
                    navController.navigate(Routes.listDetail(listId))
                }
            )
        }

        composable(
            route = Routes.LIST_DETAIL,
            arguments = listOf(navArgument("listId") { type = NavType.LongType })
        ) {
            ListDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
