package com.example.thoughts.ui.popup

import androidx.compose.runtime.staticCompositionLocalOf

val LocalPopupController = staticCompositionLocalOf<PopupController> {
    error("LocalPopupController not provided")
}
