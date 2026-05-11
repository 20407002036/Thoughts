package com.example.thoughts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

@Composable
fun AppRoot() {
    val session by AuthSessionManager.session.collectAsState()
    val authViewModel: AuthViewModel = viewModel()
    val journalViewModel: JournalViewModel = viewModel()
    val scope = rememberCoroutineScope()

    if (session == null) {
        val navController = rememberNavController()
        AuthNavHost(navController = navController, authViewModel = authViewModel)
    } else {
        val navController = rememberNavController()
        ThoughtsNavHost(
            navController = navController,
            journalViewModel = journalViewModel,
            authViewModel = authViewModel,
            onLogout = {
                scope.launch {
                    authViewModel.logout()
                }
            },
        )
    }
}
