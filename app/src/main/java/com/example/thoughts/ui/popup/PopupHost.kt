package com.example.thoughts.ui.popup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@Composable
fun PopupHost(
    controller: PopupController,
    modifier: Modifier = Modifier,
    bottomClearance: Int = 96,
) {
    val toast by controller.toast.collectAsState()
    val modal by controller.modal.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        if (toast != null) {
            ToastView(
                toast = toast!!,
                onDismiss = controller::clearToast,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = bottomClearance.dp)
                    .navigationBarsPadding()
            )
        }

        if (modal != null) {
            ModalView(
                modal = modal!!,
                onDismiss = controller::dismissModal,
            )
        }
    }
}

@Composable
private fun ToastView(
    toast: ToastPopup,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(toast) {
        delay(toast.durationMs)
        onDismiss()
    }

    val scheme = MaterialTheme.colorScheme
    val (containerColor, contentColor, borderColor) = when (toast.kind) {
        PopupKind.Success -> Triple(
            scheme.primaryContainer,
            scheme.onPrimaryContainer,
            scheme.primary.copy(alpha = 0.2f),
        )
        PopupKind.Error -> Triple(
            scheme.errorContainer,
            scheme.onErrorContainer,
            scheme.error.copy(alpha = 0.2f),
        )
        PopupKind.Neutral -> Triple(
            scheme.surface,
            scheme.onSurface,
            scheme.outlineVariant.copy(alpha = 0.4f),
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        tonalElevation = 2.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .border(1.dp, borderColor, RoundedCornerShape(999.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                toast.message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
            )

            if (!toast.actionLabel.isNullOrBlank() && toast.onAction != null) {
                TextButton(
                    onClick = {
                        toast.onAction.invoke()
                        onDismiss()
                    },
                ) {
                    Text(
                        toast.actionLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModalView(
    modal: ModalPopup,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val (badgeBg, badgeFg, icon) = when (modal.kind) {
                        PopupKind.Error -> Triple(
                            MaterialTheme.colorScheme.errorContainer,
                            MaterialTheme.colorScheme.error,
                            Icons.Default.Error,
                        )
                        PopupKind.Success -> Triple(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primary,
                            Icons.Default.CheckCircle,
                        )
                        PopupKind.Neutral -> Triple(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                            Icons.Default.Error,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(badgeBg, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = badgeFg,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        modal.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        modal.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = modal.primary.onClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Text(modal.primary.label)
                    }

                    val secondary = modal.secondary
                    if (secondary != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = secondary.onClick) {
                            Text(
                                secondary.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 1.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onDismiss) {
                            Text(
                                "DISMISS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 1.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
