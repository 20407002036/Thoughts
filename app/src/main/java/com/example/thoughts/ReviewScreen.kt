package com.example.thoughts

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    navController: NavHostController,
    journalViewModel: JournalViewModel,
    playerViewModel: AudioPlayerViewModel = viewModel()
) {
    val draft = journalViewModel.currentDraft.collectAsState().value ?: createFallbackDraft()
    val uploadState = journalViewModel.uploadState.collectAsState().value
    val backendResult = journalViewModel.backendResult.collectAsState().value

    // Initialize player when draft is loaded and has audio
    val isExpired by playerViewModel.isExpired.collectAsState()
    LaunchedEffect(isExpired) {
        if (isExpired) {
            // In review screen, we might need to re-fetch the backend result to get a new URL
            // For now, if it expires here, we just retry the normal load or wait for a refresh
        }
    }

    LaunchedEffect(draft) {
        val audioAsset = draft.audioAsset
        val audioSource = audioAsset?.remoteUrl ?: audioAsset?.localPath
        if (audioSource != null) {
            playerViewModel.loadAudio(audioSource)
        }
    }

    Scaffold(
        topBar = {
            MindfulTopAppBar(onLogout = {})
        },
        bottomBar = {
            MindfulBottomNavigation(navController)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        // Show upload state overlay if still processing
        if (uploadState == AudioUploadState.Uploading || uploadState == AudioUploadState.Processing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (uploadState == AudioUploadState.Uploading) "Uploading..." else "Processing...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (uploadState == AudioUploadState.Uploading)
                            "Sending your recording to the backend"
                        else
                            "Transcribing and analyzing your recording",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        } else {
            // Show normal review content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Hero Header
                ReviewHeroHeader(draft, backendResult)

                Spacer(modifier = Modifier.height(32.dp))

                // Audio Player Section
                draft.audioAsset?.let {
                    AudioPlayerCard(playerViewModel)
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Transcription Block
                MindfulTranscriptionCard(draft, backendResult)

                Spacer(modifier = Modifier.height(32.dp))

                // Analysis Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        MindfulMoodAnalysisCard(draft.moodAnalysis, backendResult)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                MindfulThemesCard(draft.tags, backendResult)

                Spacer(modifier = Modifier.height(32.dp))

                // AI Summary Card
                AiSummaryCard(draft.takeaway, backendResult)

                Spacer(modifier = Modifier.height(32.dp))

                // Key Insights
                KeyInsightsSection(backendResult)

                Spacer(modifier = Modifier.height(120.dp))
            }
            
            // Floating Save Button at bottom or part of header as in design?
            // The design shows Edit and Export in the hero. Let's add a "Save" action to hero or top.
        }
    }
}

@Composable
fun ReviewHeroHeader(draft: JournalEntryDraft, backendResult: IngestionResponse?) {
    val title = backendResult?.analysis?.title?.takeIf { it.isNotBlank() }
        ?: draft.title?.takeIf { it.isNotBlank() }
        ?: "Journal Entry"

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = CircleShape
            ) {
                Text(
                    "ARCHIVES",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "OCT 24, 2023 • 08:14 AM",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            buildAnnotatedString {
                append(title)
            },
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                lineHeight = 52.sp,
                letterSpacing = (-1).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { /* Share */ },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export", color = MaterialTheme.colorScheme.onSurface)
            }
            Button(
                onClick = { /* Save/Edit logic */ },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Entry")
            }
        }
    }
}

@Composable
fun MindfulTranscriptionCard(draft: JournalEntryDraft, backendResult: IngestionResponse?) {
    val transcriptText = backendResult?.transcript?.takeIf { it.isNotBlank() }
        ?: draft.transcriptText
    val wordCount = transcriptText.split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .size

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(32.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        "Transcription",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = CircleShape
                ) {
                    Text(
                        "$wordCount WORDS",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                transcriptText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontStyle = FontStyle.Italic,
                    lineHeight = 28.sp,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun MindfulMoodAnalysisCard(moodAnalysis: MoodAnalysis?, backendResult: IngestionResponse?) {
    val analysisLabel = moodAnalysis?.label
        ?: backendResult?.analysis?.mood
        ?: ""
    val analysisExplanation = moodAnalysis?.explanation
        ?: backendResult?.analysis?.summary
        ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SentimentSatisfied, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                analysisLabel,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "MOOD ANALYSIS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                analysisExplanation,
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MindfulThemesCard(tags: List<JournalTag>, backendResult: IngestionResponse?) {
    val displayTags = backendResult?.analysis?.themes?.takeIf { it.isNotEmpty() }
        ?: tags.map { it.name }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "CONNECTED THEMES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            FlowRow(
                mainAxisSpacing = 8.dp,
                crossAxisSpacing = 8.dp,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                displayTags.forEach { tag ->
                    Surface(
                        color = Color.White,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                        shadowElevation = 1.dp
                    ) {
                        Text(
                            "#$tag",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AiSummaryCard(takeaway: String?, backendResult: IngestionResponse?) {
    val summary = backendResult?.analysis?.summary
        ?: takeaway.orEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(32.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "MINDFUL SUMMARY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                summary,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun KeyInsightsSection(backendResult: IngestionResponse?) {
    val insights = backendResult?.analysis?.insights.orEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(32.dp)) {
            Text(
                "KEY INSIGHTS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            if (insights.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    insights.forEachIndexed { index, insight ->
                        InsightItem(index + 1, insight)
                    }
                }
            }
        }
    }
}

@Composable
fun InsightItem(number: Int, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(content = content, modifier = modifier) { measurables, constraints ->
        val placeholders = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val layoutWidth = constraints.maxWidth
        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0

        placeholders.forEach { placeable ->
            if (currentRowWidth + placeable.width + mainAxisSpacing.toPx() > layoutWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentRowWidth = 0
            }
            currentRow.add(placeable)
            currentRowWidth += placeable.width + mainAxisSpacing.toPx().toInt()
        }
        if (currentRow.isNotEmpty()) rows.add(currentRow)

        val height = rows.sumOf { row -> row.maxOf { it.height } } + (rows.size - 1) * crossAxisSpacing.toPx().toInt()
        
        layout(layoutWidth, height) {
            var y = 0
            rows.forEach { row ->
                val rowHeight = row.maxOf { it.height }
                val rowWidth = row.sumOf { it.width } + (row.size - 1) * mainAxisSpacing.toPx().toInt()
                var x = when (horizontalArrangement) {
                    Arrangement.Center -> (layoutWidth - rowWidth) / 2
                    Arrangement.End -> layoutWidth - rowWidth
                    else -> 0
                }
                row.forEach { placeable ->
                    placeable.place(x, y)
                    x += placeable.width + mainAxisSpacing.toPx().toInt()
                }
                y += rowHeight + crossAxisSpacing.toPx().toInt()
            }
        }
    }
}


fun createFallbackDraft(): JournalEntryDraft {
    return JournalEntryDraft(
        id = "",
        recordingSessionId = "",
        title = "",
        transcriptText = "",
        updatedAtMillis = System.currentTimeMillis(),
    )
}

