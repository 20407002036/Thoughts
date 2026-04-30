package com.example.thoughts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivesScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            MindfulTopAppBar(onLogout = {})
        },
        bottomBar = {
            MindfulBottomNavigation(navController)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                MindfulSearchSection()
            }
            item {
                MindfulFilterChips()
            }
            
            // Group: September 2023
            item {
                MindfulArchiveGroupHeader("September 2023", "12 entries")
            }
            items(septemberEntries) { entry ->
                MindfulArchiveEntryCard(entry)
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }

            // Group: August 2023
            item {
                MindfulArchiveGroupHeader("August 2023", "28 entries")
            }
            items(augustEntries) { entry ->
                MindfulArchiveEntryCard(entry)
            }
        }
    }
}

@Composable
fun MindfulSearchSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        OutlinedTextField(
            value = "",
            onValueChange = {},
            placeholder = { 
                Text(
                    "Search your reflections...", 
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                ) 
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
            singleLine = true
        )
    }
}

@Composable
fun MindfulFilterChips() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Active chip
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            shadowElevation = 4.dp
        ) {
            Text(
                "All Entries",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }

        // Inactive chip with icon
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = CircleShape,
            onClick = {}
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.SentimentSatisfied, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                    "Mood: Happy",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun MindfulArchiveGroupHeader(title: String, countText: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            countText.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = 10.sp
            ),
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
        )
    }
}

data class MindfulArchiveEntry(
    val title: String,
    val date: String,
    val summary: String,
    val tag: String,
    val tagIcon: ImageVector,
    val tagColor: Color = Color(0xFF7b5e53)
)

val septemberEntries = listOf(
    MindfulArchiveEntry(
        "The Architectural Balance of Routine",
        "Sept 14, 2023 • 08:45 AM",
        "Today I explored the intersection of structured workflows and creative freedom. Finding the stillness within the chaos of the city...",
        "CALM",
        Icons.Default.AutoAwesome
    ),
    MindfulArchiveEntry(
        "Navigating Technical Friction",
        "Sept 12, 2023 • 10:12 PM",
        "The project's architectural constraints are becoming clearer. It is not about the limitations, but how we dance within them...",
        "WORK",
        Icons.Default.Lightbulb,
        tagColor = Color(0xFF373831)
    )
)

val augustEntries = listOf(
    MindfulArchiveEntry(
        "Solitude vs Isolation",
        "Aug 29, 2023 • 06:20 AM",
        "Reflecting on the difference between being alone and feeling lonely. True solitude is the curator of the soul's gallery...",
        "REFLECTION",
        Icons.Default.Favorite,
        tagColor = Color(0xFF8D6E63)
    )
)

@Composable
fun MindfulArchiveEntryCard(entry: MindfulArchiveEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        entry.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        entry.date.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                    )
                }
                
                Surface(
                    color = entry.tagColor.copy(alpha = 0.05f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            entry.tagIcon,
                            contentDescription = null,
                            tint = entry.tagColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            entry.tag,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = entry.tagColor
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                entry.summary,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                maxLines = 2
            )
        }
    }
}
