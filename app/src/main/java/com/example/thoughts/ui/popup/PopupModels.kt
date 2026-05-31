package com.example.thoughts.ui.popup

enum class PopupKind {
    Neutral,
    Success,
    Error,
}

data class ToastPopup(
    val message: String,
    val kind: PopupKind = PopupKind.Neutral,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val durationMs: Long = 2500L,
)

data class PopupButton(
    val label: String,
    val onClick: () -> Unit,
)

data class ModalPopup(
    val kind: PopupKind = PopupKind.Neutral,
    val title: String,
    val message: String,
    val primary: PopupButton,
    val secondary: PopupButton? = null,
)

data class SelectionOption(
    val label: String,
    val onClick: () -> Unit,
    val isSelected: Boolean = false,
)

data class SelectionPopup(
    val title: String,
    val options: List<SelectionOption>,
)
