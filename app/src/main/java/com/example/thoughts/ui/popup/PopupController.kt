package com.example.thoughts.ui.popup

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PopupController {
    private val _toast = MutableStateFlow<ToastPopup?>(null)
    val toast: StateFlow<ToastPopup?> = _toast.asStateFlow()

    private val _modal = MutableStateFlow<ModalPopup?>(null)
    val modal: StateFlow<ModalPopup?> = _modal.asStateFlow()

    private val _selection = MutableStateFlow<SelectionPopup?>(null)
    val selection: StateFlow<SelectionPopup?> = _selection.asStateFlow()

    fun showToast(toast: ToastPopup) {
        _toast.value = toast
    }

    fun clearToast() {
        _toast.value = null
    }

    fun showModal(modal: ModalPopup) {
        _modal.value = modal
    }

    fun dismissModal() {
        _modal.value = null
    }

    fun showSelection(selection: SelectionPopup) {
        _selection.value = selection
    }

    fun dismissSelection() {
        _selection.value = null
    }
}
