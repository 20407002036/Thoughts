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
    object Processing : Screen("processing")
    object EntryDetail : Screen("entry/{entryId}")
    object Profile : Screen("profile")
    object Insights : Screen("insights")
    object Settings : Screen("settings")
}

fun entryDetailRoute(entryId: String): String = Screen.EntryDetail.route.replace("{entryId}", entryId)

@Composable
fun ThoughtsNavHost(
    navController: NavHostController,
    journalViewModel: JournalViewModel,
    onLogout: () -> Unit,
    authViewModel: AuthViewModel? = null,
    startDestination: String = Screen.Dashboard.route,
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController, journalViewModel, onLogout)
        }
        composable(Screen.Record.route) {
            RecordScreen(navController, journalViewModel)
        }
        composable(Screen.Processing.route) {
            ProcessingScreen(navController, journalViewModel)
        }
        composable(Screen.Review.route) {
            ReviewScreen(navController, journalViewModel)
        }
        composable(Screen.EntryDetail.route) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
            EntryDetailScreen(navController, journalViewModel, entryId)
        }
        composable(Screen.Archives.route) {
            ArchivesScreen(navController, journalViewModel)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController, journalViewModel)
        }
        composable(Screen.Insights.route) {
            InsightsScreen(navController, journalViewModel)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController, journalViewModel, authViewModel ?: AuthViewModel())
        }
    }
}
