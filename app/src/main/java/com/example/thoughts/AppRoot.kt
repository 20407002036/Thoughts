package com.example.thoughts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.thoughts.ui.events.UiAction
import com.example.thoughts.ui.events.UiEvent
import com.example.thoughts.ui.popup.LocalPopupController
import com.example.thoughts.ui.popup.PopupController
import com.example.thoughts.ui.popup.PopupHost
import com.example.thoughts.ui.popup.PopupKind
import com.example.thoughts.ui.popup.ToastPopup
import com.example.thoughts.ui.popup.ModalPopup
import com.example.thoughts.ui.popup.PopupButton
import com.example.thoughts.ui.theme.ThoughtsTheme
import kotlinx.coroutines.launch

@Composable
fun AppRoot() {
    val themeViewModel: ThemeViewModel = viewModel()
    val themeMode by themeViewModel.themeMode.collectAsState()
    
    val session by AuthSessionManager.session.collectAsState()
    val authViewModel: AuthViewModel = viewModel()
    val journalViewModel: JournalViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val popupController = remember { PopupController() }

    CompositionLocalProvider(LocalPopupController provides popupController) {
        ThoughtsTheme(themeMode = themeMode) {
            Box(modifier = Modifier.fillMaxSize()) {
                LaunchedEffect(authViewModel) {
                authViewModel.uiEvents.collect { event ->
                    when (event) {
                        is UiEvent.Toast -> {
                            popupController.showToast(
                                ToastPopup(
                                    message = event.message,
                                    kind = event.kind,
                                )
                            )
                        }
                        is UiEvent.Modal -> {
                            val primary = PopupButton(label = event.primaryLabel) {
                                // Auth modals not used yet; just dismiss
                                popupController.dismissModal()
                            }
                            val secondary = event.secondaryLabel?.let { label ->
                                PopupButton(label = label) { popupController.dismissModal() }
                            }

                            popupController.showModal(
                                ModalPopup(
                                    kind = event.kind,
                                    title = event.title,
                                    message = event.message,
                                    primary = primary,
                                    secondary = secondary,
                                )
                            )
                        }
                    }
                }
            }

            LaunchedEffect(journalViewModel) {
                journalViewModel.uiEvents.collect { event ->
                    when (event) {
                        is UiEvent.Toast -> {
                            popupController.showToast(
                                ToastPopup(
                                    message = event.message,
                                    kind = event.kind,
                                )
                            )
                        }
                        is UiEvent.Modal -> {
                            val primary = PopupButton(label = event.primaryLabel) {
                                when (event.action) {
                                    UiAction.RetryUpload -> journalViewModel.retryUpload()
                                    null -> Unit
                                }
                                popupController.dismissModal()
                            }
                            val secondary = event.secondaryLabel?.let { label ->
                                PopupButton(label = label) { popupController.dismissModal() }
                            }

                            popupController.showModal(
                                ModalPopup(
                                    kind = event.kind,
                                    title = event.title,
                                    message = event.message,
                                    primary = primary,
                                    secondary = secondary,
                                )
                            )
                        }
                    }
                }
            }

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

            PopupHost(controller = popupController)
        }
    }
}
}