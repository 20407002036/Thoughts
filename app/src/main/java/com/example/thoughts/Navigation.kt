package com.example.thoughts

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

sealed class Screen(val route: String) {
    object Login : Screen("auth_login")
    object SignUp : Screen("auth_signup")
    object Dashboard : Screen("dashboard")
    object Record : Screen("record")
    object Review : Screen("review")
    object Archives : Screen("archives")
}

@Composable
fun ThoughtsNavHost(
    navController: NavHostController,
    journalViewModel: JournalViewModel,
    onLogout: () -> Unit,
    startDestination: String = Screen.Dashboard.route,
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController, onLogout)
        }
        composable(Screen.Record.route) {
            RecordScreen(navController, journalViewModel)
        }
        composable(Screen.Review.route) {
            ReviewScreen(navController, journalViewModel)
        }
        composable(Screen.Archives.route) {
            ArchivesScreen(navController)
        }
    }
}
