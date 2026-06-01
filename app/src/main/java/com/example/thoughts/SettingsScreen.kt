package com.example.thoughts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.thoughts.ui.theme.ThoughtsColors
import kotlinx.coroutines.launch

data class SettingsItem(
    val icon: ImageVector,
    val label: String,
    val hint: String = "",
    val action: (() -> Unit)? = null
)

data class SettingsSection(
    val title: String,
    val items: List<SettingsItem>
)

@Composable
fun SettingsScreen(navController: NavHostController, journalViewModel: JournalViewModel, authViewModel: AuthViewModel) {
    val scope = rememberCoroutineScope()
    val session by AuthSessionManager.session.collectAsState()
    val prefs by journalViewModel.userPreferences.collectAsState()
    
    LaunchedEffect(Unit) {
        journalViewModel.loadProfile()
        journalViewModel.loadPreferences()
    }

    val profile by journalViewModel.userProfile.collectAsState()
    
    val rawName = profile?.display_name_compat?.trim().orEmpty().ifBlank {
        session?.displayName?.trim().orEmpty() 
    }
    val displayName = rawName.ifBlank { "Connected account" }
    
    val email = profile?.email?.trim().orEmpty().ifBlank { 
        session?.email?.trim().orEmpty() 
    }.ifBlank { "Signed in" }
    
    val avatarLabel = displayName.firstOrNull { it.isLetter() }?.uppercaseChar()?.toString() ?: "?"

    val notificationsStatus = if (prefs?.notifications_enabled == true) "Enabled" else "Disabled"
    val appearanceMode = prefs?.appearance_mode?.replaceFirstChar { it.uppercase() } ?: "Auto"
    val audioQuality = prefs?.audio_quality?.replaceFirstChar { it.uppercase() } ?: "High"
    val language = prefs?.language?.uppercase() ?: "EN"
    val currentPrefs = prefs ?: PreferencesResponse()
    val nextAppearanceMode = when (currentPrefs.appearance_mode.lowercase()) {
        "light" -> "dark"
        "dark" -> "auto"
        else -> "light"
    }

    val settingsSections = listOf(
        SettingsSection(
            title = "Personal",
            items = listOf(
                SettingsItem(
                    Icons.Default.Notifications,
                    "Notifications",
                    notificationsStatus,
                    action = {
                        journalViewModel.savePreferences(
                            currentPrefs.copy(notifications_enabled = !currentPrefs.notifications_enabled)
                        )
                    }
                ),
                SettingsItem(
                    Icons.Default.Brightness4,
                    "Appearance",
                    appearanceMode,
                    action = {
                        journalViewModel.savePreferences(currentPrefs.copy(theme = nextAppearanceMode))
                    }
                ),
                SettingsItem(Icons.Default.Mic, "Audio Settings", "$audioQuality • $language")
            )
        ),
        SettingsSection(
            title = "Privacy",
            items = listOf(
                SettingsItem(Icons.Default.Lock, "End-to-end Encryption", "On"),
                SettingsItem(Icons.Default.Help, "Help & Support", ""),
                SettingsItem(
                    Icons.Default.ExitToApp,
                    "Sign Out",
                    "",
                    action = {
                        scope.launch {
                            authViewModel.logout()
                        }
                    }
                )
            )
        )
    )

    Scaffold(
        topBar = { MindfulTopAppBar() },
        bottomBar = { MindfulBottomNavigation(navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // User section (optional)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ThoughtsColors.Blush.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(56.dp)
                                .background(
                                    color = ThoughtsColors.Blush.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                avatarLabel,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ThoughtsColors.Blush
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                displayName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                email,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Settings sections
            settingsSections.forEach { section ->
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            section.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                section.items.forEachIndexed { index, item ->
                                    SettingsItemRow(
                                        item = item,
                                        divider = index < section.items.size - 1,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { item.action?.invoke() }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(120.dp)) }
        }
    }
}

@Composable
fun SettingsItemRow(
    item: SettingsItem,
    divider: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .width(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.label,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.hint.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        item.hint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(18.dp)
            )
        }
        if (divider) {
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
