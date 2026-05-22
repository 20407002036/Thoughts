package com.example.thoughts.ui.events

import com.example.thoughts.ui.popup.PopupKind

sealed interface UiAction {
    data object RetryUpload : UiAction
}

sealed interface UiEvent {
    data class Toast(
        val message: String,
        val kind: PopupKind = PopupKind.Neutral,
    ) : UiEvent

    data class Modal(
        val kind: PopupKind,
        val title: String,
        val message: String,
        val primaryLabel: String,
        val secondaryLabel: String? = null,
        val action: UiAction? = null,
    ) : UiEvent
}
