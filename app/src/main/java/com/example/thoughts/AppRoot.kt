package com.example.thoughts

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch

@Composable
fun AppRoot() {
    val themeViewModel: ThemeViewModel = viewModel()
    val themeMode by themeViewModel.themeMode.collectAsState()

    val journalViewModel: JournalViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val popupController = remember { PopupController() }

    var isUnlocked by remember { mutableStateOf(false) }
    val context = LocalContext.current

    CompositionLocalProvider(LocalPopupController provides popupController) {
        ThoughtsTheme(themeMode = themeMode) {
            Box(modifier = Modifier.fillMaxSize()) {
                LaunchedEffect(Unit) {
                    val activity = context as? FragmentActivity
                    if (activity != null) {
                        AppBiometricManager.authenticate(
                            activity = activity,
                            onSuccess = { isUnlocked = true },
                            onError = { error ->
                                popupController.showToast(
                                    ToastPopup(
                                        message = "Biometric lock: $error",
                                        kind = PopupKind.Error,
                                    )
                                )
                            }
                        )
                    } else {
                        // Fallback if activity is not FragmentActivity
                        isUnlocked = true
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
if (isUnlocked) {
    val navController = rememberNavController()
    ThoughtsNavHost(
        navController = navController,
        journalViewModel = journalViewModel,
        onLogout = {
            // No-op in local-first mode
        },
    )
} else {

                    // Locked state: Show a simple locked overlay or nothing
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Could add a "Unlock" button here if biometric failed
                    }
                }

                PopupHost(controller = popupController)
            }
        }
    }
}
