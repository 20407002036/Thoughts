package com.example.thoughts

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay

@Composable
fun ProcessingScreen(navController: NavHostController, journalViewModel: JournalViewModel) {
    val uploadState by journalViewModel.uploadState.collectAsState()
    val backendResult by journalViewModel.backendResult.collectAsState()

    // Local animated pseudo-progress while backend processes (until uploaded)
    var progress by remember { mutableStateOf(20f) }
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600, easing = LinearEasing)
    )

    LaunchedEffect(uploadState) {
        if (uploadState == AudioUploadState.Processing) {
            // advance fake progress until uploaded or failed
            while (progress < 98f && journalViewModel.uploadState.value == AudioUploadState.Processing) {
                delay(700)
                progress += (3..8).random()
            }
        }

        if (uploadState == AudioUploadState.Uploaded) {
            // navigate to review when processing completes
            navController.navigate(Screen.Review.route)
        }
    }

    Scaffold(
        topBar = { MindfulTopAppBar() },
        bottomBar = { MindfulBottomNavigation(navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Processing Audio…", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Taking a moment to weave your words into a digital sanctuary.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        CircularProgressIndicator(modifier = Modifier
                            .size(72.dp)
                            .padding(top = 18.dp), color = MaterialTheme.colorScheme.primary)

                        // Progress bar (simple)
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Box(modifier = Modifier
                                .fillMaxWidth(animated / 100f)
                                .height(12.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape))
                        }

                        Text("${animated.toInt()}%", modifier = Modifier.padding(top = 8.dp))

                        if (uploadState == AudioUploadState.Failed) {
                            Text("Upload failed. Tap Retry or Cancel.", color = MaterialTheme.colorScheme.error)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = { journalViewModel.retryUpload() }) { Text("Retry") }
                                Button(onClick = { navController.navigate(Screen.Record.route) }) { Text("Cancel") }
                            }
                        } else {
                            Button(onClick = { navController.navigate(Screen.Record.route) }, modifier = Modifier.padding(top = 8.dp)) {
                                Text("Cancel Transcription")
                            }
                        }
                    }
                }
            }
        }
    }
}
